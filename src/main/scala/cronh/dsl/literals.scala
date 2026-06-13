package cronh.dsl

import scala.compiletime.{codeOf, error, requireConst}

import cronh.domain.{Hour, Minute, MonthDay}

extension (inline value: Int) {

  /** This hour of the day, validated at compile time: `9.h` compiles, `24.h` is
    * a compile error. Requires an integer literal; use
    * [[cronh.domain.Hour.apply]] for runtime values.
    */
  inline def h: Hour = {
    requireConst(value)
    inline if (value >= Hour.MinValue && value <= Hour.MaxValue) Hour(value)
    else
      error(
        "Hour must be between " + Hour.MinValue.toString + " and " +
          Hour.MaxValue.toString + ", got " + codeOf(value)
      )
  }

  /** This minute of the hour, validated at compile time: `30.m` compiles,
    * `60.m` is a compile error. Requires an integer literal; use
    * [[cronh.domain.Minute.apply]] for runtime values.
    */
  inline def m: Minute = {
    requireConst(value)
    inline if (value >= Minute.MinValue && value <= Minute.MaxValue)
      Minute(value)
    else
      error(
        "Minute must be between " + Minute.MinValue.toString + " and " +
          Minute.MaxValue.toString + ", got " + codeOf(value)
      )
  }

  /** This day of the month, validated at compile time: `15.dom` compiles,
    * `32.dom` is a compile error. Requires an integer literal; use
    * [[cronh.domain.MonthDay.apply]] for runtime values.
    */
  inline def dom: MonthDay = {
    requireConst(value)
    inline if (value >= MonthDay.MinValue && value <= MonthDay.MaxValue)
      MonthDay(value)
    else
      error(
        "MonthDay must be between " + MonthDay.MinValue.toString + " and " +
          MonthDay.MaxValue.toString + ", got " + codeOf(value)
      )
  }
}
