package cronh.dsl

import cronh.domain.fieldTypes.{Hour, Minute, DayOfMonth}
import munit.FunSuite

class LiteralsTest extends FunSuite {

  test("valid literals construct domain values") {
    assertEquals(9.h, Hour(9))
    assertEquals(0.min, Minute(0))
    assertEquals(59.min, Minute(59))
    assertEquals(23.h, Hour(23))
    assertEquals(1.dom, DayOfMonth(1))
    assertEquals(31.dom, DayOfMonth(31))
  }

  test("24.h is a compile error naming the valid range") {
    assert(compileErrors("24.h").contains("Hour must be between 0 and 23"))
  }

  test("60.min is a compile error naming the valid range") {
    assert(compileErrors("60.min").contains("Minute must be between 0 and 59"))
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
    assert(compileErrors("-1.min").nonEmpty)
  }

  test("non-literal arguments fail with a friendly message") {
    val errors = compileErrors("val x = 9; x.h")
    assert(errors.contains("requires an integer literal"), errors)
    assert(!errors.contains("inline if"), errors)
  }
}
