package cronh.domain

import cronh.domain.Generators.given
import cronh.domain.fieldTypes.Minute
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
}
