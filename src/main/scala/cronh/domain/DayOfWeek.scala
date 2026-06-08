package cronh.domain

/** A day of the week.
  *
  * The numeric rendering of each day is dialect-specific and therefore lives in
  * the dialect renderer, not here.
  */
enum DayOfWeek {
  case Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
}

object DayOfWeek {
  given Ordering[DayOfWeek] = Ordering.by(_.ordinal)
}
