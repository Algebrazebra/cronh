package cronh.domain

/** A cron hour. Valid values are 0-23. */
opaque type Hour = Int
object Hour {

  /** Smart constructor for Hour with runtime checks.
    *
    * Examples:
    *   - `Hour(5)` returns a valid `Hour`
    *   - `Hour(24)` throws [[IllegalArgumentException]]
    */
  def apply(value: Int): Hour = {
    require(
      value >= 0 && value <= 23,
      s"Hour must be between 0 and 23, got $value"
    )
    value
  }

  given Ordering[Hour] = Ordering.Int

  given DomainBounds[Hour] with {
    val domain: IndexedSeq[Hour] = (0 to 23).map(Hour(_))
  }

  /** The underlying numeric value (0-23). */
  extension (hour: Hour) def value: Int = hour

}
