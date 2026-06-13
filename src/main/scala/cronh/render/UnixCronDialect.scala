package cronh.render

import cronh.domain.DayOfWeek

/** The classic Unix/Vixie dialect: Sunday = 0, Monday = 1, ..., Saturday = 6.
  *
  * Ranges ending on Sunday (e.g. `Friday-Sunday`) would naively render as the
  * inverted, invalid `5-0`; the shared [[CronDialect.renderDayOfWeek]] splits
  * them at the term level before numbering, so this dialect emits the valid
  * `5-6,0` without needing its own override.
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
}
