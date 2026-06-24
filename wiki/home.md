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
| **Source** | [github.com/L8-Alphine/HytaleExtras](https://github.com/L8-Alphine/HytaleExtras) |

New here? Start with **[Getting Started](getting-started)**.

---

## Highlights

HyExtras is split into six [internal modules](modules) you can enable independently:

- **TriggerExtras** — 41 [effects](effects) and 14 [conditions](conditions) for per-player variables,
  persistent tags, cooldowns, messaging, rewards, cross-volume tags, and the
  [interaction bridge](interaction-bridge).
- **Per-player visibility & packets** — hide/show players with rule-driven targeting, volume
  visibility policy, camera control, titles/action bars, and NPC targeting protection.
- **TagNPC** — runtime tags, variables, and visibility for UUID-backed NPCs/mobs.
- **Floating Items** — decorative, non-pickup item displays.
- **Image Icons** — provider-scoped local/remote PNG/GIF icons for developer mods.
- **PlaceholderAPI bridge** — `%placeholder%` support in HyExtras text.

Plus a stable **[`HyExtrasApi`](developer-api)** facade and a full **[`/hextras`](commands)** admin
command tree.

---

## At a glance

### Effects ([full reference](effects))

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

### Conditions ([full reference](conditions))

`variable_condition` · `cooldown_ready` · `has_tag` · `math_condition` · `is_operator` ·
`player_hidden` · `world_time_between` · `interaction_type` · `volume_has_tag` · `tag_npc_has_tag` ·
`tag_npc_variable_condition` · `tag_npc_visible_condition` · `floating_item_exists` ·
`floating_item_intangible`

---

## Documentation

### Start here
- [Getting Started](getting-started) — install, core concepts, and a first volume.

### Reference
- [Effects](effects) — all 41 trigger effects with parameters and examples.
- [Conditions](conditions) — all 14 trigger conditions.
- [Commands](commands) — the full `/hextras` command tree.
- [Configuration](configuration) — every `hyextras.properties` key.
- [String Templates](string-templates) — placeholders, rule predicates, color codes.

### Systems
- [Player State](player-state) — variables, persistent tags, cooldowns.
- [Visibility & Packets](visibility-and-packets) — per-player view, camera, targeting, volume policy.
- [Interaction Bridge](interaction-bridge) — interaction events, blocking, interactable volumes.
- [TagNPC](tag-npc) — runtime entity state.
- [Floating Items](floating-items) — decorative item displays.
- [Image Icons](image-icons) — provider-scoped icons.
- [Modules](modules) — the six internal modules.

### Developers
- [Developer API](developer-api) — the stable `HyExtrasApi` facade.
- [Architecture](architecture) — package map, lifecycle, data flow.
- [Internals: Services](internals-services) · [Internals: TriggerExtras](internals-trigger-extras) · [Internals: Packet Stack](internals-packet-stack)
- [Trigger Volume Research](trigger-volume-research) — native Hytale API notes.

---

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

See [Getting Started](getting-started) for the full walkthrough.
