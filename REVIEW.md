# Code review — `mzell/update-dsl` working tree (medium effort)

**Scope:** uncommitted changes to `DESIGN.md`, `README.md`, `examples/Schedules.scala`, plus the
new untracked files `CLAUDE.md`, `TODO.md`, `WrappingRangePlan.md`, and
`src/test/scala/cronh/dsl/ExamplesTest.scala`. The committed range vs upstream only deletes
`CODE_REVIEW.md`.

**Verification:** every documented cron string, compile-error claim, and "in flux" status was
checked against the actual source (`Schedule.scala`, `aliases.scala`, `WeekdaySelector.scala`,
`Time.scala`, `ScheduleTest.scala`, the fieldTypes companions), and the full test suite was run:
**83/83 pass**, including the new `ExamplesTest`. The README/example strings are all accurate.
Findings below are ranked most severe first. No changes were made.

---

## Findings

### 2. `DESIGN.md` §2.11 claims bare-hour time is "unrepresentable" — it is representable today — CONFIRMED

`DESIGN.md:100` — *"The blessed form is `at(9.h, 0.min)` (or `at(Time)`); the ambiguity is
unrepresentable rather than documented away."*

The vararg-hours overload `TimePhase.at(h: Hour, hs: Hour*)` (`Schedule.scala:142`) makes
`Schedule.weekdays.at(9.h)` compile and silently pin the minute to 0 — `ScheduleTest.scala:13`
and `:17` (`at(noon)`) exercise exactly this form and pass. The same bullet's own parenthetical
and §6 Q1 admit the overload exists, so the lead claim contradicts both the code and the rest of
the paragraph.

**Failure scenario:** a reader (or future session) trusts the "unrepresentable" guarantee, writes
`at(9.h)` expecting a compile error to force explicitness, gets a silently minute-0-pinned
schedule instead — precisely the ambiguity class §2.11 exists to prevent. Suggested wording:
"the ambiguity is *meant to become* unrepresentable; a vararg-hours overload currently defeats
this (§6 Q1)".

---

## Noted outside the review scope (pre-existing, not part of this diff)

- `src/main/scala/cronh/dsl/Time.scala:45`: the parse-error interpolation variable is named
  `$bullshit` and its value lands in a user-facing `IllegalArgumentException` message pattern —
  worth renaming before any release.
- `src/main/scala/cronh/dsl/aliases.scala`: the scaladoc examples `Schedule.daily.on(Weekdays)`
  and `Schedule.on(Mon, Fri).at(noon)` don't reflect the current phase API — `daily` returns
  `TimePhase`, which has no `.on`, and `.on` doesn't accept `WeekdaySelector` yet (§6 Q3).
- `WrappingRangePlan.md` is untracked and referenced by nothing in the diff — confirm it's meant
  to be committed.

---

## Findings (JSON)

```json
[
  {
    "file": "CLAUDE.md",
    "line": 20,
    "summary": "Newly added CLAUDE.md documents the pre-rewrite architecture (no renderer/DSL, MonthDay, types under cronh.domain), all false against the current code.",
    "failure_scenario": "Future sessions load CLAUDE.md as instruction-overriding truth, plan against a data-model-only codebase, and edit against the removed MonthDay/cronh.domain layout."
  },
  {
    "file": "DESIGN.md",
    "line": 100,
    "summary": "§2.11 claims bare-hour time is 'unrepresentable', but the vararg at(h, hs*) overload makes Schedule.weekdays.at(9.h) compile and silently pin minute 0 (ScheduleTest passes it).",
    "failure_scenario": "Reader relies on the documented guarantee, writes at(9.h) expecting a compile error, and ships a schedule with a silently defaulted minute — the exact ambiguity the section claims is impossible."
  },
  {
    "file": "examples/Schedules.scala",
    "line": 5,
    "summary": "Header says examples are mirrored by ScheduleTest and HumanReadableTest, but the mirror is now the new ExamplesTest; several examples exist only there.",
    "failure_scenario": "An example is edited, the named tests contain no matching assertion, and the example/test honesty contract silently drifts."
  },
  {
    "file": "src/test/scala/cronh/dsl/ExamplesTest.scala",
    "line": 65,
    "summary": "compileErrors assertions have no positive control, dropping the scope-sanity check the deleted PhantomTest had (per DESIGN.md).",
    "failure_scenario": "A rename makes a snippet fail to compile for an unrelated reason (e.g. Month import), the nonEmpty assertion stays green, and the documented phase-type rejection is no longer actually tested."
  }
]
```
