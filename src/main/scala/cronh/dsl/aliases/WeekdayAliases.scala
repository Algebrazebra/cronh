package cronh.dsl.aliases

import cronh.dsl.*
import cronh.domain.fieldTypes.DayOfWeek

private[dsl] trait WeekdayAliases {

  /** Short alias for Monday. */
  val Mon: DayOfWeek = DayOfWeek.Monday

  /** Short alias for Tuesday. */
  val Tue: DayOfWeek = DayOfWeek.Tuesday

  /** Short alias for Wednesday. */
  val Wed: DayOfWeek = DayOfWeek.Wednesday

  /** Short alias for Thursday. */
  val Thu: DayOfWeek = DayOfWeek.Thursday

  /** Short alias for Friday. */
  val Fri: DayOfWeek = DayOfWeek.Friday

  /** Short alias for Saturday. */
  val Sat: DayOfWeek = DayOfWeek.Saturday

  /** Short alias for Sunday. */
  val Sun: DayOfWeek = DayOfWeek.Sunday

  /** Long alias for Monday. */
  val Mondays: DayOfWeek = DayOfWeek.Monday

  /** Long alias for Tuesday. */
  val Tuesdays: DayOfWeek = DayOfWeek.Tuesday

  /** Long alias for Wednesday. */
  val Wednesdays: DayOfWeek = DayOfWeek.Wednesday

  /** Long alias for Thursday. */
  val Thursdays: DayOfWeek = DayOfWeek.Thursday

  /** Long alias for Friday. */
  val Fridays: DayOfWeek = DayOfWeek.Friday

  /** Long alias for Saturday. */
  val Saturdays: DayOfWeek = DayOfWeek.Saturday

  /** Long alias for Sunday. */
  val Sundays: DayOfWeek = DayOfWeek.Sunday

  /** Monday to Friday (inclusive). */
  val Weekdays: DayOfWeekRange =
    Range[DayOfWeek](from = Mondays, to = Fridays)

  /** Saturday and Sunday. */
  val Weekends: DayOfWeekRange =
    Range[DayOfWeek](from = Saturdays, to = Sundays)
}
