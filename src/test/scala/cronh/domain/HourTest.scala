package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*

class HourTest extends ScalaCheckSuite {

  private val ord = summon[Ordering[Hour]]

  property("Only valid hour values can be created") = forAll { (n: Int) =>
    if (n < 0 || n >= 24) intercept[IllegalArgumentException](Hour(n)): Unit
    else Hour(n): Unit
  }

  property("Generated Hour stays within [0, 23]") = forAll { (h: Hour) =>
    ord.lteq(Hour(0), h) && ord.lteq(h, Hour(23))
  }

  property("Ordering is reflexive") = forAll { (h: Hour) =>
    ord.compare(h, h) == 0
  }

  property("Ordering is antisymmetric") = forAll { (a: Hour, b: Hour) =>
    ord.compare(a, b).sign == -ord.compare(b, a).sign
  }

  property("Ordering is transitive") = forAll { (a: Hour, b: Hour, c: Hour) =>
    if (ord.lteq(a, b) && ord.lteq(b, c)) ord.lteq(a, c)
    else true
  }

  property("Equality is reflexive") = forAll { (h: Hour) =>
    h == h
  }

  property("Same int yields equal Hour") = forAll(Gen.choose(0, 23)) { n =>
    Hour(n) == Hour(n)
  }
}
