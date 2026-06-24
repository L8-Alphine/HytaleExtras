# TagNPC: Runtime Entity State

TagNPC stores runtime **tags** and **variables** for any UUID-backed entity (NPC, mob, or other
non-player entity), plus per-viewer **visibility** state. It is the entity-side analogue of
[player state](player-state) and is **runtime-only** (not persisted) in this version.

It's useful when a Trigger Volume context gives you a mob/NPC entity UUID, or when another mod calls
the [Developer API](developer-api#tagnpc) with an entity UUID. HyExtras also indexes UUID-backed
non-player entities with positions so tools can pick the closest one to a player.

> **Key normalization:** tag and variable keys are trimmed and lower-cased, so `Quest_Guard` and
> `quest_guard` are the same key.

---

## Targeting entities from triggers

Every `tag_npc_*` [effect](effects#tagnpc-entity-state) and
[condition](conditions#tagnpc) shares a targeting trio:

| `Target` | Selects |
|---|---|
| `triggering_entity` (default) | The entity that triggered the volume. |
| `entity_uuid` | The explicit UUID in `EntityUuid`. |
| `target_tag` | All tracked entities carrying the tag in `TargetTag`. |

```json
[
  { "type": "tag_npc_add_tag",      "eventType": "ENTER", "Target": "triggering_entity", "Tag": "quest_guard" },
  { "type": "tag_npc_set_variable", "eventType": "ENTER", "Target": "triggering_entity", "Key": "mood", "Value": "alert" },
  { "type": "tag_npc_has_tag",      "Tag": "quest_guard" }
]
```

---

## Tags & variables

- **Tags:** `tag_npc_add_tag`, `tag_npc_remove_tag`; condition `tag_npc_has_tag`.
- **Variables:** `tag_npc_set_variable`, `tag_npc_add_variable`, `tag_npc_remove_variable`; condition
  `tag_npc_variable_condition`. Numeric increment treats a missing/non-numeric value as `0`.

Each entity's state tracks a `lastSeen` timestamp. With
[`tagNpc.clearStateOnEntityUnload=true`](configuration#tagnpc), stale indexed entities (unseen for
~10 s or whose entity reference is no longer valid) are pruned along with their state.

---

## Visibility

`tag_npc_hide_entity` / `tag_npc_show_entity` (and condition `tag_npc_visible_condition`) toggle
per-viewer visibility by delegating to the [PacketAPI entity visibility](visibility-and-packets#non-player-entity-visibility)
state. The `ViewerUuid` field selects the viewer (defaults to the triggering player).

**Important:** visual hiding of non-player entities requires **both** `advancedPacketActions=true`
and `entityPacketFiltering=true`. Without them, HyExtras records the state but returns a failure
message indicating the packet path is unavailable — the entity remains visible.

---

## Admin commands

Use [`/hextras tagnpc`](commands#hextras-tagnpc) for explicit UUID edits, or the `near` subcommands
to act on the closest indexed NPC/mob to a player without copying UUIDs:

```
/hextras tagnpc near info     Alphine 12
/hextras tagnpc near tag-add  Alphine 12 quest_guard
/hextras tagnpc tag list      6f1c…-uuid
```

---

## Developer API

The facade exposes the full surface — `addEntityTag`, `setEntityVariable`, `incrementEntityVariable`,
`hideEntityFromViewer`, `snapshotTaggedEntities`, `findNearestTagNpcEntity`, and more. See
[Developer API → TagNPC](developer-api#tagnpc).

```java
api.addEntityTag(entityUuid, "quest_guard");
api.setEntityVariable(entityUuid, "mood", "alert");
UUID nearest = api.findNearestTagNpcEntity(playerUuid, 12.0D);
```

## See also

- [Effects](effects#tagnpc-entity-state) · [Conditions](conditions#tagnpc)
- [Visibility & Packets](visibility-and-packets) — the entity-filtering requirement.
- [Configuration](configuration#tagnpc).
