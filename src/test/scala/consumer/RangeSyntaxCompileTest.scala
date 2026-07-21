package consumer

import cronh.domain.fieldTypes.Month
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
  }
}
