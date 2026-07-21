package cronh.dsl

import cronh.domain.fieldTypes.{DayOfMonth, DayOfWeek, Hour, Minute, Month}

import scala.quoted.*

/** Macro implementations backing compile-time validation of literal ranges.
  *
  * A range is rejected at compile time only when both endpoints can be reduced
  * to known cron-domain values. If either endpoint is dynamic, the generated
  * expression delegates to the normal runtime-validated constructors.
  */
private object RangeMacros {

  private final case class LiteralEndpoint(rank: Int, source: String)

  def inclusiveMonth(from: Expr[Month], to: Expr[Month])(using
      Quotes
  ): Expr[MonthRange] =
    inclusive(from, to, monthEndpoint, "month")('{ Range($from, $to) })

  def inclusiveDayOfWeek(from: Expr[DayOfWeek], to: Expr[DayOfWeek])(using
      Quotes
  ): Expr[DayOfWeekRange] =
    inclusive(from, to, dayOfWeekEndpoint, "day-of-week")(
      '{ Range($from, $to) }
    )

  def inclusiveDayOfMonth(from: Expr[DayOfMonth], to: Expr[DayOfMonth])(using
      Quotes
  ): Expr[DayOfMonthRange] =
    inclusive(from, to, dayOfMonthEndpoint, "day-of-month")(
      '{ Range($from, $to) }
    )

  def inclusiveHour(from: Expr[Hour], to: Expr[Hour])(using
      Quotes
  ): Expr[HourRange] =
    inclusive(from, to, hourEndpoint, "hour")('{ Range($from, $to) })

  def inclusiveMinute(from: Expr[Minute], to: Expr[Minute])(using
      Quotes
  ): Expr[MinuteRange] =
    inclusive(from, to, minuteEndpoint, "minute")('{ Range($from, $to) })

  def exclusiveMonth(from: Expr[Month], endExclusive: Expr[Month])(using
      Quotes
  ): Expr[MonthRange] =
    exclusive(from, endExclusive, monthEndpoint, "month")(
      '{ Range.exclusive($from, $endExclusive) }
    )

  def exclusiveDayOfWeek(
      from: Expr[DayOfWeek],
      endExclusive: Expr[DayOfWeek]
  )(using Quotes): Expr[DayOfWeekRange] =
    exclusive(from, endExclusive, dayOfWeekEndpoint, "day-of-week")(
      '{ Range.exclusive($from, $endExclusive) }
    )

  def exclusiveDayOfMonth(
      from: Expr[DayOfMonth],
      endExclusive: Expr[DayOfMonth]
  )(using Quotes): Expr[DayOfMonthRange] =
    exclusive(from, endExclusive, dayOfMonthEndpoint, "day-of-month")(
      '{ Range.exclusive($from, $endExclusive) }
    )

  def exclusiveHour(from: Expr[Hour], endExclusive: Expr[Hour])(using
      Quotes
  ): Expr[HourRange] =
    exclusive(from, endExclusive, hourEndpoint, "hour")(
      '{ Range.exclusive($from, $endExclusive) }
    )

  def exclusiveMinute(from: Expr[Minute], endExclusive: Expr[Minute])(using
      Quotes
  ): Expr[MinuteRange] =
    exclusive(from, endExclusive, minuteEndpoint, "minute")(
      '{ Range.exclusive($from, $endExclusive) }
    )

  private def inclusive[T: Type](
      from: Expr[T],
      to: Expr[T],
      endpoint: Expr[T] => Option[LiteralEndpoint],
      fieldName: String
  )(runtimeFallback: Expr[Range[T]])(using Quotes): Expr[Range[T]] = {
    (endpoint(from), endpoint(to)) match {
      case (Some(start), Some(end)) if start.rank > end.rank =>
        quotes.reflect.report.errorAndAbort(
          s"Invalid inclusive $fieldName range `${start.source} to ${end.source}`: " +
            s"the start `${start.source}` is after the end `${end.source}`. " +
            "Descending ranges are not supported; put the earlier value " +
            "first or list boundary-spanning values explicitly.",
          quotes.reflect.Position.ofMacroExpansion
        )
      case _ => ()
    }
    runtimeFallback
  }

  private def exclusive[T: Type](
      from: Expr[T],
      endExclusive: Expr[T],
      endpoint: Expr[T] => Option[LiteralEndpoint],
      fieldName: String
  )(runtimeFallback: Expr[Range[T]])(using Quotes): Expr[Range[T]] = {
    (endpoint(from), endpoint(endExclusive)) match {
      case (Some(start), Some(end)) if start.rank == end.rank =>
        quotes.reflect.report.errorAndAbort(
          s"Invalid exclusive $fieldName range `${start.source} until ${end.source}`: " +
            s"this range is empty because `until` excludes `${end.source}`. " +
            s"Use `${start.source} to ${end.source}` to select that single value, " +
            "or choose a later exclusive end.",
          quotes.reflect.Position.ofMacroExpansion
        )
      case (Some(start), Some(end)) if start.rank > end.rank =>
        quotes.reflect.report.errorAndAbort(
          s"Invalid exclusive $fieldName range `${start.source} until ${end.source}`: " +
            s"the start `${start.source}` is after the exclusive end `${end.source}`. " +
            "Descending ranges are not supported; put the earlier value " +
            "first or list boundary-spanning values explicitly.",
          quotes.reflect.Position.ofMacroExpansion
        )
      case _ => ()
    }
    runtimeFallback
  }

  private def hourEndpoint(expr: Expr[Hour])(using
      Quotes
  ): Option[LiteralEndpoint] =
    stripInlined(expr) match {
      case '{ Hour(${ Expr(value) }) } =>
        Some(LiteralEndpoint(value, s"$value.h"))
      case _ => None
    }

  private def minuteEndpoint(expr: Expr[Minute])(using
      Quotes
  ): Option[LiteralEndpoint] =
    stripInlined(expr) match {
      case '{ Minute(${ Expr(value) }) } =>
        Some(LiteralEndpoint(value, s"$value.min"))
      case _ => None
    }

  private def dayOfMonthEndpoint(expr: Expr[DayOfMonth])(using
      Quotes
  ): Option[LiteralEndpoint] =
    stripInlined(expr) match {
      case '{ DayOfMonth(${ Expr(value) }) } =>
        Some(LiteralEndpoint(value, s"$value.${ordinalSuffix(value)}"))
      case _ => None
    }

  private def monthEndpoint(expr: Expr[Month])(using
      Quotes
  ): Option[LiteralEndpoint] =
    stripInlined(expr) match {
      case '{ Month.January }   => Some(LiteralEndpoint(1, "Month.January"))
      case '{ Month.February }  => Some(LiteralEndpoint(2, "Month.February"))
      case '{ Month.March }     => Some(LiteralEndpoint(3, "Month.March"))
      case '{ Month.April }     => Some(LiteralEndpoint(4, "Month.April"))
      case '{ Month.May }       => Some(LiteralEndpoint(5, "Month.May"))
      case '{ Month.June }      => Some(LiteralEndpoint(6, "Month.June"))
      case '{ Month.July }      => Some(LiteralEndpoint(7, "Month.July"))
      case '{ Month.August }    => Some(LiteralEndpoint(8, "Month.August"))
      case '{ Month.September } =>
        Some(LiteralEndpoint(9, "Month.September"))
      case '{ Month.October }  => Some(LiteralEndpoint(10, "Month.October"))
      case '{ Month.November } => Some(LiteralEndpoint(11, "Month.November"))
      case '{ Month.December } => Some(LiteralEndpoint(12, "Month.December"))
      case _                   => None
    }

  private def dayOfWeekEndpoint(expr: Expr[DayOfWeek])(using
      Quotes
  ): Option[LiteralEndpoint] =
    stripInlined(expr) match {
      case '{ DayOfWeek.Monday } =>
        Some(LiteralEndpoint(1, "DayOfWeek.Monday"))
      case '{ DayOfWeek.Tuesday } =>
        Some(LiteralEndpoint(2, "DayOfWeek.Tuesday"))
      case '{ DayOfWeek.Wednesday } =>
        Some(LiteralEndpoint(3, "DayOfWeek.Wednesday"))
      case '{ DayOfWeek.Thursday } =>
        Some(LiteralEndpoint(4, "DayOfWeek.Thursday"))
      case '{ DayOfWeek.Friday } =>
        Some(LiteralEndpoint(5, "DayOfWeek.Friday"))
      case '{ DayOfWeek.Saturday } =>
        Some(LiteralEndpoint(6, "DayOfWeek.Saturday"))
      case '{ DayOfWeek.Sunday } =>
        Some(LiteralEndpoint(7, "DayOfWeek.Sunday"))
      case _ => None
    }

  private def ordinalSuffix(value: Int): String =
    if value % 100 >= 11 && value % 100 <= 13 then "th"
    else
      value % 10 match {
        case 1 => "st"
        case 2 => "nd"
        case 3 => "rd"
        case _ => "th"
      }

  private def stripInlined[T: Type](expr: Expr[T])(using Quotes): Expr[T] = {
    import quotes.reflect.*

    def loop(term: Term): Term = term match {
      case Inlined(_, _, inner) => loop(inner)
      case Typed(inner, _)      => loop(inner)
      case other                => other
    }

    loop(expr.asTerm).asExprOf[T]
  }
}
