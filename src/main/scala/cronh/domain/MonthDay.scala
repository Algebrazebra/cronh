package cronh.domain

/** A day of the month. Valid values are 1-31. */
opaque type MonthDay = Int

object MonthDay {

  /** Smallest valid day of month. `inline` so it is a compile-time constant
    * usable from the `15.dom` literal checks, keeping a single source of truth
    * for the bounds.
    */
  inline val MinValue = 1

  /** Largest valid day of month. See [[MinValue]]. */
  inline val MaxValue = 31

  /** Smart constructor for MonthDay with runtime validation. */
  def apply(value: Int): MonthDay = {
    require(
      value >= MinValue && value <= MaxValue,
      s"MonthDay must be between $MinValue and $MaxValue, got $value"
    )
    value
  }

  given Ordering[MonthDay] = Ordering.Int

  given DomainBounds[MonthDay] with {
    val domain: IndexedSeq[MonthDay] = (MinValue to MaxValue).map(MonthDay(_))
  }

  /** The underlying numeric value (1-31). */
  extension (day: MonthDay) def value: Int = day
}
