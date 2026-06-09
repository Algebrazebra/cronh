package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class MonthTest extends ScalaCheckSuite {

  private val ord = summon[Ordering[Month]]

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

  property("Month ordering is reflexive") = forAll { (m: Month) =>
    ord.compare(m, m) == 0
  }

  property("Month ordering is antisymmetric") = forAll { (a: Month, b: Month) =>
    ord.compare(a, b).sign == -ord.compare(b, a).sign
  }

  property("Month ordering is transitive") = forAll {
    (a: Month, b: Month, c: Month) =>
      if (ord.lteq(a, b) && ord.lteq(b, c)) ord.lteq(a, c)
      else true
  }

  property("Month ordering agrees with .value") = forAll {
    (a: Month, b: Month) =>
      ord.compare(a, b).sign == a.value.compare(b.value).sign
  }

  property("Month equality is reflexive") = forAll { (m: Month) =>
    m == m
  }
}
