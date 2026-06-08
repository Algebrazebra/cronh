package cronh.domain

import munit.FunSuite

class CronExpressionTest extends FunSuite {

  test("CronExpression can be constructed with all five fields") {
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

  test("CronExpression equality is structural") {
    val a = CronExpression(
      minute = Field.single(Minute(30)),
      hour = Field.single(Hour(14)),
      dayOfMonth = Field.all,
      month = Field.all,
      dayOfWeek = Field.all
    )
    val b = CronExpression(
      minute = Field.single(Minute(30)),
      hour = Field.single(Hour(14)),
      dayOfMonth = Field.all,
      month = Field.all,
      dayOfWeek = Field.all
    )
    assertEquals(a, b)
  }
}
