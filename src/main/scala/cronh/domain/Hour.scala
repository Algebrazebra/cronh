package cronh.domain

/** A cron hour. Valid values are 0-23.
  *
  * Because `Hour` is an opaque alias for `Int`, it shares `Int`'s universal
  * equality: under default (non-strict) equality a cross-unit comparison such
  * as `Hour(5) == Minute(5)` compiles and returns `true`. To turn such
  * mismatches into compile errors, enable `-language:strictEquality`; the
  * same-type [[scala.CanEqual]] instance in the companion then permits only
  * `Hour`-to-`Hour` comparisons. Without that flag, compare the underlying
  * `.value`s explicitly when crossing units.
  */
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

  /** Restricts equality to `Hour`-to-`Hour` under `-language:strictEquality`,
    * so `Hour(5) == Minute(5)` no longer compiles.
    */
  given CanEqual[Hour, Hour] = CanEqual.derived

  given DomainBounds[Hour] with {
    val domain: IndexedSeq[Hour] = (0 to 23).map(Hour(_))
  }

  /** The underlying numeric value (0-23). */
  extension (hour: Hour) def value: Int = hour

}
