# Getting Started

HyExtras is a **modular creator and developer toolkit for Hytale servers**. It extends the native
Trigger Volume system with extra effects and conditions, adds per-player state (variables, persistent
tags, cooldowns), packet-backed features (visibility, camera, titles), and several opt-in systems
(TagNPC, Floating Items, Image Icons). It does **not** replace or shadow native Trigger Volume
functionality, and it is not affiliated with or endorsed by Hypixel Studios.

## Compatibility

| | |
|---|---|
| Server API line | Hytale `0.5.x` |
| Built/tested against | `0.5.6` |
| Manifest range | `>=0.5.0 <0.6.0` |
| HyExtras version | `1.0.2` |
| Java | 25 |

Future `0.6.x` releases should be treated as a new compatibility pass because server APIs may change.

## Install

1. Download or build `hyextras-<version>.jar` (see [Build from source](#build-from-source)).
2. Drop it into your server's `mods/` folder.
3. Start the server. On first run HyExtras writes `hyextras.properties` with documented defaults — see
   [Configuration](configuration).
4. (Optional) Install [PlaceholderAPI for Hytale](string-templates#placeholderapi) to enable
   `%placeholder%` resolution.

## Build from source

```bash
./gradlew shadowJar     # build the fat jar into build/libs/
./gradlew deployMod     # build and copy into .hytale-server/mods/
```

The build compiles against Hytale Server API `0.5.6` while the packaged manifest declares the broader
`0.5.x` range. See [Architecture](architecture) for the project layout.

---

## Core concepts

### Trigger Volumes
Trigger Volumes are a **native** Hytale feature: regions that fire `effects` when something happens
(a player enters, a tag changes, etc.), optionally gated by `conditions`. HyExtras registers extra
effect and condition **types** you can drop into any volume alongside the native ones.

### Effects vs. Conditions
- **[Effects](effects)** *do* something (set a variable, send a title, hide a player…).
- **[Conditions](conditions)** *gate* effects — all conditions on a volume must pass or the effects
  are skipped (and rejection effects may run instead).

### Event types
Every effect/condition has an `eventType` telling the volume *when* it applies. Common values:

| Event | Fires when |
|---|---|
| `ENTER` | An entity enters the volume. |
| `EXIT` | An entity leaves the volume. |
| `TAG_ADDED` | A tag is added to the volume (also used by the [interaction bridge](interaction-bridge)). |
| `TAG_REMOVED` | A tag is removed from the volume. |

### The JSON shape
Effects and conditions are JSON objects with a `type`, an `eventType`, and type-specific fields:

```json
{ "type": "send_title", "eventType": "ENTER", "Title": "Welcome, {player}!" }
```

Field names are case-sensitive and match the [Effects](effects) / [Conditions](conditions)
reference exactly. Text fields support [String Templates](string-templates).

### Per-player state
HyExtras tracks state per player, with different lifetimes (full details in
[Player State](player-state)):

| State | Lifetime |
|---|---|
| Variables | Runtime only — cleared on disconnect. |
| Cooldowns | Runtime only — cleared on disconnect. |
| Visibility overrides | Runtime only — cleared on disconnect. |
| Tags | **Persistent** — saved to disk, survive restarts. |

---

## Your first volume

A "hidden ruins" zone that greets the player once every 5 minutes and tracks a visit counter:

```json
{
  "conditions": [
    { "type": "cooldown_ready", "eventType": "ENTER", "Name": "ruins_intro" }
  ],
  "effects": [
    { "type": "apply_cooldown",  "eventType": "ENTER", "Name": "ruins_intro", "Duration": 300.0 },
    { "type": "add_variable",    "eventType": "ENTER", "Key": "ruins_visits", "Delta": 1 },
    { "type": "send_title",      "eventType": "ENTER", "Title": "The Hidden Ruins", "Subtitle": "Visit #{variable:ruins_visits}" }
  ]
}
```

What happens on enter:
1. `cooldown_ready` passes only if `ruins_intro` isn't active.
2. `apply_cooldown` starts a 5-minute cooldown so this won't re-fire immediately.
3. `add_variable` bumps the per-player visit counter.
4. `send_title` greets the player, substituting the counter via a [placeholder](string-templates).

From here, explore the [Effects](effects) and [Conditions](conditions) reference, then the
[system deep-dives](home#systems) for visibility, the interaction bridge, TagNPC, and more.

---

## Native vs. HyExtras

HyExtras intentionally does **not** duplicate features the native Trigger Volume system already
provides. Prefer the native types for these:

- **Native effects:** `SendMessage`, `Teleport`, `GiveItem`, `PlaySound`, `ShowEventTitle`,
  `SetGameMode`, `DamageEntity`, `SetVelocity`, `PlaceBlock`, `SetWeather`, `PlayVfx`, `PastePrefab`,
  `ControlDoors`, `EnableVolume`, `DisableVolume`, `DeleteVolume`, `ModifyTags`, `ReplaceBlockType`,
  `RunRootInteraction`, `EntityEffect`, `SetMusic`, `TriggerNpcMarkers`, `VolumeState`.
- **Native conditions:** `Permission`, `Cooldown`, `RandomChance`, `Tag`, `Item`, `BlockType`,
  `GameMode`, `PlayerCount`.

HyExtras adds what the native system doesn't: per-player variables/tags/cooldowns, cross-volume tag
messaging, rule-driven player visibility, camera control, the interaction bridge, TagNPC, floating
items, image icons, and a developer API. See [What HyExtras adds](home#at-a-glance).

## Next steps

- [Effects](effects) · [Conditions](conditions) · [Commands](commands) · [Configuration](configuration)
- [Documentation index](home) — every guide in one place.
- [Developer API](developer-api) — drive HyExtras from another mod.
