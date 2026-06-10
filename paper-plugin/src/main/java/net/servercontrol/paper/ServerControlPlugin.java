package net.servercontrol.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerControlPlugin extends JavaPlugin {
    private final ControlState state = new ControlState();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private HandshakeManager handshakeManager;
    private CustomDeathSoundManager customDeathSoundManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfigKeys();
        getConfig().options().copyDefaults(true);
        migrateConfigKeys();
        saveConfig();
        state.load(getConfig());
        state.write(getConfig());
        saveConfig();

        handshakeManager = new HandshakeManager(this, state);
        handshakeManager.register();
        customDeathSoundManager = new CustomDeathSoundManager(this);
        customDeathSoundManager.start();

        getServer().getPluginManager().registerEvents(new RestrictionListener(this, state), this);

        ServerControlCommand command = new ServerControlCommand(this, state);
        registerCommand("servercontrol", command);
    }

    @Override
    public void onDisable() {
        if (handshakeManager != null) {
            handshakeManager.unregister();
        }
        if (customDeathSoundManager != null) {
            customDeathSoundManager.stop();
        }
    }

    public ControlState state() {
        return state;
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    public Component configuredComponent(String path) {
        return miniMessage.deserialize(getConfig().getString(path, ""));
    }

    public boolean requireClientMod() {
        return true;
    }

    public String serverControlVersion() {
        return getPluginMeta().getVersion();
    }

    public long handshakeTimeoutMillis() {
        return Math.max(1000L, getConfig().getLong("handshake.timeout-millis", 5000L));
    }

    public HandshakeManager handshakeManager() {
        return handshakeManager;
    }

    public CustomDeathSoundManager customDeathSoundManager() {
        return customDeathSoundManager;
    }

    public void reloadServerControlConfig() {
        reloadConfig();
        migrateConfigKeys();
        getConfig().options().copyDefaults(true);
        migrateConfigKeys();
        saveConfig();
        state.load(getConfig());
        state.write(getConfig());
        saveConfig();
        customDeathSoundManager.reload();
        customDeathSoundManager.syncAll();
        handshakeManager.broadcastState();
    }

    public void saveStateAndBroadcast() {
        state.write(getConfig());
        saveConfig();
        customDeathSoundManager.syncAll();
        handshakeManager.broadcastState();
    }

    public void playDeathSoundNow() {
        if (!customDeathSoundManager.playForVerifiedPlayers(state.deathSoundVolumePercentRounded())) {
            playVanillaDeathSound();
        }
    }

    private void playVanillaDeathSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_IRON_GOLEM_DEATH,
                    SoundCategory.MASTER,
                    1.0F,
                    1.0F
            );
        }
    }

    private void registerCommand(String name, ServerControlCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    private void migrateConfigKeys() {
        if (!getConfig().contains("features.custom-death-sound")
                && getConfig().contains("mechanics.custom-death-sound-enabled")) {
            getConfig().set(
                    "features.custom-death-sound",
                    getConfig().getBoolean("mechanics.custom-death-sound-enabled", false)
            );
        }

        getConfig().set("mechanics.custom-death-sound-enabled", null);
        getConfig().set("features.death-lightning", null);
        getConfig().set("mechanics.death-lightning-sound", null);
    }
}
