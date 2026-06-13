# Code review findings — phases 2–8 (`design-phases-2-8`)

Max-effort review of commit `1364de9` (`git diff master...HEAD`). Every
**Confirmed** finding was verified empirically by executing the scenario
against the built library; quoted outputs are observed, not speculated.

Severity scale: **high** (wrong schedule reaches production silently) ·
**medium** (defeats a documented guarantee or wrong user-visible output) ·
**low** (hardening, doc honesty, maintainability) · **info** (style/perf nit).

Status: all findings **open** as of 2026-06-11.

---

## 1. `.at` silently overwrites an existing hour constraint

- **Severity:** high
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/dsl/ScheduleOps.scala:12`

`.between` and `Schedule.hourly` leave `Time = Unset`, and `.at(hour[, minute])`
unconditionally replaces the hour field — the exact silent-overwrite class the
phantom types exist to prevent.

- `Schedule.weekdays.between(9.h, 17.h).at(10.h).toCron == "0 10 * * 1-5"` —
  the 9–17 range is silently destroyed.
- `Schedule.hourly.at(9.h).toCron == "0 9 * * *"` — an hourly schedule
  silently becomes once-daily.

*Suggested fix:* a distinct `Time` state for "hour constrained" (set by
`between`/`hourly`) on which only the minute-only `.at(30.m)` overload is
available.

## 2. `humanReadable` renders `Term.All` as the word "every" inside lists

- **Severity:** medium
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/render/HumanReadable.scala:88`

Legal inputs that reach the generic fallbacks produce ungrammatical output:

- minute `All`, hour `Range(9, 17)` →
  `"At minute every past hour 9 through 17, every day"`
- dayOfMonth `Field.from(Term.All, Term.Single(MonthDay(1)))` (legal redundant
  input per DESIGN.md §4.4) → `"At 9:00 AM, on day every and 1 of the month"`

*Suggested fix:* normalize the field first (collapses `All`-containing lists,
Phase 6 machinery already exists) or special-case `All` out of `describe`.

## 3. Public `copy` bypasses the phantom guarantees

- **Severity:** medium
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/domain/CronExpression.scala:19`

`Schedule.daily.at(9.h).copy(hour = Field.single(Hour(14))).toCron ==
"0 14 * * *"`, still typed `CronExpression[Status.Set, ...]` — a `Set`-tagged
expression re-timed silently through the front door.

*Suggested fix:* document `copy` as the deliberate escape hatch in
DESIGN.md §2.10, or make the constructor/`copy` `private[cronh]` and expose a
factory (weigh against §2.9's "storable plain values" goal).

## 4. `.in` called twice silently drops the earlier month constraint

- **Severity:** medium
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/dsl/ScheduleOps.scala:69`

`Schedule.daily.at(9.h).in(Month.June).in(Month.July).toCron == "0 9 * 7 *"` —
June vanishes without warning, inconsistent with the no-silent-overwrite
guarantee enforced for time and day.

*Suggested fix:* a `Month <: MonthSpec` phantom mirroring `DaySpec`.

## 5. Cross-unit opaque equality answers `true`

- **Severity:** medium (pre-existing, re-exposed by this PR's surface growth)
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/domain/Minute.scala:4` (also `Hour`,
  `MonthDay`)

All three opaque unit types erase to `Int` with universal equality:
`Minute(5) == Hour(5)` is `true`. A guard like
`if (job.minute == configuredHour)` type-checks and matches.

*Suggested fix:* `CanEqual` instances restricted to same-type pairs plus
`-language:strictEquality`, or explicit `equals` guidance in scaladoc.

## 6. Default `renderDayOfWeek` still carries the inverted-range bug

- **Severity:** medium (latent until a second dialect exists)
- **Status:** Confirmed by inspection
- **Where:** `src/main/scala/cronh/render/CronDialect.scala:21`

`UnixCronDialect` patches the Monday-first-ordering vs Sunday=0 inversion in
its override via string splicing (`"6,0"`), but the shared default
implementation a future dialect inherits still emits inverted, invalid ranges
(e.g. Quartz Sun=1 would render `Range(Saturday, Sunday)` as `7-1`).

*Suggested fix:* rewrite `Range(x, Sunday)` at the term level
(`Range(x, Saturday) :: Single(Sunday)`) in the shared path before generic
rendering, so every dialect benefits once.

## 7. Non-literal `.h`/`.m`/`.dom` fails with a compiler-internal message

- **Severity:** low
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/dsl/literals.scala:15`

`compileErrors("val x = 9; x.h")` reports ``Cannot reduce `inline if` because
its condition is not a constant value`` before any friendlier guidance; the
scaladoc's "use `Hour.apply` for runtime values" hint never reaches the user.

*Suggested fix:* check `constValueOpt` (or reorder so `requireConst`'s message
dominates) and emit a custom `compiletime.error` for non-literals.

## 8. `.on(Field.all)` marks `ByWeekday` while constraining nothing

- **Severity:** low
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/dsl/ScheduleOps.scala:51`

`Schedule.daily.on(Field.all).toCron == "0 0 * * *"` — the phantom claims a
weekday constraint that does not exist, and a later `.onDay(1.dom)` is a
spurious compile error on that chain.

*Suggested fix:* restrict the overload to curated values
(`Weekdays`/`Weekends`) or reject `All`-containing fields.

## 9. `examples/Schedules.scala` overclaims test coverage and cannot rot-check

- **Severity:** low
- **Status:** Confirmed (grep)
- **Where:** `examples/Schedules.scala:4`

The header says every expression is mirrored by an acceptance test, but
`nightlyBackup` (`"30 2 * * *"`) and `summerReport` (`"0 0 1 6,7 *"`) have no
mirroring test, and the file is not compiled by the build.

*Suggested fix:* add the two missing assertions and/or compile the examples
(scala-cli or a sample subproject).

## 10. Unit bounds duplicated in three unsynchronized places

- **Severity:** low
- **Status:** Confirmed by inspection
- **Where:** `src/main/scala/cronh/dsl/literals.scala:13` (vs
  `Hour.scala:15`-style constructors and `DomainBounds`)

Bounds and error wording exist in the smart constructors, the inline literal
checks, and the `DomainBounds` enumerations. A future change to one copy
leaves `23.h` and `Hour(23)` free to disagree.

*Suggested fix:* inline-accessible bounds constants on each companion as the
single source.

## 11. Weekday/weekend idiom recognition is structural only

- **Severity:** low (accepted degradation)
- **Status:** Confirmed (executed)
- **Where:** `src/main/scala/cronh/render/HumanReadable.scala:57`

`Schedule.on(Mon, Tue, Wed, Thu, Fri).at(9.h).humanReadable` renders the
long form instead of `"on weekdays"`.

*Suggested fix:* normalize before matching (also resolves finding 2).

## 12. Per-call allocation in `Schedule` entry points and `normalized`

- **Severity:** info
- **Status:** Confirmed by inspection
- **Where:** `src/main/scala/cronh/dsl/Schedule.scala:24`

Entry points are `def`s allocating identical immutable expressions per call;
`Field.normalized` rebuilds `domain.zipWithIndex.toMap` per invocation. `val`
entry points and a precomputed index on `DomainBounds` cost nothing.

## 13. `scala.collection.Set` ascription in `isWeekend`

- **Severity:** info
- **Status:** Confirmed by inspection
- **Where:** `src/main/scala/cronh/render/HumanReadable.scala:77`

The read-only supertype with full qualification suggests a shadowing concern
that doesn't exist in `cronh.render`; a plain immutable
`Set[Term[DayOfWeek]](...)` reads cleanly.

---

Refuted during verification (recorded so they aren't re-raised): `-1.m`
correctly triggers the range check (`Scala 3` lexes `-1` as a literal), and
`*,5-6,0` output for wildcard-in-list day fields is per-design (DESIGN.md
§4.4: accepted as redundant, collapsible via `normalized`).
