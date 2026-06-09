package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop._

class MinuteTest extends ScalaCheckSuite {

  private val ord = summon[Ordering[Minute]]

  property("Only valid minute values can be created") = forAll { (n: Int) =>
    if (n < 0 || n >= 60) intercept[IllegalArgumentException](Minute(n)): Unit
    else Minute(n): Unit
  }

  property("Generated Minute stays within [0, 59]") = forAll { (m: Minute) =>
    ord.lteq(Minute(0), m) && ord.lteq(m, Minute(59))
  }

  property("Ordering is reflexive") = forAll { (m: Minute) =>
    ord.compare(m, m) == 0
  }

  property("Ordering is antisymmetric") = forAll { (a: Minute, b: Minute) =>
    ord.compare(a, b).sign == -ord.compare(b, a).sign
  }

  property("Ordering is transitive") = forAll {
    (a: Minute, b: Minute, c: Minute) =>
      if (ord.lteq(a, b) && ord.lteq(b, c)) ord.lteq(a, c)
      else true
  }

  property("Equality is reflexive") = forAll { (m: Minute) =>
    m == m
  }

  property("Same int yields equal Minute") = forAll(Gen.choose(0, 59)) { n =>
    Minute(n) == Minute(n)
  }
}
