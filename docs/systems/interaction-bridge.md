# Interaction Bridge

The interaction bridge lets a Trigger Volume react when a player **interacts** with something
(a door, chest, lever, entity…) while inside the volume — something native volumes don't expose. It
also powers interaction **blocking** and runtime **interactable prompts**.

The bridge listens to `PlayerInteractEvent` and `UseBlockEvent.Pre` globally, so it fires for any
player in any world. Because it runs synchronously *before* the native interaction resolves,
[`cancel_interaction`](../effects.md#cancel_interaction) reliably blocks the interaction in time.

**Sections:** [Basic bridge](#enabling-the-basic-bridge) · [Interaction types](#interaction-types) ·
[Interaction blocking](#interaction-blocking) · [Interactable volumes](#interactable-volumes)

---

## Enabling the basic bridge

1. Add the static tag **`hextras:interact`** to the volume in the editor (any value, e.g. `1`).
2. Add conditions/effects to that volume with event type **`TAG_ADDED`**.
   - The bridge fires them with a synthetic context where `tagKey = "hextras_interact"` and
     `tagValue = <InteractionType>` (e.g. `Use`, `Primary`, `Secondary`).
   - Conditions evaluate first; all must pass or effects are skipped.
3. To deny the interaction, add [`cancel_interaction`](../effects.md#cancel_interaction) to the effects
   (or rejection effects). To shove the player back, add
   [`push_back_player`](../effects.md#push_back_player).

Filter by interaction kind with the [`interaction_type`](../conditions.md#interaction_type) condition:

```json
{
  "staticTags": { "hextras:interact": "1" },
  "conditions": [ { "type": "interaction_type", "eventType": "TAG_ADDED", "Interaction": "use" } ],
  "effects": [
    { "type": "cancel_interaction", "eventType": "TAG_ADDED" },
    { "type": "send_rich_message",  "eventType": "TAG_ADDED", "Message": "&cThis door is sealed." }
  ]
}
```

> Interactions are de-duplicated within a ~50 ms window per player+block so a single physical
> interaction doesn't double-fire.

---

## Interaction types

The `tagValue` is the native `InteractionType` name. The [`interaction_type`](../conditions.md#interaction_type)
condition maps friendly values to these. Full list: `primary`, `secondary`, `ability1`–`ability3`,
`use`, `pick`, `pickup`, `collision_enter`, `collision_leave`, `collision`, `entity_stat_effect`,
`swap_to`, `swap_from`, `death`, `wielding`, `projectile_spawn`, `projectile_hit`, `projectile_miss`,
`projectile_bounce`, `held`, `held_offhand`, `equipped`, `dodge`, `game_mode_swap`.

---

## Interaction blocking

Block *all* interactions for players inside a volume, with sub-volume overrides. The blocked/allowed
volume IDs are runtime-only sets (cleared on restart).

| Effect | Behavior |
|---|---|
| [`block_volume_interactions`](../effects.md#block_volume_interactions) | Adds a volume to the blocked set. Players inside it can't interact with anything. |
| [`allow_volume_interactions`](../effects.md#allow_volume_interactions) | Adds a sub-volume to the allowed-override set; players inside it bypass the block. |

Resolution: if a player is in **any** blocked volume **and not** in any allowed override volume, the
interaction is cancelled. Both effects accept `Mode` (`enable`/`disable`/`toggle`).

```json
// Lock the dungeon door area...
{ "type": "block_volume_interactions", "eventType": "ENTER", "VolumeId": "dungeon_main" }
// ...but keep the key-hole alcove usable.
{ "type": "allow_volume_interactions", "eventType": "ENTER", "VolumeId": "keyhole_alcove" }
```

---

## Interactable volumes

Give a volume an action-bar interaction **prompt**. Prompts are delivered through the PacketAPI
action-bar path (so a native prompt backend can replace them later), and resolve Hytale
`interactionHints.*` localization keys (with built-in English fallbacks) plus `{action}`, `{key}`,
and `{name}` substitutions.

### Static (editor) tags

| Static tag | Meaning |
|---|---|
| `hextras:interactable=true` | Marks the volume interactable without the legacy `hextras:interact` tag. |
| `hextras:interaction_message=interactionHints.open` | Hint key or direct prompt text. |
| `hextras:interaction_action=<text>` | `{action}` value / synthetic tag value. |
| `hextras:interaction_key=<text>` | `{key}` substitution (e.g. `F`). |
| `hextras:interaction_name=<text>` | `{name}` substitution. |
| `hextras:interaction_type=use` | Optional interaction-type filter. |

### Runtime overrides

[`set_volume_interactable`](../effects.md#set_volume_interactable) and
[`clear_volume_interactable`](../effects.md#clear_volume_interactable) override the static tags until
cleared or restart — useful when a quest step should make something interactable temporarily.

```json
[
  { "type": "set_volume_interactable",   "eventType": "ENTER", "VolumeId": "reward_chest", "Message": "interactionHints.open", "Action": "open", "Key": "F", "Name": "reward chest", "InteractionType": "use" },
  { "type": "clear_volume_interactable", "eventType": "EXIT",  "VolumeId": "reward_chest" }
]
```

---

## See also

- [Effects](../effects.md#interaction) — interaction effects.
- [`interaction_type` condition](../conditions.md#interaction_type).
- [Internals: TriggerExtras](../internals/trigger-extras.md) — the bridge implementation.
