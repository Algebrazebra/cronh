package cronh.render

import cronh.domain.fieldTypes.DayOfWeek
import cronh.domain.{Field, Term}

/** The classic Unix/Vixie dialect: Sunday = 0, Monday = 1, ..., Saturday = 6.
  */
object UnixCronDialect extends CronDialect {

  given dayOfWeekRender: Render[DayOfWeek] = { day =>
    (day match {
      case DayOfWeek.Sunday    => 0
      case DayOfWeek.Monday    => 1
      case DayOfWeek.Tuesday   => 2
      case DayOfWeek.Wednesday => 3
      case DayOfWeek.Thursday  => 4
      case DayOfWeek.Friday    => 5
      case DayOfWeek.Saturday  => 6
    }).toString
  }

  /** The model orders weekdays Monday-first, so a range ending on Sunday (e.g.
    * `Friday-Sunday`) would naively render as the inverted — and invalid —
    * `5-0`. Such ranges are split into the equivalent POSIX-valid list instead:
    * `5-6,0`.
    */
  override protected def renderDayOfWeek(field: Field[DayOfWeek]): String =
    field.terms
      .map {
        case Term.Range(from, DayOfWeek.Sunday) =>
          from match {
            case DayOfWeek.Sunday   => "0"
            case DayOfWeek.Saturday => "6,0"
            case earlier            => s"${dayOfWeekRender.render(earlier)}-6,0"
          }
        case term => renderTerm(term)
      }
      .mkString(",")
}
