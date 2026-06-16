package cronh.domain

import cronh.domain.FieldState.Unset

/** A POSIX cron expression with five fields.
  *
  * The five phantom type parameters track DSL state and carry no runtime
  * representation: one [[FieldState]] per field, recording whether that field
  * has been constrained by the DSL ([[FieldState.Set]]) or is still at its
  * default ([[FieldState.Unset]]). The order mirrors the constructor (cron
  * order): minute, hour, day-of-month, month, day-of-week.
  *
  * The tags let the DSL reject — at compile time — a second write to an
  * already-constrained field (`.at(9.h).at(14.h)`, `.in(June).in(July)`), keep
  * `.on`/`.onThe` mutually exclusive (each requires both day fields `Unset`,
  * DESIGN.md §4.5), and enforce coarse→fine ordering (each verb requires the
  * strictly-finer fields `Unset`). They do *not* gate rendering: every
  * `CronExpression` is a valid cron with a `.toCron` — an unset field simply
  * renders its default (`0` for time, `*` for dates).
  *
  * All parameters are covariant so that a directly-constructed expression
  * (whose phantoms infer to `Nothing`) conforms to every state and remains
  * usable with the DSL and the renderers. Equality ignores the phantoms: two
  * expressions with the same five fields are equal.
  */
final case class CronExpression[
    +Min <: FieldState,
    +Hr <: FieldState,
    +Dom <: FieldState,
    +Mon <: FieldState,
    +Dow <: FieldState
](
    minute: Field[Minute],
    hour: Field[Hour],
    dayOfMonth: Field[MonthDay],
    month: Field[Month],
    dayOfWeek: Field[DayOfWeek]
) {

  /** Reassigns the phantom flags without copying.
    *
    * The phantoms have no runtime representation, so the cast is safe by
    * construction. Concentrated here so DSL extensions don't each need their
    * own cast (DESIGN.md §2.10).
    */
  private[cronh] def retag[
      A <: FieldState,
      B <: FieldState,
      C <: FieldState,
      D <: FieldState,
      E <: FieldState
  ]: CronExpression[A, B, C, D, E] =
    this.asInstanceOf[CronExpression[A, B, C, D, E]]
}

/** A cron expression with nothing set yet: the natural starting point for the
  * DSL (`Schedule.daily`, `Schedule.in(June)`, ...). Renders `0 0 * * *`.
  */
type FreshCron = CronExpression[Unset, Unset, Unset, Unset, Unset]
