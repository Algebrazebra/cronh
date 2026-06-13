package cronh.domain

/** A day of the month. Valid values are 1-31.
  *
  * Because `MonthDay` is an opaque alias for `Int`, it shares `Int`'s universal
  * equality: under default (non-strict) equality a cross-unit comparison such
  * as `MonthDay(5) == Hour(5)` compiles and returns `true`. To turn such
  * mismatches into compile errors, enable `-language:strictEquality`; the
  * same-type [[scala.CanEqual]] instance in the companion then permits only
  * `MonthDay`-to-`MonthDay` comparisons. Without that flag, compare the
  * underlying `.value`s explicitly when crossing units.
  */
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

  /** Restricts equality to `MonthDay`-to-`MonthDay` under
    * `-language:strictEquality`, so `MonthDay(5) == Hour(5)` no longer
    * compiles.
    */
  given CanEqual[MonthDay, MonthDay] = CanEqual.derived

  given DomainBounds[MonthDay] with {
    val domain: IndexedSeq[MonthDay] = (1 to 31).map(MonthDay(_))
  }

  /** The underlying numeric value (1-31). */
  extension (day: MonthDay) def value: Int = day
}
