package cronh.domain

/** A day of the month. Valid values are 1-31.
  *
  * A distinct value type: as a case class it has nominal identity, so a
  * cross-unit comparison such as `MonthDay(5) == Hour(5)` is `false` rather
  * than silently `true`.
  */
final case class MonthDay private (value: Int) derives CanEqual

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
    new MonthDay(value)
  }

  given Ordering[MonthDay] = Ordering.by(_.value)

  given DomainBounds[MonthDay] with {
    val domain: IndexedSeq[MonthDay] = (MinValue to MaxValue).map(MonthDay(_))
  }

}
