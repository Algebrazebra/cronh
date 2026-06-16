package cronh.dsl

import cronh.domain.*
import cronh.domain.FieldState.{Set, Unset}

/** Entry points for building schedules fluently.
  *
  * Every entry builds from one blank slate — `0 0 * * *`, daily at midnight —
  * by applying the refinement verbs in [[ScheduleOps]]. The blank slate encodes
  * the asymmetric defaults (DESIGN.md): an unconstrained *time* field renders
  * `0`, an unconstrained *date* field renders `*`. So `Schedule.in(June)` is
  * `0 0 * 6 *` (midnight daily in June), not `* * * 6 *` — every-minute is an
  * explicit opt-in via [[everyMinute]].
  *
  * `daily`/`hourly`/… are values on this object, not refinements, so there is
  * no `.daily` to chain onto an expression: `Schedule.monthly.daily` does not
  * typecheck.
  */
object Schedule {

  private val zeroMinute = Field.single(Minute(0))
  private val zeroHour = Field.single(Hour(0))

  /** The blank slate every entry builds from: minute `0`, hour `0`, all date
    * fields `*` — i.e. `0 0 * * *`.
    */
  private val blank: FreshCron =
    CronExpression(zeroMinute, zeroHour, Field.all, Field.all, Field.all)

  /** Every day at 00:00 (`0 0 * * *`); the blank slate, fully refinable. */
  val daily: FreshCron = blank

  /** Every hour on the hour (`0 * * * *`); refine the minute with `.at(30.m)`.
    */
  val hourly: CronExpression[Unset, Set, Unset, Unset, Unset] = blank.everyHour

  /** The first of every month at 00:00 (`0 0 1 * *`). */
  val monthly: CronExpression[Unset, Unset, Set, Unset, Unset] =
    blank.onThe(MonthDay(1))

  /** Every January 1st at 00:00 (`0 0 1 1 *`). */
  val yearly: CronExpression[Unset, Unset, Set, Set, Unset] =
    blank.in(Month.January).onThe(MonthDay(1))

  /** Monday through Friday at 00:00 (`0 0 * * 1-5`); refine with `.at`. */
  val weekdays: CronExpression[Unset, Unset, Unset, Unset, Set] =
    blank.on(Weekdays)

  /** Saturday and Sunday at 00:00 (`0 0 * * 6,0`). */
  val weekends: CronExpression[Unset, Unset, Unset, Unset, Set] =
    blank.on(Weekends)

  /** Every minute of every day (`* * * * *`) — the explicit high-frequency
    * opt-in, since unset time renders `0 0`.
    */
  val everyMinute: CronExpression[Set, Set, Unset, Unset, Unset] =
    blank.everyMinute

  /** These months, at 00:00 by default. */
  def in(
      first: Month,
      rest: Month*
  ): CronExpression[Unset, Unset, Unset, Set, Unset] =
    blank.in(first, rest*)

  /** These weekdays, at 00:00 by default. */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): CronExpression[Unset, Unset, Unset, Unset, Set] =
    blank.on(first, rest*)

  /** A prebuilt weekday selection (e.g. `Weekdays`, `Mon to Fri`), at 00:00. */
  def on(
      days: WeekdaySelector
  ): CronExpression[Unset, Unset, Unset, Unset, Set] =
    blank.on(days)

  /** These days of the month, at 00:00 by default. Reads "on the 15th". */
  def onThe(
      first: MonthDay,
      rest: MonthDay*
  ): CronExpression[Unset, Unset, Set, Unset, Unset] =
    blank.onThe(first, rest*)

  /** The inclusive hour range `[from, to]`; refine the minute with `.at(30.m)`.
    */
  def between(
      from: Hour,
      to: Hour
  ): CronExpression[Unset, Set, Unset, Unset, Unset] =
    blank.between(from, to)

  /** At this hour and minute (`m h * * *`). */
  def at(
      hour: Hour,
      minute: Minute
  ): CronExpression[Set, Set, Unset, Unset, Unset] =
    blank.at(hour, minute)

  /** At this hour, on the hour (`0 h * * *`). */
  def at(hour: Hour): CronExpression[Set, Set, Unset, Unset, Unset] =
    blank.at(hour)

  /** At this minute past hour 0 (`m 0 * * *`); for every hour use
    * `Schedule.hourly.at(minute)`.
    */
  def at(minute: Minute): CronExpression[Set, Unset, Unset, Unset, Unset] =
    blank.at(minute)
}
