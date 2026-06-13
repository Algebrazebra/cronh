package cronh.dsl

import scala.annotation.targetName

import cronh.domain.*

extension [D <: DaySpec, M <: MonthSpec](
    expression: CronExpression[Status.Unset, D, M]
) {

  /** Sets the time of day. Only available while the time is unset: a second
    * `.at` is a compile error rather than a silent overwrite.
    */
  def at(hour: Hour, minute: Minute): CronExpression[Status.Set, D, M] =
    expression
      .copy(minute = Field.single(minute), hour = Field.single(hour))
      .retag[Status.Set, D, M]

  /** Sets the hour, on the hour (minute 0). */
  @targetName("atHour")
  def at(hour: Hour): CronExpression[Status.Set, D, M] =
    at(hour, Minute(0))

  /** Sets only the minute, keeping the hour field as already constrained.
    * Useful after [[between]]: `.between(9.h, 17.h).at(30.m)`.
    */
  @targetName("atMinute")
  def at(minute: Minute): CronExpression[Status.Set, D, M] =
    expression.copy(minute = Field.single(minute)).retag[Status.Set, D, M]

  /** Constrains the hour to the inclusive range `[from, to]` without marking
    * the time as set, so the minute can still be chosen with `.at(30.m)`.
    */
  def between(from: Hour, to: Hour): CronExpression[Status.Unset, D, M] =
    expression.copy(hour = Field.range(from, to))
}

extension [T <: Status, M <: MonthSpec](
    expression: CronExpression[T, DaySpec.NoDay, M]
) {

  /** Restricts the schedule to these weekdays. Mutually exclusive with
    * [[onDay]]: setting both is a compile error (DESIGN.md §2.15).
    */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): CronExpression[T, DaySpec.ByWeekday, M] =
    expression
      .copy(dayOfWeek = Field.of(first, rest*))
      .retag[T, DaySpec.ByWeekday, M]

  /** Restricts the schedule to a prebuilt day-of-week field, e.g.
    * `.on(Weekdays)` or `.on(Weekends)`.
    */
  def on(days: Field[DayOfWeek]): CronExpression[T, DaySpec.ByWeekday, M] =
    expression.copy(dayOfWeek = days).retag[T, DaySpec.ByWeekday, M]

  /** Restricts the schedule to these days of the month. Mutually exclusive with
    * [[on]]: setting both is a compile error (DESIGN.md §2.15).
    */
  def onDay(
      first: MonthDay,
      rest: MonthDay*
  ): CronExpression[T, DaySpec.ByMonthDay, M] =
    expression
      .copy(dayOfMonth = Field.of(first, rest*))
      .retag[T, DaySpec.ByMonthDay, M]
}

extension [T <: Status, D <: DaySpec](
    expression: CronExpression[T, D, MonthSpec.Unset]
) {

  /** Restricts the schedule to these months. Only available while the month is
    * unconstrained: a second `.in` is a compile error rather than silently
    * dropping the earlier months.
    */
  def in(first: Month, rest: Month*): CronExpression[T, D, MonthSpec.Set] =
    expression.copy(month = Field.of(first, rest*)).retag[T, D, MonthSpec.Set]
}
