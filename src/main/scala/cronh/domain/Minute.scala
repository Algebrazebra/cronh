package cronh.domain

/** A cron minute. Valid values are 0-59. */
opaque type Minute = Int

object Minute {

  /** Smart constructor for Minute with runtime checks.
    *
    * Examples:
    *   - `Minute(5)` compiles
    *   - `Minute(61)` fails at run-time
    */
  def apply(value: Int): Minute = {
    if (value >= 0 && value <= 59) value
    else
      throw new IllegalArgumentException(invalidValueMessage(value))
  }

  given Ordering[Minute] = Ordering.Int

  private def invalidValueMessage(value: Int) =
    s"Invalid minute value: $value. Minute value must be between 0 and 59."
}
