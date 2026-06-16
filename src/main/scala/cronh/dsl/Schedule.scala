package cronh.dsl

import cronh.domain.*
import cronh.dsl.FieldState.{Set, Unset}

/** The fluent schedule builder: a [[cronh.domain.CronExpression]] tagged at the
  * type level with one [[FieldState]] per field (cron order: minute, hour,
  * day-of-month, month, day-of-week).
  *
  * The tags are a *DSL* concern and live here, not on the domain
  * `CronExpression` (DESIGN.md §2.1). `Schedule` is an `opaque type` aliasing
  * `CronExpression`, so it carries the phantoms with **zero runtime cost** — at
  * runtime a `Schedule` simply *is* its `CronExpression` — while keeping them
  * invisible to the domain and to callers outside this package.
  *
  * The tags let the builder reject — at compile time — a second write to an
  * already-chosen field (`.at(9.h).at(14.h)`, `.in(June).in(July)`), keep
  * `.on`/`.onThe` mutually exclusive (DESIGN.md §4.5), and enforce coarse→fine
  * ordering. They never gate rendering: every `Schedule` yields a valid cron
  * via `.toCron` / `.toCronExpression`.
  *
  * Parameters are covariant so a more-constrained schedule conforms wherever a
  * less-constrained one is expected; the phantoms have no runtime witness, so
  * this is sound.
  */
opaque type Schedule[
    +Min <: FieldState,
    +Hr <: FieldState,
    +Dom <: FieldState,
    +Mon <: FieldState,
    +Dow <: FieldState
] = CronExpression

/** A schedule with nothing chosen yet — the starting point for every builder.
  * Renders `0 0 * * *` (daily at midnight).
  */
type FreshCron = Schedule[Unset, Unset, Unset, Unset, Unset]

/** Entry points for building schedules fluently.
  *
  * Every entry builds from one blank slate — `0 0 * * *`, daily at midnight —
  * by applying the refinement verbs in `ScheduleOps`. The blank slate encodes
  * the asymmetric defaults (DESIGN.md): an unconstrained *time* field renders
  * `0`, an unconstrained *date* field renders `*`. So `Schedule.in(June)` is
  * `0 0 * 6 *` (midnight daily in June), not `* * * 6 *` — every-minute is an
  * explicit opt-in via [[everyMinute]].
  *
  * `daily`/`hourly`/… are values on this object, not refinements, so there is
  * no `.daily` to chain onto a schedule: `Schedule.monthly.daily` does not
  * typecheck.
  */
object Schedule {

  /** Cross the opaque boundary. Transparent only inside this file; exposed
    * `private[dsl]` so the refinement verbs (another file, where `Schedule` is
    * opaque) can wrap and unwrap without an `asInstanceOf`.
    */
  private[dsl] def wrap[
      A <: FieldState,
      B <: FieldState,
      C <: FieldState,
      D <: FieldState,
      E <: FieldState
  ](expression: CronExpression): Schedule[A, B, C, D, E] = expression

  private[dsl] def underlying[
      A <: FieldState,
      B <: FieldState,
      C <: FieldState,
      D <: FieldState,
      E <: FieldState
  ](schedule: Schedule[A, B, C, D, E]): CronExpression = schedule

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
  val hourly: Schedule[Unset, Set, Unset, Unset, Unset] = blank.everyHour

  /** The first of every month at 00:00 (`0 0 1 * *`). */
  val monthly: Schedule[Unset, Unset, Set, Unset, Unset] =
    blank.onThe(MonthDay(1))

  /** Every January 1st at 00:00 (`0 0 1 1 *`). */
  val yearly: Schedule[Unset, Unset, Set, Set, Unset] =
    blank.in(Month.January).onThe(MonthDay(1))

  /** Monday through Friday at 00:00 (`0 0 * * 1-5`); refine with `.at`. */
  val weekdays: Schedule[Unset, Unset, Unset, Unset, Set] =
    blank.on(Weekdays)

  /** Saturday and Sunday at 00:00 (`0 0 * * 6,0`). */
  val weekends: Schedule[Unset, Unset, Unset, Unset, Set] =
    blank.on(Weekends)

  /** Every minute of every day (`* * * * *`) — the explicit high-frequency
    * opt-in, since unset time renders `0 0`.
    */
  val everyMinute: Schedule[Set, Set, Unset, Unset, Unset] =
    blank.everyMinute

  /** These months, at 00:00 by default. */
  def in(
      first: Month,
      rest: Month*
  ): Schedule[Unset, Unset, Unset, Set, Unset] =
    blank.in(first, rest*)

  /** These weekdays, at 00:00 by default. */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): Schedule[Unset, Unset, Unset, Unset, Set] =
    blank.on(first, rest*)

  /** A prebuilt weekday selection (e.g. `Weekdays`, `Mon to Fri`), at 00:00. */
  def on(days: WeekdaySelector): Schedule[Unset, Unset, Unset, Unset, Set] =
    blank.on(days)

  /** These days of the month, at 00:00 by default. Reads "on the 15th". */
  def onThe(
      first: MonthDay,
      rest: MonthDay*
  ): Schedule[Unset, Unset, Set, Unset, Unset] =
    blank.onThe(first, rest*)

  /** The inclusive hour range `[from, to]`; refine the minute with `.at(30.m)`.
    */
  def between(
      from: Hour,
      to: Hour
  ): Schedule[Unset, Set, Unset, Unset, Unset] =
    blank.between(from, to)

  /** At this hour and minute (`m h * * *`). */
  def at(
      hour: Hour,
      minute: Minute
  ): Schedule[Set, Set, Unset, Unset, Unset] =
    blank.at(hour, minute)

  /** At this hour, on the hour (`0 h * * *`). */
  def at(hour: Hour): Schedule[Set, Set, Unset, Unset, Unset] =
    blank.at(hour)

  /** At this minute past hour 0 (`m 0 * * *`); for every hour use
    * `Schedule.hourly.at(minute)`.
    */
  def at(minute: Minute): Schedule[Set, Unset, Unset, Unset, Unset] =
    blank.at(minute)
}
