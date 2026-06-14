# cronh — Design & Progress

A living document for the `cronh` Scala 3 cron DSL. Captures the locked-in design decisions, the rejected alternatives that shaped them, the current type model, and where each implementation phase stands.

> **Status snapshot (2026-06-11):** Phases 0–8 are complete: domain model, Unix renderer, fluent DSL, phantom types, property tests, normalization, human-readable output, and polish (README, examples, compile-time literals). Phase 5b (parser round-trip) and everything under [Future / out of v1](#future--out-of-v1) remain deferred. Implementation drifts from the original plan are noted under [Drift from the plan](#5-drift-from-the-plan). §4 records how every known cron edge case is dispatched by the model.

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

- **Day-of-month vs day-of-week.** The POSIX/Vixie baseline ORs the two when both are non-`*` (the schedule fires if *either* matches). The library adopts Vixie OR as its baseline semantics. The Quartz dialect (future) adds `?` ("no specific value") so a user can mark one of the two fields as irrelevant, expressing the AND-style "match only the other field." Encoding this in the model would burn one dialect's convention into the type (cf. §2.11). Note the *DSL* goes further than the model here: its `.on`/`.onDay` exclusivity refuses to even construct the both-restricted case, so the fluent API is a strict, OR-free subset of what the model can represent (§4.5, §7 Q7).
- **Never-fire combinations.** `0 0 30 2 *` (February 30th) is valid in every field yet can never match. Detecting it requires cross-field analysis and is scoped as a future phase, not part of the core model (see §4.6).

- **Rejected:** Baking OR/AND or impossible-date rejection into `CronExpression`'s constructor. It couples the unit-agnostic container to dialect-specific run semantics and to a calendar model the core deliberately omits.

### 2.16 Phantom markers are nested; phantom parameters are covariant

The Phase 4 markers live inside companion-style objects (`Status.Set`, `Status.Unset`, `DaySpec.NoDay`, `DaySpec.ByWeekday`, `DaySpec.ByMonthDay`), and `CronExpression[+Time, +Day]` is covariant in both phantoms.

- **Rejected:** top-level `Set`/`Unset` traits (the plan's spelling) — a top-level `Set` in `cronh.domain` shadows `scala.collection.immutable.Set` for every file in the package.
- **Rejected:** invariant phantoms. A directly constructed `CronExpression(...)` infers its phantoms to `Nothing`; with invariance such a value would match *no* DSL extension and need explicit `FreshCron` ascriptions everywhere. With covariance `Nothing` conforms to every state, so raw domain values remain fully DSL-usable, while `Set`/`Unset` (siblings) still exclude each other.

### 2.17 Unix dialect splits ranges that end on Sunday

The model orders weekdays Monday-first, so `Range(Friday, Sunday)` is valid; under Sun=0 numbering it would naively render as the inverted — and invalid — `5-0`. `UnixCronDialect` renders such ranges as the semantically identical POSIX-valid list `5-6,0` via an overridable `renderDayOfWeek` hook on `CronDialect`.

- **Rejected:** rendering Sunday as `7` in range ends (Vixie accepts it, POSIX does not; it also makes one value render two ways).
- **Rejected:** forbidding Sunday-terminated ranges in the model — they are semantically meaningful; the limitation is the dialect's, so the fix lives in the dialect (cf. §2.11).

### 2.18 The Unix dialect is the default given

`CronDialect`'s companion provides `given default: CronDialect = UnixCronDialect`, so `expr.toCron` works out of the box; a local given shadows it to select another dialect.

- **Rejected:** requiring an explicit dialect import for every render. POSIX/Vixie is the library's declared baseline (§2.12); making the baseline ambient costs nothing and keeps quick-start friction near zero.

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

// === CronExpression (phantoms track DSL state; no runtime cost) ===
final case class CronExpression[+Time <: Status, +Day <: DaySpec](
  minute:     Field[Minute],
  hour:       Field[Hour],
  dayOfMonth: Field[MonthDay],
  month:      Field[Month],
  dayOfWeek:  Field[DayOfWeek]
)
type FreshCron = CronExpression[Status.Unset, DaySpec.NoDay]

// === Normalization (cronh.domain, opt-in) ===
trait DomainBounds[A] { def domain: IndexedSeq[A] }   // full ascending domain
extension [A](field: Field[A])
  def normalized(using Ordering[A], DomainBounds[A]): Field[A]
  def isNormalized(using Ordering[A], DomainBounds[A]): Boolean

// === Rendering (cronh.render) ===
trait Render[A] { def render(value: A): String }      // per-unit token
def renderTerm[A](term: Term[A])(using Render[A]): String
def renderField[A](field: Field[A])(using Render[A]): String
trait CronDialect {                                   // owns Render[DayOfWeek]
  given dayOfWeekRender: Render[DayOfWeek]
  final def render(expression: CronExpression[?, ?]): String
}
object UnixCronDialect extends CronDialect            // Sun=0; default given
extension (e: CronExpression[?, ?])
  def toCron(using CronDialect): String
  def humanReadable: String

// === DSL (cronh.dsl) ===
object Schedule  // daily, hourly, weekdays, weekends, monthly, yearly, on, onDay
// extensions: .at / .on / .onDay / .between / .in, phantom-constrained
// literals: 9.h, 30.m, 15.dom (compile-time validated)
// aliases: Mon..Sun, midnight, noon, Weekdays, Weekends
```

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

**DSL vs model — the OR case is unbuildable through the fluent API.** The `DaySpec` phantom makes `.on` (day-of-week) and `.onDay` (day-of-month) mutually exclusive: whichever fires first retags the expression away from `DaySpec.NoDay`, removing the other from scope (a compile error). So every `CronExpression` the *DSL* builds has **at most one** day field restricted — it can never construct the both-restricted OR case, and therefore never emits a schedule whose Vixie meaning diverges from the naive AND reading. This holds regardless of field *width*: `.on(Mon…Sun)` (a full-domain weekday set) still renders `* * * * 0-6` with day-of-month `*`, so only one field is restricted and the OR rule never triggers. The `DaySpec` tag records *which lever set the day*, not how much it narrows, so a full-domain selection is a valid choice that correctly tags `ByWeekday` — not a bug.

The *model*, by contrast, is a superset: five independent fields can hold both-restricted (e.g. a future parser reading `30 4 1,15 * 5`), and both renderers already interpret that — `humanReadable` reads it as "X or Y" (Phase 7), and the OR meaning lives in interpretation, not construction, exactly where §2.15 puts it.

**Limitation (tracked, §7 Q7).** The DSL therefore *cannot* express the legitimate "1st of the month **or** every Friday" pattern at all — a deliberate trade of expressiveness for never emitting the OR footgun. Whether to add an escape hatch, and how the exclusivity should behave under future dialects that don't OR the two fields (e.g. Quartz's `?`, where the natural reading is AND), is an open question.

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
| Package layout | `cron.domain` + `cron.model` + `cron.dialect` + `cron.dsl` + `cron.render` | Three packages: `cronh.domain`, `cronh.render` (dialects folded in), `cronh.dsl` | **Resolved at Phase 2/3.** Five packages was over-granular; dialect *is* rendering, and `model` never earned a separate namespace. |
| Range validation site | Plan put it on `Field.range`. | Lives on `Term.Range.apply` *and* therefore implicitly on `Field.range`. | **Better than the plan.** Catches `Term.Range(5, 2)` at the raw level too. Keep. |
| `Field.from` | Latest plan iteration suggested dropping in favor of `++`. | `Field.from` is implemented and tested. | Worth keeping for mixed-term construction without three-`++`-chains. Light surface area; revisit if it becomes redundant in practice. |
| Validation throw style | Plan used `require(...)`. | Standardized on `require` across `Minute`, `Hour`, `MonthDay`, `Range`. | **Resolved.** |
| `Minute(60)` test | Plan example. | 60/24/0/32 boundary tests exist (PR #7). | **Resolved.** |
| Phantom marker names | Top-level `Set`, `Unset`, `NoDay`, ... | Nested: `Status.Set`, `DaySpec.NoDay`, ... | Deliberate — top-level `Set` shadows `scala.Set` (§2.16). |
| Phantom variance | Unspecified (implicitly invariant). | `CronExpression[+Time, +Day]` covariant. | Deliberate — keeps directly constructed values DSL-usable (§2.16). |
| `monthly`/`yearly` day phantom | Unspecified. | Typed `DaySpec.ByMonthDay`: their day *is* their meaning, so `.on`/`.onDay` are conflicts; use `Schedule.onDay(...)` for other dates. | Consistent with the "no silent overwrite" rule; only `daily`'s *time* stays `Unset` per the Phase 3 decision. |
| Normalization strength | "merges overlapping ranges" | Also merges *adjacent* runs (`1-3,4-6` → `1-6`) via full-domain enumeration (`DomainBounds`). | **Stronger than the plan**, still semantics-preserving. Keep. |
| `.at` minute-only overload | Phase 4 sketch mentioned `.at(0.m)` after `between`. | Implemented; needs `@targetName("atMinute")` because `Hour` and `Minute` both erase to `Int`. | Erasure tax on opaque types; invisible to users. |

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
- ✅ Decide: keep `Field.from` — kept; mixed-term construction without `++` chains earns its surface area (see §7 Q1)

### Phase 2 — Unix dialect renderer ✅

- ✅ `Render[A]` typeclass with instances for `Minute`, `Hour`, `MonthDay`, `Month`
- ✅ `renderTerm[A]` and `renderField[A]` parameterized over `Render[A]`
- ✅ `CronDialect` trait that *provides* `Render[DayOfWeek]` (dialect-specific numbering) and renders the full expression
- ✅ `UnixCronDialect` (Sun=0, Mon=1, …, Sat=6; ranges ending on Sunday split into POSIX-valid lists, §2.17)
- ✅ Extension: `CronExpression.toCron(using d: CronDialect): String` (Unix is the default given, §2.18)
- ✅ Acceptance tests on hand-built values (`RenderTest`)

Resolved: `Render[_]` instances live in `cronh.render` (in the `Render` companion, so they're in implicit scope without imports).

### Phase 3 — Fluent DSL ✅

- ✅ `Int` extensions: `.h`, `.m`, `.dom` (implemented directly as compile-time-validated inline defs, see Phase 8)
- ✅ Convenience values: `midnight`, `noon`, `Weekdays`, `Weekends`, short DayOfWeek aliases (`Mon`, `Tue`, …)
- ✅ `Schedule` entry points: `daily`, `hourly`, `weekdays`, `weekends`, `monthly`, `yearly`, `on(...)`, `onDay(...)`
- ✅ Extensions on `CronExpression`: `.at`, `.on`, `.onDay`, `.between`, `.in`

Decision locked as planned: `Schedule.daily` is `Time = Unset` with default minute/hour = 0 — "every day, sensible default time", overridable with `.at`. (`monthly`/`yearly` fix their *day* as `ByMonthDay`; see §5.)

### Phase 4 — Phantom types ✅

- ✅ Marker traits: `Status` (`Status.Set`, `Status.Unset`), `DaySpec` (`DaySpec.NoDay`, `DaySpec.ByWeekday`, `DaySpec.ByMonthDay`) — nested, §2.16
- ✅ Parameterize `CronExpression[+Time <: Status, +Day <: DaySpec]`
- ✅ Type alias `FreshCron = CronExpression[Status.Unset, DaySpec.NoDay]`
- ✅ Re-type each extension with phantom constraints
- ✅ `compileErrors(...)` tests for double-set and DoW/DoM conflict (`PhantomTest`, with a positive control verifying the snippets see the right scope)
- ✅ `between` does not flip `Time` (constrains hour, leaves minute settable via `.at(30.m)` — minute-only `.at` overload)

### Phase 5 — Property-based testing ✅ (5b deferred)

- ✅ ScalaCheck generators for `Minute`, `Hour`, `MonthDay`, `Month`, `DayOfWeek`
- ✅ Generators for `Term[A]` and `Field[A]`
- ✅ Properties: render determinism, five-field output structure, `Field.all → "*"`, range bounds preserved
- ✅ Semigroup associativity: `(f1 ++ f2) ++ f3 == f1 ++ (f2 ++ f3)`
- ➖ Phase 5b: cron-string parser + round-trip `parse(c.toCron) == Right(c)` (deferred — significant work, defer past v1)

### Phase 6 — Field normalization ✅

- ✅ `DomainBounds[A]` typeclass (full ascending domain enumeration; domains are ≤ 60 values, so this beats a successor/predecessor API)
- ✅ `Field[A].normalized(using Ordering[A], DomainBounds[A])` removes duplicates, merges overlapping *and adjacent* ranges, collapses to `Field.all` when total
- ✅ Collapse any field containing `Term.All` to `Field.all` (Vixie OR semantics; see §4.4)
- ✅ `Field[A].isNormalized` predicate
- ✅ Idempotence + semantic-equivalence tests (`NormalizationTest`)

### Phase 7 — Human-readable output ✅

- ✅ `CronExpression.humanReadable: String` (in `cronh.render`; dialect-independent)
- ✅ Pattern-match each field, build natural-language phrases; both day fields set reads as "X or Y" (Vixie OR, §4.5)
- ✅ Special cases: `Field.range(Mon, Fri) → "weekdays"`, `Field.of(Sat, Sun) → "weekends"`
- ✅ Time formatting: 12h with AM/PM by default; a 24h option remains open for a later pass

### Phase 8 — Polish ✅

- ✅ Scaladoc with examples on every public symbol
- ✅ README rewrite: before/after comparison, quick-start, DSL reference table
- ✅ `examples/` directory with realistic schedules (illustrative; mirrored by acceptance tests so the expected strings stay honest)
- ✅ Compile-time literal validation: `inline def h: Hour` with `compiletime.error` for out-of-range literals (plus `requireConst` so non-literals fail with a clear message)

### Future / out of v1 ➖

- Vixie extension: `Step[A](base: Term[A], n: Int)` as a `sealed trait VixieTerm[+A] extends Term[A]`. DSL: `.every(15, Minutes)`. Likely promoted ahead of Quartz because most Unix cron implementations actually accept it.
- Quartz dialect: 6-field cron, `L`/`W`/`#`/`?`, dialect-specific term subtypes for `MonthDay` and `DayOfWeek`.
- Parser: `String => Either[ParseError, CronExpression]`.
- Never-fire detection: cross-field analysis flagging impossible dates (`30 2` February 30th, `31` in 30-day months, leap-only `29 2`). Syntactically valid, unambiguous, and silently never executes — the highest-value diagnostic. Deferred because it needs a calendar model the core omits (see §4.6).
- `cronh-cats` module: `Semigroup[Field[A]]`, `Eq`, `Show`.

---

## 7. Open Questions

Tracked here so they're not lost between sessions.

1. ~~**Should `Field.from` survive?**~~ **Resolved: kept.** The DSL extensions ended up using `Field.of`/`Field.range`/`Field.single` internally, but `Field.from` remains the only one-call way to build a mixed-shape field (and the test generators lean on it). Revisit only if it rots.
2. ~~**Package layout when Phase 2/3 land.**~~ **Resolved: split into `cronh.{domain, render, dsl}`.** Dialects folded into `render`; no separate `model` package. See §5.
3. **Phantom-type API ergonomics.** Current failure mode is a missing-extension error ("value at is not a member of CronExpression[Status.Set, ...]"), which names the offending state but doesn't say *why*. Acceptable for now; revisit with `@implicitNotFound`-style helpers or `compiletime.error` shims if users stumble.
4. ~~**`between` semantics.**~~ **Resolved as planned:** `.between(9.h, 17.h)` sets the hour range and does not flip `Time`; a minute-only `.at(30.m)` overload (via `@targetName`, see §5) provides the follow-on. The stricter "must call `.at` before `.toCron`" alternative was dropped — a renderable `0 9-17 * * 1-5` is a perfectly meaningful schedule on its own.
5. ~~**Validation style.**~~ **Resolved: standardized on `require`** across all smart constructors.
6. **24-hour time formatting for `humanReadable`.** Currently 12h AM/PM only. Add a config knob (or a locale-aware formatter) if demand exists.
7. **DSL cannot express the day-of-month-OR-day-of-week pattern.** The `.on`/`.onDay` exclusivity (§4.5) makes the fluent API a strict subset of Vixie: it builds only schedules where at most one day field is restricted, so the OR case (`30 4 1,15 * 5` = "1st and 15th *or* every Friday") is unbuildable. Conservative and correct under Vixie OR, but two things to revisit: (a) whether to offer an opt-in escape hatch — a combined `.onDayOrWeekday(...)` builder, or a relaxation of the `DaySpec` phantom — for users who genuinely want OR; (b) how the exclusivity should interact with **dialects that don't OR the two fields**. Under Quartz, `?` marks one day field irrelevant and the default reading is AND, where setting both fields is meaningful and unsurprising — so the exclusivity that protects users under Vixie may be unnecessary, or differently-motivated, there. Whether such dialects even land (and whether the model should carry a per-dialect "day-combination semantics" notion) is itself open. Both depend on run semantics that §2.15 deliberately keeps out of the model, so this stays open until the dialect/scheduler layer exists.

---

## 8. How to use this document

- When starting work on a phase: read its section here, then the corresponding section in `old_convo.txt` for full rationale if needed.
- When making a non-trivial design decision: add it to §2 with a one-line rejected alternative.
- When a phase progresses: flip its checkboxes in §5. If something surprised you, add it to §6.
- When the implementation diverges from the plan: capture it in §4 with a verdict (keep / fix / monitor).

The plan in `old_convo.txt` is **historical**; this document is **current**. Where they disagree, this document wins.
