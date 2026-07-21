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

  def inclusiveMonth(from: Expr[Month], to: Expr[Month])(using
      Quotes
  ): Expr[MonthRange] =
    inclusive(from, to, monthRank, "month")('{ Range($from, $to) })

  def inclusiveDayOfWeek(from: Expr[DayOfWeek], to: Expr[DayOfWeek])(using
      Quotes
  ): Expr[DayOfWeekRange] =
    inclusive(from, to, dayOfWeekRank, "day-of-week")(
      '{ Range($from, $to) }
    )

  def inclusiveDayOfMonth(from: Expr[DayOfMonth], to: Expr[DayOfMonth])(using
      Quotes
  ): Expr[DayOfMonthRange] =
    inclusive(from, to, dayOfMonthRank, "day-of-month")(
      '{ Range($from, $to) }
    )

  def inclusiveHour(from: Expr[Hour], to: Expr[Hour])(using
      Quotes
  ): Expr[HourRange] =
    inclusive(from, to, hourRank, "hour")('{ Range($from, $to) })

  def inclusiveMinute(from: Expr[Minute], to: Expr[Minute])(using
      Quotes
  ): Expr[MinuteRange] =
    inclusive(from, to, minuteRank, "minute")('{ Range($from, $to) })

  private def inclusive[T: Type](
      from: Expr[T],
      to: Expr[T],
      rank: Expr[T] => Option[Int],
      fieldName: String
  )(runtimeFallback: Expr[Range[T]])(using Quotes): Expr[Range[T]] = {
    rejectIf(
      from,
      to,
      rank,
      (start, end) => start > end,
      s"Start of an inclusive $fieldName range must be less than or equal to its end."
    )
    runtimeFallback
  }

  private def rejectIf[T](
      from: Expr[T],
      to: Expr[T],
      rank: Expr[T] => Option[Int],
      invalid: (Int, Int) => Boolean,
      message: String
  )(using Quotes): Unit =
    (rank(from), rank(to)) match {
      case (Some(start), Some(end)) if invalid(start, end) =>
        quotes.reflect.report.errorAndAbort(message)
      case _ => ()
    }

  private def hourRank(expr: Expr[Hour])(using Quotes): Option[Int] =
    stripInlined(expr) match {
      case '{ Hour(${ Expr(value) }) } => Some(value)
      case _                           => None
    }

  private def minuteRank(expr: Expr[Minute])(using Quotes): Option[Int] =
    stripInlined(expr) match {
      case '{ Minute(${ Expr(value) }) } => Some(value)
      case _                             => None
    }

  private def dayOfMonthRank(expr: Expr[DayOfMonth])(using
      Quotes
  ): Option[Int] =
    stripInlined(expr) match {
      case '{ DayOfMonth(${ Expr(value) }) } => Some(value)
      case _                                 => None
    }

  private def monthRank(expr: Expr[Month])(using Quotes): Option[Int] =
    stripInlined(expr) match {
      case '{ Month.January }   => Some(1)
      case '{ Month.February }  => Some(2)
      case '{ Month.March }     => Some(3)
      case '{ Month.April }     => Some(4)
      case '{ Month.May }       => Some(5)
      case '{ Month.June }      => Some(6)
      case '{ Month.July }      => Some(7)
      case '{ Month.August }    => Some(8)
      case '{ Month.September } => Some(9)
      case '{ Month.October }   => Some(10)
      case '{ Month.November }  => Some(11)
      case '{ Month.December }  => Some(12)
      case _                    => None
    }

  private def dayOfWeekRank(expr: Expr[DayOfWeek])(using
      Quotes
  ): Option[Int] =
    stripInlined(expr) match {
      case '{ DayOfWeek.Monday }    => Some(1)
      case '{ DayOfWeek.Tuesday }   => Some(2)
      case '{ DayOfWeek.Wednesday } => Some(3)
      case '{ DayOfWeek.Thursday }  => Some(4)
      case '{ DayOfWeek.Friday }    => Some(5)
      case '{ DayOfWeek.Saturday }  => Some(6)
      case '{ DayOfWeek.Sunday }    => Some(7)
      case _                        => None
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
