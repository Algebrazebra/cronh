package cronh.domain

import cronh.domain.Generators.given
import cronh.domain.fieldTypes.{DayOfWeek, DomainBounds, Hour, Minute, Month}
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class NormalizationTest extends ScalaCheckSuite {

  private def minutes(values: Int*): List[Minute] = values.toList.map(Minute(_))

  /** The set of domain values a field matches, by brute-force expansion. */
  private def covered[A](field: Field[A])(using
      ord: Ordering[A],
      bounds: DomainBounds[A]
  ): Set[A] =
    field.terms.flatMap {
      case Term.All             => bounds.domain
      case Term.Single(value)   => value :: Nil
      case Term.Range(from, to) =>
        bounds.domain.filter(v => ord.gteq(v, from) && ord.lteq(v, to))
    }.toSet

  test("a field containing All collapses to Field.all") {
    val field = Field.from(Term.All, Term.Single(Minute(1)))
    assertEquals(field.normalized, Field.all: Field[Minute])
  }

  test("overlapping ranges merge: 1-5,3-7 becomes 1-7") {
    val field =
      Field.range(Minute(1), Minute(5)) ++ Field.range(Minute(3), Minute(7))
    assertEquals(field.normalized, Field.range(Minute(1), Minute(7)))
  }

  test("a value inside a range is absorbed: 1-5,3 becomes 1-5") {
    val field = Field.range(Minute(1), Minute(5)) ++ Field.single(Minute(3))
    assertEquals(field.normalized, Field.range(Minute(1), Minute(5)))
  }

  test("duplicates collapse: 1,1 becomes 1") {
    val field = Field.of(Minute(1), Minute(1))
    assertEquals(field.normalized, Field.single(Minute(1)))
  }

  test("a tautological full span collapses to Field.all") {
    assertEquals(
      Field.range(Minute(0), Minute(59)).normalized,
      Field.all: Field[Minute]
    )
    assertEquals(
      Field.range(Hour(0), Hour(23)).normalized,
      Field.all: Field[Hour]
    )
    assertEquals(
      Field.range(Month.January, Month.December).normalized,
      Field.all: Field[Month]
    )
  }

  test("a degenerate range collapses to a single: 5-5 becomes 5") {
    assertEquals(
      Field.range(Minute(5), Minute(5)).normalized,
      Field.single(Minute(5))
    )
  }

  test("adjacent runs merge: 1-3,4-6 becomes 1-6") {
    val field =
      Field.range(Minute(1), Minute(3)) ++ Field.range(Minute(4), Minute(6))
    assertEquals(field.normalized, Field.range(Minute(1), Minute(6)))
  }

  test("output is in ascending order") {
    val field = Field.of(Minute(30), Minute(5))
    assertEquals(field.normalized, Field.of(Minute(5), Minute(30)))
  }

  test("isNormalized distinguishes canonical from redundant fields") {
    assert(Field.of(Minute(5), Minute(30)).isNormalized)
    assert(!Field.of(Minute(1), Minute(1)).isNormalized)
    assert(!Field.from[Minute](Term.All, Term.Single(Minute(1))).isNormalized)
  }

  property("normalization is idempotent") = forAll { (field: Field[Minute]) =>
    field.normalized.normalized == field.normalized
  }

  property("normalization preserves the covered value set") = forAll {
    (field: Field[Minute]) =>
      covered(field.normalized) == covered(field)
  }

  property("normalized fields satisfy isNormalized") = forAll {
    (field: Field[Hour]) =>
      field.normalized.isNormalized
  }

  property("normalization preserves semantics for weekdays") = forAll {
    (field: Field[DayOfWeek]) =>
      covered(field.normalized) == covered(field)
  }

  test("normalization keeps non-redundant mixed terms minimal") {
    val field = Field.from(
      Term.Single(Minute(15)),
      Term.Range(Minute(1), Minute(5)),
      Term.Single(Minute(7))
    )
    val expected = Field.from(
      Term.Range(Minute(1), Minute(5)),
      Term.Single(Minute(7)),
      Term.Single(Minute(15))
    )
    assertEquals(field.normalized, expected)
    assertEquals(minutes(1, 2, 3, 4, 5, 7, 15).toSet, covered(field))
  }
}
