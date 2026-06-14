package cronh.dsl

import cronh.domain.Month
import cronh.render.toCron
import munit.FunSuite

class ScheduleTest extends FunSuite {

  test("Schedule.daily.at(14.h, 30.m) renders 30 14 * * *") {
    assertEquals(Schedule.daily.at(14.h, 30.m).toCron, "30 14 * * *")
  }

  test("Schedule.weekdays.at(9.h) renders 0 9 * * 1-5") {
    assertEquals(Schedule.weekdays.at(9.h).toCron, "0 9 * * 1-5")
  }

  test("Schedule.on(Mon, Fri).at(noon) renders 0 12 * * 1,5") {
    assertEquals(Schedule.on(Mon, Fri).at(noon).toCron, "0 12 * * 1,5")
  }

  test("Schedule.weekdays.between(9.h, 17.h) renders 0 9-17 * * 1-5") {
    assertEquals(
      Schedule.weekdays.between(9.h, 17.h).toCron,
      "0 9-17 * * 1-5"
    )
  }

  test("between keeps the minute settable: .at(30.m) refines it") {
    assertEquals(
      Schedule.weekdays.between(9.h, 17.h).at(30.m).toCron,
      "30 9-17 * * 1-5"
    )
  }

  test("Schedule.daily renders 0 0 * * *") {
    assertEquals(Schedule.daily.toCron, "0 0 * * *")
  }

  test("Schedule.hourly renders 0 * * * *") {
    assertEquals(Schedule.hourly.toCron, "0 * * * *")
  }

  test("Schedule.monthly renders 0 0 1 * *") {
    assertEquals(Schedule.monthly.toCron, "0 0 1 * *")
  }

  test("Schedule.yearly renders 0 0 1 1 *") {
    assertEquals(Schedule.yearly.toCron, "0 0 1 1 *")
  }

  test("Schedule.weekends.at(8.h) renders 0 8 * * 6,0") {
    assertEquals(Schedule.weekends.at(8.h).toCron, "0 8 * * 6,0")
  }

  test("Schedule.onDay(1.dom, 15.dom).at(6.h) renders 0 6 1,15 * *") {
    assertEquals(Schedule.onDay(1.dom, 15.dom).at(6.h).toCron, "0 6 1,15 * *")
  }

  test(".on(Weekdays) accepts a prebuilt field") {
    assertEquals(Schedule.daily.on(Weekdays).at(9.h).toCron, "0 9 * * 1-5")
  }

  test(".at(midnight) uses the midnight alias") {
    assertEquals(Schedule.daily.at(midnight).toCron, "0 0 * * *")
  }

  test(".in restricts the month") {
    assertEquals(
      Schedule.daily.at(9.h).in(Month.June, Month.July).toCron,
      "0 9 * 6,7 *"
    )
  }

  test(".on after .at still works (day spec independent of time)") {
    assertEquals(Schedule.daily.at(9.h).on(Tue, Thu).toCron, "0 9 * * 2,4")
  }

  // Mirrors examples/Schedules.scala `nightlyBackup`, whose header claims every
  // expression is covered by an acceptance test.
  test("nightlyBackup renders 30 2 * * *") {
    assertEquals(Schedule.daily.at(2.h, 30.m).toCron, "30 2 * * *")
  }

  // Mirrors examples/Schedules.scala `summerReport`.
  test("summerReport renders 0 0 1 6,7 *") {
    assertEquals(
      Schedule.monthly.at(midnight).in(Month.June, Month.July).toCron,
      "0 0 1 6,7 *"
    )
  }
}
