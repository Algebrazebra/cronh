package cronh.dsl

import cronh.domain.fieldTypes.*
import cronh.domain.{CronExpression, Field}

/** # Entry point for the scheduling DSL.
  *
  * ## Design considerations
  *
  * The DSL is designed to read easily and naturally. It uses a fluent interface
  * for building a cron expression from the largest time unit (months) to the
  * smallest (minutes). This goes against the field order of POSIX cron
  * expressions. However, this is an intentional design choice. By imposing an
  * order on the DSL methods, it's easier to guarantee correctness and natural
  * language legibility.
  *
  * ## Usage examples
  *
  * TODO: basic example going from month to minute show that time can also be
  * specified with time literal (requires import)
  *
  * of course, month and day can be skipped (weekdays or daily method) selecting
  * one or many selecting a range
  *
  * // Examples Schedule.in(June).onThe(15.th).at(9.h, 0.min)
  *
  * ## What's not possible
  *
  * Tricky beast: The OR between DoM and DoW
  *
  * // What's not possible: Schedule.at(9.h) has two problems:
  *   1. It soudns like it's a one-off, not recurring
  *   2. Even for people that know it's about a cron expression, the recurrence
  *      is not obvious. Most would probably implicitly assume a daily
  *      recurrence, but I think it's better to be explicit. Also, people would
  *      likely assume that this scheduling happens at 0.min, but the most
  *      idiomatic cron way would be that this means every minute at 9.h Again!
  *      Better to be explicit! Therefore, the DSL does not allow this and
  *      forces: Schedule.daily.at(9.h, 0.min)
  */
object Schedule extends MonthSelection, DaySelection {
  protected def cronExpr: CronExpressionBuilder = CronExpressionBuilder.allUnset
}

/** This trait carries the month-selection methods. */
trait MonthSelection {
  protected def cronExpr: CronExpressionBuilder

  def in(m: Month, ms: Month*): MonthChosen = MonthChosen(
    cronExpr.copy(month = Some(Field.of(m, ms: _*)))
  )

  def in(ms: MonthRange): MonthChosen = {
    MonthChosen(
      cronExpr.copy(month = Some(ms.toField))
    )
  }

}

/** This trait carries the day-selection methods. */
trait DaySelection {
  protected def cronExpr: CronExpressionBuilder

  /** Configures daily scheduling.
    *
    * Ends the day selection phase and begins the time selection phase. In cron
    * terms, this means setting both day of month and day of week to `*`.
    */
  def daily: TimePhase = TimePhase(
    cronExpr.copy(dayOfMonth = Some(Field.all), dayOfWeek = Some(Field.all))
  )

  /** Configures scheduling for weekdays, i.e., Monday to Friday. */
  def weekdays: DayOfWeekChosen = DayOfWeekChosen(
    cronExpr.copy(dayOfWeek =
      Some(Field.range(DayOfWeek.Monday, DayOfWeek.Friday))
    )
  )

  /** Configures scheduling for weekends, i.e., Saturday and Sunday. */
  def weekends: DayOfWeekChosen = DayOfWeekChosen(
    cronExpr.copy(dayOfWeek = Some(Field.of(Saturday, Sunday)))
  )

  /** Configures scheduling for one or many days of the week. */
  def on(w: DayOfWeek, ws: DayOfWeek*): DayOfWeekChosen = DayOfWeekChosen(
    cronExpr.copy(dayOfWeek = Some(Field.of(w, ws: _*)))
  )

  def on(ws: DayOfWeekRange): DayOfWeekChosen = DayOfWeekChosen(
    cronExpr.copy(dayOfWeek = Some(ws.toField))
  )

  /** Configures scheduling for one or many days of the month. */
  def onThe(d: DayOfMonth, ds: DayOfMonth*): DayOfMonthChosen =
    DayOfMonthChosen(
      cronExpr.copy(dayOfMonth = Some(Field.of(d, ds: _*)))
    )

  def onThe(ds: DayOfMonthRange): DayOfMonthChosen =
    DayOfMonthChosen(
      cronExpr.copy(dayOfMonth = Some(ds.toField))
    )
}

final class DayOfWeekChosen(cronExpr: CronExpressionBuilder)
    extends TimePhase(cronExpr) {
  def orOnThe(d: DayOfMonth, ds: DayOfMonth*): TimePhase = TimePhase(
    cronExpr.copy(dayOfMonth = Some(Field.of(d, ds: _*)))
  )

  def orOnThe(ds: DayOfMonthRange): TimePhase = TimePhase(
    cronExpr.copy(dayOfMonth = Some(ds.toField))
  )
}

final class DayOfMonthChosen(cronExpr: CronExpressionBuilder)
    extends TimePhase(cronExpr) {
  def orOn(w: DayOfWeek, ws: DayOfWeek*): TimePhase = TimePhase(
    cronExpr.copy(dayOfWeek = Some(Field.of(w, ws: _*)))
  )
  def orOn(ws: DayOfWeekRange): TimePhase = TimePhase(
    cronExpr.copy(dayOfWeek = Some(ws.toField))
  )
}

/** Represents that the user set the month of the cron expression. */
final class MonthChosen(protected val base: CronExpressionBuilder)
    extends DaySelection {
  protected def cronExpr: CronExpressionBuilder = base
}

/** The time phase is the first place where the DSL's terminal verbs lie.
  * Terminal verbs are the methods that finalize the configuration of the
  * schedule by converting the intermediate [[CronExpressionBuilder]]
  * representation to [[CronExpression]].
  */
class TimePhase(protected val cronExpr: CronExpressionBuilder):

  /** Configures the time of day. */
  // TODO: example of how to use the time literal time"13:00" or time"1 am"
  // TODO: explanation why it's not possible to use multiple time literals
  def at(time: Time): CronExpression = {
    cronExpr
      .copy(
        hour = Some(Field.of(time.hour)),
        minute = Some(Field.of(time.minute))
      )
      .build()
  }

  def at(h: Hour, m: Minute): CronExpression = cronExpr
    .copy(hour = Some(Field.of(h)), minute = Some(Field.of(m)))
    .build()

  // TODO: do I want this even?
  // It's not explicit what happens with the minutes.
  // If I allow minutes to be specified, I need HourFixed as return type.
  // Then having at(15, 17).min(7, 47, 55) isn't very readable.
  // Then again, how else would I represent the product of these hours and minutes?
  // NO; we will have 09:00 -->. then at(9.h.) doesn't make sense because it could be written explicitly
  // only other use case would be at(9.h).every(15.min)
  def at(h: Hour, hs: Hour*): CronExpression = cronExpr
    .copy(hour = Some(Field.of(h, hs: _*)), minute = Some(Field.of(0.min)))
    .build()

  /*
  def hourly: HourFixed =
    HourFixed(
      model.copy(hour = FieldSpec.Star, minute = FieldSpec.Nums(List(0)))
    )
   */
  def between(lo: Hour, hi: Hour): Any = ???

  def everyMinute: CronExpression =
    cronExpr.copy(minute = Some(Field.all)).build()

/** A variant of [[CronExpression]] that allows representing unset fields with
  * `Option`.
  *
  * In cron, the default field is `*`. For a natural language DSL, this
  * assumption can lead to misunderstandings or bugs. Keeping track of which
  * fields were set and which weren't makes hidden assumptions visible.
  *
  * For example, let's imagine we configure our schedule with
  * `Schedule.daily.at(9.h)` If an unset field is implicitly set to `*`, the
  * schedule above would run every minute of the 9th hour.
  *
  * The DSL tries to avoid ambiguities like this, but in any event, I think it's
  * better to be explicit.
  */
private[dsl] final case class CronExpressionBuilder(
    minute: Option[Field[Minute]],
    hour: Option[Field[Hour]],
    dayOfMonth: Option[Field[DayOfMonth]],
    month: Option[Field[Month]],
    dayOfWeek: Option[Field[DayOfWeek]]
) {

  /** Converts to a [[CronExpression]] by setting unset fields to `*`. */
  def build(): CronExpression = CronExpression(
    minute.getOrElse(Field.all),
    hour.getOrElse(Field.all),
    dayOfMonth.getOrElse(Field.all),
    month.getOrElse(Field.all),
    dayOfWeek.getOrElse(Field.all)
  )
}
object CronExpressionBuilder {
  def allUnset: CronExpressionBuilder = {
    CronExpressionBuilder(None, None, None, None, None)
  }
}
