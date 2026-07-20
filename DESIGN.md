# cronh design

`cronh` is a Scala 3 library for constructing five-field Vixie cron
expressions through a readable, type-safe DSL. The implementation is split into
three layers: domain, DSL, and rendering.

## Domain

`CronExpression` stores the five fields in cron order: minute, hour, day of
month, month, and day of week. It is a plain immutable value and has no DSL or
dialect concerns.

Each field is a non-empty ordered list of `Term`s. A term is one of:

- `All` (`*`)
- `Single(value)`
- an inclusive `Range(from, to)`

`Field` preserves insertion order and accepts redundant terms. Normalization is
explicit through `field.normalized`; it collapses wildcards, duplicates, and
overlapping or adjacent coverage into a canonical field.

Cron positions use distinct types. `Minute`, `Hour`, and `DayOfMonth` validate
dynamic values at construction; `Month` and `DayOfWeek` are enums. Ranges
validate their ordering. Invalid values and backwards ranges therefore cannot
reach a renderer through the public constructors.

## DSL

The DSL builds the same domain model through ordered phases:

1. optionally select months;
2. select a day recurrence;
3. select hours and minutes to complete the expression.

Intermediate types expose only valid next operations. This prevents incomplete
schedules, selecting months after days, or completing the time twice. The
private `CronExpressionBuilder` tracks unset fields until a terminal operation
produces a `CronExpression`.

Vixie cron combines restricted day-of-month and day-of-week fields with OR. The
DSL requires that relationship to be written explicitly with `orOn` or
`orOnThe`.

Integer literals such as `9.h`, `30.min`, and `15.th` are checked by macros.
`time"..."` validates constant strings at compile time and interpolated strings
at runtime. Range syntax supports inclusive `to` and exclusive-end `until`;
`between(start, end)` is the named equivalent for hour ranges. Both range forms
require ascending endpoints and never wrap around a field boundary.

Cron fields describe sets, not elapsed durations. Interval helpers therefore
expand to concrete minute or hour marks anchored at the beginning of their
field. They do not claim to model arbitrary wall-clock intervals.

## Rendering

Rendering is separate from construction. `Render[A]` renders field values,
while `CronDialect` renders a complete expression and owns weekday numbering.
The default `UnixCronDialect` uses Sunday = 0 through Saturday = 6. It also
splits ranges ending on Sunday when numeric rendering would otherwise invert
the range; for example, Friday through Sunday becomes `5-6,0`.

`toCron` preserves the expression's field and term order. It does not normalize
implicitly. `humanReadable` is a separate best-effort English renderer that
recognizes common forms such as weekdays and weekends.

## Key decisions

The following choices explain the less obvious parts of the implementation.

| Decision | Alternative not used | Reason |
| --- | --- | --- |
| Model cron as immutable data, with the DSL layered on top | Put scheduling behavior directly on a mutable or domain-level builder | Keeps construction, storage, and rendering independently usable. |
| Use a distinct type for each cron position | Use raw `Int` values or one generic numeric field type | Prevents values from being used in the wrong field and keeps bounds local to each type. Nominal wrapper types also preserve meaningful equality between units. |
| Represent a field as a non-empty ordered list of `Term`s | Use a `Set`, allow empty fields, or add a nested `OneOf` term | Makes invalid empty fields unrepresentable, supports mixed singles and ranges, and preserves deterministic rendering without duplicate representations of lists. |
| Represent `All` as a regular term | Make wildcard a separate field shape | Allows the domain to faithfully represent any syntactically valid term list, including redundant lists containing `*`. |
| Preserve input and make normalization explicit | Deduplicate, sort, or merge during construction or rendering | Avoids silently rewriting user data and keeps rendering predictable. |
| Use intermediate DSL phase types backed by a private builder | Put state phantom parameters on `CronExpression`, use last-write-wins setters, or default incomplete schedules silently | Keeps the domain value simple while making incomplete or ambiguous fluent schedules fail to compile. |
| Require `orOn` and `orOnThe` when both day fields are restricted | Hide Vixie OR behind ordinary chaining or imply AND | Makes cron's surprising day-of-month/day-of-week semantics visible at the call site. |
| Keep weekday encoding in `CronDialect` | Store a cron number on `DayOfWeek` | Weekday numbering belongs to a cron dialect, not to the calendar value. |
| Split Unix weekday ranges that end on Sunday | Render Sunday as both `0` and `7`, or reject those ranges in the domain | Preserves one Unix encoding for Sunday while keeping valid calendar ranges representable. |
| Expand interval helpers to concrete field marks | Add step terms or a generic `every(hours)` API | Reflects the field-anchored behavior cron can actually express and avoids implying arbitrary elapsed-time intervals. |
| Keep the core dependency-free | Depend on Cats for abstractions such as `Semigroup` | The small domain model does not justify a production dependency. |

### Retired: wrapping ranges

Wrapping ranges were considered for every finite cron domain, with descending
endpoints crossing the domain boundary: for example, Friday through Monday,
December through March, or hour 22 through hour 2. The implementation could
split such a selection into two ordinary ranges, but that representation would
make the DSL look more interval-oriented than cron actually is.

Cron evaluates its fields independently and combines their selected values as a
cross-product. Consequently, wrapping both hours and minutes would not describe
one continuous wall-clock interval, and wrapping days of the month would select
nominal dates independently in every month despite differing month lengths.
Weekday and month wrapping can be read naturally in isolation, but supporting
it only for those fields would make range behavior inconsistent across the DSL.

The DSL therefore rejects descending endpoints for every field type. This also
keeps accidentally reversed endpoints from silently becoming a much broader
schedule. Callers who intend a boundary-spanning selection can list its values
explicitly, making the underlying set semantics visible at the call site.

## Boundaries

The core has no production dependencies and deliberately models only the
five-field baseline. It does not parse cron expressions, model seconds or time
zones, check whether calendar combinations can ever occur, or represent
dialect-specific extensions such as `L`, `W`, `#`, and `?`.
