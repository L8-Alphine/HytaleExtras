# Commands Reference

All HyExtras admin commands live under the root **`/hextras`** command. Player arguments accept any
online player name; entity arguments take a raw UUID.

**Groups:** [var](#hextras-var) · [tag](#hextras-tag) · [cooldown](#hextras-cooldown) ·
[list](#hextras-list) · [debug](#hextras-debug) · [modules / module](#hextras-modules--hextras-module) ·
[tagnpc](#hextras-tagnpc) · [floatingitems](#hextras-floatingitems) · [reload](#hextras-reload)

---

## `/hextras var`

Manage per-player [variables](player-state#variables) (runtime-only).

| Command | Description |
|---|---|
| `/hextras var get <player> <key>` | Print a variable value (`(not set)` if absent). |
| `/hextras var set <player> <key> <value>` | Set a variable (value is greedy — may contain spaces). |
| `/hextras var add <player> <key> <amount>` | Add an integer amount to a numeric variable. |
| `/hextras var increment <player> <key> <amount>` | Alias of `add`. |
| `/hextras var del <player> <key>` | Delete a variable. |
| `/hextras var list <player>` | List all variables for a player. |

```
/hextras var set Alphine score 100
/hextras var add Alphine score 5
```

---

## `/hextras tag`

Manage persistent [player tags](player-state#tags). All mutations persist immediately.

| Command | Description |
|---|---|
| `/hextras tag add <player> <tag>` | Add a persistent tag. |
| `/hextras tag remove <player> <tag>` | Remove a persistent tag. |
| `/hextras tag set <player> <tag> <true\|false>` | Add (`true`) or remove (`false`) a tag. |
| `/hextras tag has <player> <tag>` | Check whether a player has a tag. |
| `/hextras tag list <player>` | List a player's tags. |
| `/hextras tag clear <player>` | Remove all tags and delete the persisted tag file. |

```
/hextras tag add Alphine storyline_a_active
/hextras tag has Alphine storyline_a_active
```

---

## `/hextras cooldown`

Inspect/clear named [cooldowns](player-state#cooldowns).

| Command | Description |
|---|---|
| `/hextras cooldown check <player> <name>` | Show remaining cooldown time. |
| `/hextras cooldown clear <player> <name>` | Clear a cooldown. |

---

## `/hextras list`

| Command | Description |
|---|---|
| `/hextras list actions` | List all registered HyExtras effect type IDs. |
| `/hextras list conditions` | List all registered HyExtras condition type IDs. |

---

## `/hextras debug`

| Command | Description |
|---|---|
| `/hextras debug player <player>` | Dump a player's variables, cooldowns, tags, and hidden-entity state. |

---

## `/hextras modules` / `/hextras module`

Manage [internal modules](modules). `module` actions respect the `allowInGameToggle` and
`reloadable` flags in [configuration](configuration#internal-modules).

| Command | Description |
|---|---|
| `/hextras modules` | List internal modules and their state. |
| `/hextras module info <module>` | Show one module's state and config. |
| `/hextras module enable <module>` | Enable a live-toggleable module and persist the config key. |
| `/hextras module disable <module>` | Disable a live-toggleable module and persist the config key. |
| `/hextras module reload <module>` | Reload a reloadable module. |

Module ids: `trigger_extras`, `placeholder_api`, `packet_api`, `image_icons`, `tag_npc`, `floating_items`.

---

## `/hextras tagnpc`

Admin tools for [TagNPC](tag-npc) runtime entity state. The `tag`/`var`/`hide`/`show`
groups take an explicit `<entityUuid>`; the `near` group resolves the closest indexed NPC/mob to a
player so you don't need to copy UUIDs.

**By explicit UUID:**

| Command | Description |
|---|---|
| `/hextras tagnpc tag add <entityUuid> <tag>` | Add a runtime entity tag. |
| `/hextras tagnpc tag remove <entityUuid> <tag>` | Remove a runtime entity tag. |
| `/hextras tagnpc tag list <entityUuid>` | List entity tags. |
| `/hextras tagnpc var set <entityUuid> <key> <value>` | Set an entity variable (greedy value). |
| `/hextras tagnpc var get <entityUuid> <key>` | Read an entity variable. |
| `/hextras tagnpc var add <entityUuid> <key> <amount>` | Add to a numeric entity variable. |
| `/hextras tagnpc var list <entityUuid>` | List entity variables. |
| `/hextras tagnpc hide <entityUuid> <viewer>` | Hide an entity from a player viewer. |
| `/hextras tagnpc show <entityUuid> <viewer>` | Show an entity to a player viewer. |

**By proximity (`near`)** — each searches within `<radius>` blocks of `<player>`:

| Command | Description |
|---|---|
| `/hextras tagnpc near info <player> <radius>` | Show the closest indexed NPC/mob and its state. |
| `/hextras tagnpc near tag-add <player> <radius> <tag>` | Tag the closest NPC/mob. |
| `/hextras tagnpc near tag-remove <player> <radius> <tag>` | Untag the closest NPC/mob. |
| `/hextras tagnpc near var-set <player> <radius> <key> <value>` | Set a variable on the closest NPC/mob. |
| `/hextras tagnpc near var-add <player> <radius> <key> <amount>` | Add to a variable on the closest NPC/mob. |

```
/hextras tagnpc near tag-add Alphine 12 quest_guard
```

---

## `/hextras floatingitems`

Manage decorative [floating items](floating-items). Each subcommand requires a permission
node (`hyextras.floatingitems.*`).

| Command | Permission | Description |
|---|---|---|
| `/hextras floatingitems list` | `hyextras.floatingitems.list` | List floating item IDs. |
| `/hextras floatingitems info <id>` | `hyextras.floatingitems.info` | Show item id, item, quantity, persistent/intangible state, position. |
| `/hextras floatingitems create <id> <player> <itemId> [persistent]` | `hyextras.floatingitems.create` | Create at a player's position. `persistent` defaults to `floatingItems.defaultPersistent`. |
| `/hextras floatingitems remove <id>` | `hyextras.floatingitems.remove` | Remove a floating item. |
| `/hextras floatingitems intangible <id> <true\|false>` | `hyextras.floatingitems.modify` | Set intangible state. |
| `/hextras floatingitems reload` | `hyextras.floatingitems.reload` | Re-render and save persistent floating items. |

```
/hextras floatingitems create ruins_key Alphine hytale:gold_coin true
```

---

## `/hextras reload`

| Command | Description |
|---|---|
| `/hextras reload` | Reload `hyextras.properties` at runtime. |

---

## See also

- [Configuration](configuration) — what `/hextras reload` and `/hextras module` change.
- [Player State](player-state) — the data behind `var`, `tag`, and `cooldown`.
- [TagNPC](tag-npc) and [Floating Items](floating-items) — the systems behind those groups.
