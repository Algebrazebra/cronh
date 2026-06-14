package cronh.dsl

import cronh.domain.*
import cronh.domain.FieldState.{Set, Unset}

/** Sets the time of day. Requires both the minute and hour still unset, so a
  * second `.at(hour, ...)` is a compile error rather than a silent overwrite.
  */
extension [Dom <: FieldState, Mon <: FieldState, Dow <: FieldState](
    expression: CronExpression[Unset, Unset, Dom, Mon, Dow]
) {

  /** Sets the time of day. Only available while both the minute and hour are
    * unset: a second `.at` is a compile error rather than a silent overwrite.
    */
  def at(hour: Hour, minute: Minute): CronExpression[Set, Set, Dom, Mon, Dow] =
    expression
      .copy(minute = Field.single(minute), hour = Field.single(hour))
      .retag[Set, Set, Dom, Mon, Dow]

  /** Sets the hour, on the hour (minute 0). */
  def at(hour: Hour): CronExpression[Set, Set, Dom, Mon, Dow] =
    at(hour, Minute(0))
}

/** Sets only the minute, leaving the hour as already constrained. Requires the
  * minute unset but accepts any hour state, so it remains available after
  * [[between]] or `Schedule.hourly` (where `.at(hour)` is rejected).
  */
extension [
    Hr <: FieldState,
    Dom <: FieldState,
    Mon <: FieldState,
    Dow <: FieldState
](expression: CronExpression[Unset, Hr, Dom, Mon, Dow]) {

  /** Sets only the minute, keeping the hour field as already constrained.
    * Useful after [[between]]: `.between(9.h, 17.h).at(30.m)`.
    */
  def at(minute: Minute): CronExpression[Set, Hr, Dom, Mon, Dow] =
    expression.copy(minute = Field.single(minute)).retag[Set, Hr, Dom, Mon, Dow]
}

/** Constrains the hour to a range. Requires the hour unset and marks it set, so
  * a later `.at(hour)` cannot silently overwrite the range while `.at(30.m)`
  * can still refine the minute.
  */
extension [
    Min <: FieldState,
    Dom <: FieldState,
    Mon <: FieldState,
    Dow <: FieldState
](expression: CronExpression[Min, Unset, Dom, Mon, Dow]) {

  /** Constrains the hour to the inclusive range `[from, to]`. The hour is now
    * fixed, so only the minute-only `.at(30.m)` overload remains; a later
    * `.at(hour)` that would silently destroy the range is a compile error.
    */
  def between(from: Hour, to: Hour): CronExpression[Min, Set, Dom, Mon, Dow] =
    expression.copy(hour = Field.range(from, to)).retag[Min, Set, Dom, Mon, Dow]
}

/** Day constraints. Both `.on` and `.onDay` require *both* day fields still
  * unset, so picking one removes the other from scope — mutual exclusivity as a
  * precondition (DESIGN.md §2.15) rather than an unrepresentable state, leaving
  * the both-set case available to future non-OR dialects.
  */
extension [Min <: FieldState, Hr <: FieldState, Mon <: FieldState](
    expression: CronExpression[Min, Hr, Unset, Mon, Unset]
) {

  /** Restricts the schedule to these weekdays. Mutually exclusive with
    * [[onDay]]: setting both is a compile error (DESIGN.md §2.15).
    */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): CronExpression[Min, Hr, Unset, Mon, Set] =
    expression
      .copy(dayOfWeek = Field.of(first, rest*))
      .retag[Min, Hr, Unset, Mon, Set]

  /** Restricts the schedule to a prebuilt weekday selection, e.g.
    * `.on(Weekdays)` or `.on(Weekends)`.
    *
    * A [[WeekdaySelector]] has no wildcard inhabitant, so this cannot be handed
    * a `*` that would mark the result constrained while matching every day —
    * that mistake is now a compile error rather than a runtime exception. Leave
    * the day unconstrained instead of trying to pass a wildcard here.
    */
  def on(days: WeekdaySelector): CronExpression[Min, Hr, Unset, Mon, Set] =
    expression.copy(dayOfWeek = days.toField).retag[Min, Hr, Unset, Mon, Set]

  /** Restricts the schedule to these days of the month. Mutually exclusive with
    * [[on]]: setting both is a compile error (DESIGN.md §2.15).
    */
  def onDay(
      first: MonthDay,
      rest: MonthDay*
  ): CronExpression[Min, Hr, Set, Mon, Unset] =
    expression
      .copy(dayOfMonth = Field.of(first, rest*))
      .retag[Min, Hr, Set, Mon, Unset]
}

/** Restricts the schedule to a set of months. Requires the month unset, so a
  * second `.in` is a compile error rather than silently dropping the earlier
  * months.
  */
extension [
    Min <: FieldState,
    Hr <: FieldState,
    Dom <: FieldState,
    Dow <: FieldState
](expression: CronExpression[Min, Hr, Dom, Unset, Dow]) {

  /** Restricts the schedule to these months. Only available while the month is
    * unconstrained: a second `.in` is a compile error rather than silently
    * dropping the earlier months.
    */
  def in(first: Month, rest: Month*): CronExpression[Min, Hr, Dom, Set, Dow] =
    expression
      .copy(month = Field.of(first, rest*))
      .retag[Min, Hr, Dom, Set, Dow]
}
