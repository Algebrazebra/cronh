package cronh.domain

/** A day of the month. Valid values are 1-31. */
opaque type MonthDay = Int

object MonthDay {

  /** Smart constructor for MonthDay with runtime validation. */
  def apply(value: Int): MonthDay = {
    require(
      value >= 1 && value <= 31,
      s"MonthDay must be between 1 and 31, got $value"
    )
    value
  }

  given Ordering[MonthDay] = Ordering.Int

  /** The underlying numeric value (1-31). */
  extension (day: MonthDay) def value: Int = day
}
