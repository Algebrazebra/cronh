package cronh.domain

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class HourTest extends ScalaCheckSuite {

  property("Only valid hour values can be created") = forAll { (n: Int) =>
    if (n < 0 || n >= 24) intercept[IllegalArgumentException](Hour(n)): Unit
    else Hour(n): Unit
  }
}
