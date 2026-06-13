package cronh.dsl

import munit.FunSuite

class PhantomTest extends FunSuite {

  test("control: a valid chain type-checks inside compileErrors") {
    assertEquals(compileErrors("Schedule.daily.at(9.h)"), "")
  }

  test("setting the time twice is a compile error") {
    assert(compileErrors("Schedule.daily.at(9.h).at(14.h)").nonEmpty)
  }

  test("setting the time twice after between is a compile error") {
    assert(
      compileErrors(
        "Schedule.daily.between(9.h, 17.h).at(30.m).at(45.m)"
      ).nonEmpty
    )
  }

  test("between after .at is a compile error") {
    assert(compileErrors("Schedule.daily.at(9.h).between(9.h, 17.h)").nonEmpty)
  }

  test(".on after .onDay is a compile error") {
    assert(compileErrors("Schedule.daily.onDay(1.dom).on(Mon)").nonEmpty)
  }

  test(".onDay after .on is a compile error") {
    assert(compileErrors("Schedule.daily.on(Mon).onDay(1.dom)").nonEmpty)
  }

  test(".on after Schedule.weekdays is a compile error") {
    assert(compileErrors("Schedule.weekdays.on(Mon)").nonEmpty)
  }

  test(".onDay after Schedule.monthly is a compile error") {
    assert(compileErrors("Schedule.monthly.onDay(15.dom)").nonEmpty)
  }

  test(".in twice is a compile error (no silent overwrite)") {
    assert(
      compileErrors(
        "Schedule.daily.at(9.h).in(cronh.domain.Month.June)" +
          ".in(cronh.domain.Month.July)"
      ).nonEmpty
    )
  }

  test(".in after Schedule.yearly is a compile error") {
    assert(
      compileErrors("Schedule.yearly.in(cronh.domain.Month.March)").nonEmpty
    )
  }

  test("valid chains compile and run") {
    val a = Schedule.daily.at(9.h)
    val b = Schedule.daily.on(Mon).at(noon)
    val c = Schedule.weekdays.between(9.h, 17.h).at(30.m)
    val d = Schedule.monthly.at(6.h).in(cronh.domain.Month.March)
    assert(a != b && c != d)
  }
}
