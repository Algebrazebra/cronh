package cronh.dsl

import cronh.domain.{Hour, Minute, MonthDay}
import munit.FunSuite

class LiteralsTest extends FunSuite {

  test("valid literals construct domain values") {
    assertEquals(9.h, Hour(9))
    assertEquals(0.m, Minute(0))
    assertEquals(59.m, Minute(59))
    assertEquals(23.h, Hour(23))
    assertEquals(1.dom, MonthDay(1))
    assertEquals(31.dom, MonthDay(31))
  }

  test("24.h is a compile error naming the valid range") {
    assert(compileErrors("24.h").contains("Hour must be between 0 and 23"))
  }

  test("60.m is a compile error naming the valid range") {
    assert(compileErrors("60.m").contains("Minute must be between 0 and 59"))
  }

  test("0.dom and 32.dom are compile errors naming the valid range") {
    assert(
      compileErrors("0.dom").contains("MonthDay must be between 1 and 31")
    )
    assert(
      compileErrors("32.dom").contains("MonthDay must be between 1 and 31")
    )
  }

  test("negative literals are compile errors") {
    assert(compileErrors("-1.m").nonEmpty)
  }

  test("non-literal arguments are compile errors") {
    assert(compileErrors("val x = 9; x.h").nonEmpty)
  }
}
