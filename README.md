# HyExtras

**A modular creator and developer toolkit for Hytale servers.** HyExtras extends the native Trigger
Volume system with extra effects and conditions, per-player state, packet-backed features, and several
opt-in systems — all toggleable so server owners only run what they need.

> HyExtras does **not** replace, shadow, or compete with any native Trigger Volume functionality, and
> it is not affiliated with or endorsed by Hypixel Studios.

| | |
|---|---|
| **Version** | 1.0.2 |
| **Target** | Hytale Server `0.5.x` (built/tested on `0.5.6`, manifest `>=0.5.0 <0.6.0`) |
| **Author** | Alphine |

📖 **[Full documentation →](docs/README.md)** · 🚀 **[Getting Started →](docs/getting-started.md)** ·
🧩 **[Developer API →](docs/DEVELOPER_API.md)**

---

## Highlights

HyExtras is split into six [internal modules](docs/systems/modules.md) you can enable independently:

- **TriggerExtras** — 41 [effects](docs/effects.md) and 14 [conditions](docs/conditions.md) for
  per-player variables, persistent tags, cooldowns, messaging, rewards, cross-volume tags, and the
  interaction bridge.
- **Per-player visibility & packets** — hide/show players with rule-driven targeting, volume
  visibility policy, camera control, titles/action bars, and NPC targeting protection.
- **TagNPC** — runtime tags, variables, and visibility for UUID-backed NPCs/mobs.
- **Floating Items** — decorative, non-pickup item displays.
- **Image Icons** — provider-scoped local/remote PNG/GIF icons for developer mods.
- **PlaceholderAPI bridge** — `%placeholder%` support in HyExtras text.

Plus a stable **[`HyExtrasApi`](docs/DEVELOPER_API.md)** facade and a full
**[`/hextras`](docs/commands.md)** admin command tree.

---

## At a glance

### Effects ([full reference](docs/effects.md))

| Category | Type IDs |
|---|---|
| Variables & state | `set_variable`, `add_variable`/`increment_variable`, `calculate_variable`, `remove_variable` |
| Cooldowns | `apply_cooldown` |
| Commands | `run_command`, `run_reward_command` |
| Messaging | `send_title`, `send_rich_message`, `send_reward_message` |
| Items | `give_item_reward`, `remove_item` |
| Camera / voice / physics | `set_camera`, `set_voice_activity`, `push_back_player` |
| Player visibility | `player_hide_entity`, `player_show_entity`, `clear_player_overrides` |
| Volume control & tags | `trigger_named_volume`, `toggle_trigger_enabled`, `set_volume_tag`, `remove_volume_tag` |
| Interaction | `cancel_interaction`, `block_volume_interactions`, `allow_volume_interactions`, `set_volume_interactable`, `clear_volume_interactable` |
| Persistent tags | `add_tag`, `remove_tag` |
| TagNPC | `tag_npc_add_tag`, `tag_npc_remove_tag`, `tag_npc_set_variable`, `tag_npc_add_variable`, `tag_npc_remove_variable`, `tag_npc_hide_entity`, `tag_npc_show_entity` |
| Floating items | `floating_item_create`, `floating_item_move`, `floating_item_remove`, `floating_item_set_intangible` |

### Conditions ([full reference](docs/conditions.md))

`variable_condition` · `cooldown_ready` · `has_tag` · `math_condition` · `is_operator` ·
`player_hidden` · `world_time_between` · `interaction_type` · `volume_has_tag` · `tag_npc_has_tag` ·
`tag_npc_variable_condition` · `tag_npc_visible_condition` · `floating_item_exists` ·
`floating_item_intangible`

---

## Install

1. Drop `hyextras-<version>.jar` into your server's `mods/` folder.
2. Start the server — HyExtras writes [`hyextras.properties`](docs/configuration.md) with documented
   defaults on first run.
3. (Optional) Install PlaceholderAPI for Hytale to enable `%placeholder%` resolution.

## Quick start

A "hidden ruins" zone that greets the player once every 5 minutes and counts visits:

```json
{
  "conditions": [
    { "type": "cooldown_ready", "eventType": "ENTER", "Name": "ruins_intro" }
  ],
  "effects": [
    { "type": "apply_cooldown", "eventType": "ENTER", "Name": "ruins_intro", "Duration": 300.0 },
    { "type": "add_variable",   "eventType": "ENTER", "Key": "ruins_visits", "Delta": 1 },
    { "type": "send_title",     "eventType": "ENTER", "Title": "The Hidden Ruins", "Subtitle": "Visit #{variable:ruins_visits}" }
  ]
}
```

See the [Getting Started](docs/getting-started.md) guide for a full walkthrough, and
[`examples/hyextras-examples.json`](examples/hyextras-examples.json) for more snippets.

---

## Documentation

| Doc | Contents |
|---|---|
| [Getting Started](docs/getting-started.md) | Install, concepts, and a first volume. |
| [Effects](docs/effects.md) | All 41 trigger effects with parameters and examples. |
| [Conditions](docs/conditions.md) | All 14 trigger conditions. |
| [Commands](docs/commands.md) | The full `/hextras` command tree. |
| [Configuration](docs/configuration.md) | Every `hyextras.properties` key. |
| [String Templates](docs/string-templates.md) | Placeholders, rule predicates, color codes. |
| [Systems](docs/README.md#systems) | Deep-dives: [player state](docs/systems/player-state.md), [visibility & packets](docs/systems/visibility-and-packets.md), [interaction bridge](docs/systems/interaction-bridge.md), [TagNPC](docs/systems/tag-npc.md), [floating items](docs/systems/floating-items.md), [image icons](docs/systems/image-icons.md), [modules](docs/systems/modules.md). |
| [Developer API](docs/DEVELOPER_API.md) | The stable `HyExtrasApi` facade. |
| [Architecture](docs/architecture.md) & [Internals](docs/internals/services.md) | Contributor reference. |

---

## Developer API

Other server mods can use the stable `HyExtrasApi` facade for variables, tags, cooldowns, visibility,
camera/title helpers, rules, image icons, TagNPC, and floating items:

```java
HyExtrasApi api = HyExtrasApi.get();
api.setVariableString(playerUuid, "partyId", "alpha");
api.addTag(playerUuid, "storyline_a_active");
api.hidePlayerFrom(viewerUuid, targetUuid, true);
api.sendActionBar(playerUuid, "Party: " + api.getVariableString(playerUuid, "partyId"));
```

See the [Developer API reference](docs/DEVELOPER_API.md) for the full method list.

---

## Building

```bash
./gradlew shadowJar     # build the fat jar into build/libs/
./gradlew deployMod     # build and copy into .hytale-server/mods/
./gradlew cleanDeploy   # clean, rebuild, and deploy
```

Built with Gradle against Hytale Server API `0.5.6`, Java 25. The packaged manifest declares the
broader `0.5.x` range.

### Running

Use the included IDE run configurations:

- **Run Hytale Server** — builds, deploys, and starts the server.
- **Debug Hytale Server** — same, with a remote debugger on port 5005.
- **Build Mod** — compiles without deploying or starting.

---

## License & credits

Authored by **Alphine**, **Krystian Carter**, and **Hyzion Studios**. HyExtras is source-available for
non-commercial use under the custom terms in [LICENSE](LICENSE). HyExtras is an independent community project and is
not affiliated with or endorsed by Hypixel Studios; "Hytale" is a trademark of its respective owner.
