package cronh.domain.fieldTypes

/** A cron week day.
  *
  * The numeric rendering of each day is dialect-specific and therefore lives in
  * the dialect renderer, not here.
  */
enum DayOfWeek {
  case Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
}

object DayOfWeek {
  given Ordering[DayOfWeek] = Ordering.by(_.ordinal)

  given DomainBounds[DayOfWeek] with {
    val domain: IndexedSeq[DayOfWeek] = DayOfWeek.values.toIndexedSeq
  }
}
