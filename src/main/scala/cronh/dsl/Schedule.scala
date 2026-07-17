package cronh.dsl

import cronh.domain.fieldTypes.*
import cronh.domain.{CronExpression, Field}
import cronh.dsl.aliases.{Saturdays, Sundays}

/** # Entry point for the scheduling DSL.
  *
  * ## Design considerations
  *
  * The DSL is designed to read easily and naturally. It uses a fluent interface
  * for building a cron expression from the largest time unit (months) to the
  * smallest (minutes). This goes against the field order of cron expressions.
  * However, this is an intentional design choice. By imposing an order on the
  * DSL methods, it's easier to guarantee semantics and natural language
  * legibility.
  *
  * ## Usage examples
  *
  * The DSL methods specify parts of the desired schedule until we hit a
  * "terminal" method that returns a [[CronExpression]]. Generally, the methods
  * are organized in two phases: the day phase and the time phase.
  *
  * ### The day phase
  *
  * All examples in this section are unfinished, because they leave out the time
  * phase
  *
  * We can specify the month and the day of the month or the day of the week:
  * ```scala
  * Schedule.in(January).on(Mondays)
  * Schedule.in(January).onThe(1.st)
  * ```
  *
  * It's possible to select multiple values, pre-defined aliases or ranges:
  * ```scala
  * Schedule.in(January, February, March).on(Mondays, Tuesdays, Wednesdays)
  * Schedule.in(January to March).on(Monays to Wednesdays)
  * Schedule.in(CQ1).on(Weekdays)
  * Schedule.in(CQ1).on(Weekends)
  * ```
  *
  * And in case you want to combine weekdays with days of the month, you can.
  * Just keep in mind, they combine with "OR" and not "AND" as the DSL tries to
  * make clear:
  * ```scala
  * Schedule.in(June).on(Mondays).orOnThe(15.th
  * Schedule.in(June).onThe(15.th).orOn(Mondays)
  * ```
  *
  * And of course, setting the month is optional:
  * ```
  * Schedule.on(Mondays)
  * Schedule.onThe(15.th)
  * ```
  *
  * To schedule every day, simply use `daily` to skip straight to the time
  * phase.
  * ```scala
  * Schedule.daily
  * ```
  *
  * ### The time phase
  *
  * After completing the day phase, we can start the time phase. All the
  * examples use `daily` as the shortest way to complete the day phase and enter
  * the time phase.
  *
  * The simplest time schedule is hourly at a specific minute mark:
  * ```scala
  * Schedule.daily.everyHour
  * Schedule.daily.everyHour(at = 30.min)
  * ```
  *
  * Specifying an exact time is also straightforward:
  * ```scala
  * Schedule.daily.at(time"09:00")
  * Schedule.daily.at(time"9:00 am")
  *
  * // or without the string interpolation
  * Schedule.daily.at(Time(hour=9.h, minute=0.min)
  *
  * // The examples now complete the schedule, returning CronExpression.
  * // This makes the `toCron` available:
  * Schedule.daily.at(time"09:00").toCron == "0 9 * * *"
  * ```
  *
  * Setting multiple times is also possible, but because we are dealing with
  * cron syntax, it's not quite as straightforward as the previous example.
  * Instead, we have to specify the hour and minute marks which form a
  * cross-product.
  * ```scala
  * Schedule.daily.at(9.h).at(0.min)
  * Schedule.daily
  *   .at(9.h, 13.h)
  *   .at(0.min, 30.min) // fires at 9:00, 9:30, 13:00 and 13:30
  *
  * // We can also apply range syntax here. Oh, and it's inclusive of the end!
  * Schedule.daily
  *   .at(9.h to 13.h)
  *   .everyMinute // fires every minute from 09:00 to 13:59 inclusive
  *
  * // There is also the between method which has an exclusive end:
  * Schedule.daily
  *   .between(9.h, 13.h)
  *   .everyMinute // fires every minute from 09:00 to 12:59
  * ```
  *
  * ### Completed schedule
  *
  * Methods that return a [[CronExpression]] are terminal methods, because they
  * complete the schedule definition. Then, the `toCron` method is available to
  * produce the cron schedule as a string.
  *
  * ## What's not possible
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
    cronExpr.copy(dayOfWeek = Some(Field.of(Saturdays, Sundays)))
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

/** This trait carries the minute-selection methods. */
trait MinuteSelection {
  protected def cronExpr: CronExpressionBuilder

  /** Configures the scheduling for every minute. */
  def everyMinute: CronExpression =
    cronExpr.copy(minute = Some(Field.all)).build()

  /** Configures the scheduling in an interval of the given minutes.
    *
    * **Footgun alert!** Cron cannot actually schedule something every X minutes
    * if X is not a divisor of 60. For example, scheduling every 25 minutes
    * means firing at: 09:00, 09:25, 09:50, 10:00, 10:25, etc. This means every
    * third interval is only 10 minutes long instead of the expected 25!
    *
    * Choose one of the following values to avoid the potential footgun: 1, 2,
    * 3, 4, 5, 6, 10, 12, 15, 20, 30
    */
  def every(minutes: Minute): CronExpression = {
    val minuteField = minutes.value match {
      case 1 => Field.all
      case x =>
        val minuteMarks = (0 until 60 by x).map(Minute(_))
        Field.of(minuteMarks.head, minuteMarks.tail: _*)
    }
    cronExpr.copy(minute = Some(minuteField)).build()
  }
}

/** Represents that the user set the hour of the cron expression. */
final class HourChosen(protected val base: CronExpressionBuilder)
    extends MinuteSelection {
  protected def cronExpr: CronExpressionBuilder = base

  /** Configures the scheduling for the given minute marks. */
  def at(m: Minute, ms: Minute*): CronExpression = cronExpr
    .copy(
      minute = Some(Field.of(m, ms: _*))
    )
    .build()

  def at(ms: MinuteRange): CronExpression = cronExpr
    .copy(
      minute = Some(ms.toField)
    )
    .build()

}

/** The time phase is the first place where the DSL's terminal verbs lie.
  * Terminal verbs are the methods that finalize the configuration of the
  * schedule by converting the intermediate [[CronExpressionBuilder]]
  * representation to [[CronExpression]].
  */
class TimePhase(protected val cronExpr: CronExpressionBuilder)
    extends MinuteSelection:

  /** Configures the time of the schedule.
    *
    * You can specify a `Time` using a string interpolated literal:
    * `time"13:30"`.
    *
    * The following snippet illustrates both options in a fully specified
    * schedule:
    * ```scala
    * Schedule.daily.at(time"13:30")
    * Schedule.daily.at(time"1:30 pm")
    * Schedule.daily.at(Time(hour = 13, minute = 30))
    * ```
    *
    * Due to the nature of cron, setting multiple times like this in a single
    * schedule is not possible. For this use case, you'll have to use the much
    * less intuitive alternative method overloads.
    */
  def at(time: Time): CronExpression = {
    cronExpr
      .copy(
        hour = Some(Field.of(time.hour)),
        minute = Some(Field.of(time.minute))
      )
      .build()
  }

  def at(h: Hour, hs: Hour*): HourChosen = HourChosen(
    cronExpr
      .copy(hour = Some(Field.of(h, hs: _*)))
  )

  def at(hs: HourRange): HourChosen = HourChosen(
    cronExpr.copy(hour = Some(hs.toField))
  )

  /** Configures the scheduling for the given range of hours.
    *
    * Note that the end hour is exclusive! For example, scheduling
    * `.between(9.h, 17.h).at(0.min)` will not fire at 17:00. This method is a
    * more readable alternative to using range syntax like
    * `.at(9.h until 17.h)`. This is in contrast to the alternative range syntax
    * `.at(9.h to 17.h)` which is inclusive!
    */
  def between(startInclusive: Hour, endExclusive: Hour): HourChosen = {
    if (startInclusive == endExclusive) {
      throw IllegalArgumentException(
        "The start and end of the range defined by `.between` must not be same."
      )
    }
    // TODO: replace inclusive range with exclusive, i.e. `until` when it's implemented
    val equivalentInclusiveEnd = Hour(endExclusive.value - 1)
    at(Range[Hour](from = startInclusive, to = equivalentInclusiveEnd))
  }

  // Not offering a `every(hours: Hour)` method similar to `every(minutes: Minute)` is a deliberate choice.
  // The problem with this method would be that it allows continuing with all methods of `HourChosen` which
  // allows the following: Schedule.daily.every(2.h).at(15.min, 45.min)
  // Unfortunately, at first glance this would suggest that it runs every 2 hours.
  // The intervals aren't equal and neither of them are actually 2 hours.
  // Additionally, cron schedules cannot schedule regular hourly intervals for intervals that aren't even divisors of 24.
  // Therefore, it was decided to offer `every{Two, Three, Four}Hours` instead, which circumvents both issues.

  /** Configures the scheduling to every hour at the given minute mark,
    * defaulting to the full hour.
    */
  def everyHour(at: Minute = 0.min): CronExpression = everyXHours(1)(at)

  /** Configures the scheduling to every hour on the hour. */
  val everyHour: CronExpression = everyHour(at = 0.min)

  /** Configures the scheduling to every second hour (starting at midnight) at
    * the given minute mark, defaulting to the full hour.
    */
  def everyTwoHours(at: Minute = 0.min): CronExpression = everyXHours(2)(at)

  /** Configures the scheduling to every third hour (starting at midnight) at
    * the given minute mark, defaulting to the full hour.
    */
  def everyThreeHours(at: Minute = 0.min): CronExpression = everyXHours(3)(at)

  /** Configures the scheduling to every fourth hour (starting at midnight) at
    * the given minute mark, defaulting to the full hour.
    */
  def everyFourHours(at: Minute = 0.min): CronExpression = everyXHours(4)(at)

  private def everyXHours(x: 1 | 2 | 3 | 4)(at: Minute): CronExpression = {
    val hourField = x match {
      case 1 => Field.all
      case x =>
        val hourMarks = (0 until 24 by x).map(Hour(_))
        Field.of(hourMarks.head, hourMarks.tail: _*)
    }
    cronExpr
      .copy(
        hour = Some(hourField),
        minute = Some(Field.of(at))
      )
      .build()
  }

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
