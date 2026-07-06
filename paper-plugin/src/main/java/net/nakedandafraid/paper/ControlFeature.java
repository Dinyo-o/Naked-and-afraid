package net.nakedandafraid.paper;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ControlFeature {
    TAB("tab-list", "tab", "tabs", "tablist", "playerlist"),
    CHAT("public-chat", "chat", "publicchat"),
    DEATH("death-messages", "death", "deaths", "deathmessages"),
    JOIN("join-messages", "join", "joins", "joinmessages"),
    QUIT("quit-messages", "quit", "quits", "quitmessages"),
    ADVANCEMENT("advancement-messages", "advancement", "advancements", "achievement", "achievements"),
    ARMOR_DAMAGE("armor-damage", "armor", "armour", "armordamage", "armourdamage"),
    DEATH_SOUND("death-sound", "deathsound", "deathsounds", "golemsound", "irongolem");

    private final String configPath;
    private final String[] aliases;

    ControlFeature(String configPath, String... aliases) {
        this.configPath = configPath;
        this.aliases = aliases;
    }

    public String configPath() {
        return configPath;
    }

    public String displayName() {
        return configPath;
    }

    public static Optional<ControlFeature> fromInput(String input) {
        String normalized = normalize(input);
        return Arrays.stream(values())
                .filter(feature -> feature.matches(normalized))
                .findFirst();
    }

    private boolean matches(String normalized) {
        if (normalize(configPath).equals(normalized)) {
            return true;
        }

        for (String alias : aliases) {
            if (normalize(alias).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
