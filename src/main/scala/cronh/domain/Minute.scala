package cronh.domain

/** A cron minute. Valid values are 0-59. */
opaque type Minute = Int

object Minute {

  /** Smallest valid minute. `inline` so it is a compile-time constant usable
    * from the `30.m` literal checks, keeping a single source of truth for the
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
    value
  }

  given Ordering[Minute] = Ordering.Int

  given DomainBounds[Minute] with {
    val domain: IndexedSeq[Minute] = (MinValue to MaxValue).map(Minute(_))
  }

  /** The underlying numeric value (0-59). */
  extension (minute: Minute) def value: Int = minute

}
