package cronh.domain

import munit.FunSuite

class CronExpressionTest extends FunSuite {

  test("CronExpression has the five expected named fields") {
    val expr = CronExpression(
      minute = Field.single(Minute(0)),
      hour = Field.single(Hour(9)),
      dayOfMonth = Field.all,
      month = Field.all,
      dayOfWeek = Field.range(DayOfWeek.Monday, DayOfWeek.Friday)
    )
    assertEquals(expr.minute, Field.single(Minute(0)))
    assertEquals(expr.hour, Field.single(Hour(9)))
  }
}
