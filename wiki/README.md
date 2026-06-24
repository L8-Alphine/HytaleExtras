# HyExtras — Wiki source pages

These Markdown files are the source content for the public
[HytaleModding Wiki](https://wiki.hytalemodding.dev/mod/hyextras) pages. They mirror the repo
[`docs/`](../docs) tree, adapted for the wiki.

> This `README.md` is a note for maintainers — it is **not** a wiki page. Don't paste it into the wiki.

## How to use

- **One file = one wiki page.** The filename (without `.md`) is the page **slug**, e.g.
  `effects.md` → `/mod/hyextras/effects`.
- To publish, open the mod's page on the HytaleModding Wiki, click **Add Page**, set the title and the
  matching slug, and paste the file's contents.
- **Cross-page links use bare slugs** (relative), e.g. `[Effects](effects)` and
  `[apply_cooldown](effects#apply_cooldown)`. These resolve to sibling pages under
  `/mod/hyextras/`. Anchors (`#heading`) come from headings, GitHub-style.
- **Page order / sidebar** is configured in the wiki's website UI, not in these files.
- Images, if added later, are uploaded via the mod's management page and embedded with standard
  Markdown image syntax.

## Pages

| Slug | Title |
|---|---|
| `home` | HyExtras (landing) |
| `getting-started` | Getting Started |
| `effects` | Effects Reference |
| `conditions` | Conditions Reference |
| `commands` | Commands Reference |
| `configuration` | Configuration Reference |
| `string-templates` | String Templates, Rules & Color Codes |
| `player-state` | Player State: Variables, Tags & Cooldowns |
| `visibility-and-packets` | Visibility & Packets |
| `interaction-bridge` | Interaction Bridge |
| `tag-npc` | TagNPC: Runtime Entity State |
| `floating-items` | Floating Items |
| `image-icons` | Image Icons |
| `modules` | Internal Modules |
| `developer-api` | Developer API |
| `architecture` | Architecture |
| `internals-services` | Internals: Service Classes |
| `internals-trigger-extras` | Internals: TriggerExtras |
| `internals-packet-stack` | Internals: Packet Stack |
| `trigger-volume-research` | Trigger Volume Research |

## Keeping in sync

When you change `docs/`, re-mirror into `wiki/` (the transform just flattens paths and rewrites links:
`foo.md` → `foo`, `systems/foo.md` → `foo`, `internals/foo.md` → `internals-foo`, `DEVELOPER_API.md`
→ `developer-api`). The `home` page is maintained here directly, separate from `docs/README.md`.
