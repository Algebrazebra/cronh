package cronh.dsl

import cronh.domain.*
import cronh.domain.FieldState.{Set, Unset}

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
    * widest, so it is [[FieldState.Set]]: refine only the minute with
    * `.at(30.m)`; `.at(hour)` (which would silently make it once-daily) is a
    * compile error.
    */
  val hourly: CronExpression[Unset, Set, Unset, Unset, Unset] =
    CronExpression(zeroMinute, Field.all, Field.all, Field.all, Field.all)

  /** Monday through Friday at 00:00 by default. */
  def weekdays: CronExpression[Unset, Unset, Unset, Unset, Set] =
    CronExpression(zeroMinute, zeroHour, Field.all, Field.all, Weekdays.toField)

  /** Saturday and Sunday at 00:00 by default. */
  def weekends: CronExpression[Unset, Unset, Unset, Unset, Set] =
    CronExpression(zeroMinute, zeroHour, Field.all, Field.all, Weekends.toField)

  /** The first of every month at 00:00 by default. For other days of the month
    * use [[onDay]].
    */
  val monthly: CronExpression[Unset, Unset, Set, Unset, Unset] =
    CronExpression(zeroMinute, zeroHour, firstDay, Field.all, Field.all)

  /** Every January 1st at 00:00 by default. The month is already constrained,
    * so `.in` is unavailable (it would silently replace January).
    */
  val yearly: CronExpression[Unset, Unset, Set, Set, Unset] =
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
  ): CronExpression[Unset, Unset, Unset, Unset, Set] =
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
  ): CronExpression[Unset, Unset, Set, Unset, Unset] =
    CronExpression(
      zeroMinute,
      zeroHour,
      Field.of(first, rest*),
      Field.all,
      Field.all
    )
}
