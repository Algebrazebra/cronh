- Rework the tests to focus on behavior
- Undefined cron expressions (`*,1`, overlapping ranges, etc.) — RESOLVED, see DESIGN.md §4 (Edge-Case Handling).
  - Baseline is Vixie, which has no formal standard; behavior is implementation-defined.
  - `*,1` is *not* illegal under Vixie — a field is the OR of its elements, so a field containing `*` denotes everything. It is accepted and normalized to `Field.all` (DESIGN §4.4).
  - Overlapping ranges are redundant, not illegal — accepted as written, canonicalized by Phase 6 `normalized`.
- FreshCron rename
- .dom implicit method constructor on literal
- readme redo
  - document OR decision to be compatible with Vixie and Quartz

DSL possibilities


Min Hour DOM MON DOW

Not allowing DOW and DOM to be set at the same time to sidestep OR vs AND problem?
Or allow with dialect-specific extension? Schedule.in(June to Sept).on(Friday).orOn(15.dom)?
MONTH has to come first --> otherwise it reads like DOM and MONTH belong together
It's funny cause orOn sounds like it's scheduled either-or, and andOn would indicate it's both but is logically incorrect


Defaults:
- daily --> only time (min, hour) via `at` or `between`
- weekdays --> time (min, hour) --> (Months?)
- weekends --||--
- on(DOW) --||--

- in(June, Dec).on(1, 3, 15)
- monthly.on(1, 3, 15) --> time (at or between)
  vs.- on(1,3,15).in(June)


Feature: except syntax

Have short form weekdays and long form (Mon, Monday)
Have short form months and long form (Jan, January)


Test wraparound for ranges like MonthRange, etc.

Except syntax on ranges: Monday to Friday except (Wednesday, Thursday)