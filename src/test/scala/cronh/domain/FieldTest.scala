package cronh.domain

import munit.FunSuite

class FieldTest extends FunSuite {

  test("Backwards ranges cannot be constructed") {
    intercept[IllegalArgumentException] {
      Field.range(Minute(5), Minute(1))
    }
  }

  test("Field.all is assignable as Field[Minute] via covariance") {
    val f: Field[Minute] = Field.all
    assertEquals(f.terms.head, Term.All)
  }

  test("Field.of produces a non-empty field with one term per value") {
    val f = Field.of(Minute(0), Minute(15), Minute(30))
    assertEquals(f.terms.length, 3)
  }

  test("++ concatenates two fields") {
    val a = Field.single(Minute(5))
    val b = Field.range(Minute(10), Minute(20))
    val combined = a ++ b
    assertEquals(combined.terms.length, 2)
  }

  test("++ is associative") {
    val a = Field.single(Minute(1))
    val b = Field.single(Minute(2))
    val c = Field.single(Minute(3))
    assertEquals((a ++ b) ++ c, a ++ (b ++ c))
  }
}
