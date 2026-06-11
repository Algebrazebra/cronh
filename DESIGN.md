# cronh — Design & Progress

A living document for the `cronh` Scala 3 cron DSL. Captures the locked-in design decisions, the rejected alternatives that shaped them, the current type model, and where each implementation phase stands.

> **Status snapshot (2026-06-11):** Phase 1 (domain model) is largely complete. Phases 2–8 are not yet started. The implementation matches the final plan with a few small drifts noted under [Drift from the plan](#5-drift-from-the-plan). §4 records how every known cron edge case is dispatched by the model.

---

## 1. Vision

Cron strings (`0 30 9 * * MON-FRI`) are write-only. The goal of `cronh` is a Scala 3 library where a schedule is described in code that **reads aloud naturally** and is **caught wrong at compile time** when it would otherwise be a 3 AM page.

The target surface:

```scala
Schedule.daily.at(14.h, 30.m)              // "30 14 * * *"
Schedule.weekdays.at(9.h)                  // "0 9 * * 1-5"
Schedule.on(Mon, Fri).at(noon)             // "0 12 * * 1,5"
Schedule.weekdays.between(9.h, 17.h)       // "0 9-17 * * 1-5"
```

The DSL is the *surface*. Underneath sits a strictly-typed ADT, a dialect-aware renderer, and phantom-type machinery that prevents silent overwrites like `.at(9.h).at(14.h)`.

Non-goals for v1: Vixie steps (`*/n`), Quartz extensions (`L`, `W`, `#`), parsing cron strings back into the model, Cats integration.

---

## 2. Design Principles (Locked In)

Each principle is stated as a rule, with the considered alternative that was rejected.

### 2.1 Data first; DSL on top

The ADT (`Term`, `Field`, `CronExpression`) models cron faithfully, independent of any fluent syntax. The DSL is sugar that produces ADT values.

- **Rejected:** Builder-style API (`Cron.withMinutes(0).withHours(9)`). Even immutable `withX` chains are still builders — they couple "configuration" to "object identity" and can't be composed from a `List[Modifier]`.

### 2.2 Domain types per cron position

Each position has its own validated type: `Minute` (0–59), `Hour` (0–23), `MonthDay` (1–31), plus enums `Month` and `DayOfWeek`. They're Scala 3 `opaque type`s over `Int`, validated in the smart constructor.

- **Rejected:** A single generic `Int`-valued `CronField`. Lets nonsense like `Cron(minute = Exact(70))` type-check. Pushes validation responsibility into the renderer, which then needs to know domains.

### 2.3 Wildcard is a value, not a shape

`Term.All` sits *alongside* `Single` and `Range`, not as a parallel alternative to the "real" cases. A wildcard inside a list (`*,5` in cron) is then naturally `Field(All :: Single(5) :: Nil)` — same machinery as any other term. This is faithful to the Vixie baseline, where a field is the bitwise OR of its elements, so any field containing `*` already denotes every value. Such a field is therefore *redundant*, not illegal — `*,5` is accepted and collapses to `Field.all` under Phase 6 normalization (see §4.4).

- **Rejected:** `Wildcard` as a separate top-level alternative to `Exact`/`Range`/`Step`. Doesn't compose inside lists — you can't put a wildcard "inside" a `Many` if `Many: List[Int]`.

### 2.4 No `OneOf` term — composition lives on `Field`

A cron list (`1,5-10,15`) is a non-empty list of terms. That non-emptiness invariant lives in `Field`, not in a `Term.OneOf(::[A])` case. Otherwise there are two ways to write the same value (`Field(OneOf(1,3))` vs `Field(One(1) :: One(3))`) and consumers must normalize between them.

- **Rejected:** `Term.OneOf(values: ::[A])`. Creates a normalization problem; strictly less expressive than `Field` (can't hold mixed shapes).
- **Considered ergonomic counter:** Wrapping every value in `Term.Single` is friction. Resolved by smart constructors at the `Field` level (`Field.of(1, 3, 5)`).

### 2.5 Term names describe structure, not precision

`Term.All`, `Term.Single`, `Term.Range`. "Exact" was rejected because it falsely implied other shapes were "approximate" — `Range(9, 17)` is just as precise as `Single(9)`, it just specifies more values.

- **Rejected alternatives:** `Every`/`One`/`Range` (`Every` collides with the future `.every(15, Minutes)` step modifier); `All`/`Point`/`Span` (`Point` is overly abstract); `Star`/`Number`/`Dash` (ties the model to cron syntax).
- **Tradeoff:** `Term.Range` shadows `scala.Range`; namespaced inside `Term`, only collides under unusual import styles.

### 2.6 Composition operator lives on `Field`, not `Term`

`Term[A] ++ Term[A]` doesn't naturally close — two terms ORed together are a *list of terms*, which is exactly what `Field` is. Putting `++` on `Term` would require either reintroducing `OneOf` or returning `Field` from a `Term` method (asymmetric).

- **Decision:** `++` on `Field`; users wrap with the factory methods before composing.

### 2.7 Non-emptiness in the types; redundancy in functions

`Field.terms: ::[Term[A]]` makes empty fields unrepresentable. Redundancy (duplicates, subsumed ranges, overlapping intervals) is **accepted as valid input** — it's syntactically and semantically legal cron — and exposed as an explicit `normalized` operation (Phase 6).

- **Rejected:** `Field.terms: Set[Term[A]]`. Only deduplicates exact equality (misses subsumption), loses insertion order (breaks rendering predictability), requires a hand-rolled `NonEmptySet`, and conflates structural with semantic invariants.

### 2.8 `require` only for real invariants

Backwards ranges (`Range(5, 2)`), domain violations (`Minute(99)`), and empty fields are rejected at construction. Redundancy is *not*.

### 2.9 Fluent syntax via extension methods on `CronExpression`

`a.b.c` is achieved by extension methods on the domain type — every intermediate `.b` returns a real `CronExpression` value, composable and storable.

- **Rejected:** Intermediate builder types (`At("14:30").on(Monday, Friday)`). Each builder type exists only to host the next method; you can't store `At("14:30")` and later attach days conditionally without diving into the builder's internals.

### 2.10 Phantom types track what's been set

`CronExpression[Time <: Status, Day <: DaySpec]` carries type-level flags. `.at` is only callable when `Time = Unset`, returns `Time = Set`. Same for `.on` / `.onDay` with `Day`.

- **Why this matters:** Without phantoms, `Schedule.daily.at(9.h).at(14.h)` silently overwrites, and `Schedule.daily.every(15, Minutes).at(9.h)` destroys the step. With phantoms, both are compile errors.
- **Rejected alternative:** Document "last write wins" and accept the footgun. For a domain that gets paged on at 3 AM, the type cost is worth it.
- **Implementation tradeoff:** Phantom flag changes use a `private[cronh]` cast helper to avoid `copy` re-typing every call. Concentrated in one place, safe by construction.

### 2.11 Rendering is dialect-bound

`DayOfWeek` has no `value` field — its numeric encoding differs across dialects (Unix: Sun=0; Quartz: Sun=1). The `Render[DayOfWeek]` instance lives inside the dialect, not on the enum.

- **Rejected:** `DayOfWeek.toCron: Int` on the enum. Burns one dialect into the type; any second dialect requires a workaround.

### 2.12 POSIX baseline; dialects extend later

The core `Term[+A]` is POSIX-only (`All`, `Single`, `Range`). Steps (`*/n`) are a Vixie extension — added later as `sealed trait VixieTerm[+A] extends Term[A]`. Quartz adds domain-specific subtypes for `MonthDay` and `DayOfWeek`.

### 2.13 No external dependencies in core

Standard library only. `Field` is a semigroup under `++`, but `cats.Semigroup` is *not* depended on in v1.

- **Rejected:** Cats integration in core. Brings ~3MB and `|+|` jargon to users who don't need it. Revisit as a separate `cronh-cats` module if demand exists.

### 2.14 Scala 3 only

`opaque type`, `extension`, `enum`, and `given` are used freely. Cross-building to 2.13 was considered and rejected: cost ≈ doubles MVP effort, the 2.13 surface would be substantially noisier (implicit classes × phantom-type constraints), and no concrete 2.13 consumer exists.

### 2.15 Cross-field semantics live in the dialect, not the model

`CronExpression` stores five independent fields and attaches no meaning to their *combination*. Two combination questions are explicitly pushed to the renderer/dialect layer:

- **Day-of-month vs day-of-week.** The POSIX/Vixie baseline ORs the two when both are non-`*` (the schedule fires if *either* matches). The library adopts Vixie OR as its baseline semantics. The Quartz dialect (future) adds `?` ("no specific value") so a user can mark one of the two fields as irrelevant, expressing the AND-style "match only the other field." Encoding this in the model would burn one dialect's convention into the type (cf. §2.11).
- **Never-fire combinations.** `0 0 30 2 *` (February 30th) is valid in every field yet can never match. Detecting it requires cross-field analysis and is scoped as a future phase, not part of the core model (see §4.6).

- **Rejected:** Baking OR/AND or impossible-date rejection into `CronExpression`'s constructor. It couples the unit-agnostic container to dialect-specific run semantics and to a calendar model the core deliberately omits.

---

## 3. Current Type Model

```scala
// === Domain types (cronh.domain) ===
opaque type Minute   = Int   // 0–59, validated
opaque type Hour     = Int   // 0–23, validated
opaque type MonthDay = Int   // 1–31, validated

enum Month(val value: Int):
  case January extends Month(1)
  // ... December extends Month(12)

enum DayOfWeek:
  case Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday

// Orderings provided for all five.

// === Term: three variants ===
sealed trait Term[+A]
object Term:
  case object All                                extends Term[Nothing]
  final case class Single[+A](value: A)          extends Term[A]
  final case class Range[+A] private (from: A, to: A) extends Term[A]
  object Range:
    def apply[A: Ordering](from: A, to: A): Range[A]  // validates from <= to

// === Field: non-empty list of terms ===
final case class Field[+A] private (terms: ::[Term[A]]):
  def ++[B >: A](other: Field[B]): Field[B]

object Field:
  val all: Field[Nothing]
  def single[A](value: A): Field[A]
  def range[A: Ordering](from: A, to: A): Field[A]
  def of[A](first: A, rest: A*): Field[A]
  def from[A](first: Term[A], rest: Term[A]*): Field[A]   // mixed-term composition

// === CronExpression ===
final case class CronExpression(
  minute:     Field[Minute],
  hour:       Field[Hour],
  dayOfMonth: Field[MonthDay],
  month:      Field[Month],
  dayOfWeek:  Field[DayOfWeek]
)
```

Phantom-type parameters (Phase 4) will turn this into `CronExpression[Time <: Status, Day <: DaySpec]`.

---

## 4. Edge-Case Handling

Cron has no ratified specification. POSIX defines the five-field structure but not parser behavior for invalid or ambiguous input; the closest reference is **Vixie cron (1988)**, the de facto baseline this library targets (§2.12). Every case below is therefore an explicit design decision, not a deviation from a standard.

The model dispatches each known edge case through one of five strategies:

| Strategy | Mechanism | Example |
| --- | --- | --- |
| **Unrepresentable** | the type model gives no way to build it | `*-5`, `1,,3` |
| **Rejected at construction** | smart constructor throws `IllegalArgumentException` | `5-3`, `Minute(60)` |
| **Eliminated by design** | the type removes the ambiguity entirely | `0` vs `7` for Sunday |
| **Accepted as redundant** | legal input, preserved as written, canonicalized by Phase 6 `normalized` | `1-5,3-7`, `*,1` |
| **Out of scope / future** | non-goal for v1, tracked for a later phase | `*/n`, `L`/`W`/`#`/`?`, Feb 30 |

These map onto the usual notion of validation tiers: *unrepresentable* + *rejected* are hard errors caught at construction; *accepted as redundant* are the "valid but likely unintentional" cases, surfaced (not blocked) via normalization; never-fire detection is its own future tier (§4.6).

### 4.1 Structurally impossible — unrepresentable

There is no parser in v1; values are built from typed constructors, so whole classes of string-level malformation simply have no representation.

| Construct | Why it can't be built |
| --- | --- |
| `*-5`, `5-*` (wildcard as a range bound) | `Term.Range[A]` holds two `A` values; `All` is `Term[Nothing]`, not an `A`, so it cannot sit inside a range. |
| `1,,3`, `,1`, `1,` (empty list elements) | Fields are composed from `Term` values, not by splitting a string on commas — there is no empty slot to represent. |
| empty field | `Field` wraps `::[Term[A]]` (a non-empty cons cell). |
| `MON-3` (named/numeric mix in a range) | `Range[DayOfWeek]` requires both bounds be `DayOfWeek`; `Range[Month]`, both `Month`. The two spellings can't be mixed in one range. |
| a value in the wrong position (e.g. an hour in the minute field) | `CronExpression` types each field by its unit (§2.2). |

### 4.2 Rejected at construction — throws

| Construct | Guard |
| --- | --- |
| `5-3` and any inverted range | `Term.Range.apply` requires `from <= to`. |
| `60` (minute), `24` (hour), `0` or `32` (day) | per-unit smart constructors validate the domain: `Minute` 0–59, `Hour` 0–23, `MonthDay` 1–31. |
| `13`+ (month), out-of-set weekday | `Month` and `DayOfWeek` are enums — only the twelve / seven cases exist. |

### 4.3 Eliminated by design — ambiguity removed

| Ambiguity | Resolution |
| --- | --- |
| `0` vs `7` for Sunday | `DayOfWeek` is an enum with a single `Sunday`; there is no numeric duality. The dialect renderer picks the encoding (§2.11). |
| `SUN,0`, `0,7` (two spellings of one value) | both denote the same enum case — there is only one way to name Sunday in the model. |
| `jan` vs `JAN` (case sensitivity) | a parser/renderer concern; the model holds `Month.January`, and spelling is the renderer's job. No parser in v1. |

### 4.4 Accepted as redundant — valid, canonicalized by Phase 6

The Vixie baseline treats a field as the bitwise OR of its elements: overlaps, duplicates, and full-range spans are legal and simply denote the union. The model preserves input as written (§2.7) and exposes an explicit `normalized` operation (Phase 6) to canonicalize.

| Construct | Normalized form |
| --- | --- |
| `*,1`, `1,*` (wildcard in a list) | `Field.all`. A field containing `All` already denotes every value (Vixie OR), so it collapses to `*` — accepted, never illegal (§2.3). |
| `1-5,3-7` (overlapping ranges) | merged range `1-7`. |
| `1-5,3` (value inside a range) | `1-5`. |
| `1,1` (duplicate) | `1`. |
| `0-59`, `0-23`, `1-31`, `1-12` (tautological full span) | `Field.all`. |
| `5-5` (degenerate range) | `Single(5)`. `Range.apply` permits `from == to`, so this is constructible and merely redundant. |

Normalization is **opt-in**: unnormalized fields are valid and render faithfully. `normalized` exists for readability and diff/equality comparisons, not as a gate.

### 4.5 Cross-field semantics — day-of-month vs day-of-week

When both day fields are non-`*`, Vixie cron fires if **either** matches (OR); some implementations AND them. The library adopts **Vixie OR** as its baseline (§2.15). The model attaches no semantics to the field combination — OR-vs-AND is a dialect/renderer concern. The Quartz dialect (future) introduces `?` ("no specific value") so a user can explicitly mark one of the two fields as irrelevant, expressing the AND-style "match only the other field."

### 4.6 Never-fire combinations — future phase

`0 0 30 2 *` (February 30th), `31` in a 30-day month, and `29 2` (leap-year-only) are valid in every field yet match no real date. They are the most dangerous class in practice: syntactically valid, semantically unambiguous, no runtime error, and silently never execute.

The current model cannot catch them because the conflict is **cross-field** — `MonthDay(30)` and `Month.February` are each valid in isolation. Cross-field never-fire analysis is scoped as a **future phase** (it needs a calendar model the core deliberately omits), and is the highest-value diagnostic to add (§2.15, [Future / out of v1](#future--out-of-v1)).

### 4.7 Out of scope for v1 — non-goals

| Construct | Disposition |
| --- | --- |
| steps `*/n` and every step edge case (`*/0` div-by-zero, `5/3` start/step, degenerate `*/60`, chained `1-5/2/3`, tautological `*/1`) | Vixie *extension*, added later as `VixieTerm` (§2.12). Not in the v1 `Term`. |
| `L`, `W`, `#`, `?` (Quartz), `H` (Jenkins) | future Quartz dialect. |
| seconds / year field (6- or 7-field cron) | future Quartz dialect; v1 `CronExpression` is strictly five fields. |

Because steps are absent from v1, none of their edge cases can arise — they are listed here only so the omission is explicit rather than accidental.

---

## 5. Drift from the plan

Small, intentional or harmless differences between the final plan in `old_convo.txt` and the current code:

| Topic | Plan | Current code | Verdict |
| --- | --- | --- | --- |
| Package layout | `cron.domain` + `cron.model` + `cron.dialect` + `cron.dsl` + `cron.render` | Flat: `cronh.domain` only | Fine for now; revisit when Phase 2/3 land. Splitting now would be premature. |
| Range validation site | Plan put it on `Field.range`. | Lives on `Term.Range.apply` *and* therefore implicitly on `Field.range`. | **Better than the plan.** Catches `Term.Range(5, 2)` at the raw level too. Keep. |
| `Field.from` | Latest plan iteration suggested dropping in favor of `++`. | `Field.from` is implemented and tested. | Worth keeping for mixed-term construction without three-`++`-chains. Light surface area; revisit if it becomes redundant in practice. |
| Validation throw style | Plan used `require(...)`. | Mixes `require` (Range) with explicit `throw new IllegalArgumentException` (Hour/Minute). | Cosmetic. Standardize on one style when convenient — `require` is shorter. |
| `Minute(60)` test | Plan example. | `Minute(61)` test exists; `Minute(60)` boundary not explicitly covered. | Add the 60/24/32 boundary tests during Phase 1 wrap-up. |

None of these are blockers. Documenting them so future-me doesn't think they're bugs.

---

## 6. Phase Tracker

Status legend: ✅ done · 🟡 in progress · ⬜ not started · ➖ deferred

### Phase 0 — Project setup ✅

- ✅ sbt project, Scala 3.3.7
- ✅ munit + munit-scalacheck on test scope
- ✅ tpolecat options (DevMode locally, CiMode in CI)
- ✅ scalafmt and a custom `ci` task that mirrors GitHub Actions
- ✅ GitHub Actions workflow

### Phase 1 — Domain + Term/Field ✅ (with minor follow-ups)

- ✅ Opaque types: `Minute`, `Hour`, `MonthDay`
- ✅ Enums: `Month` (with `value`), `DayOfWeek` (no value)
- ✅ Orderings for all five domain types
- ✅ `Term[+A]` ADT with `All`, `Single`, `Range`; `Range.apply` enforces `from <= to`
- ✅ `Field[+A]` with `all`, `single`, `range`, `of`, `from`, `++`
- ✅ `CronExpression` (no phantoms yet)
- ✅ Tests for `Minute`, `Hour`, `MonthDay`, `Month`, `DayOfWeek`, `Term`, `Field`, `CronExpression`
- ✅ Boundary tests: `Minute(60)`, `Hour(24)`, `MonthDay(0)` and `MonthDay(32)` failure cases (some may exist — audit)
- ⬜ Decide: keep `Field.from`, or remove once `++` chains feel natural in DSL code

### Phase 2 — Unix dialect renderer ⬜

- ⬜ `Render[A]` typeclass with instances for `Minute`, `Hour`, `MonthDay`, `Month`
- ⬜ `renderTerm[A]` and `renderField[A]` parameterized over `Render[A]`
- ⬜ `CronDialect` trait that *provides* `Render[DayOfWeek]` (dialect-specific numbering) and renders the full expression
- ⬜ `UnixCronDialect` (Sun=0, Mon=1, …, Sat=6)
- ⬜ Extension: `CronExpression.toCron(using d: CronDialect): String`
- ⬜ Acceptance tests on hand-built values (see plan §Phase 2)

Open question: where do `Render[_]` instances live? Plan says `cron.render`; a flat layout might put them next to the domain types. Decide when starting Phase 2.

### Phase 3 — Fluent DSL (no phantoms) ⬜

- ⬜ `Int` extensions: `.h`, `.m`, `.dom`
- ⬜ Convenience values: `midnight`, `noon`, `Weekdays`, `Weekends`, short DayOfWeek aliases (`Mon`, `Tue`, …)
- ⬜ `Schedule` entry points: `daily`, `hourly`, `weekdays`, `weekends`, `monthly`, `yearly`, `on(...)`, `onDay(...)`
- ⬜ Extensions on `CronExpression`: `.at`, `.on`, `.onDay`, `.between`, `.in`

Decision to lock in before coding: does `Schedule.daily` set `Time = Set` (so `.at` is a conflict) or `Time = Unset` with sensible defaults? The plan recommends `Unset` with default minute/hour = 0, treating `daily` as "every day, sensible default time" that the user can still override with `.at`. Carry that into Phase 4.

### Phase 4 — Phantom types ⬜

- ⬜ Marker traits: `Status`, `Set`, `Unset`, `DaySpec`, `NoDay`, `ByWeekday`, `ByMonthDay`
- ⬜ Parameterize `CronExpression[Time <: Status, Day <: DaySpec]`
- ⬜ Type alias `FreshCron = CronExpression[Unset, NoDay]`
- ⬜ Re-type each extension with phantom constraints
- ⬜ `compileErrors(...)` tests for double-set and DoW/DoM conflict
- ⬜ `between` does not flip `Time` (constrains hour, leaves minute settable via `.at(0.m)`)

### Phase 5 — Property-based testing ⬜

- ⬜ ScalaCheck generators for `Minute`, `Hour`, `MonthDay`, `Month`, `DayOfWeek`
- ⬜ Generators for `Term[A]` and `Field[A]`
- ⬜ Properties: render determinism, five-field output structure, `Field.all → "*"`, range bounds preserved
- ⬜ Semigroup associativity: `(f1 ++ f2) ++ f3 == f1 ++ (f2 ++ f3)`
- ➖ Phase 5b: cron-string parser + round-trip `parse(c.toCron) == Right(c)` (deferred — significant work, defer past v1)

### Phase 6 — Field normalization ⬜ (optional)

- ⬜ `DomainBounds[A]` typeclass
- ⬜ `Field[A].normalized(using Ordering[A], DomainBounds[A])` removes duplicates, merges overlapping ranges, collapses to `Field.all` when total
- ⬜ Collapse any field containing `Term.All` to `Field.all` (Vixie OR semantics; see §4.4)
- ⬜ `Field[A].isNormalized` predicate
- ⬜ Idempotence + semantic-equivalence tests

### Phase 7 — Human-readable output ⬜

- ⬜ `CronExpression.humanReadable: String`
- ⬜ Pattern-match each field, build natural-language phrases
- ⬜ Special cases: `Field.range(Mon, Fri) → "weekdays"`, `Field.of(Sat, Sun) → "weekends"`
- ⬜ Time formatting (12h with AM/PM by default; consider 24h option)

### Phase 8 — Polish ⬜

- ⬜ Scaladoc with examples on every public symbol
- ⬜ README rewrite: before/after comparison, quick-start, DSL reference table
- ⬜ `examples/` directory with realistic schedules
- ⬜ Compile-time literal validation: `inline def h: Hour` with `compiletime.error` for out-of-range literals

### Future / out of v1 ➖

- Vixie extension: `Step[A](base: Term[A], n: Int)` as a `sealed trait VixieTerm[+A] extends Term[A]`. DSL: `.every(15, Minutes)`. Likely promoted ahead of Quartz because most Unix cron implementations actually accept it.
- Quartz dialect: 6-field cron, `L`/`W`/`#`/`?`, dialect-specific term subtypes for `MonthDay` and `DayOfWeek`.
- Parser: `String => Either[ParseError, CronExpression]`.
- Never-fire detection: cross-field analysis flagging impossible dates (`30 2` February 30th, `31` in 30-day months, leap-only `29 2`). Syntactically valid, unambiguous, and silently never executes — the highest-value diagnostic. Deferred because it needs a calendar model the core omits (see §4.6).
- `cronh-cats` module: `Semigroup[Field[A]]`, `Eq`, `Show`.

---

## 7. Open Questions

Tracked here so they're not lost between sessions.

1. **Should `Field.from` survive?** Last plan iteration suggested replacing it with `++` chains. Current code keeps it. Revisit after a few real DSL extensions are written and we can see whether `++` is ergonomic enough alone.
2. **Package layout when Phase 2/3 land.** The plan splits into `cronh.{domain, model, dialect, dsl, render}`. Current layout is flat (`cronh.domain`). At which file count is the split warranted? Suggest splitting when the count crosses ~15 source files or when DSL and rendering visibly fight for namespace.
3. **Phantom-type API ergonomics.** When errors fire (`compileErrors(...)`), do messages reasonably guide the user? If not, consider `@implicitNotFound`-style helpers or `scala.compiletime.error` shims.
4. **`between` semantics.** Phase 3 plan: `.between(9.h, 17.h)` sets the hour to `Range(9, 17)` and does *not* flip `Time = Set`. Confirm with a usage example before locking — there may be a cleaner alternative where `.between(...)` returns a value that requires a follow-on `.at(m.m)` before `.toCron` is callable.
5. **Validation style.** `require(...)` vs. `throw new IllegalArgumentException(...)`. Pick one and refactor for consistency. Lean toward `require` (shorter, more idiomatic).

---

## 8. How to use this document

- When starting work on a phase: read its section here, then the corresponding section in `old_convo.txt` for full rationale if needed.
- When making a non-trivial design decision: add it to §2 with a one-line rejected alternative.
- When a phase progresses: flip its checkboxes in §5. If something surprised you, add it to §6.
- When the implementation diverges from the plan: capture it in §4 with a verdict (keep / fix / monitor).

The plan in `old_convo.txt` is **historical**; this document is **current**. Where they disagree, this document wins.
