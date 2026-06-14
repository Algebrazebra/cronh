# cronh

A Scala 3 library for describing cron schedules in code that **reads aloud
naturally** and is **caught wrong at compile time** when it would otherwise be
a 3 AM page.

## Before / after

```scala
// Before: write-only string. Is this 2:30 PM? Are weekdays 1-5 or 0-4?
val schedule = "30 14 * * 1-5"

// After: reads like the schedule it is.
val schedule = Schedule.weekdays.at(14.h, 30.m).toCron  // "30 14 * * 1-5"
```

## Quick start

```scala
import cronh.dsl.*
import cronh.render.*

Schedule.daily.at(14.h, 30.m).toCron        // "30 14 * * *"
Schedule.weekdays.at(9.h).toCron            // "0 9 * * 1-5"
Schedule.on(Mon, Fri).at(noon).toCron       // "0 12 * * 1,5"
Schedule.daily.on(Mon to Fri).at(9.h).toCron // "0 9 * * 1-5"
Schedule.weekdays.between(9.h, 17.h).toCron // "0 9-17 * * 1-5"

Schedule.weekdays.at(9.h).humanReadable     // "At 9:00 AM, on weekdays"
```

## Caught at compile time

Out-of-range literals, double-set times, and conflicting day constraints are
compile errors, not 3 AM surprises:

```scala
25.h                              // error: Hour must be between 0 and 23
60.m                              // error: Minute must be between 0 and 59
Schedule.daily.at(9.h).at(14.h)   // error: time already set
Schedule.on(Mon).onDay(1.dom)     // error: day-of-week and day-of-month
                                  //        conflict (Vixie ORs them!)
```

The last one matters: classic cron fires when *either* day field matches when
both are set — almost never what was intended. `cronh` makes the combination
unrepresentable in the DSL.

## DSL reference

| Entry point | Default | Cron |
| --- | --- | --- |
| `Schedule.daily` | every day at 00:00 | `0 0 * * *` |
| `Schedule.hourly` | every hour, on the hour | `0 * * * *` |
| `Schedule.weekdays` | Mon-Fri at 00:00 | `0 0 * * 1-5` |
| `Schedule.weekends` | Sat & Sun at 00:00 | `0 0 * * 6,0` |
| `Schedule.monthly` | 1st of the month at 00:00 | `0 0 1 * *` |
| `Schedule.yearly` | Jan 1st at 00:00 | `0 0 1 1 *` |
| `Schedule.on(Mon, Fri)` | those weekdays at 00:00 | `0 0 * * 1,5` |
| `Schedule.onDay(1.dom, 15.dom)` | those days at 00:00 | `0 0 1,15 * *` |

| Refinement | Effect |
| --- | --- |
| `.at(14.h, 30.m)` | sets the time of day (once — twice is a compile error) |
| `.at(9.h)` | sets the hour, on the hour |
| `.between(9.h, 17.h)` | constrains the hour to a range; minute still settable via `.at(30.m)` |
| `.on(Mon, Fri)` / `.on(Mon to Fri)` / `.on(Weekdays)` | constrains weekdays (list or inclusive range) |
| `.onDay(15.dom)` | constrains days of the month |
| `.in(Month.June)` | constrains months |

Conveniences: `midnight`, `noon`, `Weekdays`, `Weekends`, and the day aliases
`Mon` … `Sun` (with long forms `Monday` … `Sunday`). Adjacent weekdays compose
into an inclusive range with `to`, mirroring the stdlib `1 to 5` convention:
`Mon to Fri` or `Monday to Friday` both render as `1-5`.

## Under the hood

The DSL is sugar over a strictly typed model in `cronh.domain`: validated
per-position types (`Minute`, `Hour`, `MonthDay`, `Month`, `DayOfWeek`), a
three-shape `Term` ADT (`All`, `Single`, `Range`), a non-empty `Field`, and a
five-field `CronExpression` whose phantom type parameters track what has been
set. You can always drop down to it:

```scala
import cronh.domain.*

val field: Field[Minute] =
  Field.of(Minute(0), Minute(30)) ++ Field.range(Minute(45), Minute(50))
```

Redundant input (`1-5,3`, duplicates, overlapping ranges) is legal cron and is
preserved as written; opt-in canonicalization is available:

```scala
(Field.range(Minute(1), Minute(5)) ++ Field.range(Minute(3), Minute(7))).normalized
// Field.range(Minute(1), Minute(7))
```

Rendering is dialect-bound. Weekday numbering differs across cron flavors, so
`DayOfWeek` carries no number; the dialect supplies it. `UnixCronDialect`
(Sunday = 0) is the default, and ranges ending on Sunday are split into
POSIX-valid lists (`Fri-Sun` → `5-6,0`).

## Scope

v1 targets the POSIX/Vixie five-field baseline. Steps (`*/n`), Quartz tokens
(`L`, `W`, `#`, `?`), seconds fields, and parsing cron strings back into the
model are out of scope for now — see [DESIGN.md](DESIGN.md) for the full
design rationale, edge-case catalog, and roadmap. Realistic usage lives in
[`examples/`](examples/Schedules.scala).

## Development

```
sbt test          # build and test
sbt ci            # exactly what GitHub Actions runs
sbt scalafmtAll   # format
```
