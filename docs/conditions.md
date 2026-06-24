# Conditions (Trigger Conditions) Reference

Complete reference for every HyExtras **condition** — the custom Trigger Volume conditions registered
by the `trigger_extras` module. There are **14 conditions**.

Conditions are evaluated before a volume's effects run; **all conditions must pass** or the effects are
skipped (and the volume's rejection effects may run instead). Like effects, each condition carries the
native `eventType` field plus its own fields below.

> **Operators & `Invert`.** Many conditions expose an `Operator` enum or an `Invert` boolean. For
> comparison operators (`equals`, `greater_than`, …) the `Value` field supplies the right-hand side.
> `Invert: true` flips the pass/fail result.

**Jump to:** [Variables & State](#variables--state) · [Permissions & Player](#permissions--player) ·
[World & Interaction](#world--interaction) · [Volume](#volume) · [TagNPC](#tagnpc) ·
[Floating Items](#floating-items)

---

## Variables & State

### `variable_condition`
Tests a per-player variable.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Key` | string | Yes | — | Variable name. |
| `Operator` | enum `exists` \| `not_exists` \| `equals` \| `not_equals` \| `greater_than` \| `less_than` | Yes | — | Comparison. `exists`/`not_exists` ignore `Value`. |
| `Value` | string | No* | — | Right-hand side for comparison operators (*required for those). |

```json
{ "type": "variable_condition", "eventType": "ENTER", "Key": "score", "Operator": "greater_than", "Value": "10" }
```

### `cooldown_ready`
Passes only when a named HyExtras cooldown is **not** active (absent or expired). Pair with
[`apply_cooldown`](effects.md#apply_cooldown).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Name` | string | Yes | — | Cooldown name to check. |

```json
{ "type": "cooldown_ready", "eventType": "ENTER", "Name": "ruins_intro" }
```

### `has_tag`
Passes when the triggering player has (or, with `Invert`, lacks) a persistent tag. See
[`add_tag`](effects.md#add_tag).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Tag` | string | Yes | — | Persistent tag to check. |
| `Invert` | boolean | No | `false` | Pass only when the player does **not** have the tag. |

```json
{ "type": "has_tag", "eventType": "ENTER", "Tag": "storyline_a_active" }
{ "type": "has_tag", "eventType": "ENTER", "Tag": "storyline_a_active", "Invert": true }
```

### `math_condition`
Evaluates an arithmetic formula and compares it to a value. The formula is template-resolved, then
evaluated by the [arithmetic engine](systems/player-state.md#arithmetic).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Formula` | string *(templated)* | Yes | — | Expression, e.g. `{variable:score} + 5`. |
| `Operator` | enum `equals` \| `not_equals` \| `greater_than` \| `less_than` \| `greater_or_equal` \| `less_or_equal` | Yes | — | Comparison. |
| `Value` | string *(templated)* | Yes | — | Right-hand side (a number or another formula). |

```json
{ "type": "math_condition", "eventType": "ENTER", "Formula": "{variable:score} + 5", "Operator": "greater_or_equal", "Value": "10" }
```

---

## Permissions & Player

### `is_operator`
Passes if the triggering player is a server operator (Hytale's operator group), or has a specific
permission.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Permission` | string | No | operator group | Custom permission to check instead of operator status. |
| `Invert` | boolean | No | `false` | Pass only when the player is **not** an operator / lacks the permission. |

```json
{ "type": "is_operator", "eventType": "ENTER" }
```

### `player_hidden`
Passes when the triggering player currently has `TargetPlayer` hidden from their view. Tests
server-side state, so it works even when the hide was applied with `UsePackets: false`. See
[`player_hide_entity`](effects.md#player_hide_entity).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `TargetPlayer` | string | Yes | — | Username to test for being hidden. |
| `Invert` | boolean | No | `false` | Pass only when the target is **not** hidden. |

```json
{ "type": "player_hidden", "eventType": "ENTER", "TargetPlayer": "OtherPlayer" }
```

---

## World & Interaction

### `world_time_between`
Passes if the current world hour (0–23) is within `[FromHour, ToHour]`. Supports midnight wrap-around
(e.g. `FromHour: 20, ToHour: 6` covers night).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `FromHour` | integer (0–23) | Yes | — | Start hour, inclusive. |
| `ToHour` | integer (0–23) | Yes | — | End hour, inclusive. |

```json
{ "type": "world_time_between", "FromHour": 6, "ToHour": 18 }
```

### `interaction_type`
Filters interactions inside the [Interaction Bridge](systems/interaction-bridge.md). Must be used on
effects with `eventType: TAG_ADDED` in volumes carrying the `hextras:interact` static tag. With no
`Interaction` set, it passes for any bridge interaction.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Interaction` | enum (see values below) | No | any | Specific interaction type to match. |
| `Invert` | boolean | No | `false` | Pass for anything **other** than the given type. |

**`Interaction` values:** `primary`, `secondary`, `ability1`, `ability2`, `ability3`, `use`, `pick`,
`pickup`, `collision_enter`, `collision_leave`, `collision`, `entity_stat_effect`, `swap_to`,
`swap_from`, `death`, `wielding`, `projectile_spawn`, `projectile_hit`, `projectile_miss`,
`projectile_bounce`, `held`, `held_offhand`, `equipped`, `dodge`, `game_mode_swap`.

```json
{ "type": "interaction_type", "eventType": "TAG_ADDED", "Interaction": "use" }
{ "type": "interaction_type", "eventType": "TAG_ADDED", "Interaction": "primary", "Invert": true }
```

---

## Volume

### `volume_has_tag`
Passes if a named volume has (or, with `Invert`, lacks) a tag key, optionally matching an exact value.
Reads both static editor tags and runtime tags. See [`set_volume_tag`](effects.md#set_volume_tag).

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `TargetVolumeId` | string | No | current volume | Volume to inspect. |
| `Key` | string | Yes | — | Tag key. |
| `Value` | string | No | any value | Require this exact value. |
| `Invert` | boolean | No | `false` | Pass when the tag is **absent** (or value mismatches). |

```json
{ "type": "volume_has_tag", "eventType": "ENTER", "TargetVolumeId": "gate_a", "Key": "opened", "Value": "1" }
```

---

## TagNPC

Conditions over runtime entity state. They share the same targeting trio as the
[TagNPC effects](effects.md#tagnpc-entity-state). See [TagNPC](systems/tag-npc.md).

**Shared targeting fields:**

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Target` | enum `triggering_entity` \| `entity_uuid` \| `target_tag` | No | `triggering_entity` | Entity selection mode. |
| `EntityUuid` | string | No | — | Explicit entity UUID. |
| `TargetTag` | string | No | — | Match entities carrying this tag. |

### `tag_npc_has_tag`
Passes if the targeted entity has a runtime tag.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `Tag` | string | Yes | — | Tag to check. |
| `Invert` | boolean | No | `false` | Pass when absent. |

```json
{ "type": "tag_npc_has_tag", "Target": "triggering_entity", "Tag": "quest_guard" }
```

### `tag_npc_variable_condition`
Tests a runtime entity variable.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `Key` | string | Yes | — | Variable name. |
| `Operator` | enum `exists` \| `not_exists` \| `equals` \| `not_equals` \| `greater_than` \| `less_than` | Yes | — | Comparison. |
| `Value` | string | No* | — | Right-hand side (*required for comparison operators). |

```json
{ "type": "tag_npc_variable_condition", "Target": "triggering_entity", "Key": "mood", "Operator": "equals", "Value": "alert" }
```

### `tag_npc_visible_condition`
Tests whether a targeted entity is visible (or hidden) for a viewer.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| *(targeting trio)* | | | | See shared fields. |
| `ViewerUuid` | string | No | triggering player | Viewer to test visibility for. |
| `ExpectedVisible` | boolean | No | `true` | Pass when the entity's visibility matches this value. |

```json
{ "type": "tag_npc_visible_condition", "Target": "target_tag", "TargetTag": "ambush_mob", "ExpectedVisible": false }
```

---

## Floating Items

### `floating_item_exists`
Passes if a floating item ID is currently registered.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Id` | string | Yes | — | Floating-item ID. |
| `Invert` | boolean | No | `false` | Pass when the ID does **not** exist. |

```json
{ "type": "floating_item_exists", "Id": "ruins_key_display" }
```

### `floating_item_intangible`
Passes if a floating item is currently marked intangible.

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `Id` | string | Yes | — | Floating-item ID. |
| `Invert` | boolean | No | `false` | Pass when the item is **tangible**. |

```json
{ "type": "floating_item_intangible", "Id": "ruins_key_display", "Invert": true }
```

---

## See also

- [Effects Reference](effects.md) — actions gated by these conditions.
- [String Templates](string-templates.md) — placeholders and the rule mini-language.
- [Native conditions](getting-started.md#native-vs-hyextras) — conditions already provided by Hytale (Permission, Cooldown, RandomChance, Tag, Item, BlockType, GameMode, PlayerCount).
