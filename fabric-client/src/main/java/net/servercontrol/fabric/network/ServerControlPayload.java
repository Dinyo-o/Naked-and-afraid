package net.servercontrol.fabric.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.servercontrol.fabric.ServerControlClient;

import java.nio.charset.StandardCharsets;

public record ServerControlPayload(
        int opcode,
        long nonce,
        int flags,
        String releaseVersion,
        String serverId,
        String sha256,
        int byteCount,
        int chunkIndex,
        int chunkCount,
        int volumePercent,
        byte[] data
) implements CustomPayload {
    public static final int MAGIC = 0x5343544c;
    public static final int PROTOCOL_VERSION = 3;
    private static final int MAX_SOUND_CHUNK_BYTES = 30000;

    public static final int OP_HELLO = 1;
    public static final int OP_HELLO_ACK = 2;
    public static final int OP_STATE = 3;
    public static final int OP_CLIENT_READY = 4;
    public static final int OP_SOUND_META = 5;
    public static final int OP_SOUND_CHUNK = 6;
    public static final int OP_PLAY_CUSTOM_DEATH_SOUND = 7;

    public static final CustomPayload.Id<ServerControlPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ServerControlClient.MOD_ID, "main"));

    public static final PacketCodec<PacketByteBuf, ServerControlPayload> CONFIG_CODEC =
            PacketCodec.ofStatic(ServerControlPayload::write, ServerControlPayload::read);

    public static final PacketCodec<RegistryByteBuf, ServerControlPayload> PLAY_CODEC =
            PacketCodec.ofStatic(ServerControlPayload::write, ServerControlPayload::read);

    public ServerControlPayload(int opcode, long nonce, int flags) {
        this(opcode, nonce, flags, "", "", "", 0, 0, 0, 100, new byte[0]);
    }

    public static ServerControlPayload helloAck(long nonce) {
        return new ServerControlPayload(OP_HELLO_ACK, nonce, 0, ServerControlClient.modVersion(),
                "", "", 0, 0, 0, 100, new byte[0]);
    }

    public static ServerControlPayload clientReady() {
        return new ServerControlPayload(OP_CLIENT_READY, 0L, 0, ServerControlClient.modVersion(),
                "", "", 0, 0, 0, 100, new byte[0]);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static <B extends PacketByteBuf> void write(B buf, ServerControlPayload payload) {
        buf.writeInt(MAGIC);
        buf.writeInt(PROTOCOL_VERSION);
        buf.writeByte(payload.opcode);

        switch (payload.opcode) {
            case OP_HELLO -> {
                buf.writeLong(payload.nonce);
                buf.writeInt(payload.flags);
                writeUtf8(buf, payload.releaseVersion);
            }
            case OP_HELLO_ACK -> {
                buf.writeLong(payload.nonce);
                writeUtf8(buf, payload.releaseVersion);
            }
            case OP_STATE -> buf.writeInt(payload.flags);
            case OP_CLIENT_READY -> writeUtf8(buf, payload.releaseVersion);
            case OP_SOUND_META -> {
                writeUtf8(buf, payload.serverId);
                writeUtf8(buf, payload.sha256);
                buf.writeInt(payload.byteCount);
                buf.writeInt(payload.chunkCount);
                buf.writeInt(payload.volumePercent);
            }
            case OP_SOUND_CHUNK -> {
                writeUtf8(buf, payload.sha256);
                buf.writeInt(payload.chunkIndex);
                buf.writeInt(payload.data.length);
                buf.writeBytes(payload.data);
            }
            case OP_PLAY_CUSTOM_DEATH_SOUND -> {
                writeUtf8(buf, payload.sha256);
                buf.writeInt(payload.volumePercent);
            }
            default -> throw new IllegalArgumentException("Unknown ServerControl opcode " + payload.opcode);
        }
    }

    private static <B extends PacketByteBuf> ServerControlPayload read(B buf) {
        int magic = buf.readInt();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Invalid ServerControl magic");
        }

        int protocolVersion = buf.readInt();
        if (protocolVersion != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported ServerControl protocol " + protocolVersion);
        }

        int opcode = buf.readUnsignedByte();
        return switch (opcode) {
            case OP_HELLO -> new ServerControlPayload(opcode, buf.readLong(), buf.readInt(), readUtf8(buf),
                    "", "", 0, 0, 0, 100, new byte[0]);
            case OP_HELLO_ACK -> new ServerControlPayload(opcode, buf.readLong(), 0, readUtf8(buf),
                    "", "", 0, 0, 0, 100, new byte[0]);
            case OP_STATE -> new ServerControlPayload(opcode, 0L, buf.readInt());
            case OP_CLIENT_READY -> new ServerControlPayload(opcode, 0L, 0, readUtf8(buf),
                    "", "", 0, 0, 0, 100, new byte[0]);
            case OP_SOUND_META -> {
                String serverId = readUtf8(buf);
                String sha256 = readUtf8(buf);
                int byteCount = buf.readInt();
                int chunkCount = buf.readInt();
                int volumePercent = buf.readInt();
                yield new ServerControlPayload(opcode, 0L, 0, "", serverId, sha256, byteCount, 0, chunkCount,
                        volumePercent, new byte[0]);
            }
            case OP_SOUND_CHUNK -> {
                String sha256 = readUtf8(buf);
                int chunkIndex = buf.readInt();
                int length = buf.readInt();
                if (length < 0 || length > MAX_SOUND_CHUNK_BYTES) {
                    throw new IllegalArgumentException("Invalid ServerControl sound chunk length " + length);
                }
                byte[] data = new byte[length];
                buf.readBytes(data);
                yield new ServerControlPayload(opcode, 0L, 0, "", "", sha256, 0, chunkIndex, 0, 100, data);
            }
            case OP_PLAY_CUSTOM_DEATH_SOUND -> {
                String sha256 = readUtf8(buf);
                int volumePercent = buf.readInt();
                yield new ServerControlPayload(opcode, 0L, 0, "", "", sha256, 0, 0, 0, volumePercent, new byte[0]);
            }
            default -> throw new IllegalArgumentException("Unknown ServerControl opcode " + opcode);
        };
    }

    private static <B extends PacketByteBuf> void writeUtf8(B buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static <B extends PacketByteBuf> String readUtf8(B buf) {
        int length = buf.readInt();
        if (length < 0 || length > 65536) {
            throw new IllegalArgumentException("Invalid ServerControl string length " + length);
        }

        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
