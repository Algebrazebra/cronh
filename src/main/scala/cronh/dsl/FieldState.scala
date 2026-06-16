package cronh.dsl

/** Phantom marker tracking whether one field of a [[Schedule]] has been
  * constrained by the DSL.
  *
  * Never instantiated; exists only at the type level, and only in the DSL — the
  * domain [[cronh.domain.CronExpression]] is plain data and knows nothing about
  * it (DESIGN.md §2.1, "data first; DSL on top"). Each of the five fields of a
  * [[Schedule]] carries its own `FieldState`, so a single uniform rule — "a
  * field may be set only while its tag is [[Unset]]" — gives every guarantee
  * the builder needs:
  *
  *   - a second write to the same field (`.at(9.h).at(14.h)`,
  *     `.in(June).in(July)`) is a compile error rather than a silent overwrite;
  *   - day-of-week (`.on`) and day-of-month (`.onThe`) stay mutually exclusive,
  *     each requiring both day fields still `Unset` (DESIGN.md §4.5);
  *   - coarse→fine ordering is enforced by each verb additionally requiring the
  *     strictly-finer fields still `Unset`.
  *
  * The markers are nested in the companion to avoid shadowing
  * `scala.collection.immutable.Set` (DESIGN.md §2.16).
  */
sealed trait FieldState

object FieldState {

  /** The field has been explicitly chosen by the DSL (e.g. via `.at`, `.in`,
    * `.on`, `.onThe`, `.between`, `.everyHour`, or `.everyMinute`).
    */
  sealed trait Set extends FieldState

  /** The field is still at its default and may be chosen. Note that the default
    * is *asymmetric* (DESIGN.md): an unset *time* field (minute, hour) renders
    * `0`, while an unset *date* field (day-of-month, month, day-of-week)
    * renders `*`. `Unset` records "not chosen by the DSL", which is what gates
    * further refinement; it does not by itself mean `*`.
    */
  sealed trait Unset extends FieldState
}
