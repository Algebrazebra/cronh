package cronh.domain.fieldTypes

/** A cron minute. Valid values are 0-59. */
final case class Minute private (value: Int) derives CanEqual

object Minute {

  /** Smallest valid minute. `inline` so it is a compile-time constant usable
    * from the `30.min` literal checks, keeping a single source of truth for the
    * bounds.
    */
  inline val MinValue = 0

  /** Largest valid minute. See [[MinValue]]. */
  inline val MaxValue = 59

  /** Smart constructor for Minute with runtime checks.
    *
    * Examples:
    *   - `Minute(5)` returns a valid `Minute`
    *   - `Minute(61)` throws [[IllegalArgumentException]]
    */
  def apply(value: Int): Minute = {
    require(
      value >= MinValue && value <= MaxValue,
      s"Minute must be between $MinValue and $MaxValue, got $value"
    )
    new Minute(value)
  }

  given Ordering[Minute] = Ordering.by(_.value)

  given DomainBounds[Minute] with {
    val domain: IndexedSeq[Minute] = (MinValue to MaxValue).map(Minute(_))
  }

}
