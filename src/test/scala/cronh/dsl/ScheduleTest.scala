package cronh.dsl

import cronh.domain.Month
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

  test("Schedule.daily renders 0 0 * * * (the blank slate)") {
    assertEquals(Schedule.daily.toCron, "0 0 * * *")
  }

  test("Schedule.hourly renders 0 * * * *") {
    assertEquals(Schedule.hourly.toCron, "0 * * * *")
  }

  test("Schedule.hourly.at(30.m) renders 30 * * * *") {
    assertEquals(Schedule.hourly.at(30.m).toCron, "30 * * * *")
  }

  test("Schedule.monthly renders 0 0 1 * *") {
    assertEquals(Schedule.monthly.toCron, "0 0 1 * *")
  }

  test("Schedule.yearly renders 0 0 1 1 *") {
    assertEquals(Schedule.yearly.toCron, "0 0 1 1 *")
  }

  test("Schedule.everyMinute renders * * * * *") {
    assertEquals(Schedule.everyMinute.toCron, "* * * * *")
  }

  test("Schedule.weekends.at(8.h) renders 0 8 * * 6,0") {
    assertEquals(Schedule.weekends.at(8.h).toCron, "0 8 * * 6,0")
  }

  test("Schedule.onThe(1.dom, 15.dom).at(6.h) renders 0 6 1,15 * *") {
    assertEquals(Schedule.onThe(1.dom, 15.dom).at(6.h).toCron, "0 6 1,15 * *")
  }

  test(".on(Weekdays) accepts a prebuilt field") {
    assertEquals(Schedule.daily.on(Weekdays).at(9.h).toCron, "0 9 * * 1-5")
  }

  test("Mon to Fri builds an inclusive weekday range") {
    assertEquals(Schedule.daily.on(Mon to Fri).at(9.h).toCron, "0 9 * * 1-5")
  }

  test("Tue to Thu renders the middle of the week") {
    assertEquals(Schedule.daily.on(Tue to Thu).at(9.h).toCron, "0 9 * * 2-4")
  }

  test("Monday to Friday: long-form day aliases work in a range") {
    assertEquals(
      Schedule.daily.on(Monday to Friday).at(9.h).toCron,
      "0 9 * * 1-5"
    )
  }

  test(".on(Field.all) does not compile: a wildcard is not a WeekdaySelector") {
    assert(
      !scala.compiletime.testing.typeChecks(
        "Schedule.daily.on(cronh.domain.Field.all)"
      )
    )
  }

  test(".at(midnight) uses the midnight alias") {
    assertEquals(Schedule.daily.at(midnight).toCron, "0 0 * * *")
  }

  // Asymmetric defaults (systemd OnCalendar style): an unset date field is `*`,
  // an unset time field is `0`. So a date-only spec is midnight, never every-minute.
  test("Schedule.in(June) defaults time to midnight, not every-minute") {
    assertEquals(Schedule.in(Month.June).toCron, "0 0 * 6 *")
  }

  test("Schedule.at(30.m) is 30 past hour 0 (00:30 daily)") {
    assertEquals(Schedule.at(30.m).toCron, "30 0 * * *")
  }

  test(
    "every-minute is an explicit opt-in: Schedule.everyMinute.in is rejected"
  ) {
    // everyMinute commits the time, so coarse-after-fine .in does not compile.
    assert(
      !scala.compiletime.testing.typeChecks(
        "Schedule.everyMinute.in(cronh.domain.Month.June)"
      )
    )
  }

  test(".everyHour after .in: hourly in June renders 0 * * 6 *") {
    assertEquals(Schedule.in(Month.June).everyHour.toCron, "0 * * 6 *")
  }

  test(".in restricts the month (coarse-to-fine: .in before .at)") {
    assertEquals(
      Schedule.in(Month.June, Month.July).at(9.h).toCron,
      "0 9 * 6,7 *"
    )
  }

  // Mirrors examples/Schedules.scala `nightlyBackup`.
  test("nightlyBackup renders 30 2 * * *") {
    assertEquals(Schedule.daily.at(2.h, 30.m).toCron, "30 2 * * *")
  }

  // Mirrors examples/Schedules.scala `summerReport`.
  test("summerReport renders 0 0 1 6,7 *") {
    assertEquals(
      Schedule.in(Month.June, Month.July).onThe(1.dom).at(midnight).toCron,
      "0 0 1 6,7 *"
    )
  }
}
