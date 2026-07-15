package cronh.dsl

import cronh.render.toCron
import munit.FunSuite

class ScheduleTest extends FunSuite {

  // Test month selection works: single month, many months

  // Test month range: inclusive and convering. is field.range
  // --> Test wraparound ranges

  // Test Day ranges: one, many, day range -->  what happens with a wraparound?
  // TODO: Document: from 15th june to 15th of august is NOT possible in cron
  // Test Day selection: when setting the 15th, it's possible to select a weekday after
  // Test Day selection can be skipped

  test("Schedule.daily.at(14.h, 30.m) renders 30 14 * * *") {
    assertEquals(Schedule.daily.at(14.h, 30.min).toCron, "30 14 * * *")
  }

  test("Schedule.weekdays.at(9.h) renders 0 9 * * 1-5") {
    assertEquals(Schedule.weekdays.at(9.h).toCron, "0 9 * * 1-5")
  }

  test("Schedule.on(Mon, Fri).at(noon) renders 0 12 * * 1,5") {
    assertEquals(Schedule.on(Mon, Fri).at(Noon).toCron, "0 12 * * 1,5")
  }

  /*
  test("Schedule.weekdays.between(9.h, 17.h) renders 0 9-17 * * 1-5") {
    assertEquals(
      Schedule.weekdays.between(9.h, 17.h).toCron,
      "0 9-17 * * 1-5"
    )
  }

  test("between keeps the minute settable: .at(30.m) refines it") {
    assertEquals(
      Schedule.weekdays.between(9.h, 17.h).at(30.min).toCron,
      "30 9-17 * * 1-5"
    )
  }
   */

  test("Schedule.weekends.at(8.h) renders 0 8 * * 6,0") {
    assertEquals(Schedule.weekends.at(8.h).toCron, "0 8 * * 6,0")
  }

  test("Schedule.onThe(1.dom, 15.dom).at(6.h) renders 0 6 1,15 * *") {
    assertEquals(Schedule.onThe(1.st, 15.th).at(6.h).toCron, "0 6 1,15 * *")
  }

  /*
  test(".on(Weekdays) accepts a prebuilt field") {
    assertEquals(Schedule.on(Weekdays).at(9.h).toCron, "0 9 * * 1-5")
  }
  
  test("Mon to Fri builds an inclusive weekday range") {
    assertEquals(Schedule.on(Monday to Friday).at(9.h).toCron, "0 9 * * 1-5")
  }

  test("Tue to Thu renders the middle of the week") {
    assertEquals(Schedule.on(Tue to Thu).at(9.h).toCron, "0 9 * * 2-4")
  }

  test("Monday to Friday: long-form day aliases work in a range") {
    assertEquals(
      Schedule.on(Monday to Friday).at(9.h).toCron,
      "0 9 * * 1-5"
    )
  }
   */
}
