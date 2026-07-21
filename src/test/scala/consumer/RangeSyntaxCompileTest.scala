package consumer

import cronh.domain.fieldTypes.{DayOfWeek, Month}
import cronh.dsl.*
import munit.FunSuite

/** Ensures macro expansions only reference API that is accessible to library
  * consumers outside the `cronh.dsl` package.
  */
class RangeSyntaxCompileTest extends FunSuite {

  test("literal range syntax compiles for an external consumer") {
    assertEquals(
      Month.January to Month.March,
      Range(Month.January, Month.March)
    )
    assertEquals(
      DayOfWeek.Monday until DayOfWeek.Friday,
      Range(DayOfWeek.Monday, DayOfWeek.Thursday)
    )
    assertEquals(9.h until 17.h, Range(9.h, 16.h))
  }
}
