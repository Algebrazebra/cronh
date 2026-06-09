package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*

class MonthDayTest extends ScalaCheckSuite {

  private val ord = summon[Ordering[MonthDay]]

  property("Only valid month-day values can be created") = forAll { (n: Int) =>
    if (n < 1 || n > 31) intercept[IllegalArgumentException](MonthDay(n)): Unit
    else MonthDay(n): Unit
  }

  property("Generated MonthDay stays within [1, 31]") = forAll {
    (d: MonthDay) =>
      ord.lteq(MonthDay(1), d) && ord.lteq(d, MonthDay(31))
  }

  property("Ordering is reflexive") = forAll { (d: MonthDay) =>
    ord.compare(d, d) == 0
  }

  property("Ordering is antisymmetric") = forAll { (a: MonthDay, b: MonthDay) =>
    ord.compare(a, b).sign == -ord.compare(b, a).sign
  }

  property("Ordering is transitive") = forAll {
    (a: MonthDay, b: MonthDay, c: MonthDay) =>
      if (ord.lteq(a, b) && ord.lteq(b, c)) ord.lteq(a, c)
      else true
  }

  property("Equality is reflexive") = forAll { (d: MonthDay) =>
    d == d
  }

  property("Same int yields equal MonthDay") = forAll(Gen.choose(1, 31)) { n =>
    MonthDay(n) == MonthDay(n)
  }
}
