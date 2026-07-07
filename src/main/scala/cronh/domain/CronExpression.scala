package cronh.domain

import cronh.domain.fieldTypes.{DayOfWeek, Hour, Minute, Month, DayOfMonth}

/** A POSIX cron expression with five fields. */
final case class CronExpression(
    minute: Field[Minute],
    hour: Field[Hour],
    dayOfMonth: Field[DayOfMonth],
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
