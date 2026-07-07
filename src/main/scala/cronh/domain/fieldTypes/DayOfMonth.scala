package cronh.domain.fieldTypes

/** A day of the month. Valid values are 1-31. */
final case class DayOfMonth private (value: Int) derives CanEqual

object DayOfMonth {

  /** Smallest valid day of month. `inline` so it is a compile-time constant
    * usable from the `15.dom` literal checks, keeping a single source of truth
    * for the bounds.
    */
  inline val MinValue = 1

  /** Largest valid day of month. See [[MinValue]]. */
  inline val MaxValue = 31

  /** Smart constructor for MonthDay with runtime validation. */
  def apply(value: Int): DayOfMonth = {
    require(
      value >= MinValue && value <= MaxValue,
      s"MonthDay must be between $MinValue and $MaxValue, got $value"
    )
    new DayOfMonth(value)
  }

  given Ordering[DayOfMonth] = Ordering.by(_.value)

  given DomainBounds[DayOfMonth] with {
    val domain: IndexedSeq[DayOfMonth] =
      (MinValue to MaxValue).map(DayOfMonth(_))
  }

}
