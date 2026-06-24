# Visibility & Packets

HyExtras can make players (and, best-effort, other entities) appear/disappear for specific viewers,
control a player's camera, send titles/action bars, and protect players from NPC targeting. These are
**per-player packet features** gated behind configuration so a server can run pure server-side logic
without touching the client.

**Sections:** [Two layers](#two-layers-state-vs-packets) · [Per-player view effects](#per-player-view-effects) ·
[Volume visibility policy](#volume-visibility-policy-tags) · [Camera](#camera) ·
[Targeting protection](#targeting-protection) · [Non-player entities](#non-player-entity-visibility)

---

## Two layers: state vs. packets

Every visibility operation has two layers:

1. **Server-side state** (`PlayerOverrideService`) — always updated. The
   [`player_hidden`](conditions#player_hidden) condition tests this layer, so it works even with no
   packets sent.
2. **Client packet** (`HiddenPlayersManager`) — only sent when **both**:
   - the effect's `UsePackets` is `true` (the default), **and**
   - `advancedPacketActions=true` and the `packet_api` module is enabled (see
     [Configuration](configuration#global-flags)).

Use `UsePackets: false` when you want condition logic (e.g. "is this player hidden?") without changing
what the client renders.

---

## Per-player view effects

| Effect | Purpose |
|---|---|
| [`player_hide_entity`](effects#player_hide_entity) | Hide a target player from the triggering viewer. |
| [`player_show_entity`](effects#player_show_entity) | Reverse a hide. |
| [`clear_player_overrides`](effects#clear_player_overrides) | Clear all overrides for the viewer and release targeting protection. |

Both hide/show support **rule-driven targeting** via `ViewerRule`/`TargetRule` and a `TargetSelector`
(`player`, `players`, `entities`). Rules use the [rule predicate](string-templates#rule-predicates)
mini-language. Example — hide all stealthed players from viewers without the reveal tag:

```json
{
  "type": "player_hide_entity",
  "eventType": "ENTER",
  "TargetSelector": "players",
  "ViewerRule": "{!hasTag:see_stealth}",
  "TargetRule": "{hasTag:stealthed}"
}
```

All visibility state clears on disconnect.

---

## Volume visibility policy tags

Volume tags can enforce visibility **without** adding an effect to every volume. When
`playerVisibilityPolicySync=true` (default), a background sync applies these to player-to-player
packets. The policy is evaluated over the volumes a viewer and target **both** occupy, in this
precedence:

| Precedence | Shared-volume tag | Result |
|---|---|---|
| 1 | `IsStoryArea=false` | **Force visible** — explicit hides are overridden. |
| 2 | `IsStoryArea=true` | **Force hidden** — players in the same volume can't see each other. |
| 3 | `GroupArea=true` | Party logic (below). |
| 4 | *(none of the above)* | Fall back to explicit `player_hide_entity` overrides. |

### Group areas
With `GroupArea=true`, visibility follows the per-player `partyId` variable
([Player State](player-state#variables)):

- Players with **different or empty** `partyId` are hidden from each other.
- Players with the **same** non-empty `partyId` can see each other.
- An optional `PartyAmount=<n>` tag caps how many party members are mutually visible: members are
  ordered deterministically (by UUID) and only the first *n* see each other; the rest are hidden by
  policy.

| Volume tag | Meaning |
|---|---|
| `IsStoryArea:true` / `:false` | Hide / force-show players sharing the volume. |
| `GroupArea:true` | Treat the volume as a party/group instance. |
| `PartyAmount:<n>` | Max mutually-visible members per party. |

> Explicit API/effect hides and volume policy interact: `hidePlayerFrom` returns `false` when a
> `FORCE_ALLOW` policy (`IsStoryArea:false` or an in-limit group) applies. See
> [Developer API → Visibility](developer-api#visibility).

---

## Camera

[`set_camera`](effects#set_camera) sends a `SetServerCamera` packet to **only** the triggering
player (camera is inherently per-player). Modes: `first_person`, `third_person`, `reset`. `Locked:true`
prevents the player switching modes while it applies. Requires `advancedPacketActions=true`.

```json
{ "type": "set_camera", "eventType": "ENTER", "Mode": "third_person", "Locked": true }
{ "type": "set_camera", "eventType": "EXIT",  "Mode": "reset" }
```

API: `setCamera(uuid, mode, locked)` / `resetCamera(uuid)`.

---

## Titles & action bars

`send_title`, `send_rich_message`, and `send_reward_message` deliver per-player text via the PacketAPI
(`sendTitle` / `sendActionBar`). The API exposes the same helpers — see
[Developer API → Packets](developer-api#packets--messaging). All return `false` for offline players.

---

## Targeting protection

HyExtras can protect a player from supported NPC targeting by clearing them from Hytale's NPC combat
`TargetMemory` (`TargetingPreventionService`). NPCs that don't use that component are ignored safely.

- Trigger volumes activate it with `PreventTargeting=true` on
  [`player_hide_entity`](effects#player_hide_entity).
- It is released by `player_show_entity` with `PreventTargeting=true`,
  [`clear_player_overrides`](effects#clear_player_overrides), disconnect, or shutdown.
- API: `protectPlayerFromTargeting`, `unprotectPlayerFromTargeting`,
  `isPlayerProtectedFromTargeting`, `snapshotTargetingProtectedPlayers`.

---

## Non-player entity visibility

Player-to-player hiding is first-class. **Non-player** entity hiding is *best-effort*: when
`advancedPacketActions=true` **and** `entityPacketFiltering=true`, HyExtras filters outbound
`EntityUpdates` for hidden viewer/entity pairs when the entity can be resolved from its network ID.
`entityPacketFiltering` is **off by default** because it runs on the packet path and can affect join
timing. This path also backs [TagNPC](tag-npc) visibility. Raw packet sending is never exposed.

## See also

- [Configuration](configuration#global-flags) — `advancedPacketActions`, `entityPacketFiltering`, `playerVisibilityPolicySync`.
- [Developer API](developer-api#visibility) — visibility, camera, targeting helpers.
- [Internals: Packet stack](internals-packet-stack) — how the services are wired.
