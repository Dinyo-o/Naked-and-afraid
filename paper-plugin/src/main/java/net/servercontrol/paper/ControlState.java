package net.servercontrol.paper;

import org.bukkit.configuration.file.FileConfiguration;

public final class ControlState {
    public static final int FLAG_TAB_DISABLED = 1;
    public static final int FLAG_CHAT_DISABLED = 1 << 1;
    public static final int FLAG_DEATH_MESSAGES_DISABLED = 1 << 2;
    public static final int FLAG_JOIN_MESSAGES_DISABLED = 1 << 3;
    public static final int FLAG_QUIT_MESSAGES_DISABLED = 1 << 4;
    public static final int FLAG_ADVANCEMENT_MESSAGES_DISABLED = 1 << 5;
    public static final int FLAG_ARMOR_DAMAGE_ENABLED = 1 << 6;
    public static final int FLAG_DEATH_SOUND_ENABLED = 1 << 7;
    public static final int FLAG_CUSTOM_DEATH_SOUND_ENABLED = 1 << 8;

    private static final double DEFAULT_ARMOR_DAMAGE_HEARTS = 1.0D;
    private static final double DEFAULT_DEATH_SOUND_VOLUME_PERCENT = 100.0D;
    private static final double MAX_ARMOR_DAMAGE_HEARTS = 5.0D;
    private static final double MAX_DEATH_SOUND_VOLUME_PERCENT = 100.0D;

    private boolean tabListEnabled = true;
    private boolean publicChatEnabled = true;
    private boolean deathMessagesEnabled = true;
    private boolean joinMessagesEnabled = true;
    private boolean quitMessagesEnabled = true;
    private boolean advancementMessagesEnabled = true;
    private boolean armorDamageEnabled;
    private boolean armorDamageMobHeadsEnabled;
    private boolean armorDamageCarvedPumpkinsEnabled;
    private boolean armorDamageElytrasEnabled;
    private boolean deathSoundEnabled;
    private boolean customDeathSoundEnabled = true;
    private double armorDamageHeartsPerSecond = DEFAULT_ARMOR_DAMAGE_HEARTS;
    private double deathSoundVolumePercent = DEFAULT_DEATH_SOUND_VOLUME_PERCENT;

    public synchronized void load(FileConfiguration config) {
        tabListEnabled = config.getBoolean(path(ControlFeature.TAB), true);
        publicChatEnabled = config.getBoolean(path(ControlFeature.CHAT), true);
        deathMessagesEnabled = config.getBoolean(path(ControlFeature.DEATH), true);
        joinMessagesEnabled = config.getBoolean(path(ControlFeature.JOIN), true);
        quitMessagesEnabled = config.getBoolean(path(ControlFeature.QUIT), true);
        advancementMessagesEnabled = config.getBoolean(path(ControlFeature.ADVANCEMENT), true);
        armorDamageEnabled = config.getBoolean(path(ControlFeature.ARMOR_DAMAGE), false);
        armorDamageMobHeadsEnabled = config.getBoolean("mechanics.armor-damage-items.mob-heads", false);
        armorDamageCarvedPumpkinsEnabled = config.getBoolean("mechanics.armor-damage-items.carved-pumpkins", false);
        armorDamageElytrasEnabled = config.getBoolean("mechanics.armor-damage-items.elytras", false);
        if (!armorDamageEnabled) {
            clearArmorDamageItemOptions();
        }
        deathSoundEnabled = config.getBoolean(path(ControlFeature.DEATH_SOUND), false);
        customDeathSoundEnabled = config.contains(path(ControlFeature.CUSTOM_DEATH_SOUND))
                ? config.getBoolean(path(ControlFeature.CUSTOM_DEATH_SOUND), false)
                : config.getBoolean("mechanics.custom-death-sound-enabled", false);
        if (!deathSoundEnabled) {
            customDeathSoundEnabled = false;
        }
        armorDamageHeartsPerSecond = normalizeArmorDamage(
                config.getDouble("mechanics.armor-damage-hearts-per-second", DEFAULT_ARMOR_DAMAGE_HEARTS)
        );
        deathSoundVolumePercent = normalizeDeathSoundVolumePercent(
                config.getDouble("mechanics.death-sound-volume-percent", DEFAULT_DEATH_SOUND_VOLUME_PERCENT)
        );
    }

    public synchronized void write(FileConfiguration config) {
        config.set(path(ControlFeature.TAB), tabListEnabled);
        config.set(path(ControlFeature.CHAT), publicChatEnabled);
        config.set(path(ControlFeature.DEATH), deathMessagesEnabled);
        config.set(path(ControlFeature.JOIN), joinMessagesEnabled);
        config.set(path(ControlFeature.QUIT), quitMessagesEnabled);
        config.set(path(ControlFeature.ADVANCEMENT), advancementMessagesEnabled);
        config.set(path(ControlFeature.ARMOR_DAMAGE), armorDamageEnabled);
        config.set(path(ControlFeature.DEATH_SOUND), deathSoundEnabled);
        config.set(path(ControlFeature.CUSTOM_DEATH_SOUND), customDeathSoundEnabled);
        config.set("mechanics.armor-damage-hearts-per-second", armorDamageHeartsPerSecond);
        config.set("mechanics.armor-damage-items.mob-heads", armorDamageMobHeadsEnabled);
        config.set("mechanics.armor-damage-items.carved-pumpkins", armorDamageCarvedPumpkinsEnabled);
        config.set("mechanics.armor-damage-items.elytras", armorDamageElytrasEnabled);
        config.set("mechanics.death-sound-volume-percent", deathSoundVolumePercent);
    }

    public synchronized boolean isEnabled(ControlFeature feature) {
        return switch (feature) {
            case TAB -> tabListEnabled;
            case CHAT -> publicChatEnabled;
            case DEATH -> deathMessagesEnabled;
            case JOIN -> joinMessagesEnabled;
            case QUIT -> quitMessagesEnabled;
            case ADVANCEMENT -> advancementMessagesEnabled;
            case ARMOR_DAMAGE -> armorDamageEnabled;
            case DEATH_SOUND -> deathSoundEnabled;
            case CUSTOM_DEATH_SOUND -> customDeathSoundEnabled;
        };
    }

    public synchronized void setEnabled(ControlFeature feature, boolean enabled) {
        switch (feature) {
            case TAB -> tabListEnabled = enabled;
            case CHAT -> publicChatEnabled = enabled;
            case DEATH -> deathMessagesEnabled = enabled;
            case JOIN -> joinMessagesEnabled = enabled;
            case QUIT -> quitMessagesEnabled = enabled;
            case ADVANCEMENT -> advancementMessagesEnabled = enabled;
            case ARMOR_DAMAGE -> {
                armorDamageEnabled = enabled;
                if (!enabled) {
                    clearArmorDamageItemOptions();
                }
            }
            case DEATH_SOUND -> {
                deathSoundEnabled = enabled;
                if (!enabled) {
                    customDeathSoundEnabled = false;
                }
            }
            case CUSTOM_DEATH_SOUND -> customDeathSoundEnabled = enabled && deathSoundEnabled;
        }
    }

    public synchronized boolean isTabListEnabled() {
        return tabListEnabled;
    }

    public synchronized boolean isPublicChatEnabled() {
        return publicChatEnabled;
    }

    public synchronized boolean isDeathMessagesEnabled() {
        return deathMessagesEnabled;
    }

    public synchronized boolean isJoinMessagesEnabled() {
        return joinMessagesEnabled;
    }

    public synchronized boolean isQuitMessagesEnabled() {
        return quitMessagesEnabled;
    }

    public synchronized boolean isAdvancementMessagesEnabled() {
        return advancementMessagesEnabled;
    }

    public synchronized boolean isArmorDamageEnabled() {
        return armorDamageEnabled;
    }

    public synchronized boolean isArmorDamageMobHeadsEnabled() {
        return armorDamageEnabled && armorDamageMobHeadsEnabled;
    }

    public synchronized boolean isArmorDamageCarvedPumpkinsEnabled() {
        return armorDamageEnabled && armorDamageCarvedPumpkinsEnabled;
    }

    public synchronized boolean isArmorDamageElytrasEnabled() {
        return armorDamageEnabled && armorDamageElytrasEnabled;
    }

    public synchronized void setArmorDamageMobHeadsEnabled(boolean enabled) {
        armorDamageMobHeadsEnabled = enabled && armorDamageEnabled;
    }

    public synchronized void setArmorDamageCarvedPumpkinsEnabled(boolean enabled) {
        armorDamageCarvedPumpkinsEnabled = enabled && armorDamageEnabled;
    }

    public synchronized void setArmorDamageElytrasEnabled(boolean enabled) {
        armorDamageElytrasEnabled = enabled && armorDamageEnabled;
    }

    public synchronized boolean isDeathSoundEnabled() {
        return deathSoundEnabled;
    }

    public synchronized boolean isCustomDeathSoundEnabled() {
        return customDeathSoundEnabled;
    }

    public synchronized double armorDamageHeartsPerSecond() {
        return armorDamageHeartsPerSecond;
    }

    public synchronized void setArmorDamageHeartsPerSecond(double armorDamageHeartsPerSecond) {
        this.armorDamageHeartsPerSecond = normalizeArmorDamage(armorDamageHeartsPerSecond);
    }

    public synchronized double armorDamageHealthPointsPerSecond() {
        return armorDamageHeartsPerSecond * 2.0D;
    }

    public synchronized int deathSoundVolumePercentRounded() {
        return (int) Math.round(deathSoundVolumePercent);
    }

    public synchronized double deathSoundVolumePercent() {
        return deathSoundVolumePercent;
    }

    public synchronized void setDeathSoundVolumePercent(double deathSoundVolumePercent) {
        this.deathSoundVolumePercent = normalizeDeathSoundVolumePercent(deathSoundVolumePercent);
    }

    public synchronized void applyNakedAndAfraidMode() {
        tabListEnabled = false;
        publicChatEnabled = false;
        deathMessagesEnabled = false;
        joinMessagesEnabled = false;
        quitMessagesEnabled = false;
        advancementMessagesEnabled = false;
        armorDamageEnabled = true;
        deathSoundEnabled = true;
        customDeathSoundEnabled = false;
        armorDamageHeartsPerSecond = 2.0D;
        armorDamageMobHeadsEnabled = false;
        armorDamageCarvedPumpkinsEnabled = false;
        armorDamageElytrasEnabled = false;
        deathSoundVolumePercent = DEFAULT_DEATH_SOUND_VOLUME_PERCENT;
    }

    public synchronized int disabledMask() {
        int flags = 0;
        if (!tabListEnabled) {
            flags |= FLAG_TAB_DISABLED;
        }
        if (!publicChatEnabled) {
            flags |= FLAG_CHAT_DISABLED;
        }
        if (!deathMessagesEnabled) {
            flags |= FLAG_DEATH_MESSAGES_DISABLED;
        }
        if (!joinMessagesEnabled) {
            flags |= FLAG_JOIN_MESSAGES_DISABLED;
        }
        if (!quitMessagesEnabled) {
            flags |= FLAG_QUIT_MESSAGES_DISABLED;
        }
        if (!advancementMessagesEnabled) {
            flags |= FLAG_ADVANCEMENT_MESSAGES_DISABLED;
        }
        if (armorDamageEnabled) {
            flags |= FLAG_ARMOR_DAMAGE_ENABLED;
        }
        if (deathSoundEnabled) {
            flags |= FLAG_DEATH_SOUND_ENABLED;
        }
        if (deathSoundEnabled && customDeathSoundEnabled) {
            flags |= FLAG_CUSTOM_DEATH_SOUND_ENABLED;
        }
        return flags;
    }

    public synchronized String statusLine() {
        return "tab-list=" + onOff(tabListEnabled)
                + ", public-chat=" + onOff(publicChatEnabled)
                + ", death-messages=" + onOff(deathMessagesEnabled)
                + ", join-messages=" + onOff(joinMessagesEnabled)
                + ", quit-messages=" + onOff(quitMessagesEnabled)
                + ", advancement-messages=" + onOff(advancementMessagesEnabled)
                + ", armor-damage=" + onOff(armorDamageEnabled)
                + " (" + armorDamageHeartsPerSecond + " hearts/sec)"
                + ", armor-damage-mob-heads=" + onOff(isArmorDamageMobHeadsEnabled())
                + ", armor-damage-carved-pumpkins=" + onOff(isArmorDamageCarvedPumpkinsEnabled())
                + ", armor-damage-elytras=" + onOff(isArmorDamageElytrasEnabled())
                + ", death-sound=" + onOff(deathSoundEnabled)
                + ", custom-death-sound-volume=" + deathSoundVolumePercent + "%"
                + ", custom-death-sound=" + onOff(customDeathSoundEnabled);
    }

    private static String path(ControlFeature feature) {
        return "features." + feature.configPath();
    }

    private static String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private static double normalizeArmorDamage(double value) {
        double bounded = Math.max(0.0D, Math.min(MAX_ARMOR_DAMAGE_HEARTS, value));
        return Math.round(bounded * 2.0D) / 2.0D;
    }

    private static double normalizeDeathSoundVolumePercent(double value) {
        return Math.max(0.0D, Math.min(MAX_DEATH_SOUND_VOLUME_PERCENT, value));
    }

    private void clearArmorDamageItemOptions() {
        armorDamageMobHeadsEnabled = false;
        armorDamageCarvedPumpkinsEnabled = false;
        armorDamageElytrasEnabled = false;
    }
}
