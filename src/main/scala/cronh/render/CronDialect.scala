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

  /** Renders the day-of-week field.
    *
    * The model orders weekdays Monday-first, so a range whose upper bound is
    * `Sunday` (the maximum) is the only one that can invert under a dialect
    * whose numbering does not also put Sunday last — e.g. a Quartz-style
    * `Sun=1` dialect would otherwise render `Range(Saturday, Sunday)` as the
    * invalid `7-1`. Such ranges are rewritten *at the term level*, before the
    * dialect's numbering is applied, into the equivalent
    * `Range(from, Saturday)` plus a trailing `Single(Sunday)` (collapsing a
    * one-element range to a single), so every dialect inherits a valid
    * rendering. Overridable for dialects with needs beyond this.
    */
  protected def renderDayOfWeek(field: Field[DayOfWeek]): String =
    field.terms
      .flatMap(splitSundayTerminatedRange)
      .map(renderTerm(_))
      .mkString(",")

  private def splitSundayTerminatedRange(
      term: Term[DayOfWeek]
  ): List[Term[DayOfWeek]] =
    term match {
      case Term.Range(DayOfWeek.Sunday, DayOfWeek.Sunday) =>
        List(Term.Single(DayOfWeek.Sunday))
      case Term.Range(from, DayOfWeek.Sunday) =>
        val head =
          if (from == DayOfWeek.Saturday) Term.Single(DayOfWeek.Saturday)
          else Term.Range(from, DayOfWeek.Saturday)
        List(head, Term.Single(DayOfWeek.Sunday))
      case other => List(other)
    }

  /** Renders the five fields, space-separated, in cron order. */
  final def render(expression: CronExpression[?, ?]): String =
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

extension (expression: CronExpression[?, ?]) {

  /** Renders this expression as a cron string under the given dialect (defaults
    * to [[UnixCronDialect]]).
    */
  def toCron(using dialect: CronDialect): String = dialect.render(expression)
}
