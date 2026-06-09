package cronh.domain

import cronh.domain.Generators.given
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

class CronExpressionTest extends ScalaCheckSuite {

  property("Construction preserves every field") = forAll {
    (
        m: Field[Minute],
        h: Field[Hour],
        d: Field[MonthDay],
        mo: Field[Month],
        dow: Field[DayOfWeek]
    ) =>
      val e = CronExpression(m, h, d, mo, dow)
      e.minute == m &&
      e.hour == h &&
      e.dayOfMonth == d &&
      e.month == mo &&
      e.dayOfWeek == dow
  }

  property("Equality is reflexive") = forAll { (e: CronExpression) =>
    e == e
  }

  property("copy() with no overrides is identity") = forAll {
    (e: CronExpression) =>
      e.copy() == e
  }

  property("Equality depends on the minute field") = forAll {
    (a: CronExpression, b: CronExpression) =>
      a.copy(minute = b.minute) == a == (a.minute == b.minute)
  }

  property("Equality depends on the hour field") = forAll {
    (a: CronExpression, b: CronExpression) =>
      a.copy(hour = b.hour) == a == (a.hour == b.hour)
  }

  property("Equality depends on the dayOfMonth field") = forAll {
    (a: CronExpression, b: CronExpression) =>
      a.copy(dayOfMonth = b.dayOfMonth) == a == (a.dayOfMonth == b.dayOfMonth)
  }

  property("Equality depends on the month field") = forAll {
    (a: CronExpression, b: CronExpression) =>
      a.copy(month = b.month) == a == (a.month == b.month)
  }

  property("Equality depends on the dayOfWeek field") = forAll {
    (a: CronExpression, b: CronExpression) =>
      a.copy(dayOfWeek = b.dayOfWeek) == a == (a.dayOfWeek == b.dayOfWeek)
  }
}
