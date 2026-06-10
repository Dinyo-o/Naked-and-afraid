package net.servercontrol.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.servercontrol.fabric.ClientRestrictionState;
import net.servercontrol.fabric.ServerControlClient;
import net.servercontrol.fabric.audio.DeathSoundClient;

public final class ServerControlNetworking {
    private static int readyRetryTicks;

    private ServerControlNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.configurationS2C().register(ServerControlPayload.ID, ServerControlPayload.CONFIG_CODEC);
        PayloadTypeRegistry.configurationC2S().register(ServerControlPayload.ID, ServerControlPayload.CONFIG_CODEC);
        PayloadTypeRegistry.playS2C().register(ServerControlPayload.ID, ServerControlPayload.PLAY_CODEC);
        PayloadTypeRegistry.playC2S().register(ServerControlPayload.ID, ServerControlPayload.PLAY_CODEC);
    }

    public static void registerReceivers() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientRestrictionState.reset();
            readyRetryTicks = 100;
            sendClientReadyIfPossible();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            readyRetryTicks = 0;
            ClientRestrictionState.reset();
            DeathSoundClient.reset();
        });

        ClientConfigurationNetworking.registerGlobalReceiver(ServerControlPayload.ID, (payload, context) -> {
            if (payload.opcode() == ServerControlPayload.OP_HELLO) {
                if (!ServerControlClient.modVersion().equals(payload.releaseVersion())) {
                    ServerControlClient.LOGGER.warn(
                            "ServerControl server version {} does not match client version {}",
                            payload.releaseVersion(),
                            ServerControlClient.modVersion()
                    );
                    return;
                }
                ClientRestrictionState.applyFlags(payload.flags());
                context.responseSender().sendPacket(ServerControlPayload.helloAck(payload.nonce()));
                ServerControlClient.LOGGER.info("Acknowledged ServerControl handshake");
            } else if (payload.opcode() == ServerControlPayload.OP_STATE) {
                ClientRestrictionState.applyFlags(payload.flags());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ServerControlPayload.ID, (payload, context) -> {
            if (payload.opcode() == ServerControlPayload.OP_HELLO) {
                if (!ServerControlClient.modVersion().equals(payload.releaseVersion())) {
                    ServerControlClient.LOGGER.warn(
                            "ServerControl server version {} does not match client version {}",
                            payload.releaseVersion(),
                            ServerControlClient.modVersion()
                    );
                    return;
                }
                ClientRestrictionState.applyFlags(payload.flags());
                ClientPlayNetworking.send(ServerControlPayload.helloAck(payload.nonce()));
                readyRetryTicks = 0;
                ServerControlClient.LOGGER.info("Acknowledged ServerControl play handshake");
            } else if (payload.opcode() == ServerControlPayload.OP_STATE) {
                context.client().execute(() -> ClientRestrictionState.applyFlags(payload.flags()));
            } else if (payload.opcode() == ServerControlPayload.OP_SOUND_META) {
                DeathSoundClient.acceptMetadata(payload);
            } else if (payload.opcode() == ServerControlPayload.OP_SOUND_CHUNK) {
                DeathSoundClient.acceptChunk(payload);
            } else if (payload.opcode() == ServerControlPayload.OP_PLAY_CUSTOM_DEATH_SOUND) {
                DeathSoundClient.play(payload);
            }
        });
    }

    public static void tickClientHandshake() {
        if (readyRetryTicks <= 0) {
            return;
        }

        readyRetryTicks--;
        if (readyRetryTicks % 10 == 0) {
            sendClientReadyIfPossible();
        }
    }

    private static void sendClientReadyIfPossible() {
        if (!ClientPlayNetworking.canSend(ServerControlPayload.ID)) {
            return;
        }

        ClientPlayNetworking.send(ServerControlPayload.clientReady());
        ServerControlClient.LOGGER.info("Announced ServerControl client to server");
    }
}
