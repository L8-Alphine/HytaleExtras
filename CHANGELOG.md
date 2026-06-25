# Changelog

All notable changes to **HyExtras** are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project aims to follow
[Semantic Versioning](https://semver.org/).

---

## [1.0.2-alpha.2] — 2026-06-25

A **fine-tuning pass** across the runtime systems: per-entity granularity, opt-in persistence,
richer conditions, a change-event API, and real NPC/Mob interaction. **Everything is
backward-compatible** — all defaults preserve the previous behavior. See
[docs/fine-tuning.md](docs/fine-tuning.md) for the full guide.

### Added
- **NPC & Mob interaction.** Right-click/use on an NPC or mob now drives interaction triggers
  (`PlayerInteractEvent.getTargetEntity()`). The interacted entity is recorded in the per-player
  variable `hextras_interacted_entity` and surfaced as a new `Target: "interacted_entity"` option on
  all TagNPC effects/conditions.
- **Opt-in variable persistence.** Set `playerVariablesPersistent=true` to persist all player
  variables, or prefix individual keys with `persist:` to persist just those. Saved to
  `{dataDir}/players/{uuid}.vars`, loaded on connect. `HyExtrasApi.saveVariables(uuid)` forces a save.
- **New variable operators** on `variable_condition` and `tagnpc_variable_condition`:
  `greater_or_equal`, `less_or_equal`, `divisible_by`, `contains`, `regex` (regex gated by
  `variable.regexEnabled`, pattern-cached, length-capped).
- **Change-event bus.** Subscribe to state changes via `HyExtrasApi.subscribe(...)`:
  `PlayerTagChangeEvent`, `PlayerVariableChangeEvent`, `EntityTagChangeEvent`,
  `EntityVariableChangeEvent`, `VisibilityChangeEvent`, `InteractionEvent`.
- **Conditional interaction prompts.** Volume tag `hextras:interaction_prompt_conditional=true` shows
  the prompt only after the effect chain's conditions pass.
- **TagNPC display names.** `HyExtrasApi.setEntityDisplayName` / `getEntityDisplayName` (also on
  `TagNpcEntityState`).
- **New config knobs:** `playerVisibilitySyncIntervalMs`, `interactionReplayWindowMs`,
  `playerVariablesPersistent`, `persistence.async`, `persistence.saveDebounceMs`,
  `variable.regexEnabled`, `tagNpc.stateRetentionSeconds`, `tagNpc.statePersistent`.
- **Unit test scaffold** (JUnit 5) covering `ValueCodec` and `ComparisonOperator`.
- New documentation: [Fine-Tuning](docs/fine-tuning.md); updated
  [Conditions](docs/conditions.md) and [Configuration](docs/configuration.md).

### Changed
- **Previously hardcoded values are now configurable:** player visibility sync interval (was 500 ms,
  now `playerVisibilitySyncIntervalMs`, clamped ≥ 50 ms and re-applied on `/hextras reload`),
  interaction replay window (was 50 ms, now `interactionReplayWindowMs`), and TagNPC state retention
  (was 10 s, now `tagNpc.stateRetentionSeconds`).
- **Player tag/variable disk writes are now atomic** (temp file + rename) and, with
  `persistence.async=true`, offloaded off the event thread with debounced coalescing; disconnect
  saves still flush synchronously for durability.
- Both variable conditions now share one operator implementation
  (`util/ComparisonOperator`), so they behave identically.

### Fixed
- TagNPC `displayName` was declared but never set — it is now a functional, developer-settable field.
- `player_hide_entity` with `TargetSelector=entities` no longer logs a WARNING on every legitimate
  targeting-protection use (downgraded to debug).
- Non-integer `PartyAmount` volume tags are no longer swallowed silently (logged at debug; behavior
  still falls back deterministically).

### Developer notes — API spike (Hytale Server 0.5.6)
- **NPC interaction is available** via `PlayerInteractEvent.getTargetEntity()` / `getTargetRef()`
  (implemented above).
- **Per-viewer appearance is partial:** `NameplateUpdate` makes nameplate/nametag override feasible
  through the outbound entity filter; glow, team, equipment, and fake-spawn are **not** exposed as
  discrete packets in 0.5.6.

### Not yet implemented (tracked follow-ups)
- Per-viewer nameplate override via the experimental entity packet filter.
- Batched hide/show packet flush; NPC-vs-mob classification in `EntityResolver`.
- Optional TagNPC disk persistence for stable/named entities (`tagNpc.statePersistent` is reserved).
- Cooldown scope selection (per-player / per-volume / global).
- Optional tag values/metadata; persistence of interaction block/allow volume state.

[1.0.2-alpha.2]: https://github.com/L8-Alphine/HytaleExtras/releases/tag/v1.0.2-alpha.2
