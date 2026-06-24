# Internals: Packet Stack

> **Contributor reference.** The `packetapi` package and the ECS systems behind per-player visibility,
> camera, titles, and targeting. Creator-facing behavior is in
> [Visibility & Packets](visibility-and-packets).

## PacketApi

`PacketApi` is the facade the rest of HyExtras uses. It owns three services and gates all packet
sends behind `packetsEnabled()` — i.e. the `packet_api` module enabled **and**
`advancedPacketActions=true`.

| Method | Sends / does |
|---|---|
| `sendTitle(...)` | `ShowEventTitle` packet (fade in/out, duration, title/subtitle). |
| `sendActionBar(...)` | `Notification` packet (`NotificationStyle.Default`). |
| `setCamera(uuid, mode, locked)` | `SetServerCamera` (`ClientCameraView.FirstPerson`/`ThirdPerson`, `isLocked`; `reset` forces unlocked first person). |
| `hidePlayer` / `showPlayer` / `shouldHidePlayer` | Delegate to `VisibilityPolicyService`. |
| `hideEntity` / `showEntity` | Record entity overrides. |
| `clearHiddenPlayers(viewer, usePackets)` | Clear overrides, release targeting protection, send show packets. |
| `applyConfig()` / `stopServices()` / `syncNow()` | Start/stop the sub-services; force a sync. |

Text is converted with [`RichText`](internals-services#supporting-utilities-util-config-codec). All sends
no-op (return `false`) for offline players or when packets are disabled.

## VisibilityPolicyService

The decision engine for player/entity visibility. `shouldHidePlayer(viewer, target)` resolves shared
volumes and applies policy precedence (`IsStoryArea=false` → force-allow, `=true` → force-hide,
`GroupArea` → party logic via `partyId`/`PartyAmount`, else explicit overrides). It also performs the
actual `HiddenPlayersManager.hidePlayer/showPlayer` calls and de-duplicates sent state via a
`syncedHiddenPairs` set. Full precedence table: [Visibility & Packets](visibility-and-packets#volume-visibility-policy-tags).

## PlayerVisibilitySyncService

A single-threaded daemon scheduler (`HyExtras-PlayerVisibilitySync`) that, every **500 ms**, walks all
online player pairs and calls `shouldHidePlayer` + `syncPlayerPacketPolicy` so volume policy applies
without a custom effect. It runs only when `advancedPacketActions` **and**
`playerVisibilityPolicySync` are enabled; `syncNow()` is invoked on player connect/disconnect for an
immediate pass. It never inspects non-player entity packets.

## PacketVisibilityService

The **experimental** outbound entity filter. When `advancedPacketActions` **and**
`entityPacketFiltering` are enabled, it registers an outbound `PacketFilter` (`PacketAdapters`) that
inspects `EntityUpdates` packets and drops updates whose target (resolved from `networkId`) should be
hidden from the viewer — players via `shouldHidePlayer`, other entities via `shouldHideEntity`. It is
best-effort (only works when the entity resolves from its network id) and runs on the packet path,
which is why it's off by default. This path backs [TagNPC](tag-npc) visibility.

## ECS systems

Two systems registered in `HyExtrasPlugin.setup()` feed the packet/visibility layer:

| System | Each tick |
|---|---|
| `TargetingPreventionSystem` | Uses `TargetingPreventionService.protectedEntityIndexes(store)` and `clearProtectedTargets(memory, indexes)` to scrub protected players from nearby NPC `TargetMemory` (known/closest hostiles). |
| `TagNpcEntityIndexSystem` | Calls `TagNpcService.indexEntity(...)` for UUID-backed entities so `near` lookups and `findNearestTagNpcEntity` have fresh positions. |

(`UseBlockInteractionSystem`, the third system, belongs to the
[interaction bridge](internals-trigger-extras#interaction-bridge).)

## Config gates summary

| Feature | Requires |
|---|---|
| Titles / action bars / camera | `packet_api` + `advancedPacketActions` |
| Player-to-player hide packets | `packet_api` + `advancedPacketActions` |
| Volume policy auto-sync | + `playerVisibilityPolicySync` |
| Non-player entity filtering | + `entityPacketFiltering` (experimental) |

## See also

- [Visibility & Packets](visibility-and-packets) — creator-facing behavior.
- [Internals: Services](internals-services) — `PlayerOverrideService`, `TargetingPreventionService`.
- [Configuration](configuration#global-flags) — the gating flags.
