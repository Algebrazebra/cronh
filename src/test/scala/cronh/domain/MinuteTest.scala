package cronh.domain

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

}
