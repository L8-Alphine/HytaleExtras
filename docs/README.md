# HyExtras Documentation

Complete documentation for **HyExtras** — a modular creator and developer toolkit for Hytale `0.5.x`
servers. New here? Start with the [Getting Started](getting-started.md) guide.

## Start here

| Doc | What's inside |
|---|---|
| [Getting Started](getting-started.md) | Install, core concepts, event types, and a first volume walkthrough. |

## Reference

| Doc | What's inside |
|---|---|
| [Effects](effects.md) | All 41 trigger effect type IDs with parameter tables and JSON examples. |
| [Conditions](conditions.md) | All 14 trigger conditions with parameter tables and examples. |
| [Commands](commands.md) | The full `/hextras` command tree. |
| [Configuration](configuration.md) | Every `hyextras.properties` key and default. |
| [Fine-Tuning](fine-tuning.md) | New config knobs, opt-in persistence, richer operators, change events, and NPC/mob interaction. |
| [String Templates](string-templates.md) | Placeholders, rule predicates, and color/style codes. |

## Systems

In-depth guides to each HyExtras subsystem — behavior, persistence, and gotchas.

| Doc | What's inside |
|---|---|
| [Player State](systems/player-state.md) | Variables, persistent tags, and cooldowns. |
| [Visibility & Packets](systems/visibility-and-packets.md) | Per-player view, packet gating, camera, targeting protection, and volume visibility policy. |
| [Interaction Bridge](systems/interaction-bridge.md) | Interaction events, blocking, and interactable volumes. |
| [TagNPC](systems/tag-npc.md) | Runtime tags/variables and visibility for UUID-backed entities. |
| [Floating Items](systems/floating-items.md) | Decorative non-pickup item displays. |
| [Image Icons](systems/image-icons.md) | Provider-scoped local/remote image icons. |
| [Modules](systems/modules.md) | The six internal modules and their lifecycle. |

## Developers

| Doc | What's inside |
|---|---|
| [Developer API](DEVELOPER_API.md) | The stable `HyExtrasApi` facade — every public method, grouped, with examples. |
| [Architecture](architecture.md) | Package map, plugin lifecycle, registration, and data flow. |
| [Internals: Services](internals/services.md) | Internal service classes and their methods. |
| [Internals: TriggerExtras](internals/trigger-extras.md) | Registries, adapter, dispatcher, bridge — and how to add a new effect/condition. |
| [Internals: Packet stack](internals/packet-internals.md) | PacketAPI and the visibility/sync/filter services. |
| [Trigger Volume Research](TRIGGER_VOLUME_RESEARCH.md) | Notes on the native Hytale Trigger Volume API. |

---

The root [README](../README.md) is the project overview and landing page.
