package net.servercontrol.fabric.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.servercontrol.fabric.ServerControlClient;
import net.servercontrol.fabric.network.ServerControlPayload;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DeathSoundClient {
    private static final int MAX_SOUND_BYTES = 20 * 1024 * 1024;
    private static final int MAX_SOUND_CHUNKS = (MAX_SOUND_BYTES / 1024) + 1;
    private static final ExecutorService AUDIO_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ServerControl MP3 Player");
        thread.setDaemon(true);
        return thread;
    });

    private static Transfer transfer;
    private static Path activeSound;
    private static String activeHash = "";
    private static String pendingPlayHash = "";
    private static int pendingPlayVolumePercent = 100;

    private DeathSoundClient() {
    }

    public static synchronized void reset() {
        transfer = null;
        activeSound = null;
        activeHash = "";
        pendingPlayHash = "";
        pendingPlayVolumePercent = 100;
    }

    public static synchronized void acceptMetadata(ServerControlPayload payload) {
        if (payload.byteCount() <= 0
                || payload.byteCount() > MAX_SOUND_BYTES
                || payload.chunkCount() <= 0
                || payload.chunkCount() > MAX_SOUND_CHUNKS) {
            ServerControlClient.LOGGER.warn("Ignoring invalid ServerControl custom death sound metadata");
            return;
        }

        Path serverDirectory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("servercontrol")
                .resolve("sounds")
                .resolve(safePathPart(payload.serverId()));
        transfer = new Transfer(
                payload.sha256(),
                payload.byteCount(),
                payload.chunkCount(),
                new byte[payload.chunkCount()][],
                serverDirectory
        );

        Path existing = soundPath(serverDirectory, payload.sha256());
        if (isValidCachedSound(existing, payload.sha256())) {
            activeSound = existing;
            activeHash = payload.sha256();
            transfer = null;
            cleanupOtherSounds(serverDirectory, existing);
            ServerControlClient.LOGGER.info("ServerControl custom death sound already cached");
            if (Objects.equals(pendingPlayHash, activeHash)) {
                playFile(activeSound, pendingPlayVolumePercent);
                pendingPlayHash = "";
            }
            return;
        }

        if (Files.exists(existing)) {
            try {
                Files.deleteIfExists(existing);
            } catch (Exception exception) {
                ServerControlClient.LOGGER.warn("Failed to replace invalid ServerControl custom death sound cache", exception);
            }
        }

        ServerControlClient.LOGGER.info("Downloading ServerControl custom death sound {} bytes", payload.byteCount());
    }

    public static synchronized void acceptChunk(ServerControlPayload payload) {
        if (transfer == null || !Objects.equals(transfer.sha256, payload.sha256())) {
            return;
        }
        if (payload.chunkIndex() < 0 || payload.chunkIndex() >= transfer.chunkCount) {
            return;
        }
        if (transfer.chunks[payload.chunkIndex()] != null) {
            return;
        }

        transfer.chunks[payload.chunkIndex()] = payload.data();
        transfer.receivedChunks++;
        transfer.receivedBytes += payload.data().length;

        if (transfer.receivedChunks == transfer.chunkCount) {
            completeTransfer();
        }
    }

    public static synchronized void play(ServerControlPayload payload) {
        if (activeSound != null && Objects.equals(activeHash, payload.sha256()) && Files.isRegularFile(activeSound)) {
            playFile(activeSound, payload.volumePercent());
            return;
        }

        pendingPlayHash = payload.sha256();
        pendingPlayVolumePercent = payload.volumePercent();
        ServerControlClient.LOGGER.info("Queued ServerControl custom death sound until download completes");
    }

    private static void completeTransfer() {
        try {
            if (transfer.receivedBytes != transfer.byteCount) {
                ServerControlClient.LOGGER.warn("Discarding incomplete ServerControl custom death sound");
                transfer = null;
                return;
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream(transfer.byteCount);
            for (byte[] chunk : transfer.chunks) {
                bytes.write(chunk);
            }

            byte[] soundBytes = bytes.toByteArray();
            String actualHash = sha256(soundBytes);
            if (!Objects.equals(actualHash, transfer.sha256)) {
                ServerControlClient.LOGGER.warn("Discarding ServerControl custom death sound with hash mismatch");
                transfer = null;
                return;
            }

            Files.createDirectories(transfer.serverDirectory);
            Path target = soundPath(transfer.serverDirectory, transfer.sha256);
            Files.write(target, soundBytes);
            deleteOtherSounds(transfer.serverDirectory, target);
            activeSound = target;
            activeHash = transfer.sha256;
            ServerControlClient.LOGGER.info("Installed ServerControl custom death sound");

            if (Objects.equals(pendingPlayHash, activeHash)) {
                playFile(activeSound, pendingPlayVolumePercent);
                pendingPlayHash = "";
            }
        } catch (Exception exception) {
            ServerControlClient.LOGGER.warn("Failed to install ServerControl custom death sound", exception);
        } finally {
            transfer = null;
        }
    }

    private static void playFile(Path path, int volumePercent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && client.options.getSoundVolume(SoundCategory.MASTER) <= 0.0F) {
            ServerControlClient.LOGGER.info("Skipped ServerControl custom death sound because Master Volume is off");
            return;
        }

        double volume = Math.max(0.0D, volumePercent / 100.0D);
        AUDIO_EXECUTOR.execute(() -> decodeAndPlay(path, volume));
    }

    private static void decodeAndPlay(Path path, double volume) {
        SourceDataLine line = null;
        try (InputStream input = Files.newInputStream(path)) {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (line == null) {
                    AudioFormat format = new AudioFormat(
                            output.getSampleFrequency(),
                            16,
                            output.getChannelCount(),
                            true,
                            false
                    );
                    line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
                    line.open(format);
                    line.start();
                }

                byte[] pcm = toPcmBytes(output.getBuffer(), output.getBufferLength(), volume);
                line.write(pcm, 0, pcm.length);
                bitstream.closeFrame();
            }
        } catch (Exception exception) {
            ServerControlClient.LOGGER.warn(
                    "Failed to play ServerControl custom death sound. The bundled decoder supports MP3 audio; "
                            + "convert MP4/AAC files to MP3 before placing them on the server.",
                    exception
            );
        } finally {
            if (line != null) {
                line.drain();
                line.close();
            }
        }
    }

    private static byte[] toPcmBytes(short[] samples, int length, double volume) {
        byte[] bytes = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            int scaled = (int) Math.round(samples[i] * volume);
            if (scaled > Short.MAX_VALUE) {
                scaled = Short.MAX_VALUE;
            } else if (scaled < Short.MIN_VALUE) {
                scaled = Short.MIN_VALUE;
            }

            bytes[i * 2] = (byte) scaled;
            bytes[i * 2 + 1] = (byte) (scaled >>> 8);
        }
        return bytes;
    }

    private static void deleteOtherSounds(Path directory, Path keep) throws Exception {
        try (var paths = Files.list(directory)) {
            for (Path path : paths.toList()) {
                String fileName = path.getFileName().toString();
                if (Files.isRegularFile(path) && !path.equals(keep) && fileName.startsWith("death-")) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void cleanupOtherSounds(Path directory, Path keep) {
        try {
            deleteOtherSounds(directory, keep);
        } catch (Exception exception) {
            ServerControlClient.LOGGER.warn("Failed to clean old ServerControl custom death sounds", exception);
        }
    }

    private static Path soundPath(Path directory, String sha256) {
        return directory.resolve("death-" + safePathPart(sha256) + ".mp3");
    }

    private static boolean isValidCachedSound(Path path, String expectedHash) {
        try {
            return Files.isRegularFile(path) && Objects.equals(sha256(Files.readAllBytes(path)), expectedHash);
        } catch (Exception exception) {
            ServerControlClient.LOGGER.warn("Ignoring invalid ServerControl custom death sound cache", exception);
            return false;
        }
    }

    private static String safePathPart(String input) {
        return input.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static final class Transfer {
        private final String sha256;
        private final int byteCount;
        private final int chunkCount;
        private final byte[][] chunks;
        private final Path serverDirectory;
        private int receivedChunks;
        private int receivedBytes;

        private Transfer(
                String sha256,
                int byteCount,
                int chunkCount,
                byte[][] chunks,
                Path serverDirectory
        ) {
            this.sha256 = sha256;
            this.byteCount = byteCount;
            this.chunkCount = chunkCount;
            this.chunks = chunks;
            this.serverDirectory = serverDirectory;
        }
    }
}
