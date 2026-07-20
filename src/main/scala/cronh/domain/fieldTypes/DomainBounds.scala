package cronh.domain.fieldTypes

/** Typeclass listing the complete, ascending domain of a cron unit.
  *
  * Used by [[Field.normalized]] to merge overlapping or adjacent terms and to
  * detect tautological full spans (e.g. `0-59` in the minute field, which
  * collapses to `*`). Cron domains are tiny (at most 60 values), so full
  * enumeration is cheap and avoids a separate successor/predecessor API.
  */
trait DomainBounds[A] {

  /** Every value of `A`, in ascending order consistent with `Ordering[A]`. */
  def domain: IndexedSeq[A]

  /** Each domain value mapped to its ascending index. Computed once per
    * instance (the instances are singletons) so [[Field.normalized]] does not
    * rebuild it on every call.
    */
  lazy val indexOf: Map[A, Int] = domain.zipWithIndex.toMap

  /** Returns the value immediately before `value` in this domain, or [[None]]
    * when `value` is the first value or is not part of the domain.
    */
  final def predecessor(value: A): Option[A] =
    indexOf.get(value).flatMap(index => domain.lift(index - 1))
}
