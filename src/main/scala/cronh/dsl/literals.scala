package cronh.dsl

import cronh.domain.{Hour, Minute, MonthDay}

extension (inline value: Int) {

  /** This hour of the day, validated at compile time: `9.h` compiles, `24.h` is
    * a compile error. Requires an integer literal; a non-literal reports a
    * tailored message pointing at [[cronh.domain.Hour.apply]] for runtime
    * values.
    */
  inline def h: Hour = ${ LiteralMacros.hImpl('value) }

  /** This minute of the hour, validated at compile time: `30.m` compiles,
    * `60.m` is a compile error. Requires an integer literal; a non-literal
    * reports a tailored message pointing at [[cronh.domain.Minute.apply]] for
    * runtime values.
    */
  inline def m: Minute = ${ LiteralMacros.mImpl('value) }

  /** This day of the month, validated at compile time: `15.dom` compiles,
    * `32.dom` is a compile error. Requires an integer literal; a non-literal
    * reports a tailored message pointing at [[cronh.domain.MonthDay.apply]] for
    * runtime values.
    */
  inline def dom: MonthDay = ${ LiteralMacros.domImpl('value) }
}
