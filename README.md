# Minecraft ServerControl

ServerControl is a paired Minecraft Java Edition 1.21.11 project:

- `fabric-client`: Fabric client mod that obeys server-authoritative restrictions and blocks the tab list locally.
- `paper-plugin`: Paper plugin that owns the settings, enforces server-side chat/message rules, verifies the client mod during a short protected join window, and syncs live updates.

## Project Structure

```text
.
+-- fabric-client/   # Fabric 1.21.11 client companion mod
+-- paper-plugin/    # PaperMC 1.21.11 server plugin
+-- docs/            # Wire protocol notes
```

## Build

Use JDK 21 or newer and Gradle 9.2+.

```bash
gradle build
```

Artifacts:

```text
fabric-client/build/libs/servercontrol-fabric-client-1.0.0.jar
paper-plugin/build/libs/servercontrol-paper-1.0.0.jar
```

Install the Fabric jar on every client, and install the Paper jar in the server `plugins/` folder. Update both jars together. The handshake requires the same protocol and exact ServerControl release version on both sides, so older client jars will not connect to newer server plugin jars, and newer client jars will not connect to older server plugin jars.

## Configuration

The Paper plugin creates `plugins/ServerControl/config.yml`:

```yaml
features:
  tab-list: true
  public-chat: true
  death-messages: true
  join-messages: true
  quit-messages: true
  advancement-messages: true
  armor-damage: false
  death-sound: false
  custom-death-sound: false

mechanics:
  # Hearts per second, 0 to 5 in 0.5-step increments.
  armor-damage-hearts-per-second: 1.0
  armor-damage-items:
    mob-heads: false
    carved-pumpkins: false
    elytras: false
  death-sound-volume-percent: 100.0
  custom-death-sound-file: "sounds/death.mp3"
  custom-death-sound-chunk-bytes: 16000
  custom-death-sound-chunks-per-tick: 4

client-assets:
  server-id: ""

handshake:
  timeout-millis: 5000
  kick-message: "<red>This server requires the ServerControl Fabric client mod.</red>"
```

Message/tab feature values use normal on/off semantics. For example, `public-chat: false` blocks normal player chat, while operator commands and `/say` still work. Gameplay effects like `armor-damage`, `death-sound`, and `custom-death-sound` are active when set to `true`. Mob heads, carved pumpkins, and elytras have separate armor-damage toggles in the Death Options GUI; they are forced off while `armor-damage` is off. Custom death sounds can only be toggled while `death-sound` is on; when custom death sounds are off, ServerControl plays the default Iron Golem death sound. `death-sound-volume-percent` controls the custom MP3 only; the Iron Golem fallback always plays at normal volume.

To replace the Iron Golem death sound with a custom sound, put an MP3 file at `plugins/ServerControl/sounds/death.mp3` and use the GUI reload button. MP4/AAC files are not decoded by the bundled client player; if one is uploaded, or if the file is larger than the 20 MB client transfer limit, the plugin falls back to the vanilla Iron Golem death sound and tells the GUI user when no valid MP3 is found. The plugin watches the file every few seconds and can send replacements to connected Fabric clients immediately when reloaded. The generated `client-assets.server-id` keeps the client's cached sound tied to this server even if the IP changes. The custom client sound ignores Minecraft category sliders and only respects Master Volume being set to 0/off. Operators can also use the GUI Settings page for custom MP3 volume and the Naked And Afraid preset.

## Commands

```text
/servercontrol gui
/servercontrol status
/servercontrol reload
/servercontrol chat on
/servercontrol chat off
/servercontrol tab on
/servercontrol tab off
/servercontrol death off
/servercontrol join off
/servercontrol quit off
/servercontrol advancement off
```

Operators have full access by default. Non-operators need `servercontrol.gui` to open the dialog GUI; inside the GUI they only see buttons for the existing feature permissions they have. If they only have the GUI permission and no feature permissions, the GUI opens with no setting buttons. `servercontrol.settings` gives access to every option in the Settings GUI page, including custom MP3 volume and Naked And Afraid Mode. Manual commands are intentionally limited to the examples above; the remaining permissions expose GUI controls only:

```text
servercontrol.all
servercontrol.command
servercontrol.gui
servercontrol.settings
servercontrol.command.status
servercontrol.command.reload
servercontrol.command.chat
servercontrol.command.tab
servercontrol.command.death
servercontrol.command.join
servercontrol.command.quit
servercontrol.command.advancement
servercontrol.command.armor-damage
servercontrol.command.armor-damage-amount
servercontrol.command.death-sound
servercontrol.command.custom-death-sound
servercontrol.command.reload-death-sound
```

## Enforcement

The Fabric client announces itself on `servercontrol:main` as soon as the connection enters play. The Paper plugin replies with a nonce challenge and requires the same nonce and exact ServerControl release version back before marking the player verified. While the player is unverified, the join message is hidden, the player list is isolated, and movement, chat, commands, interaction, item drops, block changes, and attacks are blocked; if no valid response arrives before the timeout, the player is disconnected.

Chat, death messages, join messages, quit messages, and advancement messages are enforced server-side. Advancement awards still appear in the client advancements screen; only the chat announcement is suppressed. Armor damage and death sound are also server-side so they behave correctly in Hardcore worlds. Tab list access is enforced client-side after a successful handshake, with the server as the source of truth. The client drains the configured player-list keybind every tick and mixins block `PlayerListHud#setVisible(true)` and `PlayerListHud#render`, so rebinding the key or trying to open the overlay indirectly does not bypass the restriction.

See [docs/protocol.md](docs/protocol.md) for packet layout.

## Troubleshooting

On startup, the client log should include:

```text
ServerControl client companion 1.0.0 loaded
```

When joining a ServerControl server, a successful play handshake logs:

```text
Announced ServerControl client to server
Acknowledged ServerControl play handshake
```

The Paper console logs successful play-handshake verification. If the player is rejected, the console includes the client channels Paper saw during the verification window.
