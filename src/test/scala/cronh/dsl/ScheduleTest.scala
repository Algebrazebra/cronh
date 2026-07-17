package cronh.dsl

import cronh.domain.Generators.{
  given_Arbitrary_DayOfMonth,
  given_Arbitrary_DayOfWeek
}
import cronh.domain.fieldTypes.{DayOfMonth, DayOfWeek, Hour, Minute}
import cronh.domain.fieldTypes.Month.{February, January, March}
import cronh.dsl
import cronh.dsl.aliases.*
import cronh.render.toCron
import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

class ScheduleTest extends ScalaCheckSuite {

  // 6. ranges: for month, weekdays and month days incl. wraparound
  //    check they are inclusive and covering
  // TODO: test wrap around ranges. For months: Now to March --> 11-12, 1-3

  test("DSL allows selection of one month or multiple months") {
    assertEquals(Schedule.in(January).daily.everyMinute.toCron, "* * * 1 *")
    assertEquals(
      Schedule.in(January, February).daily.everyMinute.toCron,
      "* * * 1,2 *"
    )
    assertEquals(
      Schedule.in(January, February, March).daily.everyMinute.toCron,
      "* * * 1,2,3 *"
    )
  }

  test("DSL has a weekdays shortcut") {
    assertEquals(
      Schedule.weekdays.everyMinute.toCron,
      Schedule.on(Weekdays).everyMinute.toCron
    )
  }

  test("DSL has a weekend shortcut") {
    assertEquals(
      Schedule.weekends.everyMinute.toCron,
      Schedule.on(Weekends).everyMinute.toCron
    )
  }

  test("DSL allows selection of one weekday or multiple weekdays") {
    assertEquals(Schedule.on(Mondays).everyMinute.toCron, "* * * * 1")
    assertEquals(
      Schedule.on(Mondays, Tuesdays).everyMinute.toCron,
      "* * * * 1,2"
    )
    assertEquals(
      Schedule.on(Mondays, Tuesdays, Wednesdays).everyMinute.toCron,
      "* * * * 1,2,3"
    )
  }

  test("DSL allows selection of one day of month or multiple days of month") {
    assertEquals(Schedule.onThe(1.st).everyMinute.toCron, "* * 1 * *")
    assertEquals(Schedule.onThe(1.st, 2.nd).everyMinute.toCron, "* * 1,2 * *")
    assertEquals(
      Schedule.onThe(1.st, 2.nd, 3.rd).everyMinute.toCron,
      "* * 1,2,3 * *"
    )
  }

  property("Weekday can be set after the day of month was set and vice versa") =
    forAll { (dom: DayOfMonth, weekday: DayOfWeek) =>
      assertEquals(
        Schedule.onThe(dom).orOn(weekday).everyMinute.toCron,
        Schedule.on(weekday).orOnThe(dom).everyMinute.toCron
      )
    }

  test("Long and short form of weekdays are possible") {
    val shortForm = List(Mon, Tue, Wed, Thu, Fri, Sat, Sun)
    val longForm = List(
      Mondays,
      Tuesdays,
      Wednesdays,
      Thursdays,
      Fridays,
      Saturdays,
      Sundays
    )
    shortForm.zip(longForm).foreach { case (short, long) =>
      assertEquals(
        Schedule.on(short).everyMinute.toCron,
        Schedule.on(long).everyMinute.toCron
      )
    }
  }

  test("Long and short form of months are equivalent") {
    val shortForm =
      List(Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec)
    val longForm = List(
      January,
      February,
      March,
      April,
      May,
      June,
      July,
      August,
      September,
      October,
      November,
      December
    )
    shortForm.zip(longForm).foreach { case (short, long) =>
      assertEquals(
        Schedule.in(short).daily.everyMinute.toCron,
        Schedule.in(long).daily.everyMinute.toCron
      )
    }
  }

  // The tests should test created cron schedules against the string
  // The following time cases should be tested with holding the day cases constant: daily
  // 4. Setting time with multiple .h and multiple .min
  // 5. Every minute is available (both immediately and after setting hours)

  // Test Day ranges: one, many, day range -->  what happens with a wraparound?
  // TODO: Document: from 15th june to 15th of august is NOT possible in cron
  // Test Day selection: when setting the 15th, it's possible to select a weekday after
  // Test Day selection can be skipped

  test("Time formats are equivalent") {
    import cronh.dsl.Time.*
    val hour = 14.h
    val minute = 30.min
    assertEquals(
      Schedule.daily.at(hour).at(minute).toCron,
      Schedule.daily.at(time"${hour.value}:${minute.value}").toCron
    )
    assertEquals(
      Schedule.daily.at(hour).at(minute).toCron,
      Schedule.daily.at(time"${hour.value % 12}:${minute.value} PM").toCron
    )
  }

  property("Setting multiple hours and minutes renders the correct cron") {}

  test("Schedule.daily.at(14.h).at(30.m) renders 30 14 * * *") {
    assertEquals(Schedule.daily.at(14.h).at(30.min).toCron, "30 14 * * *")
  }

  test("Schedule.weekdays.at(9.h) renders 0 9 * * 1-5") {
    assertEquals(Schedule.weekdays.at(9.h).at(0.min).toCron, "0 9 * * 1-5")
  }

  test("Schedule.on(Mon, Fri).at(noon) renders 0 12 * * 1,5") {
    assertEquals(Schedule.on(Mon, Fri).at(Noon).toCron, "0 12 * * 1,5")
  }

  test(
    "Schedule.weekdays.between(9.h, 17.h).at(0.min) renders 0 9-17 * * 1-5"
  ) {
    assertEquals(
      Schedule.weekdays.between(9.h, endExclusive = 17.h).at(0.min).toCron,
      "0 9-16 * * 1-5"
    )
  }

  test("Schedule.weekends.at(8.h).at(0.min) renders 0 8 * * 6,0") {
    assertEquals(Schedule.weekends.at(8.h).at(0.min).toCron, "0 8 * * 6,0")
  }

  test("Schedule.onThe(1.dom, 15.dom).at(6.h).at(0.min) renders 0 6 1,15 * *") {
    assertEquals(
      Schedule.onThe(1.st, 15.th).at(6.h).at(0.min).toCron,
      "0 6 1,15 * *"
    )
  }

}
