package cronh.dsl

import cronh.domain.fieldTypes.Month
import cronh.render.{humanReadable, toCron}
import munit.FunSuite

/** Mirrors the expressions in `examples/Schedules.scala` and the README so the
  * expected strings documented there stay honest. When editing an example or a
  * README snippet, update the matching assertion here (and vice versa).
  */
class ExamplesTest extends FunSuite {

  test("nightly backup at 2:30 AM") {
    assertEquals(Schedule.daily.at(2.h, 30.min).toCron, "30 2 * * *")
  }

  test("morning report on weekdays at 9 AM") {
    assertEquals(Schedule.weekdays.at(9.h, 0.min).toCron, "0 9 * * 1-5")
    assertEquals(
      Schedule.weekdays.at(9.h, 0.min).humanReadable,
      "At 9:00 AM, on weekdays"
    )
  }

  test("stand-up reminder on Monday and Friday at noon") {
    assertEquals(Schedule.on(Mon, Fri).at(12.h, 0.min).toCron, "0 12 * * 1,5")
  }

  test("payroll on the 1st and 15th at 6 AM") {
    assertEquals(
      Schedule.onThe(1.st, 15.th).at(6.h, 0.min).toCron,
      "0 6 1,15 * *"
    )
  }

  test("weekend batch job at 8 AM") {
    assertEquals(Schedule.weekends.at(8.h, 0.min).toCron, "0 8 * * 6,0")
  }

  test("summer newsletter in June and July, daily at 9 AM") {
    assertEquals(
      Schedule.in(Month.June, Month.July).daily.at(9.h, 0.min).toCron,
      "0 9 * 6,7 *"
    )
  }

  test("deadline reminder on June 15th at 9 AM") {
    assertEquals(
      Schedule.in(Month.June).onThe(15.th).at(9.h, 0.min).toCron,
      "0 9 15 6 *"
    )
  }

  test("cleanup on the 1st of the month or any Friday at 4:30 AM") {
    assertEquals(
      Schedule.onThe(1.st).orOn(Fri).at(4.h, 30.min).toCron,
      "30 4 1 * 5"
    )
    assertEquals(
      Schedule.onThe(1.st).orOn(Fri).at(4.h, 30.min).humanReadable,
      "At 4:30 AM, on day 1 of the month or on Friday"
    )
  }

  test("documented compile errors do not compile") {
    assert(compileErrors("Schedule.at(9.h, 0.min)").nonEmpty)
    assert(
      compileErrors("Schedule.daily.at(9.h, 0.min).at(14.h, 0.min)").nonEmpty
    )
    assert(compileErrors("Schedule.on(Mon).onThe(1.dom)").nonEmpty)
    assert(compileErrors("Schedule.daily.in(Month.June)").nonEmpty)
  }

  // Positive control: prove each snippet above fails because of the phase types,
  // not because a name (Schedule, .at, .daily, .on, Mon, .dom, Month) went out of
  // scope. If a rename broke resolution instead, these would start reporting
  // errors while the `.nonEmpty` assertions above stayed green and stopped
  // testing anything. (Mirrors the scope check the deleted PhantomTest had.)
  test("compile-error snippets fail on the phase types, not lost scope") {
    assertEquals(compileErrors("Schedule.daily.at(9.h, 0.min)"), "")
    assertEquals(compileErrors("Schedule.on(Mon)"), "")
    assertEquals(compileErrors("Schedule.onThe(1.st)"), "")
    assertEquals(compileErrors("Schedule.in(Month.June).daily"), "")
  }
}
