package cronh.dsl

import cronh.domain.Generators.{
  given_Arbitrary_DayOfMonth,
  given_Arbitrary_DayOfWeek,
  given_Arbitrary_Hour,
  given_Arbitrary_Minute
}
import cronh.domain.fieldTypes.{DayOfMonth, DayOfWeek, Hour, Minute}
import cronh.domain.fieldTypes.Month.{February, January, March}
import cronh.dsl
import cronh.dsl.aliases.*
import cronh.render.toCron
import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.{Gen, Prop}
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

  property("at.at works correctly") = forAll { (h: Hour, m: Minute) =>
    assertEquals(
      Schedule.daily.at(h).at(m).toCron,
      Schedule.daily.at(Time(h, m)).toCron
    )
  }

  property("Between sets an exclusive end") = {
    val bounds: Gen[(Hour, Hour)] = for {
      start <- Gen.choose(Hour.MinValue, Hour.MaxValue - 1).map(Hour(_))
      end <- Gen
        .choose(start.value + 1, Hour.MaxValue)
        .map(Hour(_))
    } yield (start, end)
    Prop.forAll(bounds) { (start, end) =>
      assertEquals(
        Schedule.daily.between(start, end).at(30.min).toCron,
        Schedule.daily
          .at(Range[Hour](start, Hour(end.value - 1)))
          .at(30.min)
          .toCron
      )
    }
  }

  test("EveryHour sets * aka Field.all") {
    assertEquals(Schedule.daily.everyHour(0.min).hour, cronh.domain.Field.all)
  }

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

  test("General unit tests for rendering cron expressions") {
    assertEquals(Schedule.daily.at(14.h).at(30.min).toCron, "30 14 * * *")
    assertEquals(Schedule.weekdays.at(9.h).at(0.min).toCron, "0 9 * * 1-5")
    assertEquals(Schedule.on(Mon, Fri).at(Noon).toCron, "0 12 * * 1,5")
    assertEquals(
      Schedule.weekdays.between(9.h, endExclusive = 17.h).at(0.min).toCron,
      "0 9-16 * * 1-5"
    )
    assertEquals(Schedule.weekends.at(8.h).at(0.min).toCron, "0 8 * * 6,0")
    assertEquals(
      Schedule.onThe(1.st, 15.th).at(6.h).at(0.min).toCron,
      "0 6 1,15 * *"
    )
    assertEquals(Schedule.daily.everyMinute.toCron, "* * * * *")
    assertEquals(Schedule.daily.every(15.min).toCron, "0,15,30,45 * * * *")
    assertEquals(
      Schedule.daily.everyTwoHours(at = 0.min).toCron,
      "0 0,2,4,6,8,10,12,14,16,18,20,22 * * *"
    )
  }
}
