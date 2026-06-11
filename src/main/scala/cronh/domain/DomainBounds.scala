package cronh.domain

/** Typeclass enumerating the complete, ascending domain of a cron unit.
  *
  * Used by [[Field.normalized]] to merge overlapping or adjacent terms and to
  * detect tautological full spans (e.g. `0-59` in the minute field, which
  * collapses to `*`). Cron domains are tiny (at most 60 values), so full
  * enumeration is cheap and avoids a separate successor/predecessor API.
  */
trait DomainBounds[A] {

  /** Every value of `A`, in ascending order consistent with `Ordering[A]`. */
  def domain: IndexedSeq[A]
}

object DomainBounds {

  given DomainBounds[Minute] with {
    val domain: IndexedSeq[Minute] = (0 to 59).map(Minute(_))
  }

  given DomainBounds[Hour] with {
    val domain: IndexedSeq[Hour] = (0 to 23).map(Hour(_))
  }

  given DomainBounds[MonthDay] with {
    val domain: IndexedSeq[MonthDay] = (1 to 31).map(MonthDay(_))
  }

  given DomainBounds[Month] with {
    val domain: IndexedSeq[Month] = Month.values.toIndexedSeq
  }

  given DomainBounds[DayOfWeek] with {
    val domain: IndexedSeq[DayOfWeek] = DayOfWeek.values.toIndexedSeq
  }
}
