# Plan: per-field tags + asymmetric defaults

Status: **approved, implementing.**

## 1. Why the current tagging doesn't generalize

The DSL today carries two bespoke phantoms: `CronExpression[Time <: Status, Day <: DaySpec]`.
Every new "don't silently overwrite field X" rule invents its *own* marker and re-threads all
the type parameters:

- `fix/04-in-overwrites-month` adds a third phantom `MonthSpec`.
- `fix/01-at-overwrites-hour` adds a new state `Status.HourSet` and splits `.at`.

Two PRs, two incompatible mechanisms, for structurally the *same* rule. **One marker per
field** turns both into one rule.

## 2. Core model

```scala
sealed trait FieldState
object FieldState:
  sealed trait Set   extends FieldState   // this field was chosen by the DSL
  sealed trait Unset extends FieldState   // still at its default; may be chosen

// domain — plain data, no DSL concern (DESIGN.md §2.1):
final case class CronExpression(minute, hour, dayOfMonth, month, dayOfWeek)

// dsl — the tags live on the builder, zero-cost over the domain value:
opaque type Schedule[
    +Min <: FieldState, +Hr <: FieldState, +Dom <: FieldState,
    +Mon <: FieldState, +Dow <: FieldState
] = CronExpression

type FreshCron = Schedule[Unset, Unset, Unset, Unset, Unset]
```

- **Tags live on `cronh.dsl.Schedule`, not the domain.** The phantom state is a builder
  concern, so the domain `CronExpression` is plain five-field data; `Schedule` is an
  `opaque type` over it, carrying the tags at **zero runtime cost** (it *is* a
  `CronExpression` at runtime) and hiding them from the domain and from callers outside the
  DSL. `retag` becomes a private `wrap`/`underlying` across the opaque boundary — no
  `asInstanceOf`, no allocation beyond the `copy`.
- **No render gate.** Every `Schedule` yields a valid cron: `.toCron` / `.humanReadable`
  delegate to the domain renderer at every step, and `.toCronExpression` is the escape
  hatch. `render`/`humanReadable` themselves stay on the plain domain `CronExpression`.
- The tags enforce: **(a)** a field may be set only while `Unset` (no silent overwrite);
  **(b)** day-of-week and day-of-month are mutually exclusive (the OR footgun, §4.5);
  **(c)** **coarse→fine ordering** — each verb requires every strictly-finer field `Unset`.
- Phantoms stay **covariant** (sound: no runtime witness). Equality ignores them — it's the
  domain value's equality.

## 3. The default of an unset field — the systemd insight

Fields split into **time** (minute, hour) and **date filters** (day-of-month, month,
day-of-week). systemd `OnCalendar` (and human intuition) reads them asymmetrically. We bake
that into the blank slate the DSL builds from:

| Field | Default when never set | Renders |
| --- | --- | --- |
| minute, hour | `0` | `0` |
| day-of-month, month, day-of-week | every | `*` |

So the empty schedule is `0 0 * * *` (**daily at midnight** — the safe reading), and:

- `Schedule.in(June)` → `0 0 * 6 *` (midnight daily in June — what a reader expects).
- High frequency is an explicit opt-in: `* * * * *` only via `.everyMinute`.

**Corollary (asymmetry made operational):** a *date* field becomes `*` by doing nothing
(it's the default); a *time* field becomes `*` only via an explicit verb (`.everyHour`,
`.everyMinute`). There is intentionally no "every day-of-month" verb — that's just the
default.

## 4. Verbs

### Entry points on `Schedule` (build from the `0 0 * * *` blank slate)

| Entry | Renders |
| --- | --- |
| `daily` | `0 0 * * *` |
| `hourly` | `0 * * * *` |
| `monthly` | `0 0 1 * *` |
| `yearly` | `0 0 1 1 *` |
| `weekdays` | `0 0 * * 1-5` |
| `weekends` | `0 0 * * 0,6` |
| `everyMinute` | `* * * * *` |
| `in(Month*)` / `on(DayOfWeek*)` / `on(WeekdaySelector)` / `onThe(MonthDay*)` | date filter set, `0 0` time |
| `between(Hour,Hour)` / `at(...)` | time set |

`daily`/`hourly`/… are methods on the `Schedule` *object*, not refinements, so there is no
`.daily`/`.hourly` to chain — `monthly.daily.hourly` doesn't typecheck.

### Refinement verbs (extensions), with coarse→fine requirements

Hierarchy, coarse→fine: `month` ▸ `{day-of-month, day-of-week}` ▸ `hour` ▸ `minute`. Each
verb requires every strictly-finer field still `Unset`.

| Verb | Requires `Unset` | Sets |
| --- | --- | --- |
| `.in(Month*)` | month, dom, dow, hour, minute | month |
| `.on(DayOfWeek*)` / `.on(WeekdaySelector)` | dom, dow, hour, minute | day-of-week |
| `.onThe(MonthDay*)` | dom, dow, hour, minute | day-of-month |
| `.between(Hour,Hour)` | hour, minute | hour (range); minute stays refinable |
| `.everyHour` | hour, minute | hour `*`; minute stays refinable |
| `.at(Hour, Minute)` | hour, minute | hour, minute |
| `.at(Hour)` | hour, minute | hour, **minute `0`** (commits the minute) |
| `.at(Minute)` | minute (hour free) | minute |
| `.everyMinute` | hour, minute | hour `*`, minute `*` |

Decisions baked in:

- `.at(Hour)` commits `minute = 0`, so `.at(9.h).at(30.m)` is a **compile error** (no double
  `at`); use `.at(9.h, 30.m)` for 9:30. `.at(Minute)` is for after `.between`/`.everyHour`
  (or bare → `30 0 * * *`).
- Coarse→fine means `.in` after a time verb, `.between` after `.at`, `.on` after `.onThe`,
  etc. are all compile errors. `monthly.at(6.h).in(March)` is rejected (it's daily-in-March).

## 5. Worked examples

```scala
Schedule.daily                          // 0 0 * * *
Schedule.in(June)                       // 0 0 * 6 *      midnight daily in June
Schedule.at(9.h)                        // 0 9 * * *
Schedule.at(9.h, 30.m)                  // 30 9 * * *
Schedule.at(30.m)                       // 30 0 * * *     00:30 daily
Schedule.hourly                         // 0 * * * *
Schedule.in(June).everyHour             // 0 * * 6 *      hourly in June
Schedule.in(June).everyHour.at(30.m)    // 30 * * 6 *
Schedule.everyMinute                    // * * * * *
Schedule.in(June).everyMinute           // * * * 6 *  (everyMinute.in would be rejected)
Schedule.weekdays.at(9.h)               // 0 9 * * 1-5
Schedule.onThe(15.dom)                  // 0 0 15 * *
Schedule.on(Mon to Fri).at(9.h)         // 0 9 * * 1-5
Schedule.in(June).between(9.h,17.h).at(30.m)  // 30 9-17 * 6 *
```

Compile errors (the safety surface):

```scala
Schedule.at(9.h).at(14.h)               // double hour          (fix/01)
Schedule.at(9.h).at(30.m)               // double at / minute committed (decision 4)
Schedule.in(June).in(July)              // double month         (fix/04)
Schedule.on(Mon).onThe(15.dom)          // day-of-week + day-of-month (OR, §4.5)
Schedule.at(9.h).in(June)               // coarse after fine    (decision 1)
Schedule.monthly.at(6.h).in(March)      // coarse after fine
```

## 6. POSIX coverage and deliberate limits

About **safety/ergonomics, not reach**. Covered: single values, lists (varargs), ranges
(`between`, `Mon to Fri`), every (`*`, via defaults for dates and `.everyHour`/`.everyMinute`
for time), all grains. Deliberately out (unchanged, DESIGN §4.5/§4.7): `*/n` steps; minute
lists/ranges and the "every minute within an hour range" case (`* 9-17 * * *`); day-of-month
**OR** day-of-week. These are orthogonal future additions.

## 7. File-by-file changes

New: `dsl/FieldState.scala` (the tag — a DSL concern, so it lives in `cronh.dsl`).

Rewritten: `domain/CronExpression.scala` (plain five-field case class, no type params);
`dsl/Schedule.scala` (`opaque type Schedule[…]` + entry points from one blank slate);
`dsl/ScheduleOps.scala` (per-field verbs over `Schedule`, coarse→fine bounds, `.onThe`,
`.everyHour`, `.everyMinute`, plus `.toCron`/`.humanReadable`/`.toCronExpression`
delegation).

Modified: `render/CronDialect.scala`, `render/HumanReadable.scala` (render plain
`CronExpression`; `HumanReadable` object made `private[cronh]` so the DSL can delegate);
`test/dsl/PhantomTest.scala` (double-set, exclusivity, ordering); `test/dsl/ScheduleTest.scala`
(new surface + asymmetric-default renders); `test/domain/Generators.scala`,
`test/render/RenderTest.scala` (`Arbitrary[CronExpression]`); `test/render/HumanReadableTest.scala`
(raw-expression cases call `HumanReadable.describe`).

Removed: `domain/Status.scala`, `domain/DaySpec.scala`.

Unchanged: `WeekdaySelector`, `aliases`, literals.

## 8. Verification

`sbt ci` (scalafmt + tpolecat warnings-as-errors + test). `PhantomTest` `compileErrors`
assertions prove each illegal chain is rejected; `ScheduleTest` pins the asymmetric-default
renders.
