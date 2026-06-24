# Floating Items

Floating Items are decorative item displays that look like floating dropped items but are **never**
rewards, pickups, or inventory drops. Use them for quest markers, display pedestals, or ambient
decoration. The renderer never spawns a collectible world-drop entity, and if the display backend
isn't available it records state and returns a failure message instead of crashing.

**Provided by:** the `floating_items` [module](modules.md). Created from
[trigger effects](../effects.md#floating-items), [commands](../commands.md#hextras-floatingitems), or the
[Developer API](../DEVELOPER_API.md#floatingitems).

---

## Creating

- **Effect:** [`floating_item_create`](../effects.md#floating_item_create) (uses `ItemStack.CODEC` for
  the `Item` field, so the editor's item browser works).
- **Command:** `/hextras floatingitems create <id> <player> <itemId> [persistent]`.
- **API:** `createFloatingItem(id, item, store, position, tuning, persistent)` or
  `createFloatingItemAtPlayer(id, player, item, tuning, persistent)`.

IDs are normalized to lowercase and must match `[a-z0-9_.-]+`. Creating with an existing ID replaces
it. The total number of items is capped by [`floatingItems.maxItems`](../configuration.md#floatingitems)
(default 512).

### Placement
`Anchor` chooses the reference point; `X/Y/Z` give explicit coordinates and `OffsetX/Y/Z` nudge from
the anchor:

| `Anchor` | Position |
|---|---|
| `triggering_entity` | The triggering entity's position. |
| `volume_center` | The current volume's center. |
| `block_position` | The interacted block position. |
| `explicit` | The `X`/`Y`/`Z` coordinates. |

### Tuning
Tuning fields fall back to [configuration defaults](../configuration.md#floatingitems) when omitted or
non-positive:

| Field | Default | Meaning |
|---|---|---|
| `Scale` | `1.0` | Visual size. |
| `VisibilityRadius` | `48.0` | Render distance. |
| `BobAmplitude` | `0.15` | Vertical bob height. |
| `RotationDegreesPerSecond` | `45.0` | Spin speed. |
| `OffsetX/Y/Z` | `0.0` | Position offset. |
| `Priority` | `0` | Render priority. |
| `Intangible` | `floatingItems.defaultIntangible` (`true`) | No collision when `true`. |

```json
{
  "type": "floating_item_create", "eventType": "ENTER",
  "Id": "ruins_key_display",
  "Item": { "itemId": "hytale:gold_coin", "quantity": 1 },
  "Anchor": "volume_center", "OffsetY": 1.0,
  "Persistent": true, "Intangible": true
}
```

---

## Managing

| Operation | Effect | Command | API |
|---|---|---|---|
| Move | [`floating_item_move`](../effects.md#floating_item_move) | — | `moveFloatingItem(id, store, position)` |
| Toggle collision | [`floating_item_set_intangible`](../effects.md#floating_item_set_intangible) | `… intangible <id> <bool>` | `setFloatingItemIntangible(id, bool)` |
| Remove | [`floating_item_remove`](../effects.md#floating_item_remove) | `… remove <id>` | `removeFloatingItem(id)` |
| Inspect | — | `… list` / `… info <id>` | `snapshotFloatingItem(id)`, `snapshotFloatingItems()`, `snapshotFloatingItemsNear(...)` |

Conditions [`floating_item_exists`](../conditions.md#floating_item_exists) and
[`floating_item_intangible`](../conditions.md#floating_item_intangible) let triggers branch on item state.

---

## Persistence

- **Persistent items** (`Persistent: true`) are saved to **`floating-items.properties`** in the data
  directory and re-rendered on startup and on `/hextras floatingitems reload`.
- **Runtime items** (the default) exist only until the server stops.

---

## See also

- [Effects → Floating Items](../effects.md#floating-items) · [Conditions](../conditions.md#floating-items)
- [Commands](../commands.md#hextras-floatingitems) · [Configuration](../configuration.md#floatingitems)
- [Developer API → FloatingItems](../DEVELOPER_API.md#floatingitems)
