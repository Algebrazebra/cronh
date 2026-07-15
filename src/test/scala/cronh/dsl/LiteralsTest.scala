package cronh.dsl

import cronh.domain.fieldTypes.{Hour, Minute, DayOfMonth}
import munit.FunSuite

class LiteralsTest extends FunSuite {

  test("valid literals construct domain values") {
    assertEquals(9.h, Hour(9))
    assertEquals(0.min, Minute(0))
    assertEquals(59.min, Minute(59))
    assertEquals(23.h, Hour(23))
    assertEquals(1.st, DayOfMonth(1))
    assertEquals(2.nd, DayOfMonth(2))
    assertEquals(3.rd, DayOfMonth(3))
    assertEquals(4.th, DayOfMonth(4))
    assertEquals(31.st, DayOfMonth(31))
  }

  test("ordinals are correctly handled for the teens 11, 12, 13") {
    assertEquals(11.th, DayOfMonth(11))
    assertEquals(12.th, DayOfMonth(12))
    assertEquals(13.th, DayOfMonth(13))
  }

  test("invalid ordinals fail with a friendly message") {
    val errors = compileErrors("1.nd")
    assert(errors.contains("takes the ordinal suffix"), errors)
    assert(!errors.contains("inline if"), errors)

  }

  test("24.h is a compile error naming the valid range") {
    assert(compileErrors("24.h").contains("Hour must be between 0 and 23"))
  }

  test("60.min is a compile error naming the valid range") {
    assert(compileErrors("60.min").contains("Minute must be between 0 and 59"))
  }

  test("0.th and 32.nd are compile errors naming the valid range") {
    assert(
      compileErrors("0.th").contains("MonthDay must be between 1 and 31")
    )
    assert(
      compileErrors("32.nd").contains("MonthDay must be between 1 and 31")
    )
  }

  test("negative literals are compile errors") {
    assert(compileErrors("-1.h").nonEmpty)
    assert(compileErrors("-1.min").nonEmpty)
    assert(compileErrors("-1.st").nonEmpty)
    assert(compileErrors("-2.nd").nonEmpty)
    assert(compileErrors("-3.rd").nonEmpty)
    assert(compileErrors("-4.th").nonEmpty)
  }

  test("non-literal arguments fail with a friendly message") {
    val errors = compileErrors("val x = 9; x.h")
    assert(errors.contains("requires an integer literal"), errors)
    assert(!errors.contains("inline if"), errors)
  }
}
