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
    inline if (value >= 0 && value <= 23) Hour(value)
    else error("Hour must be between 0 and 23, got " + codeOf(value))
  }

  /** This minute of the hour, validated at compile time: `30.m` compiles,
    * `60.m` is a compile error. Requires an integer literal; use
    * [[cronh.domain.Minute.apply]] for runtime values.
    */
  inline def m: Minute = {
    requireConst(value)
    inline if (value >= 0 && value <= 59) Minute(value)
    else error("Minute must be between 0 and 59, got " + codeOf(value))
  }

  /** This day of the month, validated at compile time: `15.dom` compiles,
    * `32.dom` is a compile error. Requires an integer literal; use
    * [[cronh.domain.MonthDay.apply]] for runtime values.
    */
  inline def dom: MonthDay = {
    requireConst(value)
    inline if (value >= 1 && value <= 31) MonthDay(value)
    else error("MonthDay must be between 1 and 31, got " + codeOf(value))
  }
}
