package cronh.dsl

import cronh.domain.fieldTypes.{DayOfMonth, Hour, Minute}

import scala.quoted.*

extension (inline value: Int) {

  /** This hour of the day, validated at compile time: `9.h` compiles, `24.h` is
    * a compiler error. Requires an integer literal; a non-literal reports a
    * tailored message pointing at [[Hour.apply]] for runtime values.
    */
  inline def h: Hour = ${ LiteralMacros.hImpl('value) }

  /** This minute of the hour, validated at compile time: `30.min` compiles,
    * `60.min` is a compiler error. Requires an integer literal; a non-literal
    * reports a tailored message pointing at [[Minute.apply]] for runtime
    * values.
    */
  inline def min: Minute = ${ LiteralMacros.mImpl('value) }

  /** This day of the month, validated at compile time.
    *
    * Valid days of the month with the correct ordinals compile: `1.st`, `2.nd`,
    * `3.rd`, `4.th`, etc.
    *
    * Invalid days of the month or incorrect ordinals are a compiler error:
    * `32.th`, `1.th`, `4.st`, etc.
    *
    * Requires an integer literal; a non-literal reports a tailored message
    * pointing at [[DayOfMonth.apply]] for runtime values.
    */
  inline def st: DayOfMonth = ${ LiteralMacros.ordinalImpl('value, "st") }

  /** This day of the month, validated at compile time.
    *
    * Valid days of the month with the correct ordinals compile: `1.st`, `2.nd`,
    * `3.rd`, `4.th`, etc.
    *
    * Invalid days of the month or incorrect ordinals are a compiler error:
    * `32.th`, `1.th`, `4.st`, etc.
    *
    * Requires an integer literal; a non-literal reports a tailored message
    * pointing at [[DayOfMonth.apply]] for runtime values.
    */
  inline def nd: DayOfMonth = ${ LiteralMacros.ordinalImpl('value, "nd") }

  /** This day of the month, validated at compile time.
    *
    * Valid days of the month with the correct ordinals compile: `1.st`, `2.nd`,
    * `3.rd`, `4.th`, etc.
    *
    * Invalid days of the month or incorrect ordinals are a compiler error:
    * `32.th`, `1.th`, `4.st`, etc.
    *
    * Requires an integer literal; a non-literal reports a tailored message
    * pointing at [[DayOfMonth.apply]] for runtime values.
    */
  inline def rd: DayOfMonth = ${ LiteralMacros.ordinalImpl('value, "rd") }

  /** This day of the month, validated at compile time.
    *
    * Valid days of the month with the correct ordinals compile: `1.st`, `2.nd`,
    * `3.rd`, `4.th`, etc.
    *
    * Invalid days of the month or incorrect ordinals are a compiler error:
    * `32.th`, `1.th`, `4.st`, etc.
    *
    * Requires an integer literal; a non-literal reports a tailored message
    * pointing at [[DayOfMonth.apply]] for runtime values.
    */
  inline def th: DayOfMonth = ${ LiteralMacros.ordinalImpl('value, "th") }
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

  /** Backs `.st`/`.nd`/`.rd`/`.th`: range-checks and additionally verifies that
    * `suffix` is the grammatically correct English ordinal suffix for the
    * literal value (so `1.th` and `15.rd` fail to compile, distinctly from an
    * out-of-range value).
    */
  def ordinalImpl(expr: Expr[Int], suffix: String)(using
      Quotes
  ): Expr[DayOfMonth] = {
    import quotes.reflect.report
    checked(
      expr,
      DayOfMonth.MinValue,
      DayOfMonth.MaxValue,
      "DayOfMonth",
      suffix
    ) { v =>
      {
        val expected = englishOrdinalSuffix(v)
        if (suffix != expected) {
          report.errorAndAbort(
            s"""$v takes the ordinal suffix $expected, not '$suffix'
               |Hint: Write $v.$expected instead!""".stripMargin
          )
        } else {
          '{ DayOfMonth(${ Expr(v) }) }
        }
      }
    }
  }

  /** Gets the grammatically correct English ordinal suffix for the value v. */
  private def englishOrdinalSuffix(v: Int): String =
    if v % 100 >= 11 && v % 100 <= 13 then "th"
    else
      v % 10 match {
        case 1 => "st"
        case 2 => "nd"
        case 3 => "rd"
        case _ => "th"
      }

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
