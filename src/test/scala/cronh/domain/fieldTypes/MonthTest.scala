package cronh.domain.fieldTypes

import cronh.domain.fieldTypes.Month
import munit.FunSuite

class MonthTest extends FunSuite {

  test("Month enumerates all twelve months") {
    assertEquals(Month.values.length, 12)
  }

  test("Month value matches calendar number") {
    assertEquals(Month.January.value, 1)
    assertEquals(Month.July.value, 7)
    assertEquals(Month.December.value, 12)
  }

  test("Month ordering follows calendar order") {
    val sorted = Month.values.toList.sorted
    assertEquals(sorted, Month.values.toList)
  }
}
