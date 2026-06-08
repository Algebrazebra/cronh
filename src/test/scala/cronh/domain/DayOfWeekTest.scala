package cronh.domain

import munit.FunSuite

class DayOfWeekTest extends FunSuite {

  test("DayOfWeek enumerates all seven days") {
    assertEquals(DayOfWeek.values.length, 7)
  }

  test("DayOfWeek ordering starts at Monday and ends at Sunday") {
    val sorted = DayOfWeek.values.toList.sorted
    assertEquals(sorted.head, DayOfWeek.Monday)
    assertEquals(sorted.last, DayOfWeek.Sunday)
  }

  test("DayOfWeek ordering is consistent with declaration order") {
    val ord = summon[Ordering[DayOfWeek]]
    assert(ord.lt(DayOfWeek.Monday, DayOfWeek.Friday))
    assert(ord.gt(DayOfWeek.Sunday, DayOfWeek.Saturday))
  }
}
