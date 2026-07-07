package cronh.domain.fieldTypes

import cronh.domain.fieldTypes.DayOfMonth
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class DayOfMonthTest extends ScalaCheckSuite {

  property("Only valid month-day values can be created") = forAll { (n: Int) =>
    if (n < 1 || n > 31) intercept[IllegalArgumentException](DayOfMonth(n)): Unit
    else DayOfMonth(n): Unit
  }

  test("MonthDay(0) is rejected") {
    intercept[IllegalArgumentException](DayOfMonth(0))
  }

  test("MonthDay(32) is rejected") {
    intercept[IllegalArgumentException](DayOfMonth(32))
  }
}
