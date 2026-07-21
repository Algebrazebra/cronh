package cronh.dsl

import cronh.domain.fieldTypes.{DayOfMonth, DayOfWeek, Hour, Minute, Month}
import munit.FunSuite

class RangeTest extends FunSuite {

  test("to accepts a range with equal endpoints") {
    assertEquals(Hour(9) to Hour(9), Range(Hour(9), Hour(9)))
  }

  test("to rejects a descending range when it is constructed") {
    val start = Hour(17)
    val end = Hour(9)
    val error = intercept[IllegalArgumentException] {
      start to end
    }

    assertEquals(
      error.getMessage,
      "requirement failed: Start of an inclusive range must be less than or equal to its end."
    )
  }

  test("to rejects descending literal ranges at compile time") {
    val errors = List(
      compileErrors("Month.December to Month.January"),
      compileErrors("DayOfWeek.Sunday to DayOfWeek.Monday"),
      compileErrors("31.st to 1.st"),
      compileErrors("17.h to 9.h"),
      compileErrors("59.min to 0.min")
    )

    errors.foreach { error =>
      assert(
        error.contains(
          "Start of an inclusive"
        ) && error.contains("range must be less than or equal to its end"),
        error
      )
    }
  }

  test("to accepts literal ranges with ascending or equal endpoints") {
    assertEquals(compileErrors("Month.January to Month.December"), "")
    assertEquals(compileErrors("DayOfWeek.Monday to DayOfWeek.Sunday"), "")
    assertEquals(compileErrors("1.st to 31.st"), "")
    assertEquals(compileErrors("9.h to 9.h"), "")
    assertEquals(compileErrors("0.min to 59.min"), "")
  }

  test("until converts exclusive ends for every cron field type") {
    assertEquals(
      Month.January until Month.April,
      Range(Month.January, Month.March)
    )
    assertEquals(
      DayOfWeek.Monday until DayOfWeek.Friday,
      Range(DayOfWeek.Monday, DayOfWeek.Thursday)
    )
    assertEquals(
      DayOfMonth(1) until DayOfMonth(4),
      Range(DayOfMonth(1), DayOfMonth(3))
    )
    assertEquals(Hour(9) until Hour(17), Range(Hour(9), Hour(16)))
    assertEquals(Minute(0) until Minute(30), Range(Minute(0), Minute(29)))
  }

  test("until rejects an empty range") {
    val error = intercept[IllegalArgumentException] {
      Hour(9) until Hour(9)
    }

    assertEquals(
      error.getMessage,
      "requirement failed: Start of an exclusive range must be less than its end."
    )
  }

  test("until rejects a descending range") {
    intercept[IllegalArgumentException] {
      Hour(17) until Hour(9)
    }
  }

  test(
    "a mixed literal and dynamic inclusive range falls back to runtime validation"
  ) {
    val end = Hour(9)

    intercept[IllegalArgumentException] {
      17.h to end
    }
  }
}
