package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*

class FieldTest extends ScalaCheckSuite {

  private val smallMinuteList: Gen[List[Minute]] =
    Gen
      .chooseNum(0, 8)
      .flatMap(
        Gen.listOfN(_, summon[org.scalacheck.Arbitrary[Minute]].arbitrary)
      )

  test("Field.all is assignable as Field[Minute] via covariance") {
    val f: Field[Minute] = Field.all
    assertEquals(f.terms.head, Term.All)
  }

  property("Field.all has exactly one term, which is Term.All") = {
    val f: Field[Minute] = Field.all
    f.terms.length == 1 && f.terms.head == Term.All
  }

  property("Field.single wraps the value in a single Term.Single") = forAll {
    (m: Minute) =>
      val f = Field.single(m)
      f.terms.length == 1 && f.terms.head == Term.Single(m)
  }

  property("Field.range round-trips for ordered endpoints") = forAll {
    (a: Minute, b: Minute) =>
      val ord = summon[Ordering[Minute]]
      val (lo, hi) = if (ord.lteq(a, b)) (a, b) else (b, a)
      val f = Field.range(lo, hi)
      f.terms.length == 1 && f.terms.head == Term.Range(lo, hi)
  }

  property("Field.range rejects backwards endpoints") =
    forAll(Generators.outOfOrder[Minute]) { case (hi, lo) =>
      intercept[IllegalArgumentException](Field.range(hi, lo)): Unit
    }

  property("Field.of wraps each value in a Term.Single") = forAll(
    summon[org.scalacheck.Arbitrary[Minute]].arbitrary,
    smallMinuteList
  ) { (head, tail) =>
    val f = Field.of(head, tail*)
    val allSingle = f.terms.forall {
      case Term.Single(_) => true
      case _              => false
    }
    f.terms.length == tail.length + 1 && allSingle
  }

  property("Field.from preserves the input term list") = forAll {
    (head: Term[Minute], tail: List[Term[Minute]]) =>
      val f = Field.from(head, tail*)
      f.terms == ::(head, tail)
  }

  property("++ concatenates term lists") = forAll {
    (a: Field[Minute], b: Field[Minute]) =>
      (a ++ b).terms == a.terms ::: b.terms
  }

  property("++ preserves total length") = forAll {
    (a: Field[Minute], b: Field[Minute]) =>
      (a ++ b).terms.length == a.terms.length + b.terms.length
  }

  property("++ is associative") = forAll {
    (a: Field[Minute], b: Field[Minute], c: Field[Minute]) =>
      ((a ++ b) ++ c) == (a ++ (b ++ c))
  }

  test("++ is not commutative") {
    val a = Field.single(Minute(1))
    val b = Field.single(Minute(2))
    assertNotEquals(a ++ b, b ++ a)
  }

  property("Field equality is reflexive") = forAll { (f: Field[Minute]) =>
    f == f
  }

  property("Field.all is not an identity for ++") = forAll {
    (f: Field[Minute]) =>
      val left = (Field.all: Field[Minute]) ++ f
      val right = f ++ (Field.all: Field[Minute])
      left != f && right != f
  }
}
