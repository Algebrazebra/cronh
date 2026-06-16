package cronh.dsl

import cronh.domain.*
import cronh.domain.FieldState.{Set, Unset}

/* The fluent refinement verbs. Each `extension` fixes `Unset` in exactly the
 * slots it requires and leaves the rest as free type parameters, so ~five
 * extension blocks cover every set/unset combination without enumerating them.
 *
 * The receiver patterns encode three rules at once (DESIGN.md, see FieldState):
 *   - a field may be set only while its own tag is `Unset` (no silent overwrite);
 *   - `.on`/`.onThe` require *both* day fields `Unset` (mutual exclusivity);
 *   - every verb requires the strictly-finer fields `Unset` (coarse→fine order:
 *     month ▸ {day-of-month, day-of-week} ▸ hour ▸ minute).
 */

/** Months. The coarsest field, so it requires every finer field still `Unset`:
  * callable only on a fresh expression (or a preset that has set nothing
  * finer).
  */
extension (expression: CronExpression[Unset, Unset, Unset, Unset, Unset]) {

  /** Restricts the schedule to these months. */
  def in(
      first: Month,
      rest: Month*
  ): CronExpression[Unset, Unset, Unset, Set, Unset] =
    expression
      .copy(month = Field.of(first, rest*))
      .retag[Unset, Unset, Unset, Set, Unset]
}

/** Day constraints. Both `.on` and `.onThe` require *both* day fields `Unset`
  * (so picking one removes the other from scope — exclusivity, DESIGN.md §4.5)
  * and the time still `Unset` (coarse→fine). The month may already be set.
  */
extension [Mon <: FieldState](
    expression: CronExpression[Unset, Unset, Unset, Mon, Unset]
) {

  /** Restricts the schedule to these weekdays. Mutually exclusive with
    * [[onThe]].
    */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): CronExpression[Unset, Unset, Unset, Mon, Set] =
    expression
      .copy(dayOfWeek = Field.of(first, rest*))
      .retag[Unset, Unset, Unset, Mon, Set]

  /** Restricts the schedule to a prebuilt weekday selection, e.g.
    * `.on(Weekdays)` or `.on(Mon to Fri)`.
    *
    * A [[WeekdaySelector]] has no wildcard inhabitant, so this cannot be handed
    * a `*` that would mark the day constrained while matching every day — that
    * mistake is a compile error rather than a runtime exception.
    */
  def on(days: WeekdaySelector): CronExpression[Unset, Unset, Unset, Mon, Set] =
    expression
      .copy(dayOfWeek = days.toField)
      .retag[Unset, Unset, Unset, Mon, Set]

  /** Restricts the schedule to these days of the month. Mutually exclusive with
    * [[on]]. Reads as "on the 15th": `.onThe(15.dom)`.
    */
  def onThe(
      first: MonthDay,
      rest: MonthDay*
  ): CronExpression[Unset, Unset, Set, Mon, Unset] =
    expression
      .copy(dayOfMonth = Field.of(first, rest*))
      .retag[Unset, Unset, Set, Mon, Unset]
}

/** Hour constraints and the full-time finishers. All require the hour and
  * minute still `Unset` (coarse→fine); the date fields may already be set.
  */
extension [Dom <: FieldState, Mon <: FieldState, Dow <: FieldState](
    expression: CronExpression[Unset, Unset, Dom, Mon, Dow]
) {

  /** Constrains the hour to the inclusive range `[from, to]`, leaving the
    * minute refinable with `.at(30.m)` (defaults to `0`):
    * `.between(9.h, 17.h)`.
    */
  def between(
      from: Hour,
      to: Hour
  ): CronExpression[Unset, Set, Dom, Mon, Dow] =
    expression
      .copy(hour = Field.range(from, to))
      .retag[Unset, Set, Dom, Mon, Dow]

  /** Every hour (`*`), leaving the minute refinable with `.at(30.m)`. The
    * explicit way to make the hour `*`, since an unset hour defaults to `0`.
    */
  def everyHour: CronExpression[Unset, Set, Dom, Mon, Dow] =
    expression.copy(hour = Field.all).retag[Unset, Set, Dom, Mon, Dow]

  /** Sets the time of day. A second `.at` is a compile error rather than a
    * silent overwrite.
    */
  def at(
      hour: Hour,
      minute: Minute
  ): CronExpression[Set, Set, Dom, Mon, Dow] =
    expression
      .copy(minute = Field.single(minute), hour = Field.single(hour))
      .retag[Set, Set, Dom, Mon, Dow]

  /** Sets the hour, on the hour (commits minute `0`), so a later `.at(30.m)` is
    * a compile error: use `.at(9.h, 30.m)` for a non-zero minute.
    */
  def at(hour: Hour): CronExpression[Set, Set, Dom, Mon, Dow] =
    at(hour, Minute(0))

  /** Every minute of every hour (`* *`) — the explicit high-frequency opt-in,
    * since unset time renders `0 0`, not `* *`.
    */
  def everyMinute: CronExpression[Set, Set, Dom, Mon, Dow] =
    expression
      .copy(minute = Field.all, hour = Field.all)
      .retag[Set, Set, Dom, Mon, Dow]
}

/** Minute-only finisher. The finest field, so it requires nothing finer — only
  * the minute still `Unset` — and accepts any hour state. It remains available
  * after [[between]] / [[everyHour]] (where `.at(hour)` is rejected) to set the
  * minute of an hour already constrained: `.between(9.h, 17.h).at(30.m)`.
  */
extension [
    Hr <: FieldState,
    Dom <: FieldState,
    Mon <: FieldState,
    Dow <: FieldState
](expression: CronExpression[Unset, Hr, Dom, Mon, Dow]) {

  /** Sets only the minute, keeping the hour as already constrained. */
  def at(minute: Minute): CronExpression[Set, Hr, Dom, Mon, Dow] =
    expression.copy(minute = Field.single(minute)).retag[Set, Hr, Dom, Mon, Dow]
}
