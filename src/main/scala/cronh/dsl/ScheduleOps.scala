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

  /** Sets only the minute, keeping the hour field as already constrained.
    * Useful after [[between]]: `.between(9.h, 17.h).at(30.m)`.
    */
  @targetName("atMinute")
  def at(minute: Minute): CronExpression[Status.Set, D] =
    expression.copy(minute = Field.single(minute)).retag[Status.Set, D]

  /** Constrains the hour to the inclusive range `[from, to]` without marking
    * the time as set, so the minute can still be chosen with `.at(30.m)`.
    */
  def between(from: Hour, to: Hour): CronExpression[Status.Unset, D] =
    expression.copy(hour = Field.range(from, to))
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

  /** Restricts the schedule to a prebuilt weekday selection, e.g.
    * `.on(Weekdays)` or `.on(Weekends)`.
    *
    * A [[WeekdaySelector]] has no wildcard inhabitant, so this cannot be handed
    * a `*` that would mark the result `ByWeekday` while matching every day —
    * that mistake is now a compile error rather than a runtime exception. Leave
    * the day unconstrained instead of trying to pass a wildcard here.
    */
  def on(days: WeekdaySelector): CronExpression[T, DaySpec.ByWeekday] =
    expression.copy(dayOfWeek = days.toField).retag[T, DaySpec.ByWeekday]

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
