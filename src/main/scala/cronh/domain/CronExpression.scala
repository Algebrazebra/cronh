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
  * The tags let the DSL reject a second write to an already-constrained field
  * (e.g. `.at(9.h).at(14.h)`, `.between(...).at(10.h)`, `.in(June).in(July)`)
  * at compile time, and let `.on`/`.onDay` stay mutually exclusive (each
  * requires both day fields still `Unset`). Because the both-day-fields-set
  * state is still representable, a future dialect whose day semantics are not
  * OR can relax that precondition without changing this type (DESIGN.md §2.15,
  * §4.5).
  *
  * All parameters are covariant so that a directly constructed expression
  * (whose phantoms infer to `Nothing`) conforms to every state and remains
  * usable with the DSL. Equality ignores the phantoms: two expressions with the
  * same five fields are equal.
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
  * DSL (`Schedule.daily`, `Schedule.hourly`, ...).
  */
type FreshCron = CronExpression[Unset, Unset, Unset, Unset, Unset]
