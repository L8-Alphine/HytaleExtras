# Fine-Tuning

This page documents the fine-tuning pass across HyExtras' runtime systems: new config knobs,
opt-in persistence, richer condition operators, a change-event bus, and NPC/Mob interaction.
Everything here is **backward-compatible** — defaults preserve the previous behavior.

## New configuration keys

All live in `hyextras.properties` (see [Configuration](configuration) for the full list).

| Key | Default | Purpose |
|---|---|---|
| `playerVisibilitySyncIntervalMs` | `500` | Interval between player visibility policy sync passes (clamped to ≥ 50 ms). Applied on reload. |
| `interactionReplayWindowMs` | `50` | Window in which a repeated identical interaction is de-duplicated as a replay. |
| `playerVariablesPersistent` | `false` | Persist **all** player variables to disk (opt-in). |
| `persistence.async` | `true` | Offload tag/variable disk I/O to a background thread (disconnect saves still flush synchronously). |
| `persistence.saveDebounceMs` | `1000` | Coalesce repeated background saves for the same player. |
| `variable.regexEnabled` | `true` | Allow the `regex` operator in variable conditions. |
| `tagNpc.stateRetentionSeconds` | `10` | Inactivity seconds before runtime TagNPC entity state is pruned. |
| `tagNpc.statePersistent` | `false` | Reserved for opt-in TagNPC disk persistence of stable/named entities. |

## Variable persistence

Player variables are memory-only by default (cleared on disconnect — legacy behavior). To persist:

- Set `playerVariablesPersistent=true` to persist every variable, **or**
- Prefix individual keys with `persist:` (e.g. `persist:questStage`) to persist just those.

Persisted variables are written to `{dataDir}/players/{uuid}.vars` (type-tagged so numbers/booleans
round-trip), loaded on connect, and saved on disconnect. Tags continue to persist as before. Writes
are now **atomic** (temp file + rename) and, with `persistence.async=true`, off the event thread.

`HyExtrasApi.saveVariables(uuid)` forces a save without clearing.

## Richer variable operators

`variable_condition` and `tagnpc_variable_condition` share one operator set:

`exists`, `not_exists`, `equals`, `not_equals`, `greater_than`, `less_than`,
**`greater_or_equal`**, **`less_or_equal`**, **`divisible_by`**, **`contains`**, **`regex`**.

Numeric operators coerce both sides to a long (decimal strings truncate). `regex` matches the whole
value, is pattern-cached, capped at 256 chars, and gated by `variable.regexEnabled`.

```json
{ "type": "variable_condition", "Key": "lap", "Operator": "divisible_by", "Value": "5" }
{ "type": "variable_condition", "Key": "name", "Operator": "regex", "Value": "[A-Z][a-z]+" }
```

## Change events (developer API)

Subscribe to state changes instead of polling:

```java
AutoCloseable handle = HyExtrasApi.get().subscribe(
        HyExtrasEvents.PlayerVariableChangeEvent.class,
        e -> log("var " + e.key() + " -> " + e.value()));
// later: handle.close();
```

Event types: `PlayerTagChangeEvent`, `PlayerVariableChangeEvent`, `EntityTagChangeEvent`,
`EntityVariableChangeEvent`, `VisibilityChangeEvent`, `InteractionEvent`. Listeners run synchronously
on the posting thread and are isolated (one throwing listener can't break others). See
[Developer API](developer-api).

## NPC & Mob interaction

`PlayerInteractEvent` carries the interacted entity, so right-click/use on an NPC or mob now drives
interaction triggers. When a player interacts with an entity, HyExtras records its UUID in the
per-player variable `hextras_interacted_entity` and fires an `InteractionEvent` with `targetEntity`.

Target the interacted entity from any TagNPC effect/condition:

```json
// Give the clicked NPC a tag, then react to it
{ "type": "tagnpc_add_tag", "Target": "interacted_entity", "Tag": "talked_to" }
{ "type": "tagnpc_has_tag", "Target": "interacted_entity", "Tag": "merchant" }
```

`Target: "interacted_entity"` (or `EntityUuid: "{variable:hextras_interacted_entity}"`) resolves to
the NPC/mob the player most recently interacted with. See [TagNPC](tag-npc) and
[Interaction Bridge](interaction-bridge).

## Conditional interaction prompts

By default an interactable volume's prompt always shows. Add the volume tag
`hextras:interaction_prompt_conditional=true` to show the prompt **only after the effect chain's
conditions pass** — e.g. show "Open" only when the door is unlocked, and use rejection effects for the
"Locked" feedback.

## TagNPC retention & display names

- Runtime entity state pruning is now configurable via `tagNpc.stateRetentionSeconds` (was a fixed 10 s).
- Entities can carry a developer-defined display name: `HyExtrasApi.setEntityDisplayName(uuid, name)` /
  `getEntityDisplayName(uuid)` (also surfaced on `TagNpcEntityState`).

## API spike findings (Hytale Server 0.5.6)

Recorded so the capability boundary is explicit:

- **NPC interaction — available.** `PlayerInteractEvent.getTargetEntity()` / `getTargetRef()` expose the
  interacted entity. Implemented (see above).
- **Per-viewer appearance — partial.** `NameplateUpdate` (a `ComponentUpdate`) makes per-viewer
  **nameplate/nametag** override feasible through the outbound `EntityUpdates` filter. Glow, team,
  equipment, and fake-spawn are **not** exposed as discrete packets in 0.5.6 and are not implemented.

## Known follow-ups (not yet implemented)

- Per-viewer nameplate override via the experimental entity packet filter.
- Batched hide/show packet flush; entity-type (NPC vs mob) classification.
- Optional TagNPC disk persistence for stable/named entities (`tagNpc.statePersistent` is reserved).
- Cooldown scope selection (per-player / per-volume / global).
- Optional tag values/metadata; persistence of interaction block/allow volume state.
