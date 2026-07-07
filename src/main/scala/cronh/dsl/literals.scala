package cronh.dsl

import cronh.domain.fieldTypes.{Hour, Minute, DayOfMonth}

import scala.quoted.*

extension (inline value: Int) {

  /** This hour of the day, validated at compile time: `9.h` compiles, `24.h` is
    * a compiler error. Requires an integer literal; a non-literal reports a
    * tailored message pointing at [[Hour.apply]] for runtime values.
    */
  inline def h: Hour = ${ LiteralMacros.hImpl('value) }

  /** This minute of the hour, validated at compile time: `30.m` compiles,
    * `60.m` is a compiler error. Requires an integer literal; a non-literal
    * reports a tailored message pointing at [[Minute.apply]] for runtime
    * values.
    */
  inline def min: Minute = ${ LiteralMacros.mImpl('value) }

  /** This day of the month, validated at compile time: `15.dom` compiles,
    * `32.dom` is a compiler error. Requires an integer literal; a non-literal
    * reports a tailored message pointing at [[DayOfMonth.apply]] for runtime
    * values.
    */
  inline def dom: DayOfMonth = ${ LiteralMacros.domImpl('value) }
}

/** Macro implementations backing the compile-time literal extensions.
  *
  * Inspecting the argument as an [[scala.quoted.Expr]] lets us distinguish a
  * non-literal (`Expr.value` is `None`) from an out-of-range literal and emit a
  * tailored message for each — rather than leaking the compiler-internal
  * "Cannot reduce `inline if`" that an `inline if` over a non-constant
  * produces.
  */
private object LiteralMacros {

  def hImpl(expr: Expr[Int])(using Quotes): Expr[Hour] =
    checked(expr, Hour.MinValue, Hour.MaxValue, "Hour", "h")(v =>
      '{ Hour(${ Expr(v) }) }
    )

  def mImpl(expr: Expr[Int])(using Quotes): Expr[Minute] =
    checked(expr, Minute.MinValue, Minute.MaxValue, "Minute", "m")(v =>
      '{ Minute(${ Expr(v) }) }
    )

  def domImpl(expr: Expr[Int])(using Quotes): Expr[DayOfMonth] =
    checked(expr, DayOfMonth.MinValue, DayOfMonth.MaxValue, "MonthDay", "dom")(
      v => '{ DayOfMonth(${ Expr(v) }) }
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
