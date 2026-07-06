package net.nakedandafraid.paper;

import org.bukkit.command.CommandSender;

public final class NakedAndAfraidPermissions {
    public static final String ALL = "nakedandafraid.all";
    public static final String GUI = "nakedandafraid.gui";
    public static final String SETTINGS = "nakedandafraid.settings";
    public static final String STATUS = "nakedandafraid.command.status";
    public static final String RELOAD = "nakedandafraid.command.reload";
    public static final String ARMOR_DAMAGE_AMOUNT = "nakedandafraid.command.armor-damage-amount";

    private NakedAndAfraidPermissions() {
    }

    public static boolean hasAccess(CommandSender sender, String permission) {
        return sender.hasPermission(ALL)
                || sender.hasPermission(permission);
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
            case TAB -> "nakedandafraid.command.tab";
            case CHAT -> "nakedandafraid.command.chat";
            case DEATH -> "nakedandafraid.command.death";
            case JOIN -> "nakedandafraid.command.join";
            case QUIT -> "nakedandafraid.command.quit";
            case ADVANCEMENT -> "nakedandafraid.command.advancement";
            case ARMOR_DAMAGE -> "nakedandafraid.command.armor-damage";
            case DEATH_SOUND -> "nakedandafraid.command.death-sound";
        };
    }
}
