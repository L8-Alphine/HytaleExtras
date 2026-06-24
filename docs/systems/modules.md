# Internal Modules

HyExtras is split into six **internal modules**. Each can be enabled/disabled and (where safe)
toggled or reloaded at runtime, so a server owner only runs the systems they need. Modules are managed
by `InternalModuleManager` and configured under `modules.<id>.*` in
[`hyextras.properties`](../configuration.md#internal-modules).

## The six modules

| Module id | Provides |
|---|---|
| `trigger_extras` | All HyExtras trigger [effects](../effects.md), [conditions](../conditions.md), and the [interaction bridge](interaction-bridge.md). |
| `placeholder_api` | The PlaceholderAPI bridge for [string templates](../string-templates.md#placeholderapi). Safe when PlaceholderAPI isn't installed. |
| `packet_api` | Packet-backed [visibility, camera, title](visibility-and-packets.md) services. |
| `image_icons` | Provider-scoped [image icons](image-icons.md). |
| `tag_npc` | [TagNPC](tag-npc.md) runtime entity state. |
| `floating_items` | Decorative [floating item](floating-items.md) displays. |

## Settings

Each module has three config keys (defaults in [Configuration](../configuration.md#internal-modules)):

| Key | Effect |
|---|---|
| `enabled` | Loads the module at startup. |
| `allowInGameToggle` | Permits `/hextras module enable\|disable` to flip it live. |
| `reloadable` | Permits `/hextras module reload`. |

`packet_api` ships with `allowInGameToggle=false` and `reloadable=false` because its services hook
into player join/leave and the visibility path; changing it requires a restart.

## Lifecycle

Each module implements register/enable/disable/reload hooks (`onRegister`, `onEnable`, `onDisable`,
`onReload`). The manager tracks a state per module:

| State | Meaning |
|---|---|
| `REGISTERED` | Registered but not yet initialized. |
| `ENABLED` | Active. |
| `DISABLED` | Loaded but inactive. |
| `RELOADING` | Mid-reload (transient). |
| `RESTART_REQUIRED` | A requested change needs a server restart (module isn't runtime-toggleable/reloadable). |
| `FAILED` | An enable/disable/reload hook threw. |

At startup, modules are initialized from config. `/hextras module enable\|disable` optionally persists
the `modules.<id>.enabled` key (when `allowInGameToggle` permits) and applies the change live if the
module supports runtime toggling; otherwise it reports `RESTART_REQUIRED`. Most effects/conditions
also re-check their owning module at execution time, so disabling a module immediately stops its
behavior even mid-session.

## Commands

| Command | Description |
|---|---|
| `/hextras modules` | List modules and state. |
| `/hextras module info <id>` | Show one module's state and config. |
| `/hextras module enable\|disable <id>` | Toggle (persists when allowed). |
| `/hextras module reload <id>` | Reload a reloadable module. |

See [Commands → modules](../commands.md#hextras-modules--hextras-module).

## See also

- [Configuration](../configuration.md#internal-modules) — default settings.
- [Architecture](../architecture.md) — how modules are registered at startup.
