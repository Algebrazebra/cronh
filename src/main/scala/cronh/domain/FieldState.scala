package cronh.domain

/** Phantom marker tracking whether one field of a [[CronExpression]] has been
  * constrained by the DSL.
  *
  * Never instantiated; exists only at the type level. Each settable field of a
  * [[CronExpression]] carries its own `FieldState` parameter, so constraining
  * the same field twice (e.g. `.at(9.h).at(14.h)` or `.in(June).in(July)`) is a
  * compile error instead of a silent overwrite, and `.on`/`.onDay` can stay
  * mutually exclusive by each requiring both day fields still [[Unset]].
  *
  * The markers are nested in the companion to avoid shadowing
  * `scala.collection.immutable.Set` (DESIGN.md §2.16).
  */
sealed trait FieldState

object FieldState {

  /** The field has been explicitly constrained by the DSL (e.g. via `.at`,
    * `.between`, `.on`, `.onDay`, or `.in`).
    */
  sealed trait Set extends FieldState

  /** The field is still at its default (`*`-equivalent) and may be set. */
  sealed trait Unset extends FieldState
}
