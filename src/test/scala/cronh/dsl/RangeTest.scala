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
      "Invalid inclusive range `Hour(17) to Hour(9)`: the start `Hour(17)` " +
        "is after the end `Hour(9)`. Descending ranges are not supported; " +
        "put the earlier value first or list boundary-spanning values " +
        "explicitly."
    )
  }

  test("to rejects descending literal ranges at compile time") {
    val errors = List(
      "Month.December to Month.January" -> compileErrors(
        "Month.December to Month.January"
      ),
      "DayOfWeek.Sunday to DayOfWeek.Monday" -> compileErrors(
        "DayOfWeek.Sunday to DayOfWeek.Monday"
      ),
      "31.st to 1.st" -> compileErrors("31.st to 1.st"),
      "17.h to 9.h" -> compileErrors("17.h to 9.h"),
      "59.min to 0.min" -> compileErrors("59.min to 0.min")
    )

    errors.foreach { case (range, error) =>
      assert(
        error.contains(s"range `$range`") &&
          error.contains("the start") &&
          error.contains("is after the end") &&
          error.contains("Descending ranges are not supported"),
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
    val start = Hour(9)
    val end = Hour(9)
    val error = intercept[IllegalArgumentException] {
      start until end
    }

    assertEquals(
      error.getMessage,
      "Invalid exclusive range `Hour(9) until Hour(9)`: this range is empty " +
        "because `until` excludes `Hour(9)`. Use `Hour(9) to Hour(9)` to " +
        "select that single value, or choose a later exclusive end."
    )
  }

  test("until rejects a descending range") {
    val start = Hour(17)
    val end = Hour(9)
    val error = intercept[IllegalArgumentException] {
      start until end
    }

    assertEquals(
      error.getMessage,
      "Invalid exclusive range `Hour(17) until Hour(9)`: the start `Hour(17)` " +
        "is after the exclusive end `Hour(9)`. Descending ranges are not " +
        "supported; put the earlier value first or list boundary-spanning " +
        "values explicitly."
    )
  }

  test("until rejects empty and descending literal ranges at compile time") {
    val emptyErrors = List(
      "Month.April until Month.April" -> compileErrors(
        "Month.April until Month.April"
      ),
      "4.th until 4.th" -> compileErrors("4.th until 4.th")
    )

    emptyErrors.foreach { case (range, error) =>
      assert(
        error.contains(s"range `$range`") &&
          error.contains("this range is empty") &&
          error.contains("to select that single value"),
        error
      )
    }

    val descendingErrors = List(
      "DayOfWeek.Friday until DayOfWeek.Monday" -> compileErrors(
        "DayOfWeek.Friday until DayOfWeek.Monday"
      ),
      "17.h until 9.h" -> compileErrors("17.h until 9.h"),
      "30.min until 0.min" -> compileErrors("30.min until 0.min")
    )

    descendingErrors.foreach { case (range, error) =>
      assert(
        error.contains(s"range `$range`") &&
          error.contains("is after the exclusive end") &&
          error.contains("Descending ranges are not supported"),
        error
      )
    }
  }

  test(
    "a mixed literal and dynamic exclusive range falls back to runtime validation"
  ) {
    val end = Hour(9)

    intercept[IllegalArgumentException] {
      17.h until end
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
