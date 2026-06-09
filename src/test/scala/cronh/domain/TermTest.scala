package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class TermTest extends ScalaCheckSuite {

  property("Term.Range accepts ordered endpoints and round-trips") = forAll {
    (a: Minute, b: Minute) =>
      val ord = summon[Ordering[Minute]]
      val (lo, hi) = if (ord.lteq(a, b)) (a, b) else (b, a)
      val r = Term.Range(lo, hi)
      r.from == lo && r.to == hi
  }

  property("Term.Range rejects backwards ranges") =
    forAll(Generators.outOfOrder[Minute]) { case (hi, lo) =>
      intercept[IllegalArgumentException](Term.Range(hi, lo)): Unit
    }

  property("Term.Range with equal endpoints is valid") = forAll { (m: Minute) =>
    val r = Term.Range(m, m)
    r.from == m && r.to == m
  }

  property("Term.Single round-trips its value") = forAll { (m: Minute) =>
    Term.Single(m).value == m
  }

  property("Term.Single equality is structural") = forAll { (m: Minute) =>
    Term.Single(m) == Term.Single(m)
  }

  property("Term.Range equality is structural") = forAll {
    (a: Minute, b: Minute) =>
      val ord = summon[Ordering[Minute]]
      val (lo, hi) = if (ord.lteq(a, b)) (a, b) else (b, a)
      Term.Range(lo, hi) == Term.Range(lo, hi)
  }

  test("Term.All is a singleton") {
    assert(Term.All eq Term.All)
    assertEquals(Term.All: Term[Minute], Term.All: Term[Minute])
  }
}
