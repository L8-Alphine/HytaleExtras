# HyExtras

HyExtras is a modular creator and developer toolkit for Hytale servers, adding optional systems, APIs, and interaction tools that server owners can enable only when they need them.
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
| `calculate_variable` | Evaluate a formula string and write the numeric result to a per-player variable |
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
| `push_back_player` | Move the triggering player away from the interacted block or volume center; useful with rejection effects |
| `set_volume_interactable` | Make a volume interactable at runtime with an action-bar prompt and interaction action text |
| `clear_volume_interactable` | Clear the runtime interactable override for a volume |
| `give_item_reward` | Give an `ItemStack` reward to the triggering player; the `Item` field uses `ItemStack.CODEC` for editor item metadata/browser support |
| `run_reward_command` | Run a templated reward command as the triggering player |
| `send_reward_message` | Send a templated reward message through chat, action bar, or title display |
| `set_voice_activity` | Mute, unmute, or toggle native Hytale voice activity for the triggering or named player |
| `tag_npc_add_tag` | Add a runtime tag to the triggering entity, explicit entity UUID, or all tracked entities with a target tag |
| `tag_npc_remove_tag` | Remove a runtime TagNPC tag |
| `tag_npc_set_variable` | Set a runtime variable on a UUID-backed NPC, mob, or entity |
| `tag_npc_add_variable` | Add to a numeric TagNPC entity variable |
| `tag_npc_remove_variable` | Remove a TagNPC entity variable |
| `tag_npc_hide_entity` | Hide a tagged or explicit entity from a viewer through PacketAPI entity visibility state |
| `tag_npc_show_entity` | Clear TagNPC/PacketAPI entity hide state for a viewer |
| `floating_item_create` | Create a decorative non-pickup floating item display using `ItemStack.CODEC` |
| `floating_item_remove` | Remove a floating item display by ID |
| `floating_item_set_intangible` | Toggle the stored intangible state for a floating item |
| `floating_item_move` | Move a floating item to a triggering entity, volume center, block position, or explicit coordinate |

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
| `math_condition` | Evaluate a formula and compare it with another formula/value using numeric comparison operators |
| `tag_npc_has_tag` | Pass if a targeted UUID-backed entity has a runtime TagNPC tag |
| `tag_npc_variable_condition` | Pass if a targeted entity variable matches the configured comparison |
| `tag_npc_visible_condition` | Pass if targeted entities are visible or hidden for a viewer |
| `floating_item_exists` | Pass if a floating item ID is currently registered |
| `floating_item_intangible` | Pass if a floating item is currently marked intangible |

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
/hextras modules                         — list internal HyExtras modules
/hextras module info <module>            — show module state and config
/hextras module enable <module>          — enable a safe live-toggle module and persist config
/hextras module disable <module>         — disable a safe live-toggle module and persist config
/hextras module reload <module>          — reload a reloadable module
/hextras tagnpc tag add <entityUuid> <tag> — add a runtime tag to an entity by UUID
/hextras tagnpc tag remove <entityUuid> <tag> — remove a runtime entity tag
/hextras tagnpc tag list <entityUuid>    — list runtime tags for an entity
/hextras tagnpc var set <entityUuid> <key> <value> — set a runtime entity variable
/hextras tagnpc var get <entityUuid> <key> — read a runtime entity variable
/hextras tagnpc var add <entityUuid> <key> <amount> — add to a runtime entity variable
/hextras tagnpc var list <entityUuid>    — list runtime variables for an entity
/hextras tagnpc hide <entityUuid> <viewer> — hide an entity from a player viewer
/hextras tagnpc show <entityUuid> <viewer> — show an entity to a player viewer
/hextras tagnpc near info <player> <radius> — inspect the closest indexed NPC/mob near a player
/hextras tagnpc near tag-add <player> <radius> <tag> — tag the closest indexed NPC/mob in range
/hextras tagnpc near tag-remove <player> <radius> <tag> — remove a tag from the closest indexed NPC/mob
/hextras tagnpc near var-set <player> <radius> <key> <value> — set a variable on the closest indexed NPC/mob
/hextras tagnpc near var-add <player> <radius> <key> <amount> — add to a variable on the closest indexed NPC/mob
/hextras floatingitems list                — list decorative floating items
/hextras floatingitems info <id>           — show floating item state
/hextras floatingitems create <id> <player> <itemId> [persistent] — create at a player's position
/hextras floatingitems remove <id>         — remove a floating item
/hextras floatingitems intangible <id> <true|false> — set intangible state
/hextras floatingitems reload              — re-render and save persistent floating items
/hextras reload                          — reload hyextras.properties at runtime
```

### Configuration (`hyextras.properties`)

```properties
# Recommended: keep supported per-player packet features enabled.
# Includes player visibility packets, titles, action bars, camera packets, and API packet helpers.
advancedPacketActions=true

# Experimental: enable non-player EntityUpdates filtering for best-effort entity hiding.
# Keep false unless you are specifically testing non-player entity packet visibility.
entityPacketFiltering=false

# Print startup/preflight diagnostics for config, dependencies, packet filters, and plugin conflicts.
startupDiagnostics=true

# Recommended: automatically applies IsStoryArea/GroupArea visibility tags to player packets.
playerVisibilityPolicySync=true

# Print verbose debug info to the server log
debugMode=false

# Internal modules
modules.trigger_extras.enabled=true
modules.trigger_extras.allowInGameToggle=true
modules.trigger_extras.reloadable=true

modules.placeholder_api.enabled=true
modules.placeholder_api.allowInGameToggle=true
modules.placeholder_api.reloadable=true

modules.packet_api.enabled=true
modules.packet_api.allowInGameToggle=false
modules.packet_api.reloadable=false

modules.image_icons.enabled=true
modules.image_icons.allowInGameToggle=true
modules.image_icons.reloadable=true

modules.tag_npc.enabled=true
modules.tag_npc.allowInGameToggle=true
modules.tag_npc.reloadable=true

modules.floating_items.enabled=true
modules.floating_items.allowInGameToggle=true
modules.floating_items.reloadable=true

# StringTemplate
stringTemplate.nativePlaceholders.enabled=true
stringTemplate.placeholderApi.enabled=true
stringTemplate.placeholderApi.missingBehavior=KEEP_ORIGINAL

# ImageIcons provider assets
imageIcons.hotReload=true
imageIcons.remoteCache.enabled=true
imageIcons.remoteCache.maxBytes=5242880
imageIcons.defaultVisibilityRadius=48.0
imageIcons.maxIconsPerViewer=64

# TagNPC runtime entity state
tagNpc.defaultVisibilityRadius=64.0
tagNpc.clearStateOnEntityUnload=true

# FloatingItems decorative item displays
floatingItems.defaultPersistent=false
floatingItems.defaultIntangible=true
floatingItems.defaultVisibilityRadius=48.0
floatingItems.defaultBobAmplitude=0.15
floatingItems.defaultRotationDegreesPerSecond=45.0
floatingItems.maxItems=512
```

### What is already native (not duplicated)

The native Trigger Volume system already includes:
- **Effects**: SendMessage, Teleport, GiveItem, PlaySound, ShowEventTitle, SetGameMode, DamageEntity, SetVelocity, PlaceBlock, SetWeather, PlayVfx, PastePrefab, ControlDoors, EnableVolume, DisableVolume, DeleteVolume, ModifyTags, ReplaceBlockType, RunRootInteraction, EntityEffect, SetMusic, TriggerNpcMarkers, VolumeState
- **Conditions**: Permission, Cooldown, RandomChance, Tag, Item, BlockType, GameMode, PlayerCount

### Cross-Volume Tag Communication

`set_volume_tag` calls `TriggerVolumeManager.setTag()` which fires a native `TAG_ADDED` event on the target volume for the current entity. `remove_volume_tag` calls `removeTag()` which fires `TAG_REMOVED`. This is the intended native mechanism for volume-to-volume messaging.

The `volume_has_tag` condition reads `VolumeEntry.getRawTags()` — this includes both static tags set in the editor and runtime tags set via `set_volume_tag` / native `ModifyTags`.

### Interaction Bridge

The HyExtras interaction bridge fires volume effects when a player interacts with something while inside a volume. To enable the basic bridge on a volume:

1. Add static tag `hextras:interact` to the volume in the editor (any value, e.g. `1`).
2. Add conditions and effects to the volume with event type **`TAG_ADDED`**.
   - Conditions are evaluated first; all must pass or effects are skipped.
   - The synthetic context has `tagKey = "hextras_interact"`, `tagValue = <InteractionType name>` (e.g. `Use`, `Primary`, `Secondary`).
3. To cancel the interaction (block the door/chest/entity from responding), add `cancel_interaction` to the rejection effects or effects that should deny access.
4. To push denied players away from the door/block, add `push_back_player` to the same rejection effects. Optional fields: `Distance` (default `1.25`) and `YOffset` (default `0.0`).

The bridge uses `registerGlobal` so it fires for any player interacting in any world. Because it dispatches synchronously before the native interaction resolves, `cancel_interaction` is guaranteed to run in time.

**InteractionType dropdown values** for the HyExtras `interaction_type` condition: `primary`, `secondary`, `ability1`, `ability2`, `ability3`, `use`, `pick`, `pickup`, `collision_enter`, `collision_leave`, `collision`, `swap_to`, `swap_from`, `death`.

Interactable trigger volumes can also be declared directly with tags:

| Static tag | Meaning |
|---|---|
| `hextras:interactable=true` | Marks the volume as interactable without requiring the legacy `hextras:interact` tag |
| `hextras:interaction_message=interactionHints.generic` | Native interaction hint key or direct prompt text sent through PacketAPI action-bar messaging |
| `hextras:interaction_action=<action text>` | Synthetic tag value and `{action}` value for custom prompt text |
| `hextras:interaction_key=<key text>` | Value substituted into `{key}` for interaction hint messages |
| `hextras:interaction_name=<target name>` | Value substituted into `{name}` for interaction hint messages |
| `hextras:interaction_type=use` | Optional interaction type filter |

Runtime effects `set_volume_interactable` and `clear_volume_interactable` override the static tags until restart. Prompt delivery is isolated behind the PacketAPI/action-bar path so a native volume-level prompt backend can replace it later if Hytale exposes one.

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

### Calculate a variable with a formula
```json
{ "type": "calculate_variable", "eventType": "ENTER", "Key": "bonus", "Formula": "({variable:score} + {activeVolumeCount}) * 2" }
```

### Gate with a math condition
```json
{ "type": "math_condition", "eventType": "ENTER", "Formula": "{variable:score} + 5", "Operator": "greater_or_equal", "Value": "10" }
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

### Give creator-friendly rewards
```json
[
  { "type": "give_item_reward", "eventType": "ENTER", "Item": { "itemId": "hytale:gold_coin", "quantity": 5 }, "Message": "&aReward: 5 coins" },
  { "type": "run_reward_command", "eventType": "ENTER", "Command": "xp add {player} 25" },
  { "type": "send_reward_message", "eventType": "ENTER", "Display": "action_bar", "Message": "Reward claimed in {currentVolumeId}!" }
]
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

> **Note on mob/entity visibility:** Player-to-player visibility is first-class through Hytale's `HiddenPlayersManager`. Non-player entity visibility is best-effort in HyExtras: when `advancedPacketActions=true`, HyExtras can filter outbound entity update packets for hidden viewer/entity pairs when the entity can be resolved from its network ID. `PreventTargeting` now also clears protected players from supported Hytale NPC combat `TargetMemory`; NPCs/entities that do not use that component are ignored safely.

### Per-Player Visibility Rules

`player_hide_entity` and `player_show_entity` now support rule-driven targeting:

| Field | Meaning |
|---|---|
| `TargetPlayer` | Exact online player username, preserving the original behavior |
| `TargetSelector` | `player` (default), `players` (all matching online players), or `entities` (developer API / packet-filter path) |
| `ViewerRule` | Predicates that must match the triggering viewer |
| `TargetRule` | Predicates that must match the target player/entity |
| `PreventTargeting` | Protects the triggering player by clearing them from supported NPC combat target memory until show/clear/disconnect |

When `PreventTargeting=true` on `player_hide_entity`, HyExtras marks the triggering player as protected. `player_show_entity` with `PreventTargeting=true`, `clear_player_overrides`, disconnect, or shutdown releases that protection. This covers NPCs that use Hytale's `TargetMemory`; other entity AI systems may need their own integration later.

Supported rule predicates:

| Predicate | Meaning |
|---|---|
| `{hasTag:tag}` / `{!hasTag:tag}` | Player has / lacks a persistent HyExtras tag |
| `{variable:key=value}` / `{variable:key!=value}` | Player variable equals / does not equal a value |
| `{variable:key}` / `{!variable:key}` | Player variable exists / does not exist |
| `{eventType:ENTER}` | Current trigger event type matches |
| `{volumeTag:key=value}` / `{volumeTag:key}` | Active volume tag matches / exists |

Example: hide all stealthed players from viewers that do not have the reveal tag:

```json
{
  "type": "player_hide_entity",
  "eventType": "ENTER",
  "TargetSelector": "players",
  "ViewerRule": "{!hasTag:see_stealth}",
  "TargetRule": "{hasTag:stealthed}"
}
```

### Volume Visibility Policy Tags

Volume tags can enforce visibility policy without adding a custom effect to every volume:

| Volume tag | Behavior |
|---|---|
| `IsStoryArea:true` | Players inside the same volume are hidden from each other by default |
| `IsStoryArea:false` | Forces players inside the same volume to be visible, overriding normal hide rules |
| `GroupArea:true` | Treats the volume as a party/group instance |
| `PartyAmount:<amount>` | Maximum visible members per party inside that group area |

`GroupArea:true` uses the per-player variable `partyId` as the group identity. Players with the same non-empty `partyId` can see each other up to `PartyAmount`; extra members are hidden by visibility policy only.

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

### Interactable Trigger Volumes

Use static tags when the interaction prompt should exist as part of the volume definition:

```json
{
  "staticTags": {
    "hextras:interactable": "true",
    "hextras:interaction_message": "interactionHints.open",
    "hextras:interaction_action": "claim reward",
    "hextras:interaction_key": "E",
    "hextras:interaction_name": "reward chest",
    "hextras:interaction_type": "use"
  },
  "effects": [
    { "type": "send_reward_message", "eventType": "TAG_ADDED", "Display": "chat", "Message": "&aClaimed reward in {currentVolumeId}" }
  ]
}
```

Use runtime effects when another volume, quest step, or tag state should make the target interactable temporarily:

```json
[
  { "type": "set_volume_interactable", "eventType": "ENTER", "VolumeId": "reward_chest", "Message": "interactionHints.open", "Action": "open", "Key": "E", "Name": "reward chest", "InteractionType": "use" },
  { "type": "clear_volume_interactable", "eventType": "EXIT", "VolumeId": "reward_chest" }
]
```

### Voice Activity Zones

`set_voice_activity` uses Hytale's native `VoiceModule`. If voice is unavailable or globally disabled, HyExtras logs once per action type and safely no-ops.

```json
[
  { "type": "set_voice_activity", "eventType": "ENTER", "Mode": "mute" },
  { "type": "set_voice_activity", "eventType": "EXIT", "Mode": "unmute" }
]
```

### TagNPC Runtime Entity State

TagNPC stores runtime tags and variables for UUID-backed NPCs, mobs, and other entities. It is useful when a Trigger Volume spawn/context gives you the mob or NPC entity UUID, or when another mod calls the developer API with an entity UUID.

```json
[
  { "type": "tag_npc_add_tag", "eventType": "ENTER", "Target": "triggering_entity", "Tag": "quest_guard" },
  { "type": "tag_npc_set_variable", "eventType": "ENTER", "Target": "triggering_entity", "Key": "mood", "Value": "alert" },
  { "type": "tag_npc_has_tag", "Tag": "quest_guard" }
]
```

Actions can also target an explicit `EntityUuid` or every currently tracked entity with `TargetTag`. Visibility actions use PacketAPI entity visibility state; visual hiding requires `advancedPacketActions=true` and `entityPacketFiltering=true`.

Use `/hextras tagnpc ...` commands for explicit UUID debugging and admin edits. For in-game admin workflows without copying UUIDs, use `/hextras tagnpc near ...` to apply tags or variables to the closest indexed NPC/mob within a radius of a player.

### FloatingItems

FloatingItems creates decorative item displays that look like floating dropped items but are not rewards, pickups, or inventory drops. Trigger Volume creation uses `ItemStack.CODEC` for the `Item` field, while commands use a simple item ID and create at an online player's position.

```json
[
  {
    "type": "floating_item_create",
    "eventType": "ENTER",
    "Id": "ruins_key_display",
    "Item": { "itemId": "hytale:gold_coin", "quantity": 1 },
    "Anchor": "volume_center",
    "OffsetY": 1.0,
    "Persistent": true,
    "Intangible": true
  },
  {
    "type": "floating_item_set_intangible",
    "eventType": "TAG_ADDED",
    "Id": "ruins_key_display",
    "Intangible": true
  }
]
```

The renderer is isolated behind PacketAPI/display state and never creates collectible dropped-item entities. If PacketAPI display support is unavailable, API calls fail safely with a message and no server crash.

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
| `{hasTag:tag}` / `{!hasTag:tag}` | `true` or `false` for the triggering player's tag state |
| `{eventType}` | Current trigger event type |
| `{tagKey}` / `{tagValue}` | Current tag event key/value |
| `{volumeTag:key}` | First matching active volume tag value, or empty string |
| `{currentVolumeId}` | Current trigger volume ID |
| `{activeVolumeCount}` | Number of active volumes for the triggering player |
| `{currentVolumeTag:key}` | Tag value on the current trigger volume |
| `{volumeTag:volumeId:key}` | Tag value on a named trigger volume |
| `{volumeActive:volumeId}` | `true` when the named volume is active for the triggering player |
| `%placeholder%` | PlaceholderAPI placeholder, when PlaceholderAPI is installed and `stringTemplate.placeholderApi.enabled=true` |

`send_rich_message` and `send_title` also support simple color/style codes: `&0`-`&9`, `&a`-`&f`, `&#RRGGBB`, `&l` bold, `&o` italic, `&n` underline, `&r` reset, and `&&` for a literal ampersand.

PlaceholderAPI is optional. If it is missing or leaves a `%placeholder%` unresolved, HyExtras follows `stringTemplate.placeholderApi.missingBehavior` (`KEEP_ORIGINAL`, `EMPTY`, or `ERROR`).

---

## Developer API

Other server mods can use `org.hyzionstudios.hyextras.api.HyExtrasApi.get()` for stable access to HyExtras state and high-level packet helpers:

```java
HyExtrasApi api = HyExtrasApi.get();
api.setVariable(playerUuid, "partyId", "alpha");
api.addTag(playerUuid, "storyline_a_active");
api.hidePlayerFrom(viewerUuid, targetUuid, true);
api.protectPlayerFromTargeting(viewerUuid);
api.sendActionBar(playerUuid, "Party: " + api.getVariableString(playerUuid, "partyId"));
```

The API exposes variables, tags, cooldowns, visibility overrides, targeting protection, high-level title/message/camera helpers, rule evaluation, ImageIcons provider registration, TagNPC state, and FloatingItems displays.

### ImageIcons Developer API

ImageIcons lets developer mods own local or remote PNG/GIF assets without global ID collisions. A mod can register any readable folder, such as `mods/MysticNameTags/data/imageicons`, and then attach provider-scoped icons to player or entity UUIDs:

```java
HyExtrasApi api = HyExtrasApi.get();
api.registerImageIconProvider("mysticnametags", Path.of("mods/MysticNameTags/data/imageicons"));
api.registerRemoteImageIcon("mysticnametags", "vip.sparkle", URI.create("https://example.com/vip.gif"));

ImageIconTuning tuning = ImageIconTuning.defaults(null);
UUID attachmentId = api.attachImageIconToPlayer(playerUuid, "mysticnametags", "vip.sparkle", tuning)
        .attachmentId();
```

Local providers hot reload when `imageIcons.hotReload=true`. Remote assets are cached under the HyExtras data folder before loading. Attachments are runtime-only and are cleared on disconnect, provider unregister, module disable, and shutdown.

See [docs/DEVELOPER_API.md](docs/DEVELOPER_API.md) for the full API reference, examples, persistence rules, and visibility policy notes.

---

## Architecture

```
org.hyzionstudios.hyextras/
├── HyExtrasPlugin           — plugin entry point, singleton, player name registry
├── TriggerVolumeApiAdapter  — all native TriggerVolumesPlugin calls go through here
├── codec/
│   └── CodecHelper          — KeyedCodec factory helpers
├── module/                   — internal module lifecycle, state, and built-in module definitions
├── imageicons/                — provider-scoped image asset loading and runtime icon attachments
├── packetapi/
│   ├── PacketApi             — packet-backed visibility, title, action bar, and camera facade
│   ├── PacketCameraMode      — packet-owned camera mode enum
│   └── service/
│       ├── VisibilityPolicyService      — player/entity visibility policy and override application
│       ├── PacketVisibilityService      — entity packet filtering service
│       └── PlayerVisibilitySyncService  — player visibility policy sync loop
├── triggerextras/
│   ├── TriggerActionRegistry     — one-time registration of TriggerExtras effects
│   ├── TriggerConditionRegistry  — one-time registration of TriggerExtras conditions
│   ├── ExtraTriggerDispatcher    — synthetic trigger dispatch helper
│   ├── TriggerExtrasInteractionBridge — interaction bridge runtime, prompts, and active-volume lookup
│   ├── InteractableVolumeState — static/runtime interactable volume configuration
│   ├── service/
│   │   └── InteractionTriggerService  — cancel-pending tracking for interaction bridge
│   ├── action/                   — TriggerEffect implementations
│   ├── advanced/                 — per-player view TriggerEffect implementations
│   └── condition/                — TriggerCondition implementations
├── service/
│   ├── PlayerVariableService      — per-player key→value store
│   ├── CooldownService            — per-player named cooldown tracker
│   ├── PlayerTagService           — per-player boolean tag set, persisted to disk
│   └── TargetingPreventionService — NPC target-memory protection support
├── state/
│   ├── PlayerOverrideService  — per-viewer entity visibility tracking
│   └── RuntimeStateStore      — unified accessor (vars, cooldowns, playerOverrides, config)
├── command/                   — /hextras command tree
├── config/                    — HyExtrasConfig, ConfigLoader
└── util/
    ├── ArithmeticExpression   — small formula evaluator for TriggerExtras math
    └── StringTemplate         — native, module, volume, and PlaceholderAPI template resolution
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
