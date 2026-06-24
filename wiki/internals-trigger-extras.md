# Internals: TriggerExtras

> **Contributor reference.** How HyExtras plugs effects, conditions, and the interaction bridge into
> the native Trigger Volume system. For native-API notes see
> [Trigger Volume Research](trigger-volume-research).

## Registration

Effects and conditions are registered once at startup, each with a `BuilderCodec`:

- `TriggerActionRegistry` — `TYPE_IDS` (41 ids) and `registerAll()` map each id to its action class +
  `CODEC`. Note `add_variable` and `increment_variable` both map to `IncrementVariableAction`.
- `TriggerConditionRegistry` — `TYPE_IDS` (14) and `registerAll()`.

Both call through **`TriggerVolumeApiAdapter`**, the single choke point for native
`TriggerVolumesPlugin` calls. Every call is wrapped in try/catch so a broken type can't crash the mod:

| Adapter method | Purpose |
|---|---|
| `registerEffect(id, class, codec)` / `registerCondition(...)` | Register a type with the native plugin. |
| `getEntityUuid(ctx)` | Triggering UUID via `PlayerRef`, falling back to `UUIDComponent` (NPCs/mobs). |
| `getManagerForStore(store)` | Resolve the world's `TriggerVolumeManager`. |
| `getWorldHour(store)` | Current world hour (0–23), used by `world_time_between`. |
| `getHiddenPlayersManager(store, ref)` | The player's `HiddenPlayersManager`. |
| `getPlayerUuidByName(store, name)` | Online player lookup. |

## The CODEC pattern

Every effect/condition declares a `public static final BuilderCodec<T> CODEC` built from
`TriggerEffect.BASE_CODEC` / `TriggerCondition.BASE_CODEC` plus fields from
[`CodecHelper`](internals-services#supporting-utilities-util-config-codec):

```java
public static final BuilderCodec<MyAction> CODEC = BuilderCodec.builder(
            MyAction.class, MyAction::new, TriggerEffect.BASE_CODEC)
    .append(CodecHelper.string("Key"),    MyAction::setKey,   MyAction::getKey).add()
    .append(CodecHelper.optLong("Delta"), MyAction::setDelta, MyAction::getDelta).add()
    .build();
```

`CodecHelper.string/bool/integer/float/enumField` are **required** fields; the `opt*` variants are
optional. The string literal is the JSON key. Enums use `optEnum`/`enumField` with an alias map that
documents the friendly JSON values.

## Dispatch flow

Native enter/exit/tick events are handled by Hytale. HyExtras adds `ExtraTriggerDispatcher` for
**synthetic** events — interaction-bridge dispatch and [`trigger_named_volume`](effects#trigger_named_volume)
chaining. `dispatch(...)`:

1. Bails if the `trigger_extras` module is disabled or the volume is disabled.
2. Optionally respects the volume's per-entity cooldown.
3. Builds a `TriggerContext` (entity ref, store, event type, volume, spatial volumes, tag key/value,
   block position).
4. For each condition matching the event type: `test(ctx)`; on failure, fires the volume's **rejection
   effects** and stops; on success, runs `applyOnAccept(ctx)`.
5. Fires the volume's effects matching the event type, then records the activation for cooldown.

Every condition/effect call is individually try/caught, so one failure logs and skips rather than
aborting the dispatch.

## Interaction bridge

`TriggerExtrasInteractionBridge` (driven by `PlayerInteractEvent` and `UseBlockInteractionSystem` →
`UseBlockEvent.Pre`) turns interactions into synthetic `TAG_ADDED` dispatches with
`tagKey="hextras_interact"`. It also owns:

- `InteractionTriggerService` — tracks the cancel-pending flag set by `cancel_interaction`, plus a
  ~50 ms replay cache so one physical interaction doesn't double-fire.
- `InteractableVolumeState` — resolves static/runtime interactable config and matches interaction types.
- The runtime `interactionBlockedVolumeIds` / `interactionAllowedVolumeIds` sets.

See [Interaction Bridge](interaction-bridge) for the creator-facing behavior.

## Runtime gating

Effects/conditions call `TriggerExtrasRuntime.isEnabled()` (and often
`HyExtrasPlugin.isModuleEnabled(...)`) at the top of `execute`/`test`, so disabling the module stops
behavior immediately — even for already-loaded volumes.

## Adding a new effect or condition

1. Create the class under `triggerextras/action/` (or `advanced/`, `tagnpc/`, `floatingitems/`) for an
   effect, or `triggerextras/condition/` for a condition; extend `TriggerEffect`/`TriggerCondition`.
2. Add a `public static final BuilderCodec<T> CODEC` using `CodecHelper` fields; add a no-arg
   constructor and getters/setters for each field.
3. Implement `execute(ctx)` / `test(ctx)`. Guard with `TriggerExtrasRuntime.isEnabled()` (+ the owning
   module for non-core systems). Resolve the UUID via `TriggerVolumeApiAdapter.getEntityUuid(ctx)` and
   text via [`StringTemplate.resolve`](string-templates). Wrap the body in try/catch and log on
   failure (match the existing `[type_id] …` log style).
4. Register it: add the type id to `TYPE_IDS` and a `registerEffect`/`registerCondition` line in the
   matching registry.
5. Document it in [`docs/effects.md`](effects) or [`docs/conditions.md`](conditions) and add
   an example to `examples/hyextras-examples.json`.

## See also

- [Architecture](architecture#registration--data-flow) · [Internals: Services](internals-services) · [Internals: Packet stack](internals-packet-stack)
- [Effects](effects) · [Conditions](conditions)
