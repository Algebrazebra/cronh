package cronh.domain

/** A cron minute. Valid values are 0-59.
  *
  * Because `Minute` is an opaque alias for `Int`, it shares `Int`'s universal
  * equality: under default (non-strict) equality a cross-unit comparison such
  * as `Minute(5) == Hour(5)` compiles and returns `true`. To turn such
  * mismatches into compile errors, enable `-language:strictEquality`; the
  * same-type [[scala.CanEqual]] instance in the companion then permits only
  * `Minute`-to-`Minute` comparisons. Without that flag, compare the underlying
  * `.value`s explicitly when crossing units.
  */
opaque type Minute = Int

object Minute {

  /** Smart constructor for Minute with runtime checks.
    *
    * Examples:
    *   - `Minute(5)` returns a valid `Minute`
    *   - `Minute(61)` throws [[IllegalArgumentException]]
    */
  def apply(value: Int): Minute = {
    require(
      value >= 0 && value <= 59,
      s"Minute must be between 0 and 59, got $value"
    )
    value
  }

  given Ordering[Minute] = Ordering.Int

  /** Restricts equality to `Minute`-to-`Minute` under
    * `-language:strictEquality`, so `Minute(5) == Hour(5)` no longer compiles.
    */
  given CanEqual[Minute, Minute] = CanEqual.derived

  given DomainBounds[Minute] with {
    val domain: IndexedSeq[Minute] = (0 to 59).map(Minute(_))
  }

  /** The underlying numeric value (0-59). */
  extension (minute: Minute) def value: Int = minute

}
