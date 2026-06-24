# Internals: Service Classes

> **Contributor reference.** These classes are HyExtras internals, **not** the public API. Other mods
> should use [`HyExtrasApi`](developer-api); these may change between releases. This page
> documents them for people working on HyExtras itself.

All services are constructed in `HyExtrasPlugin.start()` and reachable via `HyExtrasPlugin.get()`
getters (e.g. `getVariableService()`). Per-player state services are cleaned up in the
[disconnect chain](architecture#plugin-lifecycle).

## At a glance

| Service | Package | State | Lifetime | Thread-safety |
|---|---|---|---|---|
| `PlayerVariableService` | `service` | `UUID → (String → Object)` | Runtime | `ConcurrentHashMap` |
| `PlayerTagService` | `service` | `UUID → Set<String>` + disk | Persistent | `ConcurrentHashMap` |
| `CooldownService` | `service` | `UUID → (String → expiryMs)` | Runtime | `ConcurrentHashMap` |
| `TargetingPreventionService` | `service` | `Set<UUID>` | Runtime | `ConcurrentHashMap.newKeySet` |
| `PlayerOverrideService` | `state` | `UUID viewer → Set<UUID>` | Runtime | `ConcurrentHashMap` |
| `RuntimeStateStore` | `state` | unified accessor | — | delegates |
| `TagNpcService` | `tagnpc` | `UUID → state` + index | Runtime | `ConcurrentHashMap` |
| `FloatingItemService` | `floatingitems` | `id → instance` + disk | Runtime/persistent | `ConcurrentHashMap` |
| `ImageIconService` | `imageicons` | providers/icons/attachments | Runtime | `ConcurrentHashMap` |
| `InternalModuleManager` | `module` | `id → ManagedModule` | — | single-threaded init |

---

## PlayerVariableService

Per-player key→value store backed by a nested `ConcurrentHashMap<UUID, ConcurrentHashMap<String,Object>>`.

- `get` / `getString` / `getLong` — typed reads (`getLong` parses Number or numeric String, else `0`).
- `set(uuid, key, value)`, `remove(uuid, key)`.
- `increment(uuid, key, delta)` — atomic `merge`; missing/non-numeric treated as `0`; returns new value.
- `clearPlayer(uuid)` (disconnect), `snapshot(uuid)` (unmodifiable copy), `hasAny(uuid)`.

## PlayerTagService

Persistent boolean tags. In-memory `ConcurrentHashMap<UUID, Set<String>>`, persisted to
`{dataDir}/players/{uuid}.tags` (a properties file, `tag=1` per tag).

- `addTag`, `removeTag`, `hasTag`, `clearTags` (also deletes the file), `snapshotTags`.
- `loadPlayer(uuid)` on connect; `saveAndClearPlayer(uuid)` on disconnect; `savePlayer(uuid)` persists
  without clearing (used by `/hextras tag` mutations).

## CooldownService

Named timers. `ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>>` where the value is an expiry
timestamp (`now + seconds×1000`).

- `isReady` (true when absent/expired), `apply(uuid, name, seconds)`, `remainingSeconds`,
  `clear`, `clearAll`, `clearPlayer` (disconnect), `snapshot` (active only).

## TargetingPreventionService

Tracks players protected from NPC targeting (`Set<UUID>`) and provides the helpers the
`TargetingPreventionSystem` uses to scrub NPC `TargetMemory`.

- `protectPlayer`, `unprotectPlayer`, `clear`, `snapshotProtectedPlayers`, `hasProtectedPlayers`.
- `protectedEntityIndexes(store)` — resolves protected players to entity indexes in a store.
- `clearProtectedTargets(memory, indexes)` — removes those indexes from a `TargetMemory`'s known/closest
  hostiles; returns the count cleared.

## PlayerOverrideService

Per-viewer entity visibility overrides: `ConcurrentHashMap<UUID viewer, Set<UUID> hidden>`.

- `hideEntity`, `showEntity`, `isEntityHidden`, `clearAll(viewer)`, `snapshotHidden(viewer)`.
- `clearPlayer(uuid)` removes the player as both a viewer **and** a hidden target in everyone else's set.

## RuntimeStateStore

A thin unified accessor over the variable, cooldown, and override services plus the active config,
handed to the command tree and refreshed on `reloadConfig()` via `updateConfig`.

## TagNpcService

Runtime entity tags/variables and a position index for UUID-backed entities. See
[TagNPC](tag-npc).

- Tag/variable methods mirror the player services but return `TagNpcResult`; **keys are lower-cased**.
- `indexEntity(...)` (called by `TagNpcEntityIndexSystem`); `findClosestEntityToPlayer` /
  `findClosestEntity` use the index; stale entries (>10 s or invalid ref) are pruned when
  `tagNpc.clearStateOnEntityUnload=true`.
- Visibility methods delegate to `PacketApi` + `PlayerOverrideService` and require
  `entityPacketFiltering` for visual effect.
- `start()`/`stop()` invoked by `TagNpcModule`.

## FloatingItemService

Decorative item displays: `ConcurrentHashMap<String id, FloatingItemInstance>`. See
[Floating Items](floating-items).

- IDs normalized to `[a-z0-9_.-]+`; total capped by `floatingItems.maxItems`.
- `createFloatingItem*`, `moveFloatingItem`, `setFloatingItemIntangible`, `removeFloatingItem`,
  `snapshot*`, `exists`, `isIntangible`.
- Persistent items are saved to `floating-items.properties`; `start()` loads + renders, `stop()` saves +
  clears, `reload()` re-renders. The `FloatingItemRenderer` never creates collectible drops and reports
  when the display backend is unavailable.

## ImageIconService

Provider-scoped icon assets and runtime attachments. See [Image Icons](image-icons).

- `providers`, `icons` (provider → icon → definition), `attachments` (attachmentId → attachment),
  `loadErrors`, all `ConcurrentHashMap`.
- Local providers loaded by walking the folder (`.png`/`.gif`); a daemon `WatchService` hot-reloads when
  `imageIcons.hotReload=true`. Remote icons are downloaded via `HttpClient` into
  `<dataDir>/image-icons-cache/`, size-limited and PNG/GIF-validated.
- `attachIcon*` require the renderer's packet backend; attachments are runtime-only and sorted per
  viewer by priority then creation time, capped by `imageIcons.maxIconsPerViewer`.

## InternalModuleManager

Registry and lifecycle for the six [modules](modules): `register`,
`initializeFromConfig`, `refreshFromConfig`, `enable`/`disable`/`reload`, `isEnabled`, `state`. Tracks a
state machine per module (`REGISTERED`/`ENABLED`/`DISABLED`/`RELOADING`/`RESTART_REQUIRED`/`FAILED`) and
persists `modules.<id>.enabled` through `ConfigLoader.updateProperty` when toggled in-game.

---

## Supporting utilities (`util/`, `config/`, `codec/`)

| Class | Role |
|---|---|
| `StringTemplate` | Resolves `{...}` template placeholders in trigger text. See [String Templates](string-templates). |
| `RuleEvaluator` | Evaluates boolean rule predicates and a subset of placeholders. |
| `ArithmeticExpression` | Recursive-descent evaluator for `calculate_variable`/`math_condition` (`+ - * / %`, parens, `true`/`false`). |
| `RichText` | Converts `&`-codes to Hytale `FormattedMessage`. |
| `MissingPlaceholderBehavior` | `KEEP_ORIGINAL` / `EMPTY` / `ERROR` for PlaceholderAPI. |
| `HyExtrasConfig` / `ConfigLoader` | Config model and properties loader/migrator. See [Configuration](configuration). |
| `CodecHelper` | `KeyedCodec` factory helpers (`string`, `optString`, `optEnum`, …) used by every effect/condition. |

## See also

- [Architecture](architecture) — how these are wired together.
- [Internals: TriggerExtras](internals-trigger-extras) · [Internals: Packet stack](internals-packet-stack)
- [Developer API](developer-api) — the stable surface that wraps these.
