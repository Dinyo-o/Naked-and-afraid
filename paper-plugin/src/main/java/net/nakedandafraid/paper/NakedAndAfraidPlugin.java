package net.nakedandafraid.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

public final class NakedAndAfraidPlugin extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 32431;

    private final ControlState state = new ControlState();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private PaperTabVisibilityManager paperTabVisibilityManager;

    @Override
    public void onEnable() {
        new Metrics(this, BSTATS_PLUGIN_ID);

        saveDefaultConfig();
        migrateConfigKeys();
        getConfig().options().copyDefaults(true);
        saveConfig();
        state.load(getConfig());
        state.write(getConfig());
        saveConfig();

        paperTabVisibilityManager = new PaperTabVisibilityManager(this, state);
        paperTabVisibilityManager.register();

        getServer().getPluginManager().registerEvents(new RestrictionListener(this, state), this);

        NakedAndAfraidCommand command = new NakedAndAfraidCommand(this, state);
        registerCommand("nakedandafraid", command);
        removeNamespacedCommandFallbacks("nakedandafraid", "na");
    }

    @Override
    public void onDisable() {
        if (paperTabVisibilityManager != null) {
            paperTabVisibilityManager.unregister();
        }
    }

    public Component configuredComponent(String path) {
        return miniMessage.deserialize(getConfig().getString(path, ""));
    }

    public void reloadNakedAndAfraidConfig() {
        reloadConfig();
        migrateConfigKeys();
        getConfig().options().copyDefaults(true);
        saveConfig();
        state.load(getConfig());
        state.write(getConfig());
        saveConfig();
        paperTabVisibilityManager.applyAll();
    }

    public void saveStateAndBroadcast() {
        state.write(getConfig());
        saveConfig();
        paperTabVisibilityManager.applyAll();
    }

    public void playDeathSoundNow() {
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

    private void registerCommand(String name, NakedAndAfraidCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    @SuppressWarnings("unchecked")
    private void removeNamespacedCommandFallbacks(String... labels) {
        if (!(getServer().getCommandMap() instanceof SimpleCommandMap commandMap)) {
            return;
        }

        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            String prefix = getName().toLowerCase(Locale.ROOT);
            for (String label : labels) {
                knownCommands.remove(prefix + ":" + label.toLowerCase(Locale.ROOT));
            }
        } catch (ReflectiveOperationException exception) {
            getLogger().warning("Could not hide namespaced command fallbacks: " + exception.getMessage());
        }
    }

    private void migrateConfigKeys() {
        getConfig().set("features.death-lightning", null);
        getConfig().set("mechanics.death-lightning-sound", null);
    }
}
