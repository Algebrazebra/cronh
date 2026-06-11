package cronh.domain

/** Phantom marker tracking how a [[CronExpression]]'s day constraint has been
  * specified.
  *
  * Never instantiated; exists only at the type level so that combining a
  * day-of-week constraint with a day-of-month constraint (a classic source of
  * surprising OR semantics, see DESIGN.md §2.15) is a compile error.
  */
sealed trait DaySpec

object DaySpec {

  /** No day constraint has been chosen yet; both `.on` and `.onDay` are
    * available.
    */
  sealed trait NoDay extends DaySpec

  /** The schedule is constrained by day of week (`.on`). */
  sealed trait ByWeekday extends DaySpec

  /** The schedule is constrained by day of month (`.onDay`). */
  sealed trait ByMonthDay extends DaySpec
}
