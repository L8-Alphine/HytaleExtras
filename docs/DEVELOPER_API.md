# HyExtras Developer API

HyExtras exposes a small stable Java facade for other server mods:

```java
import org.hyzionstudios.hyextras.api.HyExtrasApi;
import org.hyzionstudios.hyextras.imageicons.ImageIconResult;
import org.hyzionstudios.hyextras.imageicons.ImageIconTuning;

import java.net.URI;
import java.nio.file.Path;

HyExtrasApi api = HyExtrasApi.get();
```

Use this facade instead of reaching into `org.hyzionstudios.hyextras.service` or `HyExtrasPlugin` internals. Internal classes may change between releases; the facade is the intended compatibility surface for 1.0.x.

## Availability

Call the API after HyExtras has started. `HyExtrasApi.get()` always returns the facade object, but methods that need the running plugin throw `IllegalStateException` if HyExtras is not active.

```java
HyExtrasApi api = HyExtrasApi.get();
if (!api.isAvailable()) {
    return;
}
```

Online-player helpers:

```java
UUID uuid = api.getOnlinePlayerUuid("PlayerName");
boolean online = api.isPlayerOnline(uuid);
String name = api.getOnlinePlayerName(uuid);
List<String> activeVolumes = api.getActiveVolumeIds(uuid);
```

## Persistence Rules

HyExtras state intentionally has different lifetimes:

| State | Lifetime |
|---|---|
| Variables | Runtime only; cleared on disconnect |
| Cooldowns | Runtime only; cleared on disconnect |
| Visibility overrides | Runtime only; cleared on disconnect |
| Tags | Persistent; saved under the HyExtras data directory |

`partyId` is the default variable used by `GroupArea:true` volume policy.

## Variables

Variables are per-player runtime values. They are useful for session state, grouping, counters, and rule evaluation.

```java
api.setVariableString(playerUuid, "partyId", "alpha");
api.setVariable(playerUuid, "score", 10);

String partyId = api.getVariableString(playerUuid, "partyId");
long score = api.incrementVariable(playerUuid, "score", 1);

Map<String, Object> variables = api.snapshotVariables(playerUuid);
api.removeVariable(playerUuid, "score");
api.clearVariables(playerUuid);
```

Snapshots are defensive copies and safe to inspect without mutating HyExtras internals.

## Tags

Tags are persistent boolean flags. Use them for story progress, unlocks, permissions, or long-lived player state.

```java
api.addTag(playerUuid, "storyline_a_active");

if (api.hasTag(playerUuid, "storyline_a_active")) {
    api.sendActionBar(playerUuid, "Story path active");
}

Set<String> tags = api.snapshotTags(playerUuid);
api.removeTag(playerUuid, "storyline_a_active");
api.saveTags(playerUuid);
```

`clearTags(playerUuid)` removes all loaded tags and deletes the persisted tag file.

## Cooldowns

Cooldowns are per-player runtime timers separate from native trigger volume cooldowns.

```java
if (api.isCooldownReady(playerUuid, "daily_bonus")) {
    api.applyCooldown(playerUuid, "daily_bonus", 86400.0f);
    api.sendRichMessage(playerUuid, "&aDaily bonus claimed.");
}

float remaining = api.remainingCooldownSeconds(playerUuid, "daily_bonus");
Map<String, Float> cooldowns = api.snapshotCooldowns(playerUuid);

api.clearCooldown(playerUuid, "daily_bonus");
api.clearCooldowns(playerUuid);
```

## Visibility

Player-to-player visibility is first-class and uses Hytale's player visibility manager when `usePackets` is true and `advancedPacketActions=true`.

```java
api.hidePlayerFrom(viewerUuid, targetUuid, true);
boolean hidden = api.isPlayerHiddenFrom(viewerUuid, targetUuid);
api.showPlayerTo(viewerUuid, targetUuid, true);
```

Username overloads are available for online players:

```java
api.hidePlayerFrom("ViewerName", "TargetName", true);
api.showPlayerTo("ViewerName", "TargetName", true);
```

Volume policy can still override explicit visibility:

| Volume tag | API effect |
|---|---|
| `IsStoryArea:false` | Force allows visibility, so `hidePlayerFrom` returns false |
| `IsStoryArea:true` | Effective `isPlayerHiddenFrom` returns true for players sharing that volume |
| `GroupArea:true` | Visibility follows same `partyId` and `PartyAmount` policy |

For volume tags to visually apply without a custom effect, keep `playerVisibilityPolicySync=true`. This is enabled by default and only syncs player-to-player packets; it does not enable the experimental non-player entity update filter.

Best-effort entity visibility is available by entity UUID:

```java
api.hideEntityFrom(viewerUuid, entityUuid);
api.showEntityTo(viewerUuid, entityUuid);
```

This only affects outbound entity updates when both `advancedPacketActions=true` and `entityPacketFiltering=true`, and only when the server can resolve the entity from its network ID. `entityPacketFiltering` is off by default because it is experimental and runs on the packet path. It does not expose raw packet sending.

### Targeting Protection

HyExtras can also protect a player from supported NPC targeting while a visibility policy is active. This uses Hytale's NPC combat `TargetMemory` component: HyExtras removes the protected player's entity reference from hostile memory and clears them as the closest hostile. Entity AI that does not use this component is ignored safely.

```java
api.protectPlayerFromTargeting(playerUuid);
boolean protectedNow = api.isPlayerProtectedFromTargeting(playerUuid);
Set<UUID> protectedPlayers = api.snapshotTargetingProtectedPlayers();
api.unprotectPlayerFromTargeting(playerUuid);
```

Trigger volumes use the same system when `player_hide_entity` has `PreventTargeting=true`. `player_show_entity` with `PreventTargeting=true`, `clear_player_overrides`, disconnect, or shutdown releases the protection.

## Packets

The API exposes high-level packet helpers only. All helpers return `false` when the player is offline.

```java
api.sendRichMessage(playerUuid, "&aWelcome back!");
api.sendActionBar(playerUuid, "Party: " + api.getVariableString(playerUuid, "partyId"));
api.sendTitle(playerUuid, "Dungeon Started", "Stay together", 3.0f);
api.sendTitle(playerUuid, "Warning", "Low health", 2.0f, 0.1f, 0.8f);
api.setCamera(playerUuid, SetCameraAction.CameraMode.THIRD_PERSON, true);
api.resetCamera(playerUuid);
```

Messages and title text accept HyExtras rich text color/style codes. These helpers do not resolve trigger-context placeholders because they are not running inside a trigger event; use `resolveText` first if needed.

## Rules And Placeholders

Use rule helpers to evaluate the same lightweight predicates used by HyExtras visibility rules:

```java
boolean canSeeStealth = api.evaluateRule("{hasTag:see_stealth}", playerUuid);
boolean grouped = api.evaluateRule("{variable:partyId=alpha}", playerUuid);
```

Supported predicates:

| Predicate | Meaning |
|---|---|
| `{hasTag:tag}` | Player has a persistent tag |
| `{!hasTag:tag}` | Player does not have a persistent tag |
| `{variable:key}` | Player variable exists |
| `{!variable:key}` | Player variable is missing |
| `{variable:key=value}` | Player variable equals a value |
| `{variable:key!=value}` | Player variable does not equal a value |
| `{volumeTag:key}` | One active volume has a tag key |
| `{volumeTag:key=value}` | One active volume has a tag value |

Text placeholders:

```java
String text = api.resolveText(
    "Party {variable:partyId}, story={hasTag:storyline_a_active}, area={volumeTag:IsStoryArea}",
    playerUuid);
```

Supported text placeholders include `{player}`, `{uuid}`, `{variable:key}`, `{hasTag:tag}`, `{!hasTag:tag}`, and `{volumeTag:key}`.

## ImageIcons

ImageIcons is a provider-scoped API for developer mods that want local or remote PNG/GIF icons without owning HyExtras internals. Providers can use any readable folder; a common shape is a mod-owned path such as `mods/MysticNameTags/data/imageicons`.

```java
ImageIconResult provider = api.registerImageIconProvider(
    "mysticnametags",
    Path.of("mods/MysticNameTags/data/imageicons"));

ImageIconResult remote = api.registerRemoteImageIcon(
    "mysticnametags",
    "vip.sparkle",
    URI.create("https://example.com/assets/vip.gif"));

ImageIconResult attached = api.attachImageIconToPlayer(
    playerUuid,
    "mysticnametags",
    "vip.sparkle",
    ImageIconTuning.defaults(null));

if (attached.success()) {
    api.clearImageIcon(attached.attachmentId());
}
```

Local providers hot reload when `imageIcons.hotReload=true`. Remote icons are downloaded into the HyExtras cache, checked as PNG/GIF, size-limited by `imageIcons.remoteCache.maxBytes`, then loaded like local assets.

Useful inspection helpers:

```java
api.snapshotImageIconProviders();
api.snapshotImageIcons("mysticnametags");
api.snapshotImageIconAttachments();
api.snapshotImageIconLoadErrors();
```

Attachments are runtime-only and clear on disconnect, provider unregister, module disable, and shutdown. Icon IDs are provider-scoped, so `mysticnametags:vip.sparkle` and another mod's `vip.sparkle` do not collide.

## FloatingItems

FloatingItems creates decorative, non-pickup item displays. It is useful for mods that want a visible item marker without spawning a collectible dropped item entity.

```java
FloatingItemResult created = api.createFloatingItem(
    "quest_marker_gold",
    new ItemStack("hytale:gold_coin", 1),
    store,
    position,
    FloatingItemTuning.defaults(config),
    true);

api.setFloatingItemIntangible("quest_marker_gold", true);
api.moveFloatingItem("quest_marker_gold", store, newPosition);
api.removeFloatingItem("quest_marker_gold");
```

For online-player anchors:

```java
api.createFloatingItemAtPlayer(
    "player_marker",
    playerUuid,
    new ItemStack("hytale:gold_coin", 1),
    FloatingItemTuning.defaults(config),
    false);
```

Useful inspection helpers:

```java
api.snapshotFloatingItem("quest_marker_gold");
api.snapshotFloatingItems();
api.snapshotFloatingItemsNear(store, position, 16.0D);
```

FloatingItems records runtime state and optional persistent definitions, but the renderer never creates collectible world drops. If PacketAPI display support is unavailable, creation returns a failed `FloatingItemResult` message instead of crashing the caller.

## TagNPC

TagNPC stores runtime tags and variables for UUID-backed NPCs, mobs, and other entities. It is runtime-only in this first pass.

```java
api.addEntityTag(entityUuid, "quest_guard");
api.setEntityVariable(entityUuid, "mood", "alert");
api.incrementEntityVariable(entityUuid, "aggro", 1);

boolean tagged = api.hasEntityTag(entityUuid, "quest_guard");
String mood = api.getEntityVariableString(entityUuid, "mood");
Set<UUID> guards = api.snapshotTaggedEntities("quest_guard");
```

Per-viewer entity visibility delegates to PacketAPI entity visibility state:

```java
api.hideEntityFromViewer(viewerUuid, entityUuid);
boolean hidden = api.isEntityHiddenFromViewer(viewerUuid, entityUuid);
api.showEntityToViewer(viewerUuid, entityUuid);
```

Visual hiding of non-player entities requires `advancedPacketActions=true` and `entityPacketFiltering=true`; otherwise HyExtras records state and returns a failed `TagNpcResult` message for packet delivery. Trigger Volume actions can target the triggering entity, an explicit `EntityUuid`, or all tracked entities with a `TargetTag`. Admin/debug commands are available under `/hextras tagnpc` for explicit UUID edits.

HyExtras also indexes UUID-backed non-player entities with positions so tools can select the closest NPC/mob near an online player:

```java
UUID nearest = api.findNearestTagNpcEntity(playerUuid, 12.0D);
if (nearest != null) {
    api.addEntityTag(nearest, "quest_guard");
}
```

The matching command path is `/hextras tagnpc near ...`, for example `/hextras tagnpc near tag-add PlayerName 12 quest_guard`.

## Suggested Patterns

Set a party/group identity for volume policy:

```java
api.setVariableString(playerUuid, HyExtrasApi.DEFAULT_PARTY_VARIABLE, partyId);
```

Gate a custom feature behind a persistent tag:

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

## Compatibility Notes

- The facade is the supported 1.0.x API surface.
- Do not depend on internal services or action classes except enum values explicitly used by the facade, such as `SetCameraAction.CameraMode`.
- Raw packet sending is intentionally not exposed.
- Non-player entity filtering is best-effort in Hytale `0.5.6`.
