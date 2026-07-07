package cronh.domain

import cronh.domain.fieldTypes.{DayOfWeek, Hour, Minute, Month, DayOfMonth}
import org.scalacheck.{Arbitrary, Gen}

/** Shared ScalaCheck generators for every domain type.
  *
  * Import with `import Generators.given` to bring all `Arbitrary` instances
  * into scope. The non-implicit `outOfOrder` helper is also exposed for
  * properties that need to construct an invalid (descending) pair without
  * relying on filtered `forAll` quantification.
  */
object Generators {

  given Arbitrary[Minute] = Arbitrary(Gen.choose(0, 59).map(Minute(_)))

  given Arbitrary[Hour] = Arbitrary(Gen.choose(0, 23).map(Hour(_)))

  given Arbitrary[DayOfMonth] = Arbitrary(Gen.choose(1, 31).map(DayOfMonth(_)))

  given Arbitrary[Month] = Arbitrary(Gen.oneOf(Month.values.toSeq))

  given Arbitrary[DayOfWeek] = Arbitrary(Gen.oneOf(DayOfWeek.values.toSeq))

  given termArbitrary[A](using
      arb: Arbitrary[A],
      ord: Ordering[A]
  ): Arbitrary[Term[A]] = {
    val singleGen: Gen[Term[A]] = arb.arbitrary.map(Term.Single(_))
    val rangeGen: Gen[Term[A]] = for {
      a <- arb.arbitrary
      b <- arb.arbitrary
    } yield {
      val (lo, hi) = if (ord.lteq(a, b)) (a, b) else (b, a)
      Term.Range(lo, hi)
    }
    Arbitrary(
      Gen.frequency(
        1 -> Gen.const(Term.All),
        4 -> singleGen,
        3 -> rangeGen
      )
    )
  }

  given fieldArbitrary[A](using
      arb: Arbitrary[A],
      ord: Ordering[A]
  ): Arbitrary[Field[A]] = {
    val termArb = summon[Arbitrary[Term[A]]]
    Arbitrary(
      for {
        size <- Gen.chooseNum(1, 8)
        head <- termArb.arbitrary
        tail <- Gen.listOfN(size - 1, termArb.arbitrary)
      } yield Field.from(head, tail*)
    )
  }

  given Arbitrary[CronExpression] = Arbitrary(
    for {
      m <- summon[Arbitrary[Field[Minute]]].arbitrary
      h <- summon[Arbitrary[Field[Hour]]].arbitrary
      d <- summon[Arbitrary[Field[DayOfMonth]]].arbitrary
      mo <- summon[Arbitrary[Field[Month]]].arbitrary
      dow <- summon[Arbitrary[Field[DayOfWeek]]].arbitrary
    } yield CronExpression(m, h, d, mo, dow)
  )

  /** Generator for a strictly descending pair `(hi, lo)` with `hi > lo`. Useful
    * for properties asserting that `Range` rejects backwards inputs.
    */
  def outOfOrder[A](using
      arb: Arbitrary[A],
      ord: Ordering[A]
  ): Gen[(A, A)] =
    for {
      a <- arb.arbitrary
      b <- arb.arbitrary
      if !ord.equiv(a, b)
    } yield if (ord.lt(a, b)) (b, a) else (a, b)
}
