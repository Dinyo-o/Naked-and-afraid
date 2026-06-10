package net.servercontrol.paper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class ServerControlProtocol {
    public static final String CHANNEL = "servercontrol:main";
    public static final int MAGIC = 0x5343544c;
    public static final int PROTOCOL_VERSION = 3;

    public static final int OP_HELLO = 1;
    public static final int OP_HELLO_ACK = 2;
    public static final int OP_STATE = 3;
    public static final int OP_CLIENT_READY = 4;
    public static final int OP_SOUND_META = 5;
    public static final int OP_SOUND_CHUNK = 6;
    public static final int OP_PLAY_CUSTOM_DEATH_SOUND = 7;

    private ServerControlProtocol() {
    }

    public static byte[] hello(long nonce, ControlState state, String releaseVersion) {
        return encode(OP_HELLO, nonce, state.disabledMask(), releaseVersion);
    }

    public static byte[] state(ControlState state) {
        return encode(OP_STATE, 0L, state.disabledMask(), "");
    }

    public static byte[] soundMeta(String serverId, String sha256, int byteCount, int chunkCount, int volumePercent) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(128);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeHeader(output, OP_SOUND_META);
                writeUtf8(output, serverId);
                writeUtf8(output, sha256);
                output.writeInt(byteCount);
                output.writeInt(chunkCount);
                output.writeInt(volumePercent);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode ServerControl sound metadata", exception);
        }
    }

    public static byte[] soundChunk(String sha256, int chunkIndex, byte[] chunk) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(chunk.length + 128);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeHeader(output, OP_SOUND_CHUNK);
                writeUtf8(output, sha256);
                output.writeInt(chunkIndex);
                output.writeInt(chunk.length);
                output.write(chunk);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode ServerControl sound chunk", exception);
        }
    }

    public static byte[] playCustomDeathSound(String sha256, int volumePercent) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(128);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeHeader(output, OP_PLAY_CUSTOM_DEATH_SOUND);
                writeUtf8(output, sha256);
                output.writeInt(volumePercent);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode ServerControl custom sound play packet", exception);
        }
    }

    public static Optional<Packet> decode(byte[] data) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            int magic = input.readInt();
            if (magic != MAGIC) {
                return Optional.empty();
            }

            int protocolVersion = input.readInt();
            if (protocolVersion != PROTOCOL_VERSION) {
                return Optional.empty();
            }

            int opcode = input.readUnsignedByte();
            return switch (opcode) {
                case OP_HELLO -> Optional.of(new Packet(opcode, input.readLong(), input.readInt(), readUtf8(input)));
                case OP_HELLO_ACK -> Optional.of(new Packet(opcode, input.readLong(), 0, readUtf8(input)));
                case OP_STATE -> Optional.of(new Packet(opcode, 0L, input.readInt(), ""));
                case OP_CLIENT_READY -> Optional.of(new Packet(opcode, 0L, 0, readUtf8(input)));
                case OP_SOUND_META, OP_SOUND_CHUNK, OP_PLAY_CUSTOM_DEATH_SOUND ->
                        Optional.of(new Packet(opcode, 0L, 0, ""));
                default -> Optional.empty();
            };
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static byte[] encode(int opcode, long nonce, int flags, String releaseVersion) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(16);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(PROTOCOL_VERSION);
                output.writeByte(opcode);

                switch (opcode) {
                    case OP_HELLO -> {
                        output.writeLong(nonce);
                        output.writeInt(flags);
                        writeUtf8(output, releaseVersion);
                    }
                    case OP_HELLO_ACK -> {
                        output.writeLong(nonce);
                        writeUtf8(output, releaseVersion);
                    }
                    case OP_STATE -> output.writeInt(flags);
                    case OP_CLIENT_READY -> writeUtf8(output, releaseVersion);
                    default -> throw new IllegalArgumentException("Unknown ServerControl opcode " + opcode);
                }
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode ServerControl packet", exception);
        }
    }

    private static void writeHeader(DataOutputStream output, int opcode) throws IOException {
        output.writeInt(MAGIC);
        output.writeInt(PROTOCOL_VERSION);
        output.writeByte(opcode);
    }

    private static void writeUtf8(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readUtf8(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > 1024) {
            throw new IOException("Invalid ServerControl string length " + length);
        }

        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record Packet(int opcode, long nonce, int flags, String releaseVersion) {
    }
}
