package net.servercontrol.paper;

import org.bukkit.command.CommandSender;

public final class ServerControlPermissions {
    public static final String ALL = "servercontrol.all";
    public static final String GUI = "servercontrol.gui";
    public static final String SETTINGS = "servercontrol.settings";
    private static final String LEGACY_GUI = "servercontrol.command.gui";
    public static final String STATUS = "servercontrol.command.status";
    public static final String RELOAD = "servercontrol.command.reload";
    public static final String RELOAD_DEATH_SOUND = "servercontrol.command.reload-death-sound";
    public static final String ARMOR_DAMAGE_AMOUNT = "servercontrol.command.armor-damage-amount";

    private ServerControlPermissions() {
    }

    public static boolean hasAccess(CommandSender sender, String permission) {
        return sender.hasPermission(ALL)
                || sender.hasPermission(permission)
                || (GUI.equals(permission) && sender.hasPermission(LEGACY_GUI));
    }

    public static boolean canUseGui(CommandSender sender) {
        return hasAccess(sender, GUI) || hasAccess(sender, SETTINGS);
    }

    public static boolean canUseSettings(CommandSender sender) {
        return hasAccess(sender, SETTINGS);
    }

    public static boolean hasFeatureAccess(CommandSender sender, ControlFeature feature) {
        return hasAccess(sender, permissionForFeature(feature));
    }

    public static boolean hasAnyCommandAccess(CommandSender sender) {
        return canUseGui(sender)
                || hasAccess(sender, STATUS)
                || hasAccess(sender, RELOAD)
                || hasFeatureAccess(sender, ControlFeature.TAB)
                || hasFeatureAccess(sender, ControlFeature.CHAT)
                || hasFeatureAccess(sender, ControlFeature.DEATH)
                || hasFeatureAccess(sender, ControlFeature.JOIN)
                || hasFeatureAccess(sender, ControlFeature.QUIT)
                || hasFeatureAccess(sender, ControlFeature.ADVANCEMENT);
    }

    public static String permissionForFeature(ControlFeature feature) {
        return switch (feature) {
            case TAB -> "servercontrol.command.tab";
            case CHAT -> "servercontrol.command.chat";
            case DEATH -> "servercontrol.command.death";
            case JOIN -> "servercontrol.command.join";
            case QUIT -> "servercontrol.command.quit";
            case ADVANCEMENT -> "servercontrol.command.advancement";
            case ARMOR_DAMAGE -> "servercontrol.command.armor-damage";
            case DEATH_SOUND -> "servercontrol.command.death-sound";
            case CUSTOM_DEATH_SOUND -> "servercontrol.command.custom-death-sound";
        };
    }
}
