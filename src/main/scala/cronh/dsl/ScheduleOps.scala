package cronh.dsl

import scala.annotation.targetName

import cronh.domain.*

extension [D <: DaySpec](expression: CronExpression[Status.Unset, D]) {

  /** Sets the time of day. Only available while the time is unset: a second
    * `.at` is a compile error rather than a silent overwrite.
    */
  def at(hour: Hour, minute: Minute): CronExpression[Status.Set, D] =
    expression
      .copy(minute = Field.single(minute), hour = Field.single(hour))
      .retag[Status.Set, D]

  /** Sets the hour, on the hour (minute 0). */
  @targetName("atHour")
  def at(hour: Hour): CronExpression[Status.Set, D] =
    at(hour, Minute(0))

  /** Constrains the hour to the inclusive range `[from, to]` and moves the
    * expression into the [[Status.HourSet]] state: the hour is now fixed, so
    * only the minute-only `.at(30.m)` overload remains available — a later
    * `.at(hour)` that would silently destroy the range is a compile error.
    */
  def between(from: Hour, to: Hour): CronExpression[Status.HourSet, D] =
    expression.copy(hour = Field.range(from, to)).retag[Status.HourSet, D]
}

extension [D <: DaySpec](expression: CronExpression[Status.HourSet, D]) {

  /** Sets only the minute, keeping the already-constrained hour field. The only
    * `.at` available after [[between]] or `Schedule.hourly`, so the hour
    * constraint cannot be overwritten: `.between(9.h, 17.h).at(30.m)`.
    */
  @targetName("atMinute")
  def at(minute: Minute): CronExpression[Status.Set, D] =
    expression.copy(minute = Field.single(minute)).retag[Status.Set, D]
}

extension [T <: Status](expression: CronExpression[T, DaySpec.NoDay]) {

  /** Restricts the schedule to these weekdays. Mutually exclusive with
    * [[onDay]]: setting both is a compile error (DESIGN.md §2.15).
    */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): CronExpression[T, DaySpec.ByWeekday] =
    expression
      .copy(dayOfWeek = Field.of(first, rest*))
      .retag[T, DaySpec.ByWeekday]

  /** Restricts the schedule to a prebuilt day-of-week field, e.g.
    * `.on(Weekdays)` or `.on(Weekends)`.
    */
  def on(days: Field[DayOfWeek]): CronExpression[T, DaySpec.ByWeekday] =
    expression.copy(dayOfWeek = days).retag[T, DaySpec.ByWeekday]

  /** Restricts the schedule to these days of the month. Mutually exclusive with
    * [[on]]: setting both is a compile error (DESIGN.md §2.15).
    */
  def onDay(
      first: MonthDay,
      rest: MonthDay*
  ): CronExpression[T, DaySpec.ByMonthDay] =
    expression
      .copy(dayOfMonth = Field.of(first, rest*))
      .retag[T, DaySpec.ByMonthDay]
}

extension [T <: Status, D <: DaySpec](expression: CronExpression[T, D]) {

  /** Restricts the schedule to these months. */
  def in(first: Month, rest: Month*): CronExpression[T, D] =
    expression.copy(month = Field.of(first, rest*))
}
