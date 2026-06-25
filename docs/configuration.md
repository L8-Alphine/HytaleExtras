# Configuration Reference

HyExtras is configured by **`hyextras.properties`** in the plugin's data directory. The file is
written with documented defaults on first run. On later starts, any newly introduced settings are
**appended** to your existing file without overwriting your edits (a config migration), so upgrades
never clobber server-owner changes.

- **Reload at runtime:** `/hextras reload` re-reads `hyextras.properties`.
- **Module toggles** persist individual keys via the `/hextras module enable|disable` commands.
- Invalid values fall back to the default and log a warning; booleans accept `true`/`false`.

**Sections:** [Global flags](#global-flags) · [Fine-tuning](#fine-tuning) · [Internal modules](#internal-modules) ·
[String templates](#string-templates) · [ImageIcons](#imageicons) · [TagNPC](#tagnpc) ·
[FloatingItems](#floatingitems) · [Full default file](#full-default-file)

---

## Global flags

| Key | Type | Default | Description |
|---|---|---|---|
| `advancedPacketActions` | boolean | `true` | Master switch for per-player packet features: `player_hide/show_entity` packets, titles, action bars, camera packets, and the API packet helpers. See [Visibility & Packets](systems/visibility-and-packets.md). |
| `entityPacketFiltering` | boolean | `false` | **Experimental.** Filters outbound `EntityUpdates` for best-effort non-player entity hiding. Runs on the packet path and can affect join timing — leave off unless testing. Required for visual [TagNPC](systems/tag-npc.md) hiding. |
| `startupDiagnostics` | boolean | `true` | Prints preflight diagnostics (config, dependencies, packet filters, duplicate installs, plugin availability) at startup. |
| `playerVisibilityPolicySync` | boolean | `true` | Periodically applies `IsStoryArea`/`GroupArea` [volume visibility policy](systems/visibility-and-packets.md#volume-visibility-policy-tags) to player-to-player packets so volume tags work without a custom effect. |
| `debugMode` | boolean | `false` | Verbose runtime logging (connect/disconnect cleanup, interaction bridge, unknown rule tokens). |

---

## Fine-tuning

Knobs added in the fine-tuning pass. See [Fine-Tuning](fine-tuning.md) for the behavior behind each.

| Key | Type | Default | Description |
|---|---|---|---|
| `playerVisibilitySyncIntervalMs` | long | `500` | Interval between player visibility policy sync passes; clamped to ≥ 50 ms. Re-applied on reload. |
| `interactionReplayWindowMs` | long | `50` | Window in which a repeated identical interaction is de-duplicated as a replay. |
| `playerVariablesPersistent` | boolean | `false` | Persist all player variables to disk. Otherwise only `persist:`-prefixed keys persist. |
| `persistence.async` | boolean | `true` | Offload tag/variable disk writes to a background thread (disconnect still flushes synchronously). |
| `persistence.saveDebounceMs` | long | `1000` | Coalesce repeated background saves for the same player. |
| `variable.regexEnabled` | boolean | `true` | Allow the `regex` operator in variable conditions. |
| `tagNpc.stateRetentionSeconds` | long | `10` | Inactivity seconds before runtime TagNPC entity state is pruned. |
| `tagNpc.statePersistent` | boolean | `false` | Reserved for opt-in TagNPC disk persistence of stable/named entities. |

---

## Internal modules

Each of the six [modules](systems/modules.md) has three keys following the pattern
`modules.<id>.<field>`:

| Field | Meaning |
|---|---|
| `enabled` | Whether the module loads at startup. |
| `allowInGameToggle` | Whether `/hextras module enable\|disable <id>` may flip it live. |
| `reloadable` | Whether `/hextras module reload <id>` is allowed. |

Default per-module settings:

| Module id | `enabled` | `allowInGameToggle` | `reloadable` |
|---|---|---|---|
| `trigger_extras` | `true` | `true` | `true` |
| `placeholder_api` | `true` | `true` | `true` |
| `packet_api` | `true` | `false` | `false` |
| `image_icons` | `true` | `true` | `true` |
| `tag_npc` | `true` | `true` | `true` |
| `floating_items` | `true` | `true` | `true` |

> `packet_api` is intentionally not live-toggleable or reloadable because its packet services hook
> into player join/leave and visibility paths.

```properties
modules.trigger_extras.enabled=true
modules.trigger_extras.allowInGameToggle=true
modules.trigger_extras.reloadable=true
```

---

## String templates

Controls [placeholder resolution](string-templates.md).

| Key | Type | Default | Description |
|---|---|---|---|
| `stringTemplate.nativePlaceholders.enabled` | boolean | `true` | Resolve native HyExtras placeholders like `{player}`, `{uuid}`, `{variable:key}`. |
| `stringTemplate.placeholderApi.enabled` | boolean | `true` | Resolve PlaceholderAPI `%placeholder%` tokens when PlaceholderAPI is installed and the `placeholder_api` module is enabled. |
| `stringTemplate.placeholderApi.missingBehavior` | enum `KEEP_ORIGINAL` \| `EMPTY` \| `ERROR` | `KEEP_ORIGINAL` | What to do with an unresolved `%placeholder%`. |

---

## ImageIcons

Provider-scoped image assets. See [Image Icons](systems/image-icons.md).

| Key | Type | Default | Description |
|---|---|---|---|
| `imageIcons.hotReload` | boolean | `true` | Watch registered provider folders and reload changed PNG/GIF assets. |
| `imageIcons.remoteCache.enabled` | boolean | `true` | Download remote PNG/GIF icons into the HyExtras cache before loading. |
| `imageIcons.remoteCache.maxBytes` | long | `5242880` | Maximum remote icon download size in bytes (5 MiB). |
| `imageIcons.defaultVisibilityRadius` | float | `48.0` | Default viewer radius for attachments when tuning does not override it. |
| `imageIcons.maxIconsPerViewer` | int | `64` | Maximum attachments considered per viewer after priority sorting. |

---

## TagNPC

Runtime entity state. See [TagNPC](systems/tag-npc.md).

| Key | Type | Default | Description |
|---|---|---|---|
| `tagNpc.defaultVisibilityRadius` | float | `64.0` | Default visibility radius reserved for TagNPC packet-backed features. |
| `tagNpc.clearStateOnEntityUnload` | boolean | `true` | Clear runtime TagNPC state when entity-unload cleanup hooks are available. |

---

## FloatingItems

Decorative item displays. See [Floating Items](systems/floating-items.md). These values are the
fallbacks for the matching `floating_item_create` fields when those are omitted.

| Key | Type | Default | Description |
|---|---|---|---|
| `floatingItems.defaultPersistent` | boolean | `false` | Whether new floating items persist across restarts unless overridden per item. |
| `floatingItems.defaultIntangible` | boolean | `true` | Default intangible (no-collision) state for new items. |
| `floatingItems.defaultVisibilityRadius` | float | `48.0` | Default viewer radius. |
| `floatingItems.defaultBobAmplitude` | float | `0.15` | Default vertical bob amount. |
| `floatingItems.defaultRotationDegreesPerSecond` | float | `45.0` | Default decorative rotation speed. |
| `floatingItems.maxItems` | int | `512` | Maximum runtime floating items tracked. |

---

## Full default file

This is the file HyExtras writes on first run:

```properties
# HyExtras Configuration

# Enable per-player packet-backed features:
# player_hide_entity/player_show_entity packets, titles, action bars, camera packets,
# and high-level API packet helpers.
advancedPacketActions=true

# Experimental: filters outbound EntityUpdates for best-effort non-player entity hiding.
entityPacketFiltering=false

# Print startup/preflight diagnostics.
startupDiagnostics=true

# Periodically applies IsStoryArea/GroupArea volume visibility policy to player packets.
playerVisibilityPolicySync=true

# Print verbose runtime debug info.
debugMode=false

# Internal modules
modules.trigger_extras.enabled=true
modules.trigger_extras.allowInGameToggle=true
modules.trigger_extras.reloadable=true

modules.placeholder_api.enabled=true
modules.placeholder_api.allowInGameToggle=true
modules.placeholder_api.reloadable=true

modules.packet_api.enabled=true
modules.packet_api.allowInGameToggle=false
modules.packet_api.reloadable=false

modules.image_icons.enabled=true
modules.image_icons.allowInGameToggle=true
modules.image_icons.reloadable=true

modules.tag_npc.enabled=true
modules.tag_npc.allowInGameToggle=true
modules.tag_npc.reloadable=true

modules.floating_items.enabled=true
modules.floating_items.allowInGameToggle=true
modules.floating_items.reloadable=true

# StringTemplate
stringTemplate.nativePlaceholders.enabled=true
stringTemplate.placeholderApi.enabled=true
stringTemplate.placeholderApi.missingBehavior=KEEP_ORIGINAL

# ImageIcons
imageIcons.hotReload=true
imageIcons.remoteCache.enabled=true
imageIcons.remoteCache.maxBytes=5242880
imageIcons.defaultVisibilityRadius=48.0
imageIcons.maxIconsPerViewer=64

# TagNPC
tagNpc.defaultVisibilityRadius=64.0
tagNpc.clearStateOnEntityUnload=true

# FloatingItems
floatingItems.defaultPersistent=false
floatingItems.defaultIntangible=true
floatingItems.defaultVisibilityRadius=48.0
floatingItems.defaultBobAmplitude=0.15
floatingItems.defaultRotationDegreesPerSecond=45.0
floatingItems.maxItems=512
```

---

## See also

- [Modules](systems/modules.md) — what each module enables.
- [Commands](commands.md) — `/hextras reload` and `/hextras module …`.
- [Visibility & Packets](systems/visibility-and-packets.md) — the flags that gate packet features.
