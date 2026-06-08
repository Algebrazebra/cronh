package cronh.domain

import munit.FunSuite

class TermTest extends FunSuite {

  test("Term.Range rejects backwards ranges") {
    intercept[IllegalArgumentException] {
      Term.Range(Minute(5), Minute(1))
    }
  }

  test("Term.Range accepts equal endpoints") {
    val r = Term.Range(Minute(3), Minute(3))
    assertEquals(r.from, Minute(3))
    assertEquals(r.to, Minute(3))
  }
}
