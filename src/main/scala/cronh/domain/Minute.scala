package cronh.domain

/** A cron minute. Valid values are 0-59. */
opaque type Minute = Int

object Minute {

  /** Smart constructor for Minute with runtime checks.
    *
    * Examples:
    *   - `Minute(5)` returns a valid `Minute`
    *   - `Minute(61)` throws [[IllegalArgumentException]]
    */
  def apply(value: Int): Minute = {
    if (value >= 0 && value <= 59) value
    else
      throw new IllegalArgumentException(
        s"Minute must be between 0 and 59, got $value"
      )
  }

  given Ordering[Minute] = Ordering.Int

}
