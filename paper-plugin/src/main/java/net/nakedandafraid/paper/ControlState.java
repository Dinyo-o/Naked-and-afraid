package net.nakedandafraid.paper;

import org.bukkit.configuration.file.FileConfiguration;

public final class ControlState {
    private static final double DEFAULT_ARMOR_DAMAGE_HEARTS = 1.0D;
    private static final double MAX_ARMOR_DAMAGE_HEARTS = 5.0D;

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
    private boolean spectatorsCanSeeTabList;
    private double armorDamageHeartsPerSecond = DEFAULT_ARMOR_DAMAGE_HEARTS;

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
        armorDamageHeartsPerSecond = normalizeArmorDamage(
                config.getDouble("mechanics.armor-damage-hearts-per-second", DEFAULT_ARMOR_DAMAGE_HEARTS)
        );
        spectatorsCanSeeTabList = config.getBoolean("mechanics.spectators-can-see-tab-list", false);
        if (tabListEnabled) {
            spectatorsCanSeeTabList = false;
        }
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
        config.set("mechanics.armor-damage-hearts-per-second", armorDamageHeartsPerSecond);
        config.set("mechanics.armor-damage-items.mob-heads", armorDamageMobHeadsEnabled);
        config.set("mechanics.armor-damage-items.carved-pumpkins", armorDamageCarvedPumpkinsEnabled);
        config.set("mechanics.armor-damage-items.elytras", armorDamageElytrasEnabled);
        config.set("mechanics.spectators-can-see-tab-list", spectatorsCanSeeTabList);
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
        };
    }

    public synchronized void setEnabled(ControlFeature feature, boolean enabled) {
        switch (feature) {
            case TAB -> {
                tabListEnabled = enabled;
                if (enabled) {
                    spectatorsCanSeeTabList = false;
                }
            }
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
            case DEATH_SOUND -> deathSoundEnabled = enabled;
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

    public synchronized boolean canSpectatorsSeeTabList() {
        return spectatorsCanSeeTabList;
    }

    public synchronized void setSpectatorsCanSeeTabList(boolean spectatorsCanSeeTabList) {
        this.spectatorsCanSeeTabList = spectatorsCanSeeTabList && !tabListEnabled;
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

    public synchronized void applyNakedAndAfraidMode() {
        tabListEnabled = false;
        publicChatEnabled = false;
        deathMessagesEnabled = false;
        joinMessagesEnabled = false;
        quitMessagesEnabled = false;
        advancementMessagesEnabled = false;
        armorDamageEnabled = true;
        deathSoundEnabled = true;
        armorDamageHeartsPerSecond = 2.0D;
        armorDamageMobHeadsEnabled = false;
        armorDamageCarvedPumpkinsEnabled = false;
        armorDamageElytrasEnabled = false;
        spectatorsCanSeeTabList = true;
    }

    public synchronized String statusLine() {
        return "tab-list=" + onOff(tabListEnabled)
                + ", spectator-tab=" + onOff(spectatorsCanSeeTabList)
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
                + ", death-sound=" + onOff(deathSoundEnabled);
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

    private void clearArmorDamageItemOptions() {
        armorDamageMobHeadsEnabled = false;
        armorDamageCarvedPumpkinsEnabled = false;
        armorDamageElytrasEnabled = false;
    }
}
