package cronh.domain

/** A POSIX cron expression with five fields.
  *
  * The two phantom type parameters track DSL state and carry no runtime
  * representation:
  *
  *   - `Time` records whether the time of day has been set ([[Status.Set]]) or
  *     is still at its default ([[Status.Unset]]).
  *   - `Day` records whether the schedule is constrained by weekday
  *     ([[DaySpec.ByWeekday]]), by day of month ([[DaySpec.ByMonthDay]]), or
  *     not at all ([[DaySpec.NoDay]]).
  *
  * Both parameters are covariant so that a directly constructed expression
  * (whose phantoms infer to `Nothing`) conforms to every state and remains
  * usable with the DSL. Equality ignores the phantoms: two expressions with the
  * same five fields are equal.
  *
  * '''The phantoms guard the DSL surface, not this data type.''' Because this
  * is a public case class (kept a storable, constructible plain value,
  * DESIGN.md §2.9), the synthesized `apply` and `copy` are raw construction:
  * they set the fields you give them and leave the phantom tags untouched. So
  * `expr.copy(hour = ...)` on a `Status.Set` value yields another `Status.Set`
  * value with a different hour — the flag no longer reflects how the time was
  * established. Treat `copy`/`apply` as a deliberate escape hatch (DESIGN.md
  * §2.10); reach for the `Schedule` DSL when you want the no-silent-overwrite
  * guarantees.
  */
final case class CronExpression[+Time <: Status, +Day <: DaySpec](
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
  private[cronh] def retag[T <: Status, D <: DaySpec]: CronExpression[T, D] =
    this.asInstanceOf[CronExpression[T, D]]
}

/** A cron expression with nothing set yet: the natural starting point for the
  * DSL (`Schedule.daily`, `Schedule.hourly`, ...).
  */
type FreshCron = CronExpression[Status.Unset, DaySpec.NoDay]
