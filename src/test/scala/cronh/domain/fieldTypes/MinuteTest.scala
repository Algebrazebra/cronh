package cronh.domain.fieldTypes

import cronh.domain.fieldTypes.{DayOfMonth, Hour, Minute}
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class MinuteTest extends ScalaCheckSuite {

  property("Only valid minute values can be created") = forAll { (n: Int) =>
    if (n < 0 || n >= 60) intercept[IllegalArgumentException](Minute(n)): Unit
    else Minute(n): Unit
  }

  test("Minute(60) is rejected") {
    intercept[IllegalArgumentException](Minute(60))
  }

  test("Minute(-1) is rejected") {
    intercept[IllegalArgumentException](Minute(-1))
  }

  test("cross-unit comparison is false at runtime") {
    // As nominal case classes, units no longer share Int's universal equality,
    // so a same-numeric cross-unit comparison is false rather than true.
    assert(!Minute(5).equals(Hour(5)))
    assert(!Minute(5).equals(DayOfMonth(5)))
    assert(Minute(5) == Minute(5))
  }

}
