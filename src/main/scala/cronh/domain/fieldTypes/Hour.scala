package cronh.domain.fieldTypes

/** A cron hour. Valid values are 0-23. */
final case class Hour private (value: Int) derives CanEqual

object Hour {

  /** Smallest valid hour. `inline` so it is a compile-time constant usable from
    * the `9.h` literal checks, keeping a single source of truth for the bounds.
    */
  inline val MinValue = 0

  /** Largest valid hour. See [[MinValue]]. */
  inline val MaxValue = 23

  /** Smart constructor for Hour with runtime checks.
    *
    * Examples:
    *   - `Hour(5)` returns a valid `Hour`
    *   - `Hour(24)` throws [[IllegalArgumentException]]
    */
  def apply(value: Int): Hour = {
    require(
      value >= MinValue && value <= MaxValue,
      s"Hour must be between $MinValue and $MaxValue, got $value"
    )
    new Hour(value)
  }

  given Ordering[Hour] = Ordering.by(_.value)

  given DomainBounds[Hour] with {
    val domain: IndexedSeq[Hour] = (MinValue to MaxValue).map(Hour(_))
  }

}
