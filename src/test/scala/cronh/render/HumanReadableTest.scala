package cronh.render

import cronh.domain.Month
import cronh.dsl.*
import munit.FunSuite

class HumanReadableTest extends FunSuite {

  test("daily at an afternoon time") {
    assertEquals(
      Schedule.daily.at(14.h, 30.m).humanReadable,
      "At 2:30 PM, every day"
    )
  }

  test("weekday mornings") {
    assertEquals(
      Schedule.weekdays.at(9.h).humanReadable,
      "At 9:00 AM, on weekdays"
    )
  }

  test("named days join with and") {
    assertEquals(
      Schedule.on(Mon, Fri).at(noon).humanReadable,
      "At 12:00 PM, on Monday and Friday"
    )
  }

  test("an explicit Monday-through-Friday list reads as weekdays") {
    assertEquals(
      Schedule.on(Mon, Tue, Wed, Thu, Fri).at(9.h).humanReadable,
      "At 9:00 AM, on weekdays"
    )
  }

  test("weekends are recognized") {
    assertEquals(
      Schedule.weekends.humanReadable,
      "At 12:00 AM, on weekends"
    )
  }

  test("hourly schedules") {
    assertEquals(
      Schedule.hourly.humanReadable,
      "At minute 0 past every hour, every day"
    )
  }

  test("hour ranges from between") {
    assertEquals(
      Schedule.weekdays.between(9.h, 17.h).humanReadable,
      "At minute 0 past every hour from 9 AM to 5 PM, on weekdays"
    )
  }

  test("midnight is 12 AM") {
    assertEquals(Schedule.daily.humanReadable, "At 12:00 AM, every day")
  }

  test("day of month and month phrases") {
    assertEquals(
      Schedule.yearly.humanReadable,
      "At 12:00 AM, on day 1 of the month, in January"
    )
  }

  test("multiple months join with and") {
    assertEquals(
      Schedule.in(Month.June, Month.July).at(9.h).humanReadable,
      "At 9:00 AM, every day, in June and July"
    )
  }

  test("day-of-month and day-of-week together read as or (Vixie OR)") {
    // The DSL forbids combining .on and .onThe, so build the expression
    // directly from the domain model.
    import cronh.domain.*
    val expression = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(9)),
      Field.single(MonthDay(1)),
      Field.all,
      Field.single(DayOfWeek.Monday)
    )
    assertEquals(
      expression.humanReadable,
      "At 9:00 AM, on day 1 of the month or on Monday"
    )
  }

  test("a wildcard minute over an hour range reads grammatically") {
    import cronh.domain.*
    val expression = CronExpression(
      Field.all,
      Field.range(Hour(9), Hour(17)),
      Field.all,
      Field.all,
      Field.all
    )
    assertEquals(
      expression.humanReadable,
      "Every minute from 9 AM to 5 PM, every day"
    )
  }

  test("a redundant All in a list does not leak the word every") {
    import cronh.domain.*
    val expression = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(9)),
      Field.from(Term.All, Term.Single(MonthDay(1))),
      Field.all,
      Field.all
    )
    assertEquals(expression.humanReadable, "At 9:00 AM, every day")
  }
}
