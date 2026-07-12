package cronh.dsl

import cronh.domain.Field
import cronh.domain.fieldTypes.{DayOfWeek, DayOfMonth, Month}

/** Defines an inclusive range of ordered values of type T.
  *
  * The [[Ranging]] typeclass provides an inline `to` method for constructing
  * ranges in a readable way:
  * ```
  * // For two values of type T and a given Ordering[T]:
  * val start: T = ???
  * val end: T = ???
  * // You can create a range like this:
  * val range = start to end
  * ```
  *
  * The typeclass is automatically provided for all types that have an
  * [[Ordering]] instance.
  */
case class Range[T: Ordering](from: T, to: T) {
  private[dsl] def toField: Field[T] = Field.range(from, to)
}

/** A range of months, e.g., January to March */
type MonthRange = Range[Month]

/** A range of weekdays, e.g., Monday to Wednesday */
type DayOfWeekRange = Range[DayOfWeek]

/** A range of days, e.g., 5.th to 15.th */
type DayOfMonthRange = Range[DayOfMonth]

trait Ranging[T]:
  extension (from: T) infix def to(to: T): Range[T]

object Ranging:
  given [T](using Ordering[T]): Ranging[T] with
    extension (from: T) infix def to(to: T): Range[T] = Range(from, to)
