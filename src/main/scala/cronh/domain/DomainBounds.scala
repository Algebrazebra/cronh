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
