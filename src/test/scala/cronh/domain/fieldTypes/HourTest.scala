package cronh.domain.fieldTypes

import cronh.domain.fieldTypes.Hour
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class HourTest extends ScalaCheckSuite {

  property("Only valid hour values can be created") = forAll { (n: Int) =>
    if (n < 0 || n >= 24) intercept[IllegalArgumentException](Hour(n)): Unit
    else Hour(n): Unit
  }

  test("Hour(24) is rejected") {
    intercept[IllegalArgumentException](Hour(24))
  }

  test("Hour(-1) is rejected") {
    intercept[IllegalArgumentException](Hour(-1))
  }

}
