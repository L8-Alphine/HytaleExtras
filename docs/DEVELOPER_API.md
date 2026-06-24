# HyExtras Developer API

HyExtras exposes a small, stable Java facade — `org.hyzionstudios.hyextras.api.HyExtrasApi` — for other
server mods that want to use HyExtras features without depending on internal service classes.

```java
import org.hyzionstudios.hyextras.api.HyExtrasApi;
import org.hyzionstudios.hyextras.imageicons.ImageIconResult;
import org.hyzionstudios.hyextras.imageicons.ImageIconTuning;

HyExtrasApi api = HyExtrasApi.get();
```

Use this facade instead of reaching into `org.hyzionstudios.hyextras.service` or `HyExtrasPlugin`
internals. Internal classes may change between releases; the facade is the intended compatibility
surface for `1.0.x`. Contributors extending HyExtras itself should read the
[internals docs](internals/services.md).

**Sections:** [Availability](#availability) · [Player registry](#player-registry) ·
[Persistence rules](#persistence-rules) · [Variables](#variables) · [Tags](#tags) ·
[Cooldowns](#cooldowns) · [Visibility](#visibility) · [Packets & Messaging](#packets--messaging) ·
[Rules & Placeholders](#rules--placeholders) · [ImageIcons](#imageicons) · [TagNPC](#tagnpc) ·
[FloatingItems](#floatingitems) · [Suggested patterns](#suggested-patterns) ·
[Compatibility](#compatibility-notes)

---

## Availability

`HyExtrasApi.get()` always returns the facade, but methods that need the running plugin throw
`IllegalStateException` when HyExtras is not active. Guard with `isAvailable()`:

```java
HyExtrasApi api = HyExtrasApi.get();
if (!api.isAvailable()) return;
```

| Method | Returns | Notes |
|---|---|---|
| `static HyExtrasApi get()` | `HyExtrasApi` | The singleton facade. |
| `boolean isAvailable()` | `boolean` | True when HyExtras is started. |
| `static final String DEFAULT_PARTY_VARIABLE` | `"partyId"` | Variable used by group visibility policy. |

## Player registry

| Method | Returns | Notes |
|---|---|---|
| `getOnlinePlayerUuid(String username)` | `UUID` \| `null` | Online player UUID by name. |
| `isPlayerOnline(UUID player)` | `boolean` | |
| `getOnlinePlayerName(UUID player)` | `String` \| `null` | |
| `getActiveVolumeIds(UUID player)` | `List<String>` | IDs of volumes the player is inside. |

---

## Persistence rules

| State | Lifetime |
|---|---|
| Variables | Runtime only; cleared on disconnect. |
| Cooldowns | Runtime only; cleared on disconnect. |
| Visibility overrides | Runtime only; cleared on disconnect. |
| Tags | **Persistent**; saved under the HyExtras data directory. |

State methods accept UUIDs and don't require the player to be online unless noted; packet/message
helpers return `false` for offline players. See [Player State](systems/player-state.md).

---

## Variables

Per-player runtime values. See [Player State → Variables](systems/player-state.md#variables).

| Method | Returns |
|---|---|
| `getVariable(UUID player, String key)` | `Object` \| `null` |
| `getVariableString(UUID player, String key)` | `String` \| `null` |
| `setVariable(UUID player, String key, Object value)` | `void` |
| `setVariableString(UUID player, String key, String value)` | `void` |
| `incrementVariable(UUID player, String key, long delta)` | `long` (new value) |
| `removeVariable(UUID player, String key)` | `void` |
| `clearVariables(UUID player)` | `void` |
| `snapshotVariables(UUID player)` | `Map<String,Object>` (defensive copy) |

```java
api.setVariableString(playerUuid, "partyId", "alpha");
long score = api.incrementVariable(playerUuid, "score", 1);
Map<String, Object> all = api.snapshotVariables(playerUuid);
```

---

## Tags

Persistent boolean flags. See [Player State → Tags](systems/player-state.md#tags).

| Method | Returns |
|---|---|
| `addTag(UUID player, String tag)` | `void` |
| `removeTag(UUID player, String tag)` | `void` |
| `clearTags(UUID player)` | `void` (also deletes the persisted file) |
| `hasTag(UUID player, String tag)` | `boolean` |
| `snapshotTags(UUID player)` | `Set<String>` (defensive copy) |
| `saveTags(UUID player)` | `void` (persist without clearing) |

```java
api.addTag(playerUuid, "storyline_a_active");
if (api.hasTag(playerUuid, "storyline_a_active")) { /* … */ }
```

---

## Cooldowns

Per-player runtime timers. See [Player State → Cooldowns](systems/player-state.md#cooldowns).

| Method | Returns |
|---|---|
| `isCooldownReady(UUID player, String name)` | `boolean` (true when not active) |
| `applyCooldown(UUID player, String name, float seconds)` | `void` |
| `clearCooldown(UUID player, String name)` | `void` |
| `clearCooldowns(UUID player)` | `void` |
| `remainingCooldownSeconds(UUID player, String name)` | `float` (0 when inactive) |
| `snapshotCooldowns(UUID player)` | `Map<String,Float>` |

```java
if (api.isCooldownReady(playerUuid, "daily_bonus")) {
    api.applyCooldown(playerUuid, "daily_bonus", 86400.0f);
}
```

---

## Visibility

Player-to-player and best-effort entity visibility, plus targeting protection. Volume policy can still
override explicit hides — see [Visibility & Packets](systems/visibility-and-packets.md).

**Player visibility**

| Method | Returns |
|---|---|
| `hidePlayerFrom(UUID viewer, UUID target, boolean usePackets)` | `boolean` (false if policy force-allows) |
| `hidePlayerFrom(String viewerName, String targetName, boolean usePackets)` | `boolean` |
| `showPlayerTo(UUID viewer, UUID target, boolean usePackets)` | `boolean` |
| `showPlayerTo(String viewerName, String targetName, boolean usePackets)` | `boolean` |
| `isPlayerHiddenFrom(UUID viewer, UUID target)` | `boolean` (effective result after policy) |
| `snapshotHiddenPlayers(UUID viewer)` | `Set<UUID>` (explicit overrides, before policy) |
| `clearHiddenPlayers(UUID viewer)` | `void` |

**Entity visibility** (best-effort; needs `advancedPacketActions` + `entityPacketFiltering`)

| Method | Returns |
|---|---|
| `hideEntityFrom(UUID viewer, UUID entity)` | `void` |
| `showEntityTo(UUID viewer, UUID entity)` | `void` |

**Targeting protection** (NPC `TargetMemory`)

| Method | Returns |
|---|---|
| `protectPlayerFromTargeting(UUID player)` | `void` |
| `unprotectPlayerFromTargeting(UUID player)` | `void` |
| `isPlayerProtectedFromTargeting(UUID player)` | `boolean` |
| `snapshotTargetingProtectedPlayers()` | `Set<UUID>` |

```java
api.hidePlayerFrom(viewerUuid, targetUuid, true);
api.protectPlayerFromTargeting(viewerUuid);
```

---

## Packets & Messaging

High-level per-player helpers. All return `false` when the player is offline. Text accepts HyExtras
[color/style codes](string-templates.md#color--style-codes) but does **not** resolve trigger-context
placeholders (use [`resolveText`](#rules--placeholders) first if needed).

| Method | Returns |
|---|---|
| `sendRichMessage(UUID player, String message)` | `boolean` |
| `sendTitle(UUID player, String title, String subtitle, float seconds)` | `boolean` |
| `sendTitle(UUID player, String title, String subtitle, float seconds, float fadeIn, float fadeOut)` | `boolean` |
| `sendActionBar(UUID player, String message)` | `boolean` |
| `setCamera(UUID player, SetCameraAction.CameraMode mode, boolean locked)` | `boolean` |
| `resetCamera(UUID player)` | `boolean` |

```java
api.sendTitle(playerUuid, "Dungeon Started", "Stay together", 3.0f);
api.setCamera(playerUuid, SetCameraAction.CameraMode.THIRD_PERSON, true);
```

> `SetCameraAction.CameraMode` is the one internal enum the facade intentionally exposes.

---

## Rules & Placeholders

Evaluate the same predicates and placeholders used by HyExtras visibility rules. See
[String Templates](string-templates.md).

| Method | Returns |
|---|---|
| `evaluateRule(String rule, UUID player)` | `boolean` |
| `resolveText(String template, UUID player)` | `String` |

```java
boolean canSeeStealth = api.evaluateRule("{hasTag:see_stealth}", playerUuid);
String text = api.resolveText("Party {variable:partyId}", playerUuid);
```

---

## ImageIcons

Provider-scoped local/remote icons. Returns `ImageIconResult` (carries `success()`, `message()`, and
`attachmentId()` for attaches). See [Image Icons](systems/image-icons.md).

| Method | Returns |
|---|---|
| `registerImageIconProvider(String providerId, Path assetsPath)` | `ImageIconResult` |
| `registerRemoteImageIcon(String providerId, String iconId, URI remoteUri)` | `ImageIconResult` |
| `reloadImageIconProvider(String providerId)` | `ImageIconResult` |
| `unregisterImageIconProvider(String providerId)` | `ImageIconResult` |
| `attachImageIcon(UUID target, String providerId, String iconId, ImageIconTuning tuning)` | `ImageIconResult` |
| `attachImageIconToPlayer(UUID targetPlayer, String providerId, String iconId, ImageIconTuning tuning)` | `ImageIconResult` |
| `clearImageIcon(UUID attachmentId)` | `boolean` |
| `clearImageIcons(UUID target)` | `int` (count cleared) |
| `snapshotImageIconProviders()` | `Map<String, ImageIconProviderRegistration>` |
| `snapshotImageIcons(String providerId)` | `Map<String, ImageIconDefinition>` |
| `snapshotImageIcons()` | `Map<String, Map<String, ImageIconDefinition>>` |
| `snapshotImageIconAttachments()` | `Map<UUID, ImageIconAttachment>` |
| `snapshotImageIconLoadErrors()` | `Map<String, List<String>>` |

```java
api.registerImageIconProvider("mysticnametags", Path.of("mods/MysticNameTags/data/imageicons"));
ImageIconResult attached = api.attachImageIconToPlayer(
        playerUuid, "mysticnametags", "vip.sparkle", ImageIconTuning.defaults(null));
if (attached.success()) api.clearImageIcon(attached.attachmentId());
```

---

## TagNPC

Runtime tags/variables and visibility for UUID-backed entities. Mutations return `TagNpcResult`. See
[TagNPC](systems/tag-npc.md).

| Method | Returns |
|---|---|
| `addEntityTag(UUID entity, String tag)` | `TagNpcResult` |
| `removeEntityTag(UUID entity, String tag)` | `TagNpcResult` |
| `hasEntityTag(UUID entity, String tag)` | `boolean` |
| `snapshotEntityTags(UUID entity)` | `Set<String>` |
| `clearEntityTags(UUID entity)` | `TagNpcResult` |
| `setEntityVariable(UUID entity, String key, Object value)` | `TagNpcResult` |
| `getEntityVariable(UUID entity, String key)` | `Object` \| `null` |
| `getEntityVariableString(UUID entity, String key)` | `String` \| `null` |
| `incrementEntityVariable(UUID entity, String key, long delta)` | `long` |
| `removeEntityVariable(UUID entity, String key)` | `TagNpcResult` |
| `snapshotEntityVariables(UUID entity)` | `Map<String,Object>` |
| `clearEntityVariables(UUID entity)` | `TagNpcResult` |
| `hideEntityFromViewer(UUID viewer, UUID entity)` | `TagNpcResult` |
| `showEntityToViewer(UUID viewer, UUID entity)` | `TagNpcResult` |
| `isEntityHiddenFromViewer(UUID viewer, UUID entity)` | `boolean` |
| `snapshotTaggedEntities(String tag)` | `Set<UUID>` |
| `snapshotTagNpcState(UUID entity)` | `TagNpcEntityState` \| `null` |
| `snapshotAllTagNpcStates()` | `Map<UUID, TagNpcEntityState>` |
| `findNearestTagNpcEntity(UUID player, double radius)` | `UUID` \| `null` |

```java
api.addEntityTag(entityUuid, "quest_guard");
api.setEntityVariable(entityUuid, "mood", "alert");
UUID nearest = api.findNearestTagNpcEntity(playerUuid, 12.0D);
```

---

## FloatingItems

Decorative non-pickup displays. Returns `FloatingItemResult`. See
[Floating Items](systems/floating-items.md).

| Method | Returns |
|---|---|
| `createFloatingItem(String id, ItemStack item, Store<EntityStore> store, Vector3d position, FloatingItemTuning tuning, boolean persistent)` | `FloatingItemResult` |
| `createFloatingItemAtPlayer(String id, UUID player, ItemStack item, FloatingItemTuning tuning, boolean persistent)` | `FloatingItemResult` |
| `removeFloatingItem(String id)` | `FloatingItemResult` |
| `setFloatingItemIntangible(String id, boolean intangible)` | `FloatingItemResult` |
| `moveFloatingItem(String id, Store<EntityStore> store, Vector3d position)` | `FloatingItemResult` |
| `snapshotFloatingItem(String id)` | `FloatingItemInstance` \| `null` |
| `snapshotFloatingItems()` | `Map<String, FloatingItemInstance>` |
| `snapshotFloatingItemsNear(Store<EntityStore> store, Vector3d origin, double radius)` | `Map<String, FloatingItemInstance>` |

```java
api.createFloatingItemAtPlayer(
        "player_marker", playerUuid, new ItemStack("hytale:gold_coin", 1),
        FloatingItemTuning.defaults(config), false);
```

---

## Suggested patterns

Set a party/group identity for volume policy:

```java
api.setVariableString(playerUuid, HyExtrasApi.DEFAULT_PARTY_VARIABLE, partyId);
```

Gate a feature behind a persistent tag:

```java
if (!api.hasTag(playerUuid, "tutorial_complete")) {
    api.sendTitle(playerUuid, "Tutorial", "Visit the guide first", 3.0f);
    return;
}
```

Temporarily isolate two players:

```java
api.hidePlayerFrom(viewerUuid, targetUuid, true);
api.protectPlayerFromTargeting(viewerUuid);
api.applyCooldown(viewerUuid, "visibility_restore", 10.0f);
```

---

## Compatibility notes

- The facade is the supported `1.0.x` API surface.
- Do not depend on internal services or action classes except enum values explicitly used by the
  facade, such as `SetCameraAction.CameraMode`.
- Raw packet sending is intentionally not exposed.
- Non-player entity filtering is best-effort in Hytale `0.5.6`.

See [Architecture](architecture.md) and [Internals: Services](internals/services.md) for how these
calls are implemented.
