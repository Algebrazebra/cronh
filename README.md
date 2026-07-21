# CRONH
> _the **h** stands for **human**-friendly_

[![Scala Version](https://img.shields.io/badge/scala-3.x-DC322F.svg)](https://www.scala-lang.org/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.algebrazebra/cronh_3)](https://central.sonatype.com/artifact/io.github.algebrazebra/cronh_3)
[![Build Status](https://github.com/algebrazebra/cronh/actions/workflows/ci.yml/badge.svg)](https://github.com/algebrazebra/cronh/actions)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg)](https://scala-steward.org)

## Intro

This library lets you define cron schedules that **you can actually read**.

Consider the following:
```scala 3
val cron = "30 14 * * 1-5"

val cronh = Schedule.weekdays.at(time"14:30").toCron
```

Both values, `cron` and `cronh`, represent the same schedule.
Which definition do you find more readable?

What's more, you can do this type-safely.
Not a big surprise for a Scala library, isn't it?
But it does mean that we can catch a lot of errors at compile time that might otherwise be a wrong or missed job execution.

This library currently **targets Vixie cron**, arguably the most common cron implementation.

## Quick start

Install the library by adding it to your `build.sbt`:
```build.sbt
libraryDependencies += "io.github.algebrazebra" %% "cronh" % "<insert latest version>"
```

Then using it is quite straightforward:

```scala 3
import cronh.dsl.*

Schedule.daily.at(time"09:00").toCron
Schedule.in(December).onThe(24.th).at(time"20:00")
Schedule.on(Weekdays).everyHour.toCron
Schedule.in(CQ1).on(Mondays).everyTwoHours(at = 30.min).toCron

// Inclusive ranges use `to`; `until` excludes the end.
Schedule.daily.at(9.h until 17.h).at(0.min).toCron // "0 9-16 * * *"

import cronh.render.*

Schedule.weekdays.at(9.h).humanReadable // "At 9:00 AM, on weekdays"
```

For a more detailed introduction, please see the ScalaDoc of the DSL entrypoint [Schedule.scala](src/main/scala/cronh/dsl/Schedule.scala).
Sometimes the easiest way to learn is to look at a lot of examples. You can find some [here](examples/Schedules.scala).

## Correctness

Generally, this library is designed with correctness in mind.
Of course, in Scala, this means leveraging its type system and compile-time capabilities like macros.
But it's also about designing the DSL so that the change of misreading or misunderstanding the specified schedule is minimized.

### Compile time and runtime checks

In addition to basic type-safe modeling (i.e., providing a month value when a weekday value is expected), 
literals for day of month, hours, and minute are checked at compile time:
```scala
// The following won't compile:
Schedule.onThe(32.th).at(time"9:00")  // 32 is not a valid day of month
Schedule.onThe(1.th).at(time"9:00")   // it's 1.st, not 1.th
Schedule.daily.at(100.h, 61.h).at(-1.min, 61.min) // Not valid hours and minutes
```

Literal range endpoints are also ordered at compile time: `17.h to 9.h` and
`9.h until 9.h` do not compile. When either endpoint is dynamically provided,
the same ordering rules are validated at runtime.

Of course, the same checks are also made at runtime when compile time checking isn't possible.
For example, when inserting values dynamically instead of using literals.

Time string literals are currently only runtime checked.
It's possible to do this at compile time, but it's not implemented yet.

### Design considerations

The DSL is designed for explicitness to prevent misunderstandings by making the DSL read as explicit as possible.
There are also situations where human intuition is simply misaligned with how cron works.

This can mean simple things like having `on(Mondays)` instead of `on(Monday)`. 
The latter could be misinterpreted as non-reoccurring, whereas the former suggests recurrence.
Similarly, an earlier version of the DSL allowed `Schedule.at(9.h)` which has three problems:
1. The absence of a specified day suggests daily scheduling, because unspecified cron fields are assumed to be `*` 
2. The absence of a specified minute mark suggests the full hour, but following the reasoning of the previous point, it should be every minute!
3. It reads like it's not recurring when it should be.

By requiring the explicit closing of the "day phase" with `daily` and forcing the user to set the minute, 
the listed problems are solved: `Schedule.daily.at(time"09:00")`.

Additionally, the DSL tries to protect against two common cron mistakes:
1. Cron doesn't allow specifying perfect intervals in all cases.
For example, cron-scheduling for every 25 minutes won't actually work like that.
Every third interval will only be 10 minutes long as opposed to 25 minutes.
By allowing only `every{Two, Three, Four}Hours` the problem is avoided. 
It's still possible to express the "everyFiveHours" by listing the hours directly – which readers can't misinterpret.
2. Cron assumes an OR relationship between fields, including the day of month and day of week field. A lot of people intuitively expect an AND relationship.
The DSL makes this explicit: `Schedule.onThe(15.th).orOn(Mondays)` to prevent this common misinterpreation.

This section is by no means a complete description of the design considerations, but only serves to highlight a few illustrative examples of the design rationale.

## Scope

The first version targets the POSIX/Vixie five-field baseline. Steps (`*/n`), Quartz tokens
(`L`, `W`, `#`, `?`), seconds fields, and parsing cron strings back into the
model are out of scope for now — see [DESIGN.md](DESIGN.md) for more context.

## Development

```
sbt test          # build and test
sbt ci            # exactly what GitHub Actions runs
sbt scalafmtAll   # format
```

