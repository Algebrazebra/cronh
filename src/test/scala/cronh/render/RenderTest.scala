package cronh.render

import cronh.domain.*
import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class RenderTest extends ScalaCheckSuite {

  test("renders a weekday-morning expression: 0 9 * * 1-5") {
    val expression = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(9)),
      Field.all,
      Field.all,
      Field.range(DayOfWeek.Monday, DayOfWeek.Friday)
    )
    assertEquals(expression.toCron, "0 9 * * 1-5")
  }

  test("renders lists and mixed term shapes: 0,30 9-17 1,15 6 0,6") {
    val expression = CronExpression(
      Field.of(Minute(0), Minute(30)),
      Field.range(Hour(9), Hour(17)),
      Field.of(MonthDay(1), MonthDay(15)),
      Field.single(Month.June),
      Field.of(DayOfWeek.Sunday, DayOfWeek.Saturday)
    )
    assertEquals(expression.toCron, "0,30 9-17 1,15 6 0,6")
  }

  test("renders the all-wildcard expression: * * * * *") {
    val expression =
      CronExpression(Field.all, Field.all, Field.all, Field.all, Field.all)
    assertEquals(expression.toCron, "* * * * *")
  }

  test("Unix numbering: Sunday renders as 0, Saturday as 6") {
    val expression = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(0)),
      Field.all,
      Field.all,
      Field.of(DayOfWeek.Sunday, DayOfWeek.Saturday)
    )
    assertEquals(expression.toCron.split(" ").last, "0,6")
  }

  test("a weekday range ending on Sunday splits into a valid list") {
    val expression = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(0)),
      Field.all,
      Field.all,
      Field.range(DayOfWeek.Friday, DayOfWeek.Sunday)
    )
    assertEquals(expression.toCron, "0 0 * * 5-6,0")
  }

  test("a Saturday-Sunday range renders as 6,0") {
    val expression = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(0)),
      Field.all,
      Field.all,
      Field.range(DayOfWeek.Saturday, DayOfWeek.Sunday)
    )
    assertEquals(expression.toCron, "0 0 * * 6,0")
  }

  test("the shared path splits Sunday-ranges for a non-Unix dialect too") {
    // A Quartz-style dialect (Sun=1, Mon=2, ..., Sat=7) does not put Sunday
    // last, so without the shared term-level split a range ending on Sunday
    // would render as the inverted, invalid "7-1".
    object QuartzLikeDialect extends CronDialect {
      given dayOfWeekRender: Render[DayOfWeek] = { day =>
        (day match {
          case DayOfWeek.Sunday    => 1
          case DayOfWeek.Monday    => 2
          case DayOfWeek.Tuesday   => 3
          case DayOfWeek.Wednesday => 4
          case DayOfWeek.Thursday  => 5
          case DayOfWeek.Friday    => 6
          case DayOfWeek.Saturday  => 7
        }).toString
      }
    }
    val saturdayToSunday = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(0)),
      Field.all,
      Field.all,
      Field.range(DayOfWeek.Saturday, DayOfWeek.Sunday)
    )
    val fridayToSunday = CronExpression(
      Field.single(Minute(0)),
      Field.single(Hour(0)),
      Field.all,
      Field.all,
      Field.range(DayOfWeek.Friday, DayOfWeek.Sunday)
    )
    assertEquals(
      saturdayToSunday.toCron(using QuartzLikeDialect),
      "0 0 * * 7,1"
    )
    assertEquals(
      fridayToSunday.toCron(using QuartzLikeDialect),
      "0 0 * * 6-7,1"
    )
  }

  test("months render numerically: January is 1, December is 12") {
    assertEquals(renderField(Field.single(Month.January)), "1")
    assertEquals(
      renderField(Field.range(Month.January, Month.December)),
      "1-12"
    )
  }

  test("Field.all renders as *") {
    assertEquals(renderField[Minute](Field.all), "*")
  }

  property("rendering is deterministic") = forAll { (e: FreshCron) =>
    e.toCron == e.toCron
  }

  property("output always has exactly five space-separated fields") = forAll {
    (e: FreshCron) =>
      e.toCron.split(" ").length == 5
  }

  property("range bounds are preserved in minute output") = forAll {
    (a: Minute, b: Minute) =>
      val ord = summon[Ordering[Minute]]
      val (lo, hi) = if (ord.lteq(a, b)) (a, b) else (b, a)
      renderField(Field.range(lo, hi)) == s"${lo.value}-${hi.value}"
  }
}
