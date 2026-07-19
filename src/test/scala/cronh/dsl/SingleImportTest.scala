package cronh.dsl

// DO NOT import any project files other than `import cronh.dsl.*`
// ONLY non-project imports (e.g., test library imports) are allowed.
import munit.FunSuite

/** Guards the public DSL's promised single-import experience from outside the
  * `cronh.dsl` package.
  */
class SingleImportTest extends FunSuite {

  test("one DSL import exposes aliases, ranges, Schedule, and literals") {
    import cronh.dsl.*
    val schedule =
      Schedule
        .in(January to March)
        .onThe(1.st to 3.rd)
        .orOn(Mondays to Fridays)
        .at(9.h to 17.h)
        .at(0.min to 30.min)

    assert(schedule != null)
    assertEquals(Noon.toString, "12:00")
    assertEquals(Midnight.toString, "00:00")
    assertEquals(CQ1.from, Jan)
    assertEquals(Weekdays.from, Mon)
  }
}
