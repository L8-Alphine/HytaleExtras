# Effects (Trigger Actions) Reference

This is the complete reference for every HyExtras **effect** — the custom Trigger Volume actions
registered by the `trigger_extras` module. There are **41 type IDs** backed by **40 action classes**
(`add_variable` and `increment_variable` are two IDs for the same class).

Effects are added to a Trigger Volume's `effects` (or rejection-effects) list in the volume editor or
JSON. Every effect carries the native `eventType` field (e.g. `ENTER`, `EXIT`, `TAG_ADDED`,
`TAG_REMOVED`) inherited from the base trigger-effect codec, plus its own fields documented below.

> **Reading the tables.** `Required` reflects the codec: required fields use `CodecHelper.string/bool/integer/float/enumField` or a bare `KeyedCodec`; optional fields use the `opt*` helpers and may be omitted from JSON. `Default` is the value applied at runtime when an optional field is omitted. All string fields marked *templated* are resolved through [String Templates](string-templates).

**Jump to:** [Variables & State](#variables--state) · [Cooldowns](#cooldowns) · [Commands](#commands) ·
[Messaging](#messaging) · [Items](#items) · [Camera, Voice & Physics](#camera-voice--physics) ·
[Player Visibility](#player-visibility) · [Volume Control & Tags](#volume-control--tags) ·
[Interaction](#interaction) · [Persistent Player Tags](#persistent-player-tags) ·
[TagNPC](#tagnpc-entity-state) · [Floating Items](#floating-items)

Related concepts: [Player State](player-state) · [Visibility & Packets](visibility-and-packets) ·
[Interaction Bridge](interaction-bridge) · [TagNPC](tag-npc) ·
[Floating Items](floating-items) · [Developer API](developer-api).

---

## Variables & State

Per-player variables are runtime-only string/number values cleared on disconnect. See
[Player State](player-state).

### `set_variable`
Sets a per-player variable to a fixed (templated) string value.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Key` | string | Yes | — | Variable name. |
| `Value` | string *(templated)* | Yes | — | Value to store. Resolved through string templates; `""` if it resolves empty. |

```json
{ "type": "set_variable", "eventType": "ENTER", "Key": "zone", "Value": "ruins" }
```

### `add_variable` / `increment_variable`
Two type IDs for the same action — atomically adds `Delta` to a numeric variable (missing variable is
treated as `0`). Use a negative `Delta` to decrement.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Key` | string | Yes | — | Variable name. |
| `Delta` | long | No | `1` | Amount to add (may be negative). |

```json
{ "type": "add_variable", "eventType": "ENTER", "Key": "score", "Delta": 5 }
```

### `calculate_variable`
Evaluates an arithmetic formula and writes the numeric result to a variable. The formula is
template-resolved first (so it can embed `{variable:...}`), then evaluated by the
[arithmetic expression](player-state#arithmetic) engine.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Key` | string | Yes | — | Variable to write the result into. |
| `Formula` | string *(templated)* | Yes | — | Arithmetic expression, e.g. `({variable:score} + {activeVolumeCount}) * 2`. |

```json
{ "type": "calculate_variable", "eventType": "ENTER", "Key": "bonus", "Formula": "{variable:score} * 2 + 10" }
```

### `remove_variable`
Removes a per-player variable.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Key` | string | Yes | — | Variable to remove. |

```json
{ "type": "remove_variable", "eventType": "EXIT", "Key": "zone" }
```

---

## Cooldowns

Named per-player cooldowns separate from the native volume cooldown. See
[Player State](player-state#cooldowns) and the [`cooldown_ready`](conditions#cooldown_ready) condition.

### `apply_cooldown`
Applies (or resets) a named cooldown for a number of seconds.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Name` | string | Yes | — | Cooldown name. |
| `Duration` | float | Yes | — | Duration in seconds. |

```json
{ "type": "apply_cooldown", "eventType": "ENTER", "Name": "ruins_intro", "Duration": 300.0 }
```

---

## Commands

Both command effects run as the triggering player and support [string templates](string-templates)
such as `{player}`, `{uuid}`, and `{variable:key}`.

### `run_command`
Runs a command as the triggering player.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Command` | string *(templated)* | Yes | — | Command line to run (no leading slash needed). |
| `DebugName` | string | No | — | Label used in debug logging. |

```json
{ "type": "run_command", "eventType": "ENTER", "Command": "xp add {player} 25" }
```

### `run_reward_command`
Reward-focused variant of `run_command` with identical fields; kept distinct so reward flows read
clearly in the editor.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Command` | string *(templated)* | Yes | — | Command line to run as the player. |
| `DebugName` | string | No | — | Label used in debug logging. |

```json
{ "type": "run_reward_command", "eventType": "ENTER", "Command": "give {player} hytale:gold_coin 5" }
```

---

## Messaging

All messaging effects target the triggering player and support [color/style codes](string-templates#color--style-codes).

### `send_title`
Sends a title/subtitle (PacketAPI title support) and/or an action bar (PacketAPI notification). At
least one of `Title` or `ActionBar` must be set.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Title` | string *(templated)* | No | — | Title line. |
| `Subtitle` | string *(templated)* | No | — | Subtitle line (only sent with `Title`). |
| `ActionBar` | string *(templated)* | No | — | Action-bar/notification line. |
| `Duration` | float | No | `3.0` | Title hold time in seconds. |
| `FadeIn` | float | No | `0.5` | Title fade-in seconds. |
| `FadeOut` | float | No | `0.5` | Title fade-out seconds. |

```json
{ "type": "send_title", "eventType": "ENTER", "Title": "Welcome, {player}!", "Subtitle": "Zone: spawn", "Duration": 3.0 }
{ "type": "send_title", "eventType": "ENTER", "ActionBar": "Score: {variable:score}" }
```

### `send_rich_message`
Sends a chat message to the triggering player.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Message` | string *(templated)* | Yes | — | Chat message with placeholders and color codes. |

```json
{ "type": "send_rich_message", "eventType": "ENTER", "Message": "&aWelcome back {player}&r." }
```

### `send_reward_message`
Sends a templated message through chat, action bar, or title.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Message` | string *(templated)* | Yes | — | Primary message. |
| `SecondaryMessage` | string *(templated)* | No | — | Subtitle, used only when `Display=title`. |
| `Display` | enum `chat` \| `action_bar` \| `title` | No | `chat` | Where to show the message. |
| `Duration` | float | No | `3.0` | Title hold seconds (only for `Display=title`). |

```json
{ "type": "send_reward_message", "eventType": "ENTER", "Display": "action_bar", "Message": "Reward claimed in {currentVolumeId}!" }
```

---

## Items

### `give_item_reward`
Gives an `ItemStack` to the triggering player. The `Item` field uses `ItemStack.CODEC`, so the volume
editor's item browser/metadata works.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Item` | ItemStack | Yes | — | Item to give, e.g. `{ "itemId": "hytale:gold_coin", "quantity": 5 }`. |
| `Message` | string *(templated)* | No | — | Optional chat message sent with the reward. |

```json
{ "type": "give_item_reward", "eventType": "ENTER", "Item": { "itemId": "hytale:gold_coin", "quantity": 5 }, "Message": "&aReward: 5 coins" }
```

### `remove_item`
Removes item(s) from the player's inventory by item ID.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `ItemId` | string | Yes | — | Item ID to remove, e.g. `hytale:gold_coin`. |
| `Quantity` | integer | No | all matching | How many to remove. |
| `Location` | enum `inventory` \| `hotbar` \| `in_hand` | No | `inventory` | Where to remove from. |

```json
{ "type": "remove_item", "eventType": "ENTER", "ItemId": "hytale:dungeon_key", "Quantity": 1, "Location": "inventory" }
```

---

## Camera, Voice & Physics

See [Visibility & Packets](visibility-and-packets#camera) for camera behavior.

### `set_camera`
Switches the triggering player's camera. Sends a `SetServerCamera` packet to **only** that player;
requires `advancedPacketActions=true`.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Mode` | enum `first_person` \| `third_person` \| `reset` | Yes | — | Camera view. `reset` returns to unlocked first person. |
| `Locked` | boolean | No | `false` | When `true`, prevents the player from switching modes. |

```json
{ "type": "set_camera", "eventType": "ENTER", "Mode": "third_person", "Locked": true }
{ "type": "set_camera", "eventType": "EXIT", "Mode": "reset" }
```

### `set_voice_activity`
Mutes, unmutes, or toggles native Hytale voice for the triggering or a named player. No-ops safely if
voice is unavailable.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Mode` | enum `mute` \| `unmute` \| `toggle` | Yes | — | Voice action. |
| `TargetPlayer` | string | No | triggering player | Apply to a named online player instead. |

```json
{ "type": "set_voice_activity", "eventType": "ENTER", "Mode": "mute" }
```

### `push_back_player`
Moves the triggering player away from the interacted block (or volume center when no block). Useful in
rejection effects together with [`cancel_interaction`](#cancel_interaction).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Distance` | float | No | `1.25` | Horizontal push distance (clamped to ≥ 0). |
| `YOffset` | float | No | `0.0` | Vertical offset added after the push. |

```json
{ "type": "push_back_player", "Distance": 1.5, "YOffset": 0.0 }
```

---

## Player Visibility

These power the per-player "solo instance" illusion. Server-side state is always tracked; client
packets require `UsePackets=true` (default) **and** `advancedPacketActions=true`. See
[Visibility & Packets](visibility-and-packets) and the
[`player_hidden`](conditions#player_hidden) condition.

### `player_hide_entity`
Hides a target player from the triggering player's view, with optional rule-driven targeting.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `TargetPlayer` | string | No | — | Exact online username to hide. |
| `TargetSelector` | enum `player` \| `players` \| `entities` | No | `player` | `players` applies to all matching online players; `entities` is the developer/packet-filter path. |
| `ViewerRule` | string *(rule)* | No | — | [Rule predicate](string-templates#rule-predicates) the viewer must match. |
| `TargetRule` | string *(rule)* | No | — | Rule predicate the target must match. |
| `UsePackets` | boolean | No | `true` | When `false`, only server-side state changes (no client packet). |
| `PreventTargeting` | boolean | No | `false` | Also protect the viewer from supported NPC target memory. |

```json
{ "type": "player_hide_entity", "eventType": "ENTER", "TargetSelector": "players", "ViewerRule": "{!hasTag:see_stealth}", "TargetRule": "{hasTag:stealthed}" }
```

### `player_show_entity`
Reverses a hide. Same fields as `player_hide_entity`; `PreventTargeting=true` here *releases* targeting
protection.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `TargetPlayer` | string | No | — | Username to show again. |
| `TargetSelector` | enum `player` \| `players` \| `entities` | No | `player` | Same semantics as hide. |
| `ViewerRule` | string *(rule)* | No | — | Viewer must match. |
| `TargetRule` | string *(rule)* | No | — | Target must match. |
| `UsePackets` | boolean | No | `true` | Whether to send the show packet. |
| `PreventTargeting` | boolean | No | `false` | When `true`, releases targeting protection on the viewer. |

```json
{ "type": "player_show_entity", "eventType": "EXIT", "TargetPlayer": "OtherPlayer" }
```

### `clear_player_overrides`
Clears all per-player visibility overrides for the triggering player and releases targeting protection.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `UsePackets` | boolean | No | `true` | Whether to send show packets while clearing. |

```json
{ "type": "clear_player_overrides", "eventType": "EXIT" }
```

---

## Volume Control & Tags

Cross-volume messaging and enable/disable control. See the
[`volume_has_tag`](conditions#volume_has_tag) condition and
[Interaction Bridge](interaction-bridge) for tag-event flow.

### `trigger_named_volume`
Dispatches another named volume against the current entity, honoring its conditions, rejection effects,
enabled state, and cooldown.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `VolumeId` | string | Yes | — | Target volume ID to dispatch. |

```json
{ "type": "trigger_named_volume", "eventType": "ENTER", "VolumeId": "reward_fanfare_volume" }
```

### `toggle_trigger_enabled`
Enables, disables, or toggles another volume.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `VolumeId` | string | No | current volume | Target volume ID. |
| `Mode` | enum `enable` \| `disable` \| `toggle` | No | `toggle` | What to do. |

```json
{ "type": "toggle_trigger_enabled", "eventType": "ENTER", "VolumeId": "boss_arena", "Mode": "disable" }
```

### `set_volume_tag`
Sets a tag on a named volume and fires a native `TAG_ADDED` event on it for the current entity.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `TargetVolumeId` | string | No | current volume | Volume to tag. |
| `Key` | string | Yes | — | Tag key. |
| `Value` | string | No | `"1"` | Tag value. |

```json
{ "type": "set_volume_tag", "eventType": "ENTER", "TargetVolumeId": "gate_a", "Key": "opened", "Value": "1" }
```

### `remove_volume_tag`
Removes a tag from a named volume and fires `TAG_REMOVED`. An optional `Value` guards the removal
(only removes when the value matches).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `TargetVolumeId` | string | No | current volume | Volume to modify. |
| `Key` | string | Yes | — | Tag key to remove. |
| `Value` | string | No | — | If set, only removes when the current value matches. |

```json
{ "type": "remove_volume_tag", "eventType": "EXIT", "TargetVolumeId": "gate_a", "Key": "opened" }
```

---

## Interaction

These work with the [Interaction Bridge](interaction-bridge). `cancel_interaction` only has
effect inside a bridge dispatch.

### `cancel_interaction`
Cancels the native interaction (door/chest/entity) for the current bridge dispatch. No fields.

```json
{ "type": "cancel_interaction", "eventType": "TAG_ADDED" }
```

### `block_volume_interactions`
Blocks all interactions for players inside a volume (runtime-only set).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `VolumeId` | string | No | current volume | Volume to block. |
| `Mode` | enum `enable` \| `disable` \| `toggle` | No | `enable` | `enable` blocks, `disable` unblocks, `toggle` flips. |

```json
{ "type": "block_volume_interactions", "eventType": "ENTER", "VolumeId": "dungeon_main" }
```

### `allow_volume_interactions`
Marks a sub-volume as an interaction override inside a blocked area.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `VolumeId` | string | No | current volume | Override volume. |
| `Mode` | enum `enable` \| `disable` \| `toggle` | No | `enable` | Enable/disable/toggle the override. |

```json
{ "type": "allow_volume_interactions", "eventType": "ENTER", "VolumeId": "keyhole_alcove" }
```

### `set_volume_interactable`
Adds a runtime interaction prompt (action-bar hint) to a volume. Overrides static interactable tags
until cleared or restart.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `VolumeId` | string | No | current volume | Volume to make interactable. |
| `Message` | string | No | — | Hint key or direct prompt text. |
| `Action` | string | No | — | `{action}` substitution / synthetic tag value. |
| `Key` | string | No | — | `{key}` substitution (e.g. `F`). |
| `Name` | string | No | — | `{name}` substitution (target name). |
| `InteractionType` | string | No | — | Optional interaction type filter. |

```json
{ "type": "set_volume_interactable", "eventType": "ENTER", "VolumeId": "reward_chest", "Message": "interactionHints.open", "Action": "open", "Key": "F", "Name": "reward chest", "InteractionType": "use" }
```

### `clear_volume_interactable`
Clears the runtime interactable override for a volume.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `VolumeId` | string | No | current volume | Volume to clear. |

```json
{ "type": "clear_volume_interactable", "eventType": "EXIT", "VolumeId": "reward_chest" }
```

---

## Persistent Player Tags

Boolean flags that **survive reconnects and server restarts** (saved under the HyExtras data
directory). Distinct from variables. See [Player State](player-state#tags) and the
[`has_tag`](conditions#has_tag) condition.

### `add_tag`
Adds a persistent tag to the triggering player.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Tag` | string | Yes | — | Tag to add. |

```json
{ "type": "add_tag", "eventType": "ENTER", "Tag": "storyline_a_active" }
```

### `remove_tag`
Removes a persistent tag from the triggering player.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Tag` | string | Yes | — | Tag to remove. |

```json
{ "type": "remove_tag", "eventType": "ENTER", "Tag": "storyline_a_active" }
```

---

## TagNPC Entity State

Runtime tags/variables and visibility for any UUID-backed entity (NPC/mob). All TagNPC actions share a
target-selection trio. See [TagNPC](tag-npc).

**Shared targeting fields** (used by every `tag_npc_*` action below):

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Target` | enum `triggering_entity` \| `entity_uuid` \| `target_tag` | No | `triggering_entity` | How to select the entity. |
| `EntityUuid` | string | No | — | Explicit entity UUID (when `Target=entity_uuid`). |
| `TargetTag` | string | No | — | Apply to all entities carrying this tag (when `Target=target_tag`). |

### `tag_npc_add_tag`
Adds a runtime tag to the selected entity/entities.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields above. |
| `Tag` | string | Yes | — | Tag to add. |

```json
{ "type": "tag_npc_add_tag", "eventType": "ENTER", "Target": "triggering_entity", "Tag": "quest_guard" }
```

### `tag_npc_remove_tag`
Removes a runtime tag.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `Tag` | string | Yes | — | Tag to remove. |

### `tag_npc_set_variable`
Sets a runtime variable on the selected entity/entities.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `Key` | string | Yes | — | Variable name. |
| `Value` | string | Yes | — | Value to store. |

```json
{ "type": "tag_npc_set_variable", "eventType": "ENTER", "Target": "triggering_entity", "Key": "mood", "Value": "alert" }
```

### `tag_npc_add_variable`
Adds to a numeric runtime entity variable.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `Key` | string | Yes | — | Variable name. |
| `Delta` | long | No | `1` | Amount to add. |

### `tag_npc_remove_variable`
Removes a runtime entity variable.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `Key` | string | Yes | — | Variable to remove. |

### `tag_npc_hide_entity`
Hides the selected entity from a viewer through PacketAPI entity visibility state. Visual hiding of
non-player entities requires `advancedPacketActions=true` **and** `entityPacketFiltering=true`.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `ViewerUuid` | string | No | triggering player | UUID of the viewer the entity should be hidden from. |

```json
{ "type": "tag_npc_hide_entity", "eventType": "ENTER", "Target": "target_tag", "TargetTag": "ambush_mob" }
```

### `tag_npc_show_entity`
Clears the hide state for a viewer.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `ViewerUuid` | string | No | triggering player | Viewer to restore visibility for. |

---

## Floating Items

Decorative, non-pickup item displays. See [Floating Items](floating-items) and the
[`floating_item_exists`](conditions#floating_item_exists) /
[`floating_item_intangible`](conditions#floating_item_intangible) conditions.

### `floating_item_create`
Creates (or replaces) a floating item. Position comes from the `Anchor` plus optional explicit
coordinates and offsets; tuning fields fall back to [configuration](configuration#floatingitems)
defaults when omitted.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Id` | string *(templated)* | Yes | — | Unique floating-item ID. |
| `Item` | ItemStack | Yes | — | Item to display, e.g. `{ "itemId": "hytale:gold_coin", "quantity": 1 }`. |
| `Anchor` | enum `triggering_entity` \| `volume_center` \| `block_position` \| `explicit` | No | resolver default | Placement anchor. |
| `X` / `Y` / `Z` | float | No | — | Explicit coordinates (used with `Anchor=explicit`). |
| `OffsetX` / `OffsetY` / `OffsetZ` | float | No | `0.0` | Offset added to the anchor position. |
| `Persistent` | boolean | No | `floatingItems.defaultPersistent` (`false`) | Survive restarts / re-render on reload. |
| `Intangible` | boolean | No | `floatingItems.defaultIntangible` (`true`) | No collision when `true`. |
| `Scale` | float | No | `floatingItems.defaultVisibilityRadius`-tuned | Visual scale. |
| `VisibilityRadius` | float | No | `floatingItems.defaultVisibilityRadius` (`48.0`) | Render distance. |
| `BobAmplitude` | float | No | `floatingItems.defaultBobAmplitude` (`0.15`) | Vertical bob height. |
| `RotationDegreesPerSecond` | float | No | `floatingItems.defaultRotationDegreesPerSecond` (`45.0`) | Spin speed. |
| `Priority` | integer | No | `0` | Render priority. |

```json
{ "type": "floating_item_create", "eventType": "ENTER", "Id": "ruins_key_display", "Item": { "itemId": "hytale:gold_coin", "quantity": 1 }, "Anchor": "volume_center", "OffsetY": 1.0, "Persistent": true, "Intangible": true }
```

### `floating_item_move`
Moves an existing floating item to a new anchor/position.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Id` | string | Yes | — | Floating-item ID to move. |
| `Anchor` | enum (same as create) | No | — | New placement anchor. |
| `X` / `Y` / `Z` | float | No | — | Explicit coordinates. |
| `OffsetX` / `OffsetY` / `OffsetZ` | float | No | `0.0` | Offset from the anchor. |

```json
{ "type": "floating_item_move", "eventType": "TAG_ADDED", "Id": "ruins_key_display", "Anchor": "triggering_entity", "OffsetY": 1.5 }
```

### `floating_item_remove`
Removes a floating item by ID.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Id` | string | Yes | — | Floating-item ID to remove. |

```json
{ "type": "floating_item_remove", "eventType": "EXIT", "Id": "ruins_key_display" }
```

### `floating_item_set_intangible`
Sets the intangible (no-collision) state of a floating item.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Id` | string | Yes | — | Floating-item ID. |
| `Intangible` | boolean | Yes | — | `true` = no collision, `false` = collidable. |

```json
{ "type": "floating_item_set_intangible", "eventType": "TAG_ADDED", "Id": "ruins_key_display", "Intangible": false }
```

---

## See also

- [Conditions Reference](conditions) — gate these effects.
- [String Templates](string-templates) — placeholders and color codes used in templated fields.
- [Commands](commands) — inspect/modify the same state from the console.
- [Developer API](developer-api) — drive the same systems from Java.
- [What's native vs. HyExtras](getting-started#native-vs-hyextras) — effects already provided by Hytale.
