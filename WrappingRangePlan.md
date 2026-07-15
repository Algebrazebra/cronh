# Plan: Sunday-first DayOfWeek + Term.WrappingRange + Vixie rendering

## Context

The current `DayOfWeek` enum is Monday-first (ISO 8601). This causes two problems:

1. `Range(Sunday, Monday)` is **invalid** — `Sunday.ordinal (6) > Monday.ordinal (0)` fails the
   `from <= to` check, so a common Vixie range (`0-1`) has no domain representation.
2. `UnixCronDialect` needs a workaround to split any `Range(from, Sunday)` at render time
   (e.g. `Range(Friday, Sunday)` → `5-6,0`) because Sunday's Vixie number (0) is less than
   Saturday's (6), making the naive rendering `5-0` invalid Vixie syntax.

The fix is two-layered:
- **Reorder the enum to Sunday-first** (Sun=0 … Sat=6), aligning domain ordinals with Vixie
  numbering and making `Range(Sunday, Monday)` valid.
- **Add `Term.WrappingRange`** for ranges that cross the Saturday→Sunday week boundary in the
  new ordering (e.g. `WrappingRange(Saturday, Sunday)` = "Sat and Sun"). The renderer expands
  these using the split-at-boundary approach, with the Vixie Sunday=7 alias for the clean
  `SAT-SUN` case.

## Step 1 — Reorder `DayOfWeek` to Sunday-first

**File:** `src/main/scala/cronh/domain/DayOfWeek.scala`

Change the enum case order to:
```
case Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday
```

`Ordering[DayOfWeek]` (by ordinal) and `DomainBounds[DayOfWeek]` (all values in order) update
automatically. After this change `day.ordinal` equals the Vixie number for every day.

## Step 2 — Add `Term.WrappingRange`

**File:** `src/main/scala/cronh/domain/Term.scala`

Add a new case inside the `Term` sealed trait:

```scala
/** An inclusive range that wraps through the domain boundary: `[from, maxVal]` ∪ `[minVal, to]`.
  * Construct via [[WrappingRange.apply]], which enforces `from > to`.
  */
final case class WrappingRange[+A] private (from: A, to: A) extends Term[A]

object WrappingRange {
  def apply[A: Ordering](from: A, to: A): WrappingRange[A] = {
    require(
      Ordering[A].gt(from, to),
      s"Start of WrappingRange must be > end (use Range for non-wrapping spans)."
    )
    new WrappingRange(from, to)
  }
}
```

## Step 3 — Add `Field.wrappingRange` factory

**File:** `src/main/scala/cronh/domain/Field.scala`

Add a factory method alongside `range`, by analogy:

```scala
/** Matches all values from `from` through the domain boundary and back to `to`.
  *
  * Requires `from > to`; throws [[IllegalArgumentException]] otherwise.
  */
def wrappingRange[A: Ordering](from: A, to: A): Field[A] =
  Field(::(Term.WrappingRange(from, to), Nil))
```

Also update the `normalized` extension to expand `WrappingRange` into its constituent indices.
The existing fold collects domain indices; add an arm:

```scala
case Term.WrappingRange(from, to) =>
  val fromIdx = indexOf(from)
  val toIdx   = indexOf(to)
  (fromIdx until domain.size) ++ (0 to toIdx)
```

Normalization expands and rebuilds using only `Single`/`Range` — it never produces a
`WrappingRange` in its output. This is intentional: normalization is opt-in and `WrappingRange`
is a valid unnormalized form.

## Step 4 — Update `Render.scala`

**File:** `src/main/scala/cronh/render/Render.scala`

Add a `WrappingRange` arm to `renderTerm` (naive rendering, intercepted by the dialect for
day-of-week fields):

```scala
case Term.WrappingRange(from, to) => s"${r.render(from)}-${r.render(to)}"
```

## Step 5 — Update `UnixCronDialect`

**File:** `src/main/scala/cronh/render/UnixCronDialect.scala`

**`dayOfWeekRender`** simplifies to a single line — with Sunday-first ordering, `ordinal` equals
the Vixie number directly:

```scala
given dayOfWeekRender: Render[DayOfWeek] = day => day.ordinal.toString
```

**`renderDayOfWeek`** replaces the old `Range(from, Sunday)` split logic with `WrappingRange`
handling. The rule: split at the Sat/Sun boundary; use the Sunday=7 alias when `from == Saturday`
to keep the output idiomatic.

```scala
override protected def renderDayOfWeek(field: Field[DayOfWeek]): String =
  field.terms
    .map {
      case Term.WrappingRange(from, to) =>
        val lo = from.ordinal   // same as Vixie number
        val hi = to.ordinal
        if (lo == 6) {          // from == Saturday: use the 7-alias
          if (hi == 0) "6-7"             // WrappingRange(Sat, Sun)  → SAT-SUN
          else s"6-7,${if (hi == 1) "1" else s"1-$hi"}"  // Sat+Sun via alias, then Mon..to
        } else {
          if (hi == 0) s"$lo-6,0"       // from..Sat, then Sun alone
          else s"$lo-6,0-$hi"           // from..Sat, then Sun..to
        }
      case term => renderTerm(term)
    }
    .mkString(",")
```

Concrete examples:

| WrappingRange         | Output      | Meaning              |
|-----------------------|-------------|----------------------|
| `(Saturday, Sunday)`  | `6-7`       | Sat + Sun (alias)    |
| `(Saturday, Monday)`  | `6-7,1`     | Sat + Sun + Mon      |
| `(Friday, Sunday)`    | `5-6,0`     | Fri + Sat + Sun      |
| `(Thursday, Tuesday)` | `4-6,0-2`   | Thu–Sat + Sun–Tue    |

## Test changes

### `DayOfWeekTest.scala`
Update expected ordinal order: `sorted.head == Sunday`, `sorted.last == Saturday`.

### `TermTest.scala`
- Add property: `WrappingRange` smart constructor succeeds when `from > to`.
- Add property: `WrappingRange` smart constructor throws when `from <= to`.
- Reuse the existing `outOfOrder` generator from `Generators.scala` to supply `(hi, lo)` pairs.

### `Generators.scala`
Add a `wrappingRangeGen` to `termArbitrary` (generate only when the domain has at least 2
distinct values; use `outOfOrder` to produce a valid `(hi, lo)` pair):

```scala
val wrappingRangeGen: Gen[Term[A]] = outOfOrder[A].map { case (hi, lo) =>
  Term.WrappingRange(hi, lo)
}
```

Include it in the `Gen.frequency` mix alongside `singleGen` and `rangeGen`.

### `RenderTest.scala`
- Replace the existing `Range(Friday, Sunday)` test with `WrappingRange(Friday, Sunday)` —
  expected output `5-6,0` is unchanged.
- Add cases for `WrappingRange(Saturday, Sunday)` → `6-7` and
  `WrappingRange(Saturday, Monday)` → `6-7,1`.

### Files to verify (no expected changes)
- `ScheduleTest.scala` — Vixie output strings (`"0 9 * * 1-5"`, `"0 8 * * 6,0"`, etc.) remain
  correct because ordinals now equal Vixie numbers.
- `NormalizationTest.scala` — tests use Minute/Hour, not DayOfWeek ranges; should be unaffected.
- `HumanReadableTest.scala` — pattern-matches on enum cases, not ordinals; should be unaffected.
- `aliases.scala` — `Weekdays = Field.range(Monday, Friday)` (Mon.ord=1, Fri.ord=5, valid ✓);
  `Weekends = Field.of(Saturday, Sunday)` (two singles, ✓).

## Verification

```
sbt test           # all unit + property tests
sbt ci             # scalafmt + tpolecat strict mode (catches warnings-as-errors)
```

Key assertions to confirm manually:
- `Term.Range(Sunday, Monday)` constructs without throwing (Sun.ord=0 ≤ Mon.ord=1 ✓)
- `Term.WrappingRange(Saturday, Sunday)` constructs without throwing (Sat.ord=6 > Sun.ord=0 ✓)
- `Term.WrappingRange(Sunday, Saturday)` throws (Sun.ord=0 < Sat.ord=6, fails `from > to`)
- `Schedule.weekdays.at(9.h).toCron == "0 9 * * 1-5"` still holds
- `Schedule.weekends.at(8.h).toCron == "0 8 * * 6,0"` still holds
