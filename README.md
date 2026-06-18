# HyExtras

HyExtras is an unofficial server mod for Hytale that **extends** the native Trigger Volume system with new effects and conditions.
It does **not** replace, shadow, or compete with any native Trigger Volume functionality, and it is not affiliated with or endorsed by Hypixel Studios.

## Compatibility

HyExtras targets the Hytale `0.5.x` server API line.

- Built and tested against `0.5.6`
- Manifest server range: `>=0.5.0 <0.6.0`
- Future `0.6.x` releases should be treated as a new compatibility pass because server APIs may change

## What HyExtras adds

### New Effects (Actions)

| Type ID | Description |
|---|---|
| `run_command` | Run a command as the triggering player with `{player}`, `{uuid}`, `{variable:key}` substitution |
| `set_variable` | Set a per-player named variable to a string value |
| `add_variable` | Add to a numeric per-player variable; alias of `increment_variable` |
| `increment_variable` | Increment (or decrement) a numeric per-player variable |
| `remove_variable` | Remove a per-player variable |
| `apply_cooldown` | Apply a named HyExtras cooldown (separate from the native volume cooldown) |
| `trigger_named_volume` | Dispatch another named volume against the current entity, including its matching conditions, rejection effects, effects, enabled state, and cooldown |
| `remove_item` | Remove item(s) from a player's inventory, hotbar, or active in-hand slot by item ID |
| `send_title` | Send a title/subtitle via `ShowEventTitle` packet; `ActionBar` sends a `Notification` packet; supports placeholders and color codes |
| `send_rich_message` | Send a raw chat message to the triggering player with placeholders and color/style codes |
| `toggle_trigger_enabled` | Enable, disable, or toggle another named volume (`Mode`: `toggle`/`enable`/`disable`) |
| `player_hide_entity` | Hide `TargetPlayer` from the triggering player's view (server-side tracking + optional packet) |
| `player_show_entity` | Restore visibility of `TargetPlayer` for the triggering player |
| `clear_player_overrides` | Reset all per-player visibility overrides for the triggering player |
| `set_volume_tag` | Set a tag on another named volume; fires `TAG_ADDED` on the target volume for the current entity |
| `remove_volume_tag` | Remove a tag from another named volume; fires `TAG_REMOVED`. Optional `Value` guards the remove |
| `cancel_interaction` | Cancel the native interaction when inside the HyExtras interaction bridge (see below) |
| `add_tag` | Add a boolean tag to the triggering player's **persistent** tag set (survives reconnects) |
| `remove_tag` | Remove a boolean tag from the triggering player's persistent tag set |
| `block_volume_interactions` | Block all interactions for players inside a volume; optional `Mode` (`enable`/`disable`/`toggle`) and `VolumeId` |
| `allow_volume_interactions` | Mark a sub-volume as an interaction override inside a blocked area; same options as above |
| `set_camera` | Switch the triggering player's camera: `Mode` = `first_person` / `third_person` / `reset`; optional `Locked` bool |

### New Conditions

| Type ID | Description |
|---|---|
| `variable_condition` | Test a per-player variable against a value (`equals`, `not_equals`, `greater_than`, `less_than`, `exists`, `not_exists`) |
| `cooldown_ready` | Pass only when a named HyExtras cooldown is not active |
| `is_operator` | Pass if the triggering player is in Hytale's operator group; optional custom `Permission` and `Invert` |
| `world_time_between` | Pass if the world hour (0–23) is within `[FromHour, ToHour]`; supports midnight wrap-around |
| `player_hidden` | Pass if the triggering player currently has `TargetPlayer` hidden from their view |
| `volume_has_tag` | Pass if a named volume has (or lacks) a tag key, optionally matching an exact value |
| `has_tag` | Pass if the triggering player has (or lacks) a persistent tag; optional `Invert` |

### Commands

```
/hextras var get <player> <key>          — read a player variable
/hextras var set <player> <key> <value>  — set a player variable
/hextras var add <player> <key> <amount> — add to a numeric player variable
/hextras var increment <player> <key> <amount> — same as add
/hextras var del <player> <key>          — delete a player variable
/hextras var list <player>               — list all variables for a player
/hextras tag add <player> <tag>          — add a persistent player tag
/hextras tag remove <player> <tag>       — remove a persistent player tag
/hextras tag set <player> <tag> <bool>   — set a persistent player tag true/false
/hextras tag has <player> <tag>          — check a player tag
/hextras tag list <player>               — list player tags
/hextras tag clear <player>              — remove all player tags
/hextras cooldown check <player> <name>  — show remaining cooldown time
/hextras cooldown clear <player> <name>  — clear a cooldown
/hextras list actions                    — list all registered extra effect type IDs
/hextras list conditions                 — list all registered extra condition type IDs
/hextras debug player <player>           — show variables, cooldowns, tags, and hidden-entity state
/hextras reload                          — reload hyextras.properties at runtime
```

### Configuration (`hyextras.properties`)

```properties
# Enable per-player entity visibility packet sends (player_hide_entity, player_show_entity, etc.)
advancedPacketActions=true

# Print verbose debug info to the server log
debugMode=false
```

### What is already native (not duplicated)

The native Trigger Volume system already includes:
- **Effects**: SendMessage, Teleport, GiveItem, PlaySound, ShowEventTitle, SetGameMode, DamageEntity, SetVelocity, PlaceBlock, SetWeather, PlayVfx, PastePrefab, ControlDoors, EnableVolume, DisableVolume, DeleteVolume, ModifyTags, ReplaceBlockType, RunRootInteraction, EntityEffect, SetMusic, TriggerNpcMarkers, VolumeState
- **Conditions**: Permission, Cooldown, RandomChance, Tag, Item, BlockType, GameMode, PlayerCount

### Cross-Volume Tag Communication

`set_volume_tag` calls `TriggerVolumeManager.setTag()` which fires a native `TAG_ADDED` event on the target volume for the current entity. `remove_volume_tag` calls `removeTag()` which fires `TAG_REMOVED`. This is the intended native mechanism for volume-to-volume messaging.

The `volume_has_tag` condition reads `VolumeEntry.getRawTags()` — this includes both static tags set in the editor and runtime tags set via `set_volume_tag` / native `ModifyTags`.

### Interaction Bridge

The HyExtras interaction bridge fires volume effects when a player interacts with something while inside a volume. To enable it on a volume:

1. Add static tag `hextras:interact` to the volume in the editor (any value, e.g. `1`).
2. Add conditions and effects to the volume with event type **`TAG_ADDED`**.
   - Conditions are evaluated first; all must pass or effects are skipped.
   - The synthetic context has `tagKey = "hextras_interact"`, `tagValue = <InteractionType name>` (e.g. `Use`, `Primary`, `Secondary`).
3. To cancel the interaction (block the door/chest/entity from responding), add `cancel_interaction` to the effects.

The bridge uses `registerGlobal` so it fires for any player interacting in any world. Because it dispatches synchronously before the native interaction resolves, `cancel_interaction` is guaranteed to run in time.

**InteractionType dropdown values** for the HyExtras `interaction_type` condition: `primary`, `secondary`, `ability1`, `ability2`, `ability3`, `use`, `pick`, `pickup`, `collision_enter`, `collision_leave`, `collision`, `swap_to`, `swap_from`, `death`.

---

## Example configs

### Send a title on volume enter
```json
{ "type": "send_title", "eventType": "ENTER", "Title": "Welcome, {player}!", "Subtitle": "Zone: spawn", "Duration": 3.0 }
```

### Send a notification (action bar style)
```json
{ "type": "send_title", "eventType": "ENTER", "ActionBar": "Score: {variable:score}" }
```

### Send a colored chat message
```json
{ "type": "send_rich_message", "eventType": "ENTER", "Message": "&aWelcome back {player}&r to the game." }
```

### Track which zone a player is in
```json
{ "type": "set_variable", "eventType": "ENTER", "Key": "zone", "Value": "ruins" }
```

### Add to a numeric variable
```json
{ "type": "add_variable", "eventType": "ENTER", "Key": "score", "Delta": 1 }
```

### Gate an effect behind a cooldown
```json
[
  { "type": "cooldown_ready", "eventType": "ENTER", "Name": "ruins_intro" },
  { "type": "apply_cooldown", "eventType": "ENTER", "Name": "ruins_intro", "Duration": 300.0 },
  { "type": "send_title",     "eventType": "ENTER", "Title": "You entered the hidden ruins." }
]
```

### Chain into another volume
```json
{ "type": "trigger_named_volume", "eventType": "ENTER", "VolumeId": "reward_fanfare_volume" }
```

### Enable/disable a volume dynamically
```json
{ "type": "toggle_trigger_enabled", "eventType": "ENTER", "VolumeId": "boss_arena", "Mode": "disable" }
```

### Hide/show players for solo-instance illusion
```json
[
  { "type": "player_hide_entity", "eventType": "ENTER", "TargetPlayer": "OtherPlayer", "UsePackets": true },
  { "type": "player_hidden",      "eventType": "ENTER", "TargetPlayer": "OtherPlayer" }
]
```

### Clear visibility overrides on exit
```json
{ "type": "clear_player_overrides", "eventType": "EXIT" }
```

### World-time-gated effect (daytime only)
```json
{ "type": "world_time_between", "FromHour": 6, "ToHour": 18 }
```

### Player Tags (Persistent)

`add_tag` and `remove_tag` store boolean flags per-player that **survive server restarts**. Tags are saved to `{dataDir}/players/{uuid}.tags` on disconnect and loaded on connect.

They are distinct from variables: tags are flags (present / absent), not values. Use them for storyline progress, quest state, achievement gates, etc.

```json
// Mark storyline progress on volume enter
{ "type": "add_tag", "eventType": "ENTER", "Tag": "storyline_a_active" }

// Require player has the tag to proceed
{ "type": "has_tag", "eventType": "ENTER", "Tag": "storyline_a_active" }

// Invert — pass only if player does NOT have the tag
{ "type": "has_tag", "eventType": "ENTER", "Tag": "storyline_a_active", "Invert": true }
```

`/hextras debug player <name>` shows the current tag set alongside variables and cooldowns.

> **Note on mob/entity visibility:** In the current `0.5.x` API, there is no per-player mechanism to hide non-player entities (mobs, NPCs). `HiddenFromAdventurePlayers` hides from all adventure players, not a single viewer. `player_hide_entity` / `player_show_entity` only work player-to-player. Use `has_tag` conditions plus spawning/despawning prefabs (native `PastePrefab` + `DeleteVolume`) as the best available workaround for storyline-specific mob visibility.

### Volume Interaction Blocking

Block all player interactions inside a volume at runtime, with sub-volume overrides:

1. Fire `block_volume_interactions` from any volume's effect (or trigger via another effect). The named volume (or current volume if omitted) is added to a blocked set.
2. Players inside a blocked volume have their `PlayerInteractEvent` cancelled — they cannot interact with anything.
3. Place a smaller sub-volume inside the blocked area and fire `allow_volume_interactions` from it. Players inside the allowed sub-volume bypass the block.
4. Use `Mode: "toggle"` to alternate between blocked and unblocked state.

```json
// Lock the dungeon door area
{ "type": "block_volume_interactions", "eventType": "ENTER", "VolumeId": "dungeon_main" }

// But keep the key-hole sub-volume interactable
{ "type": "allow_volume_interactions", "eventType": "ENTER", "VolumeId": "keyhole_alcove" }

// Toggle the block with a lever volume
{ "type": "block_volume_interactions", "VolumeId": "dungeon_main", "Mode": "toggle" }
```

The blocked/allowed sets are runtime-only (cleared on server restart).

### Camera Triggers

`set_camera` sends a `SetServerCamera` packet to **only** the triggering player. Modes:

| `Mode` | Effect |
|---|---|
| `"first_person"` | Standard first-person view |
| `"third_person"` | Third-person / over-the-shoulder view |
| `"reset"` | Return to unlocked first-person (cancels `Locked`) |

```json
// Cinematic volume — lock to third-person when entering
{ "type": "set_camera", "eventType": "ENTER", "Mode": "third_person", "Locked": true }

// Reset on exit
{ "type": "set_camera", "eventType": "EXIT", "Mode": "reset" }
```

Camera changes are per-player (each player gets their own packet). `Locked: true` prevents the player from switching modes while inside.

---

## Per-Player View System

The `player_hide_entity`, `player_show_entity`, and `clear_player_overrides` effects work together:

1. Server-side state is **always** tracked in `PlayerOverrideService` regardless of `UsePackets`.
2. The client-side `HiddenPlayersManager` packet is only sent when **both** `UsePackets: true` (the default) **and** `advancedPacketActions=true` in `hyextras.properties`.
3. The `player_hidden` condition tests the server-side state, so it works even when `UsePackets: false`.
4. All state is cleared on player disconnect.

The packet actions stay visible in the editor, but packet sending is runtime-gated by `advancedPacketActions`. Use `UsePackets: false` when you want pure server-logic tracking (e.g. a condition check) without actually changing what the client sees.

---

## String Template Placeholders

The following placeholders are supported in `run_command`, `send_title` (`Title`/`Subtitle`/`ActionBar`), `send_rich_message`, and any other string fields that route through `StringTemplate.resolve()`:

| Placeholder | Resolves to |
|---|---|
| `{player}` | Triggering player's username |
| `{uuid}` | Triggering entity UUID |
| `{variable:key}` | Per-player variable value (empty string if not set) |

`send_rich_message` and `send_title` also support simple color/style codes: `&0`-`&9`, `&a`-`&f`, `&#RRGGBB`, `&l` bold, `&o` italic, `&n` underline, `&r` reset, and `&&` for a literal ampersand.

---

## Architecture

```
org.hyzionstudios.hyextras/
├── HyExtrasPlugin           — plugin entry point, singleton, player name registry
├── TriggerVolumeApiAdapter  — all native TriggerVolumesPlugin calls go through here
├── ExtrasRegistry           — one-time registration of 22 effects and 8 conditions
├── codec/
│   └── CodecHelper          — KeyedCodec factory helpers
├── service/
│   ├── PlayerVariableService      — per-player key→value store
│   ├── CooldownService            — per-player named cooldown tracker
│   ├── PlayerTagService           — per-player boolean tag set, persisted to disk
│   └── InteractionTriggerService  — cancel-pending tracking for interaction bridge
├── state/
│   ├── PlayerOverrideService  — per-viewer entity visibility tracking
│   └── RuntimeStateStore      — unified accessor (vars, cooldowns, playerOverrides, config)
├── action/                    — 19 TriggerEffect implementations (including tag/camera/blocking/message)
├── advanced/                  — 3 per-player view TriggerEffect implementations
├── condition/                 — 8 TriggerCondition implementations
├── command/                   — /hextras command tree
├── config/                    — HyExtrasConfig, ConfigLoader
└── util/
    └── StringTemplate         — {player}/{uuid}/{variable:key} resolution
```

---

## Building

```bash
./gradlew shadowJar
```

The build compiles against Hytale Server API `0.5.6` while the packaged manifest declares compatibility with the broader `0.5.x` line.

## Deploying

```bash
./gradlew deployMod
```

Builds the fat JAR and copies it to `.hytale-server/mods/`.

## Running

Use the included run configurations in your IDE:

- **Run Hytale Server** — Builds, deploys, and starts the server
- **Debug Hytale Server** — Same as Run, with remote debugger on port 5005
- **Build Mod** — Compiles without deploying or starting the server
