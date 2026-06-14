package cronh.dsl

import cronh.domain.{DayOfWeek, Field}

/** A non-empty selection of weekdays for use with `.on`.
  *
  * Unlike a raw `Field[DayOfWeek]`, a selector has no wildcard inhabitant: every
  * constructor below narrows the day. `.on` therefore cannot be handed a `*`
  * that would silently match every day, so the check the runtime `require` used
  * to make is now made by the type system — an illegal `.on(Field.all)` does not
  * compile rather than throwing.
  *
  * It is an opaque alias for `Field[DayOfWeek]`, so it costs nothing at runtime
  * and converts back with [[toField]] for storage in a `CronExpression`.
  */
opaque type WeekdaySelector = Field[DayOfWeek]

object WeekdaySelector {

  /** One or more specific weekdays, e.g. `WeekdaySelector(Mon, Wed, Fri)`. */
  def apply(first: DayOfWeek, rest: DayOfWeek*): WeekdaySelector =
    Field.of(first, rest*)

  /** An inclusive run of weekdays, e.g. `WeekdaySelector.range(Mon, Fri)`. */
  def range(from: DayOfWeek, to: DayOfWeek): WeekdaySelector =
    Field.range(from, to)

  extension (selector: WeekdaySelector) {

    /** The underlying field, for storage in a `CronExpression`. */
    def toField: Field[DayOfWeek] = selector

    /** Combines two selections into cron list syntax, e.g.
      * `Weekends ++ WeekdaySelector(Wed)`. Still wildcard-free by construction.
      */
    def ++(other: WeekdaySelector): WeekdaySelector =
      (selector: Field[DayOfWeek]) ++ other
  }
}
