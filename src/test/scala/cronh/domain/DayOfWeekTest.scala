package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class DayOfWeekTest extends ScalaCheckSuite {

  private val ord = summon[Ordering[DayOfWeek]]

  test("DayOfWeek enumerates all seven days") {
    assertEquals(DayOfWeek.values.length, 7)
  }

  test("DayOfWeek ordering starts at Monday and ends at Sunday") {
    val sorted = DayOfWeek.values.toList.sorted
    assertEquals(sorted.head, DayOfWeek.Monday)
    assertEquals(sorted.last, DayOfWeek.Sunday)
  }

  property("DayOfWeek ordering is reflexive") = forAll { (d: DayOfWeek) =>
    ord.compare(d, d) == 0
  }

  property("DayOfWeek ordering is antisymmetric") = forAll {
    (a: DayOfWeek, b: DayOfWeek) =>
      ord.compare(a, b).sign == -ord.compare(b, a).sign
  }

  property("DayOfWeek ordering is transitive") = forAll {
    (a: DayOfWeek, b: DayOfWeek, c: DayOfWeek) =>
      if (ord.lteq(a, b) && ord.lteq(b, c)) ord.lteq(a, c)
      else true
  }

  property("DayOfWeek ordering agrees with .ordinal") = forAll {
    (a: DayOfWeek, b: DayOfWeek) =>
      ord.compare(a, b).sign == a.ordinal.compare(b.ordinal).sign
  }

  property("DayOfWeek equality is reflexive") = forAll { (d: DayOfWeek) =>
    d == d
  }
}
