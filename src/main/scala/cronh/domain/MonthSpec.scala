package cronh.domain

/** Phantom marker tracking whether a [[CronExpression]]'s month field has been
  * constrained by the DSL.
  *
  * Never instantiated; exists only at the type level so that constraining the
  * month twice (e.g. `.in(June).in(July)`) is a compile error instead of a
  * silent overwrite, mirroring the guarantee [[Status]] gives the time and
  * [[DaySpec]] gives the day.
  */
sealed trait MonthSpec

object MonthSpec {

  /** The month is still at its default (`*`) and may be constrained with `.in`.
    */
  sealed trait Unset extends MonthSpec

  /** The month has been constrained (e.g. via `.in`); a second `.in` is a
    * compile error.
    */
  sealed trait Set extends MonthSpec
}
