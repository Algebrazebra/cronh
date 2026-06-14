package cronh.dsl

import cronh.domain.*

/** Entry points for building schedules fluently.
  *
  * Each entry point returns a complete, renderable
  * [[cronh.domain.CronExpression]] with sensible defaults (00:00 unless stated
  * otherwise). Phantom flags track what is still overridable: `daily` leaves
  * the time unset so `.at` may refine it, while `weekdays` fixes the day
  * constraint so a second `.on` is a compile error.
  */
object Schedule {

  private val zeroMinute = Field.single(Minute(0))
  private val zeroHour = Field.single(Hour(0))
  private val firstDay = Field.single(MonthDay(1))

  /** Every day at 00:00 by default; override the time with `.at`. */
  val daily: FreshCron =
    CronExpression(zeroMinute, zeroHour, Field.all, Field.all, Field.all)

  /** Every hour, on the hour, by default. The hour field is already at its
    * widest, so the time is [[Status.HourSet]]: refine only the minute with
    * `.at(30.m)`; `.at(hour)` (which would silently make it once-daily) is a
    * compile error.
    */
  def hourly: CronExpression[Status.HourSet, DaySpec.NoDay] =
    CronExpression(zeroMinute, Field.all, Field.all, Field.all, Field.all)

  /** Monday through Friday at 00:00 by default. */
  def weekdays: CronExpression[Status.Unset, DaySpec.ByWeekday] =
    CronExpression(zeroMinute, zeroHour, Field.all, Field.all, Weekdays.toField)

  /** Saturday and Sunday at 00:00 by default. */
  def weekends: CronExpression[Status.Unset, DaySpec.ByWeekday] =
    CronExpression(zeroMinute, zeroHour, Field.all, Field.all, Weekends.toField)

  /** The first of every month at 00:00 by default. For other days of the month
    * use [[onDay]].
    */
  val monthly: CronExpression[Status.Unset, DaySpec.ByMonthDay] =
    CronExpression(zeroMinute, zeroHour, firstDay, Field.all, Field.all)

  /** Every January 1st at 00:00 by default. */
  val yearly: CronExpression[Status.Unset, DaySpec.ByMonthDay] =
    CronExpression(
      zeroMinute,
      zeroHour,
      firstDay,
      Field.single(Month.January),
      Field.all
    )

  /** These weekdays, at 00:00 by default. */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): CronExpression[Status.Unset, DaySpec.ByWeekday] =
    CronExpression(
      zeroMinute,
      zeroHour,
      Field.all,
      Field.all,
      Field.of(first, rest*)
    )

  /** These days of the month, at 00:00 by default. */
  def onDay(
      first: MonthDay,
      rest: MonthDay*
  ): CronExpression[Status.Unset, DaySpec.ByMonthDay] =
    CronExpression(
      zeroMinute,
      zeroHour,
      Field.of(first, rest*),
      Field.all,
      Field.all
    )
}
