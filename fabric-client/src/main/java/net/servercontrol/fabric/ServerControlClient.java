package net.servercontrol.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.servercontrol.fabric.network.ServerControlNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerControlClient implements ClientModInitializer {
    public static final String MOD_ID = "servercontrol";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String FALLBACK_VERSION = "development";

    @Override
    public void onInitializeClient() {
        ServerControlNetworking.registerPayloads();
        ServerControlNetworking.registerReceivers();
        LOGGER.info("ServerControl client companion {} loaded", modVersion());

        ClientTickEvents.END_CLIENT_TICK.register(ServerControlClient::onEndClientTick);
    }

    public static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(FALLBACK_VERSION);
    }

    private static void onEndClientTick(MinecraftClient client) {
        ServerControlNetworking.tickClientHandshake();

        if (!ClientRestrictionState.isTabListBlocked() || client.options == null) {
            return;
        }

        consumePlayerListKey(client.options.playerListKey);
        closePlayerList(client);
    }

    public static void consumePlayerListKey(KeyBinding keyBinding) {
        while (keyBinding.wasPressed()) {
            // Drain queued presses so rebinding TAB cannot reopen the list later.
        }
        keyBinding.setPressed(false);
    }

    public static void closePlayerList(MinecraftClient client) {
        if (client.inGameHud != null) {
            client.inGameHud.getPlayerListHud().setVisible(false);
        }
    }
}
