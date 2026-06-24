# String Templates, Rules & Color Codes

HyExtras text fields support three layered features:

1. **Template placeholders** — `{...}` tokens that substitute text (e.g. `{player}`), resolved by
   `StringTemplate`.
2. **Rule predicates** — `{...}` tokens that evaluate to **true/false**, used by visibility
   rule fields and `HyExtrasApi.evaluateRule(...)`, resolved by `RuleEvaluator`.
3. **Color & style codes** — `&`-prefixed formatting in chat/title text.

Understanding which layer applies to which field avoids surprises: a hide-rule field treats
`{hasTag:vip}` as a boolean test, while a message field treats it as the literal text `true`/`false`.

**Sections:** [Where each applies](#where-each-applies) · [Template placeholders](#template-placeholders) ·
[Rule predicates](#rule-predicates) · [Color & style codes](#color--style-codes) · [PlaceholderAPI](#placeholderapi)

---

## Where each applies

| Field type | Examples | Resolver | Layer |
|---|---|---|---|
| Message/command text | `send_title`, `send_rich_message`, `send_reward_message`, `run_command`, `calculate_variable` `Formula`, `set_variable` `Value`, `floating_item_create` `Id` | `StringTemplate.resolve()` | Template placeholders (+ color codes for messages) |
| Rule fields | `player_hide_entity`/`player_show_entity` `ViewerRule` & `TargetRule`; `HyExtrasApi.evaluateRule()` | `RuleEvaluator.matches()` | Rule predicates (boolean) |
| API text | `HyExtrasApi.resolveText()` | `RuleEvaluator.resolveText()` | Template placeholders (subset) |

---

## Template placeholders

Resolved in message/command fields. A missing value generally becomes an empty string.

| Placeholder | Resolves to |
|---|---|
| `{player}` | Triggering player's username. |
| `{uuid}` | Triggering entity UUID (`unknown` if absent). |
| `{eventType}` | Current trigger event type name (e.g. `ENTER`). |
| `{variable:key}` | Per-player variable value; empty string if unset. |
| `{hasTag:tag}` / `{!hasTag:tag}` | Literal `true`/`false` for the player's persistent tag. |
| `{tagKey}` / `{tagValue}` | Current tag-event key/value (set during `TAG_ADDED`/`TAG_REMOVED` and the interaction bridge). |
| `{currentVolumeId}` | ID of the current trigger volume. |
| `{activeVolumeCount}` | Number of active volumes for the player. |
| `{currentVolumeTag:key}` | Tag value on the **current** trigger volume; empty if absent. |
| `{volumeTag:volumeId:key}` | Tag value on a **named** volume (note the two-segment form); empty if absent. |
| `{volumeActive:volumeId}` | `true` when the named volume is active for the player, else `false`. |
| `%placeholder%` | PlaceholderAPI placeholder (see below). |

> **Note:** the single-segment `{volumeTag:key}` is a *rule* predicate (below); in message text it is
> not a template placeholder. Use `{currentVolumeTag:key}` for the current volume or
> `{volumeTag:volumeId:key}` for a named one.

```json
{ "type": "send_title", "eventType": "ENTER",
  "Title": "Welcome, {player}!",
  "Subtitle": "Score {variable:score} · Zone {currentVolumeId}" }
```

---

## Rule predicates

Used by `ViewerRule`/`TargetRule` and `evaluateRule(...)`. **All** `{...}` tokens in the rule must
pass for the rule to match; a blank rule matches everything. Unknown tokens fail (and log under
`debugMode`).

| Predicate | True when |
|---|---|
| `{hasTag:tag}` | Player has the persistent tag. |
| `{!hasTag:tag}` | Player does **not** have the tag. |
| `{variable:key}` | Variable exists (non-null). |
| `{!variable:key}` | Variable is missing. |
| `{variable:key=value}` | Variable equals `value` (exact string match). |
| `{variable:key!=value}` | Variable does not equal `value`. |
| `{eventType:NAME}` | Current event type equals `NAME` (case-insensitive). |
| `{volumeTag:key}` | One active volume has the tag `key`. |
| `{volumeTag:key=value}` | One active volume has tag `key` equal to `value` (case-insensitive). |

```json
{
  "type": "player_hide_entity",
  "eventType": "ENTER",
  "TargetSelector": "players",
  "ViewerRule": "{!hasTag:see_stealth}",
  "TargetRule": "{hasTag:stealthed}{variable:partyId!=alpha}"
}
```

```java
boolean canSeeStealth = api.evaluateRule("{hasTag:see_stealth}", playerUuid);
boolean inAlpha       = api.evaluateRule("{variable:partyId=alpha}", playerUuid);
```

---

## Color & style codes

Supported in chat/title text (`send_rich_message`, `send_title`, `send_reward_message`, and
`HyExtrasApi` message helpers). Conversion is handled by `RichText`.

| Code | Effect | Code | Effect |
|---|---|---|---|
| `&0` | black `#000000` | `&8` | dark gray `#555555` |
| `&1` | dark blue `#0000AA` | `&9` | blue `#5555FF` |
| `&2` | dark green `#00AA00` | `&a` | green `#55FF55` |
| `&3` | dark aqua `#00AAAA` | `&b` | aqua `#55FFFF` |
| `&4` | dark red `#AA0000` | `&c` | red `#FF5555` |
| `&5` | dark purple `#AA00AA` | `&d` | light purple `#FF55FF` |
| `&6` | gold `#FFAA00` | `&e` | yellow `#FFFF55` |
| `&7` | gray `#AAAAAA` | `&f` | white `#FFFFFF` |

Styles & specials:

| Code | Effect |
|---|---|
| `&#RRGGBB` | Custom hex color (6 hex digits). |
| `&l` | Bold |
| `&o` | Italic |
| `&n` | Underline |
| `&r` | Reset color & styles |
| `&&` | Literal `&` |

> Only `&l`, `&o`, `&n`, `&r` styles are supported (no strikethrough/obfuscated codes). Applying a
> new color does not clear bold/italic/underline — use `&r` to reset.

```json
{ "type": "send_rich_message", "eventType": "ENTER",
  "Message": "&aWelcome &l{player}&r&a — &#FFD700VIP&r access granted." }
```

---

## PlaceholderAPI

When the optional [PlaceholderAPI for Hytale](https://github.com/HelpChat) plugin is installed and the
`placeholder_api` module is enabled, `%placeholder%` tokens are resolved after native placeholders.
If a token can't be resolved, behavior follows
[`stringTemplate.placeholderApi.missingBehavior`](configuration.md#string-templates):

| Behavior | Result |
|---|---|
| `KEEP_ORIGINAL` (default) | Leave the `%placeholder%` text as-is. |
| `EMPTY` | Replace with an empty string. |
| `ERROR` | Treat the render as failed (effect logs and skips). |

---

## See also

- [Effects](effects.md) and [Conditions](conditions.md) — which fields are templated.
- [Visibility & Packets](systems/visibility-and-packets.md) — how `ViewerRule`/`TargetRule` drive hiding.
- [Configuration](configuration.md#string-templates) — enabling/disabling each resolution layer.
- [Developer API](DEVELOPER_API.md#rules--placeholders) — `evaluateRule` and `resolveText`.
