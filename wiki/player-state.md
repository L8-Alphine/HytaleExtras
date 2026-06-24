# Player State: Variables, Tags & Cooldowns

HyExtras tracks three kinds of per-player state, each with a different lifetime:

| State | Backing service | Lifetime | Use for |
|---|---|---|---|
| Variables | `PlayerVariableService` | Runtime only; cleared on disconnect | Counters, session flags, group identity, formula inputs |
| Cooldowns | `CooldownService` | Runtime only; cleared on disconnect | Rate-limiting effects, daily/timed gates |
| Tags | `PlayerTagService` | **Persistent** — saved to disk, survive restarts | Story progress, unlocks, long-lived flags |

All three are keyed by player UUID and are thread-safe (`ConcurrentHashMap`-backed). The
[Developer API](developer-api) exposes every operation; the [`/hextras`](commands) commands
expose them to admins.

---

## Variables

Per-player key→value pairs (`PlayerVariableService`). Values are stored as objects but are typically
strings or numbers.

- **Effects:** [`set_variable`](effects#set_variable),
  [`add_variable`/`increment_variable`](effects#add_variable--increment_variable),
  [`calculate_variable`](effects#calculate_variable), [`remove_variable`](effects#remove_variable).
- **Condition:** [`variable_condition`](conditions#variable_condition).
- **Templates:** `{variable:key}` resolves the value (empty string if unset). See
  [String Templates](string-templates).
- **Increment semantics:** a missing variable is treated as `0` before adding; `Delta` may be negative.
- **API:** `setVariable`, `getVariableString`, `incrementVariable`, `snapshotVariables`, … (see
  [Developer API → Variables](developer-api#variables)).

`partyId` is the conventional variable used by the [group visibility policy](visibility-and-packets#volume-visibility-policy-tags).

---

## Tags

Persistent boolean flags (`PlayerTagService`). A tag is either present or absent — there is no value.

- **Effects:** [`add_tag`](effects#add_tag), [`remove_tag`](effects#remove_tag).
- **Condition:** [`has_tag`](conditions#has_tag) (supports `Invert`).
- **Templates/rules:** `{hasTag:tag}` / `{!hasTag:tag}`.

### Persistence
Tags are held in memory per player and written to **`{dataDir}/players/{uuid}.tags`** — a Java
properties file with one `tag=1` line per tag. The lifecycle:

| Moment | Behavior |
|---|---|
| Player connects | `loadPlayer(uuid)` reads the `.tags` file into memory. |
| Player disconnects | `saveAndClearPlayer(uuid)` writes tags to disk and frees memory. |
| `/hextras tag …` mutation | Persists immediately (`savePlayer`). |
| `clearTags(uuid)` | Removes all tags **and deletes** the `.tags` file. |

Because tags survive restarts, use them for story/quest progress, achievements, and unlock gates —
not for transient session data (use variables for that).

---

## Cooldowns

Named per-player timers (`CooldownService`), separate from the native volume cooldown.

- **Effect:** [`apply_cooldown`](effects#apply_cooldown) (`Name`, `Duration` seconds).
- **Condition:** [`cooldown_ready`](conditions#cooldown_ready) — passes only when the cooldown is
  **not** active.
- **Commands:** `/hextras cooldown check|clear`.

Internally each entry stores an expiry timestamp (`now + duration×1000 ms`). `isReady` returns true
when the entry is absent or expired; `remainingSeconds` returns `0` when not active. All cooldowns
clear on disconnect.

The classic pattern — fire once, then gate for a duration:

```json
{
  "conditions": [ { "type": "cooldown_ready", "eventType": "ENTER", "Name": "ruins_intro" } ],
  "effects": [
    { "type": "apply_cooldown", "eventType": "ENTER", "Name": "ruins_intro", "Duration": 300.0 },
    { "type": "send_title",     "eventType": "ENTER", "Title": "You entered the hidden ruins." }
  ]
}
```

---

## Arithmetic

[`calculate_variable`](effects#calculate_variable) and [`math_condition`](conditions#math_condition)
evaluate a formula with the `ArithmeticExpression` engine. The formula string is
[template-resolved first](string-templates) (so `{variable:...}` becomes a number), then evaluated.

Supported syntax:

| Feature | Notes |
|---|---|
| `+` `-` `*` `/` `%` | Standard precedence; `%` is modulo. |
| Parentheses | `( … )` for grouping. |
| Unary `+` / `-` | e.g. `-5`, `-(a+b)`. |
| Decimal literals | `3`, `3.5`. |
| `true` / `false` | Evaluate to `1` / `0`. |

- Whole-number results format as integers (`10`, not `10.0`).
- Division or modulo **by zero**, non-finite results, and unexpected tokens raise an error and the
  effect/condition logs and skips.

```json
{ "type": "calculate_variable", "eventType": "ENTER", "Key": "bonus", "Formula": "({variable:score} + {activeVolumeCount}) * 2" }
```

---

## Connect / disconnect lifecycle

On disconnect HyExtras cleans up all per-player state in one place: tags are saved then cleared,
variables and cooldowns are dropped, and visibility/targeting/icon state is released. See
[Architecture → lifecycle](architecture#plugin-lifecycle) for the full cleanup chain.

## See also

- [Developer API](developer-api) — `Variables`, `Tags`, `Cooldowns` sections.
- [Commands](commands) — `/hextras var`, `/hextras tag`, `/hextras cooldown`.
- [String Templates](string-templates) — `{variable:…}`, `{hasTag:…}`.
