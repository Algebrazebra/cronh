package cronh.render

import cronh.domain.*

extension (expression: CronExpression[?, ?, ?]) {

  /** An English description of the schedule, e.g. `"At 2:30 PM, every day"` or
    * `"At 9:00 AM, on weekdays"`.
    *
    * Times are formatted as 12-hour clock with AM/PM. Recognizes the common
    * idioms `Monday-Friday` ("on weekdays") and `Saturday,Sunday` ("on
    * weekends"); anything else falls back to a literal field description. When
    * both day fields are constrained they are joined with "or", reflecting
    * Vixie OR semantics (DESIGN.md §4.5).
    */
  def humanReadable: String = HumanReadable.describe(expression)
}

private object HumanReadable {

  def describe(expression: CronExpression[?, ?, ?]): String = {
    val phrases =
      timePhrase(expression.minute, expression.hour) ::
        dayPhrases(expression.dayOfMonth, expression.dayOfWeek) :::
        monthPhrase(expression.month).toList
    phrases.mkString(", ")
  }

  private def timePhrase(minute: Field[Minute], hour: Field[Hour]): String =
    (minute.terms, hour.terms) match {
      case (Term.Single(m) :: Nil, Term.Single(h) :: Nil) =>
        s"At ${clock(h, m)}"
      case (Term.All :: Nil, Term.All :: Nil) =>
        "Every minute"
      case (Term.Single(m) :: Nil, Term.All :: Nil) =>
        s"At minute ${m.value} past every hour"
      case (Term.All :: Nil, Term.Single(h) :: Nil) =>
        s"Every minute between ${clock(h, Minute(0))} and ${clock(h, Minute(59))}"
      case (Term.Single(m) :: Nil, Term.Range(from, to) :: Nil) =>
        s"At minute ${m.value} past every hour from ${hourName(from)} to ${hourName(to)}"
      case _ =>
        s"At minute ${describe(minute)(_.value.toString)} " +
          s"past hour ${describe(hour)(_.value.toString)}"
    }

  private def dayPhrases(
      dayOfMonth: Field[MonthDay],
      dayOfWeek: Field[DayOfWeek]
  ): List[String] = {
    val monthDays = dayOfMonth.terms match {
      case Term.All :: Nil => None
      case _               =>
        Some(s"on day ${describe(dayOfMonth)(_.value.toString)} of the month")
    }
    val weekDays = dayOfWeek.terms match {
      case Term.All :: Nil                                       => None
      case Term.Range(DayOfWeek.Monday, DayOfWeek.Friday) :: Nil =>
        Some("on weekdays")
      case terms if isWeekend(terms) => Some("on weekends")
      case _ => Some(s"on ${describe(dayOfWeek)(_.toString)}")
    }
    (monthDays, weekDays) match {
      case (None, None) => List("every day")
      // Vixie fires when either day field matches, hence "or".
      case (Some(byDate), Some(byWeekday)) => List(s"$byDate or $byWeekday")
      case _ => monthDays.toList ::: weekDays.toList
    }
  }

  private def monthPhrase(month: Field[Month]): Option[String] =
    month.terms match {
      case Term.All :: Nil => None
      case _               => Some(s"in ${describe(month)(_.toString)}")
    }

  private def isWeekend(terms: List[Term[DayOfWeek]]): Boolean = {
    val weekend: scala.collection.Set[Term[DayOfWeek]] =
      scala.collection.Set(
        Term.Single(DayOfWeek.Saturday),
        Term.Single(DayOfWeek.Sunday)
      )
    terms.toSet == weekend ||
    terms == List(Term.Range(DayOfWeek.Saturday, DayOfWeek.Sunday))
  }

  private def describe[A](field: Field[A])(show: A => String): String =
    joinAnd(field.terms.map {
      case Term.All             => "every"
      case Term.Single(value)   => show(value)
      case Term.Range(from, to) => s"${show(from)} through ${show(to)}"
    })

  private def joinAnd(parts: List[String]): String =
    if (parts.sizeIs <= 1) parts.mkString
    else s"${parts.init.mkString(", ")} and ${parts.last}"

  private def clock(hour: Hour, minute: Minute): String =
    f"${hour12(hour)}:${minute.value}%02d ${suffix(hour)}"

  private def hourName(hour: Hour): String =
    s"${hour12(hour)} ${suffix(hour)}"

  private def hour12(hour: Hour): Int =
    hour.value % 12 match {
      case 0     => 12
      case other => other
    }

  private def suffix(hour: Hour): String =
    if (hour.value < 12) "AM" else "PM"
}
