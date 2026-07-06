# Minecraft Naked And Afraid

PaperMC plugin for Minecraft Java Edition 1.21.11.

Install the Paper jar in the server `plugins` folder.

## Getting started

To begin, make sure you are an operator and type `/na gui`. From there, it should be obvious, and to use the deafult, open settings and click the Naked and afraid deafult

## Commands

```text
/nakedandafraid gui
/nakedandafraid status
/nakedandafraid reload
/nakedandafraid chat on
/nakedandafraid chat off
/nakedandafraid tab on
/nakedandafraid tab off
/nakedandafraid death off
/nakedandafraid join off
/nakedandafraid quit off
/nakedandafraid advancement off
```

also use: `/na`

## Deafult Configuration in the config file

The Paper plugin creates `plugins/NakedAndAfraid/config.yml`:

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

mechanics:
  spectators-can-see-tab-list: false
  armor-damage-hearts-per-second: 1.0
  armor-damage-items:
    mob-heads: false
    carved-pumpkins: false
    elytras: false
```

## Advanced stuff... for the nerds lol!

Permissions use the `nakedandafraid` prefix:

```text
nakedandafraid.all
nakedandafraid.command
nakedandafraid.gui
nakedandafraid.settings
nakedandafraid.command.status
nakedandafraid.command.reload
nakedandafraid.command.chat
nakedandafraid.command.tab
nakedandafraid.command.death
nakedandafraid.command.join
nakedandafraid.command.quit
nakedandafraid.command.advancement
nakedandafraid.command.armor-damage
nakedandafraid.command.armor-damage-amount
nakedandafraid.command.death-sound
```

Operators have access by default. Non-operators need `nakedandafraid.gui` to open the GUI and only see controls for permissions they have.

## Notes

Chat, death messages, join messages, quit messages, advancement messages, armor damage, tab hiding, and death sounds are all enforced server-side. The universal death sound is the Iron Golem death sound.
