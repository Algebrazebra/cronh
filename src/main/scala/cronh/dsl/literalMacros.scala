package cronh.dsl

import scala.quoted.*

import cronh.domain.{Hour, Minute, MonthDay}

/** Macro implementations backing the compile-time literal extensions in
  * `literals.scala`.
  *
  * Inspecting the argument as an [[scala.quoted.Expr]] lets us distinguish a
  * non-literal (`Expr.value` is `None`) from an out-of-range literal and emit a
  * tailored message for each — rather than leaking the compiler-internal
  * "Cannot reduce `inline if`" that an `inline if` over a non-constant
  * produces.
  */
private[dsl] object LiteralMacros {

  def hImpl(expr: Expr[Int])(using Quotes): Expr[Hour] =
    checked(expr, Hour.MinValue, Hour.MaxValue, "Hour", "h")(v =>
      '{ Hour(${ Expr(v) }) }
    )

  def mImpl(expr: Expr[Int])(using Quotes): Expr[Minute] =
    checked(expr, Minute.MinValue, Minute.MaxValue, "Minute", "m")(v =>
      '{ Minute(${ Expr(v) }) }
    )

  def domImpl(expr: Expr[Int])(using Quotes): Expr[MonthDay] =
    checked(expr, MonthDay.MinValue, MonthDay.MaxValue, "MonthDay", "dom")(v =>
      '{ MonthDay(${ Expr(v) }) }
    )

  private def checked[A](
      expr: Expr[Int],
      lo: Int,
      hi: Int,
      unit: String,
      method: String
  )(build: Int => Expr[A])(using Quotes): Expr[A] = {
    import quotes.reflect.report
    expr.value match {
      case Some(v) if v >= lo && v <= hi => build(v)
      case Some(v)                       =>
        report.errorAndAbort(s"$unit must be between $lo and $hi, got $v")
      case None =>
        report.errorAndAbort(
          s"`.$method` requires an integer literal; " +
            s"use $unit.apply(...) for runtime values"
        )
    }
  }
}
