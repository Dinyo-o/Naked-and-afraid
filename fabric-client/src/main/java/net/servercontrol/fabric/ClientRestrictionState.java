package net.servercontrol.fabric;

import net.minecraft.client.MinecraftClient;

public final class ClientRestrictionState {
    public static final int FLAG_TAB_DISABLED = 1;
    public static final int FLAG_CHAT_DISABLED = 1 << 1;
    public static final int FLAG_DEATH_MESSAGES_DISABLED = 1 << 2;
    public static final int FLAG_JOIN_MESSAGES_DISABLED = 1 << 3;
    public static final int FLAG_QUIT_MESSAGES_DISABLED = 1 << 4;
    public static final int FLAG_ADVANCEMENT_MESSAGES_DISABLED = 1 << 5;
    public static final int FLAG_ARMOR_DAMAGE_ENABLED = 1 << 6;
    public static final int FLAG_DEATH_SOUND_ENABLED = 1 << 7;
    public static final int FLAG_CUSTOM_DEATH_SOUND_ENABLED = 1 << 8;

    private static volatile boolean companionServer;
    private static volatile boolean tabListBlocked;
    private static volatile boolean publicChatBlocked;
    private static volatile boolean deathMessagesBlocked;
    private static volatile boolean joinMessagesBlocked;
    private static volatile boolean quitMessagesBlocked;
    private static volatile boolean advancementMessagesBlocked;
    private static volatile boolean armorDamageEnabled;
    private static volatile boolean deathSoundEnabled;
    private static volatile boolean customDeathSoundEnabled;

    private ClientRestrictionState() {
    }

    public static void applyFlags(int flags) {
        companionServer = true;
        tabListBlocked = hasFlag(flags, FLAG_TAB_DISABLED);
        publicChatBlocked = hasFlag(flags, FLAG_CHAT_DISABLED);
        deathMessagesBlocked = hasFlag(flags, FLAG_DEATH_MESSAGES_DISABLED);
        joinMessagesBlocked = hasFlag(flags, FLAG_JOIN_MESSAGES_DISABLED);
        quitMessagesBlocked = hasFlag(flags, FLAG_QUIT_MESSAGES_DISABLED);
        advancementMessagesBlocked = hasFlag(flags, FLAG_ADVANCEMENT_MESSAGES_DISABLED);
        armorDamageEnabled = hasFlag(flags, FLAG_ARMOR_DAMAGE_ENABLED);
        deathSoundEnabled = hasFlag(flags, FLAG_DEATH_SOUND_ENABLED);
        customDeathSoundEnabled = hasFlag(flags, FLAG_CUSTOM_DEATH_SOUND_ENABLED);

        if (tabListBlocked) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.options != null) {
                    ServerControlClient.consumePlayerListKey(client.options.playerListKey);
                }
                ServerControlClient.closePlayerList(client);
            });
        }
    }

    public static void reset() {
        companionServer = false;
        tabListBlocked = false;
        publicChatBlocked = false;
        deathMessagesBlocked = false;
        joinMessagesBlocked = false;
        quitMessagesBlocked = false;
        advancementMessagesBlocked = false;
        armorDamageEnabled = false;
        deathSoundEnabled = false;
        customDeathSoundEnabled = false;
    }

    public static boolean isCompanionServer() {
        return companionServer;
    }

    public static boolean isTabListBlocked() {
        return companionServer && tabListBlocked;
    }

    public static boolean isPublicChatBlocked() {
        return companionServer && publicChatBlocked;
    }

    public static boolean isDeathMessagesBlocked() {
        return companionServer && deathMessagesBlocked;
    }

    public static boolean isJoinMessagesBlocked() {
        return companionServer && joinMessagesBlocked;
    }

    public static boolean isQuitMessagesBlocked() {
        return companionServer && quitMessagesBlocked;
    }

    public static boolean isAdvancementMessagesBlocked() {
        return companionServer && advancementMessagesBlocked;
    }

    public static boolean isArmorDamageEnabled() {
        return companionServer && armorDamageEnabled;
    }

    public static boolean isDeathSoundEnabled() {
        return companionServer && deathSoundEnabled;
    }

    public static boolean isCustomDeathSoundEnabled() {
        return companionServer && customDeathSoundEnabled;
    }

    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) == flag;
    }
}
