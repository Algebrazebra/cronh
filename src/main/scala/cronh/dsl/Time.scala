package cronh.dsl

import cronh.domain.fieldTypes.{Hour, Minute}
import cronh.dsl.TimeParseError.{InvalidHour, InvalidMinute, InvalidTimeFormat}

/** Represents the time in 24h format HH:MM.
  *
  * Since this is used in the context of cron expressions, midnight must be
  * represented as 00:00. The time 24:00 cannot be represented. This is because
  * a cron expression doesn't accept 24 in the hour field.
  */
final case class Time(hour: Hour, minute: Minute) {

  /** Returns a string representation of the time in the 24h format HH:MM. */
  override def toString: String = f"${hour.value}%02d:${minute.value}%02d"
}
object Time {

  private val rgx_12hClock = """(\d{1,2}):(\d{2})\s*([APap][Mm])""".r
  private val rgx_12hClockHoursOnly = """(\d{1,2})\s*([APap][Mm])""".r
  private val rgx_24hClock = """(\d{1,2}):(\d{2})""".r

  /** Parses a string into a `Time` value.
    *
    * The input string can be either in the 12h or 24h format. However, input
    * strings in the 12h format are always converted to the 24h format, that's
    * the canconcial representation with [[Time]].
    */
  def parse(str: String): Either[TimeParseError, Time] = str match {
    case rgx_24hClock(hourStr, minuteStr) =>
      for {
        hour <- parseHour(hourStr)
        minute <- parseMinute(minuteStr)
      } yield Time(hour, minute)
    case rgx_12hClock(hourStr, minuteStr, period) =>
      for {
        hour <- parseHour(hourStr)
        _ <- isValid12hFormatHour(hour)
        minute <- parseMinute(minuteStr)
        meridiem <- parseMeridiem(period)
        convertedHour <- convertTo24Hour(hour, meridiem)
      } yield Time(convertedHour, minute)
    case rgx_12hClockHoursOnly(hourStr, period) =>
      for {
        hour <- parseHour(hourStr)
        _ <- isValid12hFormatHour(hour)
        meridiem <- parseMeridiem(period)
        convertedHour <- convertTo24Hour(hour, meridiem)
      } yield Time(convertedHour, Minute(0))
    case bullshit =>
      Left(
        InvalidTimeFormat(
          s"Error parsing time literal: expected HH:MM or HH:MM AM/PM, got: $bullshit"
        )
      )
  }

  /** Parses an integer represented as a string into an `Hour` value. */
  private def parseHour(hourStr: String): Either[TimeParseError, Hour] =
    hourStr.toIntOption
      .toRight[TimeParseError](
        InvalidHour(s"Error parsing hour: failed to parse \"$hourStr\" as Int.")
      )
      .flatMap(makeHour)

  /** Instantiates an `Hour` domain entity from an integer. */
  private def makeHour(h: Int): Either[TimeParseError, Hour] = try {
    Right(Hour(h))
  } catch {
    case _: IllegalArgumentException =>
      Left(
        InvalidHour(
          s"Error parsing hour: Hour must be between ${Hour.MinValue} and ${Hour.MaxValue}, got $h."
        )
      )
  }

  /** Parses an integer represented as a string into a `Minute` value. */
  private def parseMinute(minuteStr: String): Either[TimeParseError, Minute] =
    minuteStr.toIntOption
      .toRight[TimeParseError](
        InvalidMinute(
          s"Error parsing minute: failed to parse \"$minuteStr\" as Int."
        )
      )
      .flatMap(makeMinute)

  /** Instantiates a `Minute` domain entity from an integer. */
  private def makeMinute(m: Int): Either[TimeParseError, Minute] = try {
    Right(Minute(m))
  } catch {
    case _: IllegalArgumentException =>
      Left(
        InvalidMinute(
          s"Error parsing minute: Minute must be between ${Minute.MinValue} and ${Minute.MaxValue}, got $m."
        )
      )
  }

  /** Parses the meridiem modifier of the 12h format (i.e., "am" or "pm"). */
  private def parseMeridiem(
      amPmStr: String
  ): Either[TimeParseError, "am" | "pm"] = amPmStr.toLowerCase match {
    case "am"     => Right("am")
    case "pm"     => Right("pm")
    case bullshit =>
      Left(
        InvalidTimeFormat(
          s"Error parsing the meridiem: expected 'am' or 'pm', but got $bullshit"
        )
      )
  }

  /** Validates that the hour value is between 1 and 12 (inclusively). This is a
    * requirement in the 12h format (e.g., 1:00 am).
    */
  private def isValid12hFormatHour(
      hour: Hour
  ): Either[TimeParseError, Hour] = {
    val minValue12hFormat = 1
    val maxValue12hFormat = 12
    if (hour.value < minValue12hFormat)
      Left(
        InvalidHour(
          s"Error parsing hour: In the 12h format, hour must be between $minValue12hFormat and $maxValue12hFormat, got $hour."
        )
      )
    else if (hour.value > maxValue12hFormat) {
      Left(
        InvalidHour(
          s"Error parsing hour: In the 12h format, hour must be between $minValue12hFormat and $maxValue12hFormat, got $hour."
        )
      )
    } else Right(hour)
  }

  /** Converts the parsed hour value from the 12h format to the 24h format. */
  private def convertTo24Hour(
      hour: Hour,
      meridiem: "am" | "pm"
  ): Either[TimeParseError, Hour] = {
    isValid12hFormatHour(hour).map { hour =>
      Hour(
        (hour.value % 12) + (if (meridiem.equalsIgnoreCase("pm")) 12 else 0)
      )
    }
  }

  implicit class TimeStringContext(private val sc: StringContext)
      extends AnyVal {
    def time(args: Any*): Time = parse(sc.s(args*))
      .fold(err => throw new IllegalArgumentException(err.message), identity)
  }
}

sealed trait TimeParseError {
  def message: String
}
object TimeParseError {
  final case class InvalidHour(message: String) extends TimeParseError
  final case class InvalidMinute(message: String) extends TimeParseError
  final case class InvalidTimeFormat(message: String) extends TimeParseError
}
