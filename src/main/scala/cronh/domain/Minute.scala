package cronh.domain

/** A cron minute. Valid values are 0-59.
  *
  * A distinct value type: as a case class it has nominal identity, so a
  * cross-unit comparison such as `Minute(5) == Hour(5)` is `false` rather than
  * silently `true`.
  */
final case class Minute private (value: Int) derives CanEqual

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
    new Minute(value)
  }

  given Ordering[Minute] = Ordering.by(_.value)

  given DomainBounds[Minute] with {
    val domain: IndexedSeq[Minute] = (MinValue to MaxValue).map(Minute(_))
  }

}
