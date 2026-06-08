package cronh.domain

import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class MonthDayTest extends ScalaCheckSuite {

  property("Only valid month-day values can be created") = forAll { (n: Int) =>
    if (n < 1 || n > 31) intercept[IllegalArgumentException](MonthDay(n)): Unit
    else MonthDay(n): Unit
  }
}
