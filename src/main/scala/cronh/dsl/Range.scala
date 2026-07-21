package cronh.dsl

import cronh.domain.Field
import cronh.domain.fieldTypes.{
  DayOfWeek,
  DayOfMonth,
  DomainBounds,
  Month,
  Hour,
  Minute
}

/** Defines an inclusive range of ordered values of type `T`.
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
  require(
    Ordering[T].lteq(from, to),
    "Start of an inclusive range must be less than or equal to its end."
  )

  private[dsl] def toField: Field[T] = Field.range(from, to)
}

object Range:
  /** Converts the exclusive range `[from, endExclusive)` to its equivalent
    * inclusive representation.
    */
  private[dsl] def exclusive[T: Ordering: DomainBounds](
      from: T,
      endExclusive: T
  ): Range[T] =
    require(
      Ordering[T].lt(from, endExclusive),
      "Start of an exclusive range must be less than its end."
    )

    val inclusiveEnd =
      summon[DomainBounds[T]].predecessor(endExclusive).getOrElse {
        throw IllegalArgumentException(
          s"Cannot determine the value preceding $endExclusive."
        )
      }

    Range(from, inclusiveEnd)

/** A range of months, e.g., `January to March`. */
type MonthRange = Range[Month]

/** A range of weekdays, e.g., `Mondays to Wednesdays`. */
type DayOfWeekRange = Range[DayOfWeek]

/** A range of days of the month, e.g., `5.th to 15.th`. */
type DayOfMonthRange = Range[DayOfMonth]

/** A range of hours, e.g., `9.h to 17.h`. */
type HourRange = Range[Hour]

/** A range of minutes, e.g., `0.min to 30.min`. */
type MinuteRange = Range[Minute]

/** Type class for adding inclusive range syntax to ordered values. */
trait Ranging[T]:
  /** Creates the inclusive range from this value to `to`. */
  extension (from: T) infix def to(to: T): Range[T]

object Ranging:
  /** Provides range syntax for any type with an [[Ordering]]. */
  given [T](using Ordering[T]): Ranging[T] with
    extension (from: T) infix def to(to: T): Range[T] = Range(from, to)

/** Creates an inclusive range of months. Literal endpoints are validated at
  * compile time; dynamic endpoints retain the runtime validation performed by
  * [[Range]].
  */
extension (inline from: Month)
  inline infix def to(inline to: Month): MonthRange =
    ${ RangeMacros.inclusiveMonth('from, 'to) }

/** Creates an inclusive range of days of the week. Literal endpoints are
  * validated at compile time; dynamic endpoints retain runtime validation.
  */
extension (inline from: DayOfWeek)
  inline infix def to(inline to: DayOfWeek): DayOfWeekRange =
    ${ RangeMacros.inclusiveDayOfWeek('from, 'to) }

/** Creates an inclusive range of days of the month. Literal endpoints are
  * validated at compile time; dynamic endpoints retain runtime validation.
  */
extension (inline from: DayOfMonth)
  inline infix def to(inline to: DayOfMonth): DayOfMonthRange =
    ${ RangeMacros.inclusiveDayOfMonth('from, 'to) }

/** Creates an inclusive range of hours. Literal endpoints are validated at
  * compile time; dynamic endpoints retain runtime validation.
  */
extension (inline from: Hour)
  inline infix def to(inline to: Hour): HourRange =
    ${ RangeMacros.inclusiveHour('from, 'to) }

/** Creates an inclusive range of minutes. Literal endpoints are validated at
  * compile time; dynamic endpoints retain runtime validation.
  */
extension (inline from: Minute)
  inline infix def to(inline to: Minute): MinuteRange =
    ${ RangeMacros.inclusiveMinute('from, 'to) }

/** Creates an exclusive-end range of months, with compile-time validation for
  * literal endpoints and runtime validation for dynamic endpoints.
  */
extension (inline from: Month)
  inline infix def until(inline endExclusive: Month): MonthRange =
    ${ RangeMacros.exclusiveMonth('from, 'endExclusive) }

/** Creates an exclusive-end range of days of the week, with compile-time
  * validation for literal endpoints and runtime validation for dynamic
  * endpoints.
  */
extension (inline from: DayOfWeek)
  inline infix def until(inline endExclusive: DayOfWeek): DayOfWeekRange =
    ${ RangeMacros.exclusiveDayOfWeek('from, 'endExclusive) }

/** Creates an exclusive-end range of days of the month, with compile-time
  * validation for literal endpoints and runtime validation for dynamic
  * endpoints.
  */
extension (inline from: DayOfMonth)
  inline infix def until(inline endExclusive: DayOfMonth): DayOfMonthRange =
    ${ RangeMacros.exclusiveDayOfMonth('from, 'endExclusive) }

/** Creates an exclusive-end range of hours, with compile-time validation for
  * literal endpoints and runtime validation for dynamic endpoints.
  */
extension (inline from: Hour)
  inline infix def until(inline endExclusive: Hour): HourRange =
    ${ RangeMacros.exclusiveHour('from, 'endExclusive) }

/** Creates an exclusive-end range of minutes, with compile-time validation for
  * literal endpoints and runtime validation for dynamic endpoints.
  */
extension (inline from: Minute)
  inline infix def until(inline endExclusive: Minute): MinuteRange =
    ${ RangeMacros.exclusiveMinute('from, 'endExclusive) }
