package cronh.render

import cronh.domain.*

/** A cron dialect: supplies the dialect-specific [[cronh.domain.DayOfWeek]]
  * numbering and renders a full [[cronh.domain.CronExpression]].
  *
  * Everything that varies across cron flavors (weekday numbering, future `?` or
  * `L` tokens) belongs here rather than in the domain model (DESIGN.md §2.11,
  * §2.15).
  */
trait CronDialect {

  /** Dialect-specific numbering of week days. */
  given dayOfWeekRender: Render[DayOfWeek]

  /** Renders the day-of-week field. Overridable so a dialect can fix up
    * constructs its numbering cannot express as-is (see
    * [[UnixCronDialect.renderDayOfWeek]]).
    */
  protected def renderDayOfWeek(field: Field[DayOfWeek]): String =
    renderField(field)

  /** Renders the five fields, space-separated, in cron order. */
  final def render(expression: CronExpression): String =
    List(
      renderField(expression.minute),
      renderField(expression.hour),
      renderField(expression.dayOfMonth),
      renderField(expression.month),
      renderDayOfWeek(expression.dayOfWeek)
    ).mkString(" ")
}

object CronDialect {

  /** The default dialect, [[UnixCronDialect]], in implicit scope so
    * `expression.toCron` works without extra imports. Shadow with a local given
    * to render under another dialect.
    */
  given default: CronDialect = UnixCronDialect
}

extension (expression: CronExpression) {

  /** Renders this expression as a cron string under the given dialect (defaults
    * to [[UnixCronDialect]]).
    */
  def toCron(using dialect: CronDialect): String = dialect.render(expression)
}
