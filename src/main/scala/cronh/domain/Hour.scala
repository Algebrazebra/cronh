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
    if (value >= 0 && value <= 23) value
    else
      throw new IllegalArgumentException(
        s"Hour must be between 0 and 23, got $value"
      )
  }

  given Ordering[Hour] = Ordering.Int

}
