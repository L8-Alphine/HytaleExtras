# Architecture

This is a contributor-oriented overview of how HyExtras is put together: the package layout, the
plugin lifecycle, how trigger effects/conditions are registered, and how data flows from a volume
event to a state change or packet. For the per-class method details, see
[Internals: Services](internals-services), [Internals: TriggerExtras](internals-trigger-extras),
and [Internals: Packet stack](internals-packet-stack). Notes on the **native** Hytale API live
in [Trigger Volume Research](trigger-volume-research).

## Big picture

```
HyExtrasApi (public facade)
        │  delegates to
        ▼
HyExtrasPlugin ── owns ──► services (variables, tags, cooldowns, overrides, targeting,
   │                                  packetApi, imageIcons, tagNpc, floatingItems)
   │                       ── owns ──► InternalModuleManager ──► 6 modules
   │                       ── owns ──► TriggerExtrasInteractionBridge
   └── registers effects/conditions through ──► TriggerVolumeApiAdapter ──► native TriggerVolumes
```

- **[`HyExtrasApi`](developer-api)** is the only stable surface for other mods. It delegates to the
  plugin's services.
- **`HyExtrasPlugin`** is the singleton entry point: it constructs the services, registers events and
  commands, and exposes accessors.
- **Services** hold all state and behavior. They are plain classes, not the public API.
- **[Modules](modules)** gate whole feature areas on/off; most effects/conditions re-check
  their owning module at runtime.
- **`TriggerVolumeApiAdapter`** is the single choke point for native TriggerVolumes calls.

## Package layout

```
org.hyzionstudios.hyextras/
├── HyExtrasPlugin               — entry point, singleton, player registry, lifecycle
├── TriggerVolumeApiAdapter      — all native TriggerVolumesPlugin calls go through here
├── TargetingPreventionSystem    — ECS system clearing NPC TargetMemory for protected players
├── TagNpcEntityIndexSystem      — ECS system indexing UUID-backed entities for TagNPC "near"
├── I18nDefaultsLoader           — seeds interaction-hint localization defaults
├── api/            HyExtrasApi  — public facade
├── codec/          CodecHelper  — KeyedCodec factory helpers for effect/condition fields
├── command/        /hextras command tree
├── config/         HyExtrasConfig, ConfigLoader
├── module/         InternalModuleManager + the 6 module definitions
├── imageicons/     ImageIconService and supporting types
├── packetapi/      PacketApi, PacketCameraMode, service/ (visibility, sync, filter)
├── floatingitems/  FloatingItemService and supporting types
├── tagnpc/         TagNpcService and supporting types
├── service/        PlayerVariableService, CooldownService, PlayerTagService, TargetingPreventionService
├── state/          PlayerOverrideService, RuntimeStateStore
├── triggerextras/  registries, adapter glue, interaction bridge, action/, advanced/, condition/
└── util/           StringTemplate, RuleEvaluator, ArithmeticExpression, RichText, placeholder bridge
```

## Plugin lifecycle

`HyExtrasPlugin` follows the Hytale `JavaPlugin` lifecycle:

**`setup()`** — registers the module manager and interaction bridge, and registers three ECS systems
with the entity store registry: `UseBlockInteractionSystem`, `TargetingPreventionSystem`, and
`TagNpcEntityIndexSystem`.

**`start()`** — loads [config](configuration), constructs every service, builds the
`RuntimeStateStore`, initializes modules from config, runs startup diagnostics (when
`startupDiagnostics=true`), loads i18n defaults, applies packet-feature config, then registers:

- `PlayerConnectEvent` → register name/UUID, **load persisted tags**, sync packets.
- `PlayerDisconnectEvent` → the **cleanup chain** (below).
- `PlayerInteractEvent` (global) → the [interaction bridge](interaction-bridge).
- The `/hextras` command tree.

**Disconnect cleanup chain** — all per-player state is released in one place:

```
tagService.saveAndClearPlayer(uuid)   // persist tags, then drop
variableService.clearPlayer(uuid)
cooldownService.clearPlayer(uuid)
playerOverrideService.clearPlayer(uuid)
packetApi.clearPlayer(uuid)
imageIconService.clearPlayer(uuid)
tagNpcService.clearEntity(uuid)
targetingPreventionService.unprotectPlayer(uuid)
triggerExtrasInteractionBridge.clearPlayer(uuid)
```

**`reloadConfig()`** — re-reads `hyextras.properties`, updates the runtime state, refreshes modules,
and re-applies packet config (invoked by `/hextras reload`).

**`shutdown()`** — stops packet services and the imageIcons/tagNpc/floatingItems services, clears
interactable/targeting state, and drops the singleton.

The plugin also keeps the online-player registry (`playerNameToUuid`, normalized lowercase, and
`onlinePlayers`) used by name→UUID lookups and `getActiveVolumesForPlayer`.

## Registration & data flow

Effects and conditions are registered once through `TriggerVolumeApiAdapter`:

- [`TriggerActionRegistry.registerAll()`](internals-trigger-extras) registers all 41 effect type
  IDs (40 classes) with their `BuilderCodec`.
- `TriggerConditionRegistry.registerAll()` registers all 14 conditions.

At runtime, a volume event flows like this:

```
volume event (ENTER / TAG_ADDED / …)
   → TriggerContext (entity ref, store, event type, volume, tags)
   → each TriggerCondition.test(ctx)   ── all must pass ──►
   → each TriggerEffect.execute(ctx)
       → resolve UUID via TriggerVolumeApiAdapter.getEntityUuid(ctx)
       → resolve text via StringTemplate / RuleEvaluator
       → mutate a service (variables/tags/cooldowns/overrides/…)
       → optionally send a packet via PacketApi (gated by config)
```

Each effect/condition first checks `TriggerExtrasRuntime.isEnabled()` (and its owning module), so
disabling a module stops its behavior immediately.

## ECS systems

| System | Role |
|---|---|
| `UseBlockInteractionSystem` | Feeds `UseBlockEvent.Pre` into the interaction bridge so block use can be cancelled in time. |
| `TargetingPreventionSystem` | Each tick, clears protected players from nearby NPC `TargetMemory` (see [targeting protection](visibility-and-packets#targeting-protection)). |
| `TagNpcEntityIndexSystem` | Indexes UUID-backed entities with positions so [`/hextras tagnpc near`](commands#hextras-tagnpc) and `findNearestTagNpcEntity` work. |

## See also

- [Internals: Services](internals-services) · [Internals: TriggerExtras](internals-trigger-extras) · [Internals: Packet stack](internals-packet-stack)
- [Modules](modules) — the runtime gating layer.
- [Trigger Volume Research](trigger-volume-research) — native API notes.
