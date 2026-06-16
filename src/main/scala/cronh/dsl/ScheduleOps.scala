package cronh.dsl

import cronh.domain.*
import cronh.dsl.FieldState.{Set, Unset}
import cronh.render.{CronDialect, HumanReadable}

/* The fluent refinement verbs, as extensions on `Schedule`. Each `extension`
 * fixes `Unset` in exactly the slots it requires and leaves the rest as free
 * type parameters, so ~five extension blocks cover every set/unset combination
 * without enumerating them.
 *
 * The receiver patterns encode three rules at once (DESIGN.md, see FieldState):
 *   - a field may be set only while its own tag is `Unset` (no silent overwrite);
 *   - `.on`/`.onThe` require *both* day fields `Unset` (mutual exclusivity);
 *   - every verb requires the strictly-finer fields `Unset` (coarse→fine order:
 *     month ▸ {day-of-month, day-of-week} ▸ hour ▸ minute).
 *
 * `Schedule` is opaque here, so each verb edits the wrapped `CronExpression`
 * through `Schedule.{underlying, wrap}` (via the `remap` helper) and re-tags via
 * the declared return type — no `asInstanceOf`, no allocation beyond the `copy`.
 */

extension [
    Min <: FieldState,
    Hr <: FieldState,
    Dom <: FieldState,
    Mon <: FieldState,
    Dow <: FieldState
](schedule: Schedule[Min, Hr, Dom, Mon, Dow]) {

  /** Edit the wrapped expression and re-tag to the declared result type. */
  private def remap[
      A <: FieldState,
      B <: FieldState,
      C <: FieldState,
      D <: FieldState,
      E <: FieldState
  ](f: CronExpression => CronExpression): Schedule[A, B, C, D, E] =
    Schedule.wrap(f(Schedule.underlying(schedule)))
}

/** Months. The coarsest field, so it requires every finer field still `Unset`:
  * callable only on a fresh schedule (or a preset that has set nothing finer).
  */
extension (schedule: Schedule[Unset, Unset, Unset, Unset, Unset]) {

  /** Restricts the schedule to these months. */
  def in(
      first: Month,
      rest: Month*
  ): Schedule[Unset, Unset, Unset, Set, Unset] =
    schedule.remap(_.copy(month = Field.of(first, rest*)))
}

/** Day constraints. Both `.on` and `.onThe` require *both* day fields `Unset`
  * (so picking one removes the other from scope — exclusivity, DESIGN.md §4.5)
  * and the time still `Unset` (coarse→fine). The month may already be set.
  */
extension [Mon <: FieldState](
    schedule: Schedule[Unset, Unset, Unset, Mon, Unset]
) {

  /** Restricts the schedule to these weekdays. Mutually exclusive with
    * [[onThe]].
    */
  def on(
      first: DayOfWeek,
      rest: DayOfWeek*
  ): Schedule[Unset, Unset, Unset, Mon, Set] =
    schedule.remap(_.copy(dayOfWeek = Field.of(first, rest*)))

  /** Restricts the schedule to a prebuilt weekday selection, e.g.
    * `.on(Weekdays)` or `.on(Mon to Fri)`.
    *
    * A [[WeekdaySelector]] has no wildcard inhabitant, so this cannot be handed
    * a `*` that would mark the day constrained while matching every day — that
    * mistake is a compile error rather than a runtime exception.
    */
  def on(days: WeekdaySelector): Schedule[Unset, Unset, Unset, Mon, Set] =
    schedule.remap(_.copy(dayOfWeek = days.toField))

  /** Restricts the schedule to these days of the month. Mutually exclusive with
    * [[on]]. Reads as "on the 15th": `.onThe(15.dom)`.
    */
  def onThe(
      first: MonthDay,
      rest: MonthDay*
  ): Schedule[Unset, Unset, Set, Mon, Unset] =
    schedule.remap(_.copy(dayOfMonth = Field.of(first, rest*)))
}

/** Hour constraints and the full-time finishers. All require the hour and
  * minute still `Unset` (coarse→fine); the date fields may already be set.
  */
extension [Dom <: FieldState, Mon <: FieldState, Dow <: FieldState](
    schedule: Schedule[Unset, Unset, Dom, Mon, Dow]
) {

  /** Constrains the hour to the inclusive range `[from, to]`, leaving the
    * minute refinable with `.at(30.m)` (defaults to `0`):
    * `.between(9.h, 17.h)`.
    */
  def between(
      from: Hour,
      to: Hour
  ): Schedule[Unset, Set, Dom, Mon, Dow] =
    schedule.remap(_.copy(hour = Field.range(from, to)))

  /** Every hour (`*`), leaving the minute refinable with `.at(30.m)`. The
    * explicit way to make the hour `*`, since an unset hour defaults to `0`.
    */
  def everyHour: Schedule[Unset, Set, Dom, Mon, Dow] =
    schedule.remap(_.copy(hour = Field.all))

  /** Sets the time of day. A second `.at` is a compile error rather than a
    * silent overwrite.
    */
  def at(
      hour: Hour,
      minute: Minute
  ): Schedule[Set, Set, Dom, Mon, Dow] =
    schedule.remap(
      _.copy(minute = Field.single(minute), hour = Field.single(hour))
    )

  /** Sets the hour, on the hour (commits minute `0`), so a later `.at(30.m)` is
    * a compile error: use `.at(9.h, 30.m)` for a non-zero minute.
    */
  def at(hour: Hour): Schedule[Set, Set, Dom, Mon, Dow] =
    at(hour, Minute(0))

  /** Every minute of every hour (`* *`) — the explicit high-frequency opt-in,
    * since unset time renders `0 0`, not `* *`.
    */
  def everyMinute: Schedule[Set, Set, Dom, Mon, Dow] =
    schedule.remap(_.copy(minute = Field.all, hour = Field.all))
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
](schedule: Schedule[Unset, Hr, Dom, Mon, Dow]) {

  /** Sets only the minute, keeping the hour as already constrained. */
  def at(minute: Minute): Schedule[Set, Hr, Dom, Mon, Dow] =
    schedule.remap(_.copy(minute = Field.single(minute)))
}

/** Rendering, by delegating to the domain `CronExpression` renderers. Available
  * at every step — a `Schedule` is always a valid cron — and the escape hatch
  * [[toCronExpression]] hands back the plain domain value.
  */
extension [
    Min <: FieldState,
    Hr <: FieldState,
    Dom <: FieldState,
    Mon <: FieldState,
    Dow <: FieldState
](schedule: Schedule[Min, Hr, Dom, Mon, Dow]) {

  /** The underlying domain expression. */
  def toCronExpression: CronExpression = Schedule.underlying(schedule)

  /** Renders as a cron string under the given dialect (defaults to Unix). */
  def toCron(using dialect: CronDialect): String =
    dialect.render(Schedule.underlying(schedule))

  /** An English description, e.g. `"At 9:00 AM, on weekdays"`. */
  def humanReadable: String =
    HumanReadable.describe(Schedule.underlying(schedule))
}
