package net.servercontrol.paper;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ServerControlDialogMenu {
    private static final int BUTTON_WIDTH = 240;
    private static final String ARMOR_DAMAGE_AMOUNT_INPUT = "armor_damage_amount";
    private static final String DEATH_SOUND_VOLUME_INPUT = "death_sound_volume";
    private static final String MOB_HEAD_ICON = "\u2620 ";
    private static final String CARVED_PUMPKIN_ICON = "\uD83C\uDF83 ";
    private static final String ELYTRA_ICON = "\uD83E\uDEB6 ";
    private static final String MUSIC_DISC_ICON = "\uD83D\uDCBF ";
    private static final String SWORD_ICON = "\u2694 ";
    private static final FeatureEntry PUBLIC_CHAT = new FeatureEntry(
            ControlFeature.CHAT,
            "Public Chat",
            "Controls normal public player chat.",
            "Players can send normal public chat messages.",
            "Normal player chat is blocked; commands, /say, operator feedback, and moderation messages still work."
    );
    private static final FeatureEntry TAB_LIST = new FeatureEntry(
            ControlFeature.TAB,
            "Tab List Access",
            "Controls whether verified Fabric clients can open the player list overlay.",
            "Players may open the tab/player-list overlay.",
            "The Fabric client blocks the tab/player-list overlay regardless of keybind."
    );
    private static final FeatureEntry ADVANCEMENTS = new FeatureEntry(
            ControlFeature.ADVANCEMENT,
            "Advancement Messages",
            "Controls advancement announcements in chat.",
            "Advancement messages appear in chat.",
            "Advancement chat announcements are hidden, while the client advancements page still works."
    );
    private static final FeatureEntry DEATH_MESSAGES = new FeatureEntry(
            ControlFeature.DEATH,
            "Death Messages",
            "Controls vanilla death messages in chat.",
            "Death messages appear in chat.",
            "Death messages are hidden globally."
    );
    private static final FeatureEntry JOIN_MESSAGES = new FeatureEntry(
            ControlFeature.JOIN,
            "Join Messages",
            "Controls player join messages in chat.",
            "Join messages appear in chat.",
            "Join messages are hidden globally."
    );
    private static final FeatureEntry QUIT_MESSAGES = new FeatureEntry(
            ControlFeature.QUIT,
            "Quit Messages",
            "Controls player quit messages in chat.",
            "Quit messages appear in chat.",
            "Quit messages are hidden globally."
    );
    private static final FeatureEntry DEATH_SOUND = new FeatureEntry(
            ControlFeature.DEATH_SOUND,
            "Death Sound",
            "Controls the universal sound that plays when a player dies.",
            "Deaths play the synced custom sound if available, otherwise the Iron Golem death sound.",
            "ServerControl does not play a death sound."
    );
    private static final FeatureEntry CUSTOM_DEATH_SOUND = new FeatureEntry(
            ControlFeature.CUSTOM_DEATH_SOUND,
            "Custom Death Sound",
            "Controls whether the death sound uses the synced custom MP3.",
            "Deaths use the custom MP3 when one is loaded; requires Death Sound to be on.",
            "Off while Death Sound is off; otherwise deaths use the default Iron Golem death sound."
    );
    private static final FeatureEntry ARMOR_DAMAGE = new FeatureEntry(
            ControlFeature.ARMOR_DAMAGE,
            "Armor Damage",
            "Controls damage to players wearing anything in armor slots.",
            "Players wearing armor-slot items take the configured damage every second.",
            "Armor-slot items do not cause ServerControl damage."
    );
    private static final List<FeatureEntry> CHAT_FEATURES = List.of(
            PUBLIC_CHAT,
            TAB_LIST,
            ADVANCEMENTS,
            DEATH_MESSAGES,
            JOIN_MESSAGES,
            QUIT_MESSAGES
    );
    private static final List<FeatureEntry> DEATH_FEATURES = List.of(
            DEATH_MESSAGES,
            DEATH_SOUND,
            CUSTOM_DEATH_SOUND,
            ARMOR_DAMAGE
    );
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .lifetime(Duration.ofMinutes(10))
            .build();

    private ServerControlDialogMenu() {
    }

    public static void openMain(ServerControlPlugin plugin, ControlState state, Player player) {
        List<ActionButton> actions = new ArrayList<>();
        boolean hasChat = hasAnyFeatureAccess(player, CHAT_FEATURES);
        boolean hasDeath = hasDeathCategoryAccess(player);
        boolean hasSettings = hasSettingsAccess(player);
        if (hasChat) {
            actions.add(pageButton(plugin, "Chat Options", "Open chat, message, and player-list controls.",
                    menuPlayer -> openChat(plugin, state, menuPlayer)));
        }
        if (hasDeath) {
            actions.add(pageButton(plugin, "Death Options", "Open death sound, armor damage, and death-message controls.",
                    menuPlayer -> openDeath(plugin, state, menuPlayer)));
        }
        if (hasChat || hasDeath) {
            actions.add(pageButton(plugin, "All Options", "Open every setting you can access in a two-column view.",
                    menuPlayer -> openAll(plugin, state, menuPlayer)));
        }
        if (hasSettings) {
            actions.add(pageButton(plugin, "Settings", "Open custom sound volume and preset controls.",
                    menuPlayer -> openSettings(plugin, state, menuPlayer)));
        }

        show(
                player,
                Component.text("ServerControl", NamedTextColor.AQUA),
                Component.text("Choose a settings category.", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                1
        );
    }

    private static void openSettings(ServerControlPlugin plugin, ControlState state, Player player) {
        List<ActionButton> actions = new ArrayList<>();
        if (canChangeCustomDeathSoundVolume(player)) {
            actions.add(customDeathSoundVolumeButton(plugin, state, menuPlayer -> openSettings(plugin, state, menuPlayer)));
        }
        if (canApplyNakedAndAfraidMode(player)) {
            actions.add(nakedAndAfraidButton(plugin, state));
        }
        actions.add(backButton(plugin, state));

        show(
                player,
                Component.text("Settings", NamedTextColor.AQUA),
                Component.text("Custom sound volume only affects the synced MP3; the Iron Golem fallback uses normal volume.", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                1
        );
    }

    private static void openChat(ServerControlPlugin plugin, ControlState state, Player player) {
        List<ActionButton> actions = featureButtons(player, plugin, state, CHAT_FEATURES,
                menuPlayer -> openChat(plugin, state, menuPlayer));
        actions.add(backButton(plugin, state));

        show(
                player,
                Component.text("Chat Options", NamedTextColor.AQUA),
                Component.text("Click a setting to toggle it. Green is on, red is off.", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                1
        );
    }

    private static void openDeath(ServerControlPlugin plugin, ControlState state, Player player) {
        List<ActionButton> actions = deathButtons(player, plugin, state,
                menuPlayer -> openDeath(plugin, state, menuPlayer));
        actions.add(backButton(plugin, state));

        show(
                player,
                Component.text("Death Options", NamedTextColor.AQUA),
                Component.text("Click a setting to toggle it. Green is on, red is off.", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                1
        );
    }

    private static void openAll(ServerControlPlugin plugin, ControlState state, Player player) {
        List<ActionButton> chatButtons = featureButtons(player, plugin, state, CHAT_FEATURES,
                menuPlayer -> openAll(plugin, state, menuPlayer));
        List<ActionButton> deathButtons = deathButtons(player, plugin, state,
                menuPlayer -> openAll(plugin, state, menuPlayer));
        boolean twoColumns = !chatButtons.isEmpty() && !deathButtons.isEmpty();
        List<ActionButton> actions = new ArrayList<>();

        if (twoColumns) {
            actions.add(headerButton(plugin, "Chat Options", "Chat, message, and player-list controls."));
            actions.add(headerButton(plugin, "Death Options", "Death sound, armor damage, and death-message controls."));
            int rows = Math.max(chatButtons.size(), deathButtons.size());
            for (int index = 0; index < rows; index++) {
                actions.add(index < chatButtons.size()
                        ? chatButtons.get(index)
                        : spacerButton(plugin));
                actions.add(index < deathButtons.size()
                        ? deathButtons.get(index)
                        : spacerButton(plugin));
            }
        } else {
            List<ActionButton> buttons = chatButtons.isEmpty() ? deathButtons : chatButtons;
            if (!buttons.isEmpty()) {
                actions.add(headerButton(plugin, chatButtons.isEmpty() ? "Death Options" : "Chat Options",
                        chatButtons.isEmpty()
                                ? "Death sound, armor damage, and death-message controls."
                                : "Chat, message, and player-list controls."));
            }
            actions.addAll(buttons);
        }

        actions.add(backButton(plugin, state));

        show(
                player,
                Component.text("All Options", NamedTextColor.AQUA),
                Component.text("Everything you can access. Green is on, red is off.", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                twoColumns ? 2 : 1
        );
    }

    private static void show(
            Player player,
            Component title,
            Component body,
            List<ActionButton> actions,
            ActionButton closeButton,
            int columns
    ) {
        show(player, title, body, List.of(), actions, closeButton, columns);
    }

    private static void show(
            Player player,
            Component title,
            Component body,
            List<DialogInput> inputs,
            List<ActionButton> actions,
            ActionButton closeButton,
            int columns
    ) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .canCloseWithEscape(false)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(List.of(DialogBody.plainMessage(body, 320)))
                        .inputs(inputs)
                        .build())
                .type(actions.isEmpty()
                        ? DialogType.notice(closeButton)
                        : DialogType.multiAction(actions, closeButton, columns))
        );
        player.showDialog(dialog);
    }

    private static ActionButton pageButton(
            ServerControlPlugin plugin,
            String label,
            String tooltip,
            PlayerDialogAction action
    ) {
        return ActionButton.create(
                Component.text(label, NamedTextColor.WHITE),
                Component.text(tooltip, NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, action)
        );
    }

    private static ActionButton headerButton(ServerControlPlugin plugin, String label, String tooltip) {
        return ActionButton.create(
                Component.text(label, NamedTextColor.WHITE),
                Component.text(tooltip, NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> {
                })
        );
    }

    private static ActionButton spacerButton(ServerControlPlugin plugin) {
        return ActionButton.create(
                Component.text(" ", NamedTextColor.WHITE),
                Component.empty(),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> {
                })
        );
    }

    private static List<ActionButton> featureButtons(
            Player player,
            ServerControlPlugin plugin,
            ControlState state,
            List<FeatureEntry> entries,
            PlayerDialogAction reopen
    ) {
        List<ActionButton> actions = new ArrayList<>();
        for (FeatureEntry entry : entries) {
            if (ServerControlPermissions.hasFeatureAccess(player, entry.feature())) {
                actions.add(featureButton(plugin, state, entry, reopen));
            }
        }
        return actions;
    }

    private static List<ActionButton> deathButtons(
            Player player,
            ServerControlPlugin plugin,
            ControlState state,
            PlayerDialogAction reopen
    ) {
        List<ActionButton> actions = featureButtons(player, plugin, state, DEATH_FEATURES, reopen);
        if (ServerControlPermissions.hasAccess(player, ServerControlPermissions.ARMOR_DAMAGE_AMOUNT)) {
            actions.add(armorDamageAmountButton(plugin, state, reopen));
        }
        if (ServerControlPermissions.hasFeatureAccess(player, ControlFeature.ARMOR_DAMAGE)) {
            actions.add(armorDamageItemsButton(plugin, state, reopen));
        }
        if (ServerControlPermissions.hasAccess(player, ServerControlPermissions.RELOAD_DEATH_SOUND)) {
            actions.add(reloadCustomSoundButton(plugin, state, reopen));
        }
        return actions;
    }

    private static ActionButton featureButton(
            ServerControlPlugin plugin,
            ControlState state,
            FeatureEntry entry,
            PlayerDialogAction reopen
    ) {
        boolean enabled = state.isEnabled(entry.feature());
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
        String stateText = enabled ? "ON" : "OFF";
        return ActionButton.create(
                Component.text(entry.label() + ": " + stateText, color),
                Component.text(entry.summary() + "\nEnabled: " + entry.enabledDescription()
                        + "\nDisabled: " + entry.disabledDescription()
                        + "\nCurrent: " + stateText, NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> {
                    if (!ServerControlPermissions.hasFeatureAccess(player, entry.feature())) {
                        reopen.run(player);
                        return;
                    }

                    if (entry.feature() == ControlFeature.CUSTOM_DEATH_SOUND && !state.isDeathSoundEnabled()) {
                        player.sendMessage(Component.text(
                                "Turn on Death Sound before changing Custom Death Sound.",
                                NamedTextColor.RED
                        ));
                        reopen.run(player);
                        return;
                    }

                    state.setEnabled(entry.feature(), !state.isEnabled(entry.feature()));
                    plugin.saveStateAndBroadcast();
                    if (entry.feature() == ControlFeature.CUSTOM_DEATH_SOUND
                            && state.isCustomDeathSoundEnabled()
                            && !plugin.customDeathSoundManager().hasActiveSound()) {
                        player.sendMessage(Component.text(
                                "No valid custom MP3 is loaded yet, so deaths will use the Iron Golem sound until one is reloaded.",
                                NamedTextColor.YELLOW
                        ));
                    }
                    reopen.run(player);
                })
        );
    }

    private static ActionButton armorDamageItemsButton(
            ServerControlPlugin plugin,
            ControlState state,
            PlayerDialogAction reopen
    ) {
        return ActionButton.create(
                Component.text("Armor Damage Items", NamedTextColor.WHITE),
                Component.text("Choose whether mob heads, carved pumpkins, and elytras trigger armor damage.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> openArmorDamageItems(plugin, state, player, reopen))
        );
    }

    private static void openArmorDamageItems(
            ServerControlPlugin plugin,
            ControlState state,
            Player player,
            PlayerDialogAction reopen
    ) {
        if (!ServerControlPermissions.hasFeatureAccess(player, ControlFeature.ARMOR_DAMAGE)) {
            player.sendMessage(Component.text("You do not have permission to change armor damage items.", NamedTextColor.RED));
            reopen.run(player);
            return;
        }

        List<ActionButton> actions = new ArrayList<>();
        actions.add(armorDamageItemToggleButton(
                plugin,
                state,
                MOB_HEAD_ICON,
                "Mob Heads",
                "Controls whether worn mob/player heads trigger armor damage.",
                state.isArmorDamageMobHeadsEnabled(),
                menuPlayer -> {
                    if (!ensureArmorDamageEnabled(menuPlayer, state)) {
                        openArmorDamageItems(plugin, state, menuPlayer, reopen);
                        return;
                    }
                    state.setArmorDamageMobHeadsEnabled(!state.isArmorDamageMobHeadsEnabled());
                    plugin.saveStateAndBroadcast();
                    openArmorDamageItems(plugin, state, menuPlayer, reopen);
                }
        ));
        actions.add(armorDamageItemToggleButton(
                plugin,
                state,
                CARVED_PUMPKIN_ICON,
                "Carved Pumpkins",
                "Controls whether worn carved pumpkins trigger armor damage.",
                state.isArmorDamageCarvedPumpkinsEnabled(),
                menuPlayer -> {
                    if (!ensureArmorDamageEnabled(menuPlayer, state)) {
                        openArmorDamageItems(plugin, state, menuPlayer, reopen);
                        return;
                    }
                    state.setArmorDamageCarvedPumpkinsEnabled(!state.isArmorDamageCarvedPumpkinsEnabled());
                    plugin.saveStateAndBroadcast();
                    openArmorDamageItems(plugin, state, menuPlayer, reopen);
                }
        ));
        actions.add(armorDamageItemToggleButton(
                plugin,
                state,
                ELYTRA_ICON,
                "Elytras",
                "Controls whether worn elytras trigger armor damage.",
                state.isArmorDamageElytrasEnabled(),
                menuPlayer -> {
                    if (!ensureArmorDamageEnabled(menuPlayer, state)) {
                        openArmorDamageItems(plugin, state, menuPlayer, reopen);
                        return;
                    }
                    state.setArmorDamageElytrasEnabled(!state.isArmorDamageElytrasEnabled());
                    plugin.saveStateAndBroadcast();
                    openArmorDamageItems(plugin, state, menuPlayer, reopen);
                }
        ));
        actions.add(ActionButton.create(
                Component.text("Back", NamedTextColor.WHITE),
                Component.text("Return to the previous ServerControl menu.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, reopen)
        ));

        show(
                player,
                Component.text("Armor Damage Items", NamedTextColor.AQUA),
                Component.text("These item toggles only work while Armor Damage is on.", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                1
        );
    }

    private static ActionButton armorDamageItemToggleButton(
            ServerControlPlugin plugin,
            ControlState state,
            String icon,
            String label,
            String tooltip,
            boolean enabled,
            PlayerDialogAction action
    ) {
        String stateText = enabled ? "ON" : "OFF";
        return ActionButton.create(
                Component.text(icon + label + ": " + stateText, enabled ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text(tooltip + "\nRequires Armor Damage: "
                        + (state.isArmorDamageEnabled() ? "ON" : "OFF"), NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, action)
        );
    }

    private static boolean ensureArmorDamageEnabled(Player player, ControlState state) {
        if (state.isArmorDamageEnabled()) {
            return true;
        }

        player.sendMessage(Component.text(
                "Turn on Armor Damage before changing armor damage item options.",
                NamedTextColor.RED
        ));
        return false;
    }

    private static ActionButton armorDamageAmountButton(
            ServerControlPlugin plugin,
            ControlState state,
            PlayerDialogAction reopen
    ) {
        return ActionButton.create(
                Component.text("Armor Damage Amount: " + formatDamageAmount(state.armorDamageHeartsPerSecond()), NamedTextColor.WHITE),
                Component.text("Change armor damage hearts per second. Allowed values are 0 to 5 in .5 steps.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> openArmorDamageAmount(plugin, state, player, reopen))
        );
    }

    private static void openArmorDamageAmount(
            ServerControlPlugin plugin,
            ControlState state,
            Player player,
            PlayerDialogAction reopen
    ) {
        List<ActionButton> actions = new ArrayList<>();
        actions.add(ActionButton.create(
                Component.text("Apply", NamedTextColor.GREEN),
                Component.text("Save the selected armor damage amount.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                responseAction(plugin, true, (view, menuPlayer) -> {
                    if (!ServerControlPermissions.hasAccess(menuPlayer, ServerControlPermissions.ARMOR_DAMAGE_AMOUNT)) {
                        reopen.run(menuPlayer);
                        return;
                    }

                    Float selected = view.getFloat(ARMOR_DAMAGE_AMOUNT_INPUT);
                    if (selected == null || !isValidDamageAmount(selected)) {
                        menuPlayer.sendMessage(Component.text(
                                "Use a value from 0 to 5 in .5 steps.",
                                NamedTextColor.RED
                        ));
                        openArmorDamageAmount(plugin, state, menuPlayer, reopen);
                        return;
                    }

                    state.setArmorDamageHeartsPerSecond(selected);
                    plugin.saveStateAndBroadcast();
                    menuPlayer.sendMessage(Component.text(
                            "Set armor damage amount to " + formatDamageAmount(selected) + " hearts/sec.",
                            NamedTextColor.GREEN
                    ));
                    openArmorDamageAmount(plugin, state, menuPlayer, reopen);
                })
        ));
        actions.add(ActionButton.create(
                Component.text("Back", NamedTextColor.WHITE),
                Component.text("Return to the previous ServerControl menu.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, reopen)
        ));

        List<DialogInput> inputs = List.of(DialogInput.numberRange(
                        ARMOR_DAMAGE_AMOUNT_INPUT,
                        Component.text("Damage Per Second", NamedTextColor.WHITE),
                        0.0F,
                        5.0F
                )
                .width(BUTTON_WIDTH)
                .initial((float) state.armorDamageHeartsPerSecond())
                .step(0.5F)
                .build());

        show(
                player,
                Component.text("Armor Damage Amount", NamedTextColor.AQUA),
                Component.text("Choose hearts of damage per second while wearing armor-slot items.", NamedTextColor.GRAY),
                inputs,
                actions,
                closeButton(plugin),
                1
        );
    }

    private static ActionButton reloadCustomSoundButton(
            ServerControlPlugin plugin,
            ControlState state,
            PlayerDialogAction reopen
    ) {
        return ActionButton.create(
                Component.text(MUSIC_DISC_ICON + "Reload Custom Sound", NamedTextColor.WHITE),
                Component.text("Reloads the configured MP3 and sends it to online verified clients.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> {
                    if (!ServerControlPermissions.hasAccess(player, ServerControlPermissions.RELOAD_DEATH_SOUND)) {
                        reopen.run(player);
                        return;
                    }

                    if (!plugin.customDeathSoundManager().reloadAndSync()) {
                        player.sendMessage(Component.text(
                                "No valid custom death sound MP3 was found at plugins/ServerControl/"
                                        + plugin.customDeathSoundManager().configuredSoundFile() + ".",
                                NamedTextColor.RED
                        ));
                    } else if (state.isCustomDeathSoundEnabled()) {
                        player.sendMessage(Component.text(
                                "Reloaded and sent the custom death sound to online verified clients.",
                                NamedTextColor.GREEN
                        ));
                    } else {
                        player.sendMessage(Component.text(
                                "Reloaded the custom death sound. Custom death sounds are off, so deaths will use the Iron Golem sound until enabled.",
                                NamedTextColor.YELLOW
                        ));
                    }
                    reopen.run(player);
                })
        );
    }

    private static ActionButton customDeathSoundVolumeButton(
            ServerControlPlugin plugin,
            ControlState state,
            PlayerDialogAction reopen
    ) {
        return ActionButton.create(
                Component.text(MUSIC_DISC_ICON + "Custom Sound Volume: "
                        + formatPercent(state.deathSoundVolumePercent()), NamedTextColor.WHITE),
                Component.text("Change the synced custom MP3 volume. This does not change the Iron Golem fallback volume.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> openCustomDeathSoundVolume(plugin, state, player, reopen))
        );
    }

    private static void openCustomDeathSoundVolume(
            ServerControlPlugin plugin,
            ControlState state,
            Player player,
            PlayerDialogAction reopen
    ) {
        List<ActionButton> actions = new ArrayList<>();
        actions.add(ActionButton.create(
                Component.text("Apply", NamedTextColor.GREEN),
                Component.text("Save the selected custom MP3 death sound volume.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                responseAction(plugin, true, (view, menuPlayer) -> {
                    if (!canChangeCustomDeathSoundVolume(menuPlayer)) {
                        reopen.run(menuPlayer);
                        return;
                    }

                    Float selected = view.getFloat(DEATH_SOUND_VOLUME_INPUT);
                    if (selected == null || !isValidVolumePercent(selected)) {
                        menuPlayer.sendMessage(Component.text(
                                "Use a custom sound volume from 0% to 100%.",
                                NamedTextColor.RED
                        ));
                        openCustomDeathSoundVolume(plugin, state, menuPlayer, reopen);
                        return;
                    }

                    state.setDeathSoundVolumePercent(selected);
                    plugin.saveStateAndBroadcast();
                    menuPlayer.sendMessage(Component.text(
                            "Set custom death sound volume to " + formatPercent(selected) + ".",
                            NamedTextColor.GREEN
                    ));
                    openCustomDeathSoundVolume(plugin, state, menuPlayer, reopen);
                })
        ));
        actions.add(ActionButton.create(
                Component.text("Back", NamedTextColor.WHITE),
                Component.text("Return to Settings.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, reopen)
        ));

        List<DialogInput> inputs = List.of(DialogInput.numberRange(
                        DEATH_SOUND_VOLUME_INPUT,
                        Component.text("Custom MP3 Volume Percent", NamedTextColor.WHITE),
                        0.0F,
                        100.0F
                )
                .width(BUTTON_WIDTH)
                .initial((float) state.deathSoundVolumePercent())
                .step(1.0F)
                .build());

        show(
                player,
                Component.text("Custom Sound Volume", NamedTextColor.AQUA),
                Component.text("This controls only the synced custom MP3. It still saves while custom sound is off and never changes the Iron Golem fallback volume.", NamedTextColor.GRAY),
                inputs,
                actions,
                closeButton(plugin),
                1
        );
    }

    private static ActionButton nakedAndAfraidButton(ServerControlPlugin plugin, ControlState state) {
        return ActionButton.create(
                Component.text(SWORD_ICON + "Naked And Afraid Mode", NamedTextColor.WHITE),
                Component.text("Open a confirmation menu for the Naked And Afraid preset.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> openNakedAndAfraidConfirm(plugin, state, player))
        );
    }

    private static void openNakedAndAfraidConfirm(ServerControlPlugin plugin, ControlState state, Player player) {
        if (!canApplyNakedAndAfraidMode(player)) {
            player.sendMessage(Component.text("You do not have permission to apply Naked And Afraid Mode.", NamedTextColor.RED));
            openSettings(plugin, state, player);
            return;
        }

        List<ActionButton> actions = new ArrayList<>();
        actions.add(ActionButton.create(
                Component.text("Cancel", NamedTextColor.RED),
                Component.text("Return to Settings without changing anything.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, menuPlayer -> openSettings(plugin, state, menuPlayer))
        ));
        actions.add(ActionButton.create(
                Component.text("Yes", NamedTextColor.GREEN),
                Component.text("Apply the Naked And Afraid preset to every ServerControl setting.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, menuPlayer -> {
                    if (!canApplyNakedAndAfraidMode(menuPlayer)) {
                        menuPlayer.sendMessage(Component.text(
                                "You do not have permission to apply Naked And Afraid Mode.",
                                NamedTextColor.RED
                        ));
                        openSettings(plugin, state, menuPlayer);
                        return;
                    }

                    state.applyNakedAndAfraidMode();
                    plugin.saveStateAndBroadcast();
                    menuPlayer.sendMessage(Component.text(
                            "Applied Naked And Afraid Mode.",
                            NamedTextColor.GREEN
                    ));
                    openSettings(plugin, state, menuPlayer);
                })
        ));

        show(
                player,
                Component.text(SWORD_ICON + "Naked And Afraid Mode", NamedTextColor.AQUA),
                Component.text("Are you sure you want to set all settings to Naked And Afraid?", NamedTextColor.GRAY),
                actions,
                closeButton(plugin),
                2
        );
    }

    private static ActionButton backButton(ServerControlPlugin plugin, ControlState state) {
        return ActionButton.create(
                Component.text("Back", NamedTextColor.WHITE),
                Component.text("Return to the main ServerControl menu.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, true, player -> openMain(plugin, state, player))
        );
    }

    private static ActionButton closeButton(ServerControlPlugin plugin) {
        return ActionButton.create(
                Component.text("Close", NamedTextColor.WHITE),
                Component.text("Close this ServerControl menu.", NamedTextColor.GRAY),
                BUTTON_WIDTH,
                playerAction(plugin, false, Audience::closeDialog)
        );
    }

    private static DialogAction playerAction(ServerControlPlugin plugin, boolean requiresGuiAccess, PlayerDialogAction action) {
        return DialogAction.customClick((view, audience) -> {
            if (!(audience instanceof Player player)) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (requiresGuiAccess && !ServerControlPermissions.canUseGui(player)) {
                    player.closeDialog();
                    player.sendMessage(Component.text("You do not have permission to use the ServerControl GUI.", NamedTextColor.RED));
                    return;
                }

                action.run(player);
            });
        }, CALLBACK_OPTIONS);
    }

    private static DialogAction responseAction(
            ServerControlPlugin plugin,
            boolean requiresGuiAccess,
            DialogResponseAction action
    ) {
        return DialogAction.customClick((view, audience) -> {
            if (!(audience instanceof Player player)) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (requiresGuiAccess && !ServerControlPermissions.canUseGui(player)) {
                    player.closeDialog();
                    player.sendMessage(Component.text("You do not have permission to use the ServerControl GUI.", NamedTextColor.RED));
                    return;
                }

                action.run(view, player);
            });
        }, CALLBACK_OPTIONS);
    }

    private static boolean hasAnyFeatureAccess(Player player, List<FeatureEntry> entries) {
        for (FeatureEntry entry : entries) {
            if (ServerControlPermissions.hasFeatureAccess(player, entry.feature())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDeathCategoryAccess(Player player) {
        return hasAnyFeatureAccess(player, DEATH_FEATURES)
                || ServerControlPermissions.hasAccess(player, ServerControlPermissions.ARMOR_DAMAGE_AMOUNT)
                || ServerControlPermissions.hasAccess(player, ServerControlPermissions.RELOAD_DEATH_SOUND);
    }

    private static boolean hasSettingsAccess(Player player) {
        return ServerControlPermissions.canUseSettings(player);
    }

    private static boolean canChangeCustomDeathSoundVolume(Player player) {
        return ServerControlPermissions.canUseSettings(player);
    }

    private static boolean canApplyNakedAndAfraidMode(Player player) {
        return ServerControlPermissions.canUseSettings(player);
    }

    private static boolean isValidDamageAmount(float value) {
        if (value < 0.0F || value > 5.0F) {
            return false;
        }
        return Math.abs((value * 2.0F) - Math.round(value * 2.0F)) < 0.0001F;
    }

    private static boolean isValidVolumePercent(float value) {
        return value >= 0.0F && value <= 100.0F;
    }

    private static String formatDamageAmount(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return Double.toString(value);
    }

    private static String formatPercent(double value) {
        if (value == Math.rint(value)) {
            return (int) value + "%";
        }
        return value + "%";
    }

    @FunctionalInterface
    private interface PlayerDialogAction {
        void run(Player player);
    }

    @FunctionalInterface
    private interface DialogResponseAction {
        void run(DialogResponseView view, Player player);
    }

    private record FeatureEntry(
            ControlFeature feature,
            String label,
            String summary,
            String enabledDescription,
            String disabledDescription
    ) {
    }
}
