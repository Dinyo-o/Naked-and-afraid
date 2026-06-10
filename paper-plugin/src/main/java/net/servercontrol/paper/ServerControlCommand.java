package net.servercontrol.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ServerControlCommand implements CommandExecutor, TabCompleter {
    private static final List<String> VALUES = List.of("on", "off");
    private static final List<String> OFF_ONLY = List.of("off");

    private final ServerControlPlugin plugin;
    private final ControlState state;

    public ServerControlCommand(ServerControlPlugin plugin, ControlState state) {
        this.plugin = plugin;
        this.state = state;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String @NotNull [] args
    ) {
        return handleUnified(sender, args);
    }

    private boolean handleUnified(CommandSender sender, String[] args) {
        if (!ServerControlPermissions.hasAnyCommandAccess(sender)) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            if (!ensurePermission(sender, ServerControlPermissions.STATUS)) {
                return true;
            }
            sendStatus(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!ensurePermission(sender, ServerControlPermissions.RELOAD)) {
                return true;
            }
            plugin.reloadServerControlConfig();
            sender.sendMessage("ServerControl config reloaded. " + state.statusLine());
            return true;
        }

        if ("gui".equalsIgnoreCase(args[0])) {
            return openGui(sender);
        }

        Optional<ControlFeature> feature = commandFeature(args[0]);
        if (feature.isPresent()) {
            return handleFeature(sender, feature.get(), args);
        }

        sender.sendMessage("Unknown command. Use status, reload, gui, chat, tab, death, join, quit, or advancement.");
        return true;
    }

    private boolean openGui(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage("Only players can open the ServerControl GUI.");
            return true;
        }
        if (!ServerControlPermissions.canUseGui(player)) {
            sender.sendMessage("You do not have permission to open the ServerControl GUI.");
            return true;
        }

        ServerControlDialogMenu.openMain(plugin, state, player);
        return true;
    }

    private boolean handleFeature(CommandSender sender, ControlFeature feature, String[] args) {
        if (!isCommandBackedFeature(feature)) {
            sender.sendMessage("That setting can only be changed in the ServerControl GUI.");
            return true;
        }

        if (!ensurePermission(sender, ServerControlPermissions.permissionForFeature(feature))) {
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("Use " + allowedValuesText(feature) + ". Example: /servercontrol "
                    + commandName(feature) + " " + exampleValue(feature));
            return true;
        }

        Optional<Boolean> enabled = parseEnabled(args[1]);
        if (enabled.isEmpty() || !isAllowedCommandValue(feature, enabled.get())) {
            sender.sendMessage("Use " + allowedValuesText(feature) + ". Example: /servercontrol "
                    + commandName(feature) + " " + exampleValue(feature));
            return true;
        }

        state.setEnabled(feature, enabled.get());
        plugin.saveStateAndBroadcast();
        sender.sendMessage("Set " + feature.displayName() + " to " + onOff(enabled.get()) + ".");
        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("ServerControl status: " + state.statusLine());
    }

    private static Optional<Boolean> parseEnabled(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "on", "enable", "enabled", "allow", "allowed", "true" -> Optional.of(true);
            case "off", "disable", "disabled", "block", "blocked", "false" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private static boolean isCommandBackedFeature(ControlFeature feature) {
        return switch (feature) {
            case CHAT, TAB, DEATH, JOIN, QUIT, ADVANCEMENT -> true;
            case ARMOR_DAMAGE, DEATH_SOUND, CUSTOM_DEATH_SOUND -> false;
        };
    }

    private static Optional<ControlFeature> commandFeature(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "chat" -> Optional.of(ControlFeature.CHAT);
            case "tab" -> Optional.of(ControlFeature.TAB);
            case "death" -> Optional.of(ControlFeature.DEATH);
            case "join" -> Optional.of(ControlFeature.JOIN);
            case "quit" -> Optional.of(ControlFeature.QUIT);
            case "advancement" -> Optional.of(ControlFeature.ADVANCEMENT);
            default -> Optional.empty();
        };
    }

    private static String commandName(ControlFeature feature) {
        return switch (feature) {
            case CHAT -> "chat";
            case TAB -> "tab";
            case DEATH -> "death";
            case JOIN -> "join";
            case QUIT -> "quit";
            case ADVANCEMENT -> "advancement";
            case ARMOR_DAMAGE, DEATH_SOUND, CUSTOM_DEATH_SOUND -> feature.displayName();
        };
    }

    private static boolean isAllowedCommandValue(ControlFeature feature, boolean enabled) {
        return switch (feature) {
            case CHAT, TAB -> true;
            case DEATH, JOIN, QUIT, ADVANCEMENT -> !enabled;
            case ARMOR_DAMAGE, DEATH_SOUND, CUSTOM_DEATH_SOUND -> false;
        };
    }

    private static String allowedValuesText(ControlFeature feature) {
        return switch (feature) {
            case CHAT, TAB -> "on/off";
            case DEATH, JOIN, QUIT, ADVANCEMENT -> "off";
            case ARMOR_DAMAGE, DEATH_SOUND, CUSTOM_DEATH_SOUND -> "the GUI";
        };
    }

    private static String exampleValue(ControlFeature feature) {
        return switch (feature) {
            case CHAT, TAB -> "off";
            case DEATH, JOIN, QUIT, ADVANCEMENT -> "off";
            case ARMOR_DAMAGE, DEATH_SOUND, CUSTOM_DEATH_SOUND -> "";
        };
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private static boolean ensurePermission(CommandSender sender, String permission) {
        if (ServerControlPermissions.hasAccess(sender, permission)) {
            return true;
        }

        sender.sendMessage("You do not have permission to use this command.");
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            String @NotNull [] args
    ) {
        if (!ServerControlPermissions.hasAnyCommandAccess(sender)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> roots = new ArrayList<>();
            addIfPermitted(sender, roots, ServerControlPermissions.STATUS, "status");
            addIfPermitted(sender, roots, ServerControlPermissions.RELOAD, "reload");
            addIfPermitted(sender, roots, ServerControlPermissions.GUI, "gui");
            addIfPermitted(sender, roots, ServerControlPermissions.permissionForFeature(ControlFeature.CHAT), "chat");
            addIfPermitted(sender, roots, ServerControlPermissions.permissionForFeature(ControlFeature.TAB), "tab");
            addIfPermitted(sender, roots, ServerControlPermissions.permissionForFeature(ControlFeature.DEATH), "death");
            addIfPermitted(sender, roots, ServerControlPermissions.permissionForFeature(ControlFeature.JOIN), "join");
            addIfPermitted(sender, roots, ServerControlPermissions.permissionForFeature(ControlFeature.QUIT), "quit");
            addIfPermitted(sender, roots, ServerControlPermissions.permissionForFeature(ControlFeature.ADVANCEMENT), "advancement");
            return matching(roots, args[0]);
        }

        if (args.length == 2) {
            Optional<ControlFeature> feature = commandFeature(args[0]);
            if (feature.isEmpty() || !ServerControlPermissions.hasFeatureAccess(sender, feature.get())) {
                return List.of();
            }

            return switch (feature.get()) {
                case CHAT, TAB -> matching(VALUES, args[1]);
                case DEATH, JOIN, QUIT, ADVANCEMENT -> matching(OFF_ONLY, args[1]);
                case ARMOR_DAMAGE, DEATH_SOUND, CUSTOM_DEATH_SOUND -> List.of();
            };
        }

        return List.of();
    }

    private static void addIfPermitted(CommandSender sender, List<String> completions, String permission, String value) {
        if (ServerControlPermissions.hasAccess(sender, permission)) {
            completions.add(value);
        }
    }

    private static List<String> matching(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.startsWith(normalized))
                .toList();
    }
}
