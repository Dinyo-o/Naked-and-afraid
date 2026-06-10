package net.servercontrol.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public final class CustomDeathSoundManager {
    private static final long WATCH_INTERVAL_TICKS = 100L;
    private static final int MAX_SOUND_BYTES = 20 * 1024 * 1024;

    private final ServerControlPlugin plugin;
    private BukkitTask watchTask;
    private CustomSound activeSound;
    private String serverId;
    private String lastUnsupportedHash = "";
    private String lastInvalidConfiguredSoundFile = "";

    public CustomDeathSoundManager(ServerControlPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        ensureServerId();
        ensureSoundFolder();
        refresh(false);
        watchTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> refresh(true), WATCH_INTERVAL_TICKS, WATCH_INTERVAL_TICKS);
    }

    public void stop() {
        if (watchTask != null) {
            watchTask.cancel();
        }
    }

    public void reload() {
        ensureServerId();
        ensureSoundFolder();
        refresh(true);
    }

    public void syncTo(Player player) {
        CustomSound sound = activeSound;
        if (sound == null) {
            return;
        }

        sendSound(player, sound);
    }

    public void syncAll() {
        if (activeSound == null || !customSoundEnabled()) {
            return;
        }

        syncActiveSoundToVerifiedPlayers();
    }

    public boolean playForVerifiedPlayers(int volumePercent) {
        CustomSound sound = activeSound;
        if (sound == null || !customSoundEnabled()) {
            return false;
        }

        byte[] payload = ServerControlProtocol.playCustomDeathSound(sound.sha256, volumePercent);
        plugin.handshakeManager().forEachVerifiedPlayer(player -> player.sendPluginMessage(
                plugin,
                ServerControlProtocol.CHANNEL,
                payload
        ));
        return true;
    }

    public boolean reloadAndSync() {
        ensureServerId();
        ensureSoundFolder();
        if (!refresh(false)) {
            return false;
        }

        syncActiveSoundToVerifiedPlayers();
        return true;
    }

    public boolean hasActiveSound() {
        return activeSound != null;
    }

    public String configuredSoundFile() {
        return plugin.getConfig().getString("mechanics.custom-death-sound-file", "sounds/death.mp3");
    }

    private boolean refresh(boolean broadcastChanges) {
        try {
            CustomSound loaded = loadSound();
            if (sameSound(activeSound, loaded)) {
                return activeSound != null;
            }

            activeSound = loaded;
            if (activeSound == null) {
                if (broadcastChanges) {
                    plugin.getLogger().info("ServerControl custom death sound is not available; using vanilla sound.");
                }
                return false;
            }

            plugin.getLogger().info("Loaded ServerControl custom death sound " + activeSound.path.getFileName()
                    + " (" + activeSound.bytes.length + " bytes, " + activeSound.sha256 + ")");
            if (broadcastChanges) {
                syncAll();
            }
            return true;
        } catch (Exception exception) {
            activeSound = null;
            plugin.getLogger().warning("Failed to load ServerControl custom death sound: " + exception.getMessage());
            return false;
        }
    }

    private CustomSound loadSound() throws Exception {
        Path file = soundFile();
        if (!Files.isRegularFile(file)) {
            return null;
        }

        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length == 0) {
            return null;
        }
        if (bytes.length > MAX_SOUND_BYTES) {
            String unsupportedKey = "too-large:" + bytes.length + ":" + Files.getLastModifiedTime(file).toMillis();
            if (!unsupportedKey.equals(lastUnsupportedHash)) {
                plugin.getLogger().warning("Custom death sound " + file.getFileName()
                        + " is larger than the supported 20 MB client transfer limit; using vanilla death sound fallback.");
            }
            lastUnsupportedHash = unsupportedKey;
            return null;
        }

        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!isLikelyMp3(bytes)) {
            if (!sha256.equals(lastUnsupportedHash)) {
                plugin.getLogger().warning("Custom death sound " + file.getFileName()
                        + " is not an MP3 file. Convert MP4/AAC audio to MP3; using vanilla death sound fallback.");
            }
            lastUnsupportedHash = sha256;
            return null;
        }
        lastUnsupportedHash = "";

        int chunkSize = chunkSize();
        List<byte[]> chunks = new ArrayList<>();
        for (int offset = 0; offset < bytes.length; offset += chunkSize) {
            chunks.add(Arrays.copyOfRange(bytes, offset, Math.min(bytes.length, offset + chunkSize)));
        }
        return new CustomSound(file, sha256, bytes, chunks);
    }

    private void sendSound(Player player, CustomSound sound) {
        if (!player.isOnline()) {
            return;
        }

        int volumePercent = plugin.state().deathSoundVolumePercentRounded();
        player.sendPluginMessage(
                plugin,
                ServerControlProtocol.CHANNEL,
                ServerControlProtocol.soundMeta(serverId, sound.sha256, sound.bytes.length, sound.chunks.size(), volumePercent)
        );

        int chunksPerTick = chunksPerTick();
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index;

            @Override
            public void run() {
                if (!player.isOnline() || activeSound == null || !activeSound.sha256.equals(sound.sha256)) {
                    task[0].cancel();
                    return;
                }

                for (int sent = 0; sent < chunksPerTick && index < sound.chunks.size(); sent++, index++) {
                    player.sendPluginMessage(
                            plugin,
                            ServerControlProtocol.CHANNEL,
                            ServerControlProtocol.soundChunk(sound.sha256, index, sound.chunks.get(index))
                    );
                }

                if (index >= sound.chunks.size()) {
                    task[0].cancel();
                }
            }
        }, 1L, 1L);
    }

    private boolean customSoundEnabled() {
        return plugin.state().isCustomDeathSoundEnabled();
    }

    private void syncActiveSoundToVerifiedPlayers() {
        plugin.handshakeManager().forEachVerifiedPlayer(player -> {
            CustomSound sound = activeSound;
            if (sound != null) {
                sendSound(player, sound);
            }
        });
    }

    private Path soundFile() {
        String configured = plugin.getConfig().getString("mechanics.custom-death-sound-file", "sounds/death.mp3");
        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path resolved = dataFolder.resolve(configured).normalize();
        if (!resolved.startsWith(dataFolder)) {
            if (!configured.equals(lastInvalidConfiguredSoundFile)) {
                plugin.getLogger().warning("Custom death sound file must stay inside plugins/ServerControl; using sounds/death.mp3.");
            }
            lastInvalidConfiguredSoundFile = configured;
            return dataFolder.resolve("sounds/death.mp3").normalize();
        }
        lastInvalidConfiguredSoundFile = "";
        return resolved;
    }

    private void ensureSoundFolder() {
        try {
            Files.createDirectories(soundFile().getParent());
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create custom sound folder: " + exception.getMessage());
        }
    }

    private int chunkSize() {
        return Math.max(1024, Math.min(30000, plugin.getConfig().getInt("mechanics.custom-death-sound-chunk-bytes", 16000)));
    }

    private int chunksPerTick() {
        return Math.max(1, Math.min(20, plugin.getConfig().getInt("mechanics.custom-death-sound-chunks-per-tick", 4)));
    }

    private void ensureServerId() {
        String configured = plugin.getConfig().getString("client-assets.server-id", "");
        if (configured == null || configured.isBlank()) {
            configured = UUID.randomUUID().toString();
            plugin.getConfig().set("client-assets.server-id", configured);
            plugin.saveConfig();
        }
        serverId = configured;
    }

    private static boolean sameSound(CustomSound left, CustomSound right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.sha256.equals(right.sha256);
    }

    private static boolean isLikelyMp3(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') {
            return true;
        }

        for (int index = 0; index < Math.min(bytes.length - 1, 4096); index++) {
            int first = bytes[index] & 0xFF;
            int second = bytes[index + 1] & 0xFF;
            if (first == 0xFF && (second & 0xE0) == 0xE0) {
                return true;
            }
        }
        return false;
    }

    private record CustomSound(Path path, String sha256, byte[] bytes, List<byte[]> chunks) {
    }
}
