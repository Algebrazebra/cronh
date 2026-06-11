package cronh.render

import cronh.domain.*

/** Typeclass rendering a domain value as its cron-string token.
  *
  * Instances for the dialect-independent units ([[cronh.domain.Minute]],
  * [[cronh.domain.Hour]], [[cronh.domain.MonthDay]], [[cronh.domain.Month]])
  * live in the companion. There is deliberately no [[cronh.domain.DayOfWeek]]
  * instance here: weekday numbering varies by dialect (Unix counts Sunday as 0,
  * Quartz as 1), so that instance is provided by the [[CronDialect]] in use
  * (DESIGN.md §2.11).
  */
trait Render[A] {
  def render(value: A): String
}

object Render {

  def apply[A](using r: Render[A]): Render[A] = r

  given Render[Minute] = _.value.toString
  given Render[Hour] = _.value.toString
  given Render[MonthDay] = _.value.toString
  given Render[Month] = _.value.toString
}

/** Renders a single [[cronh.domain.Term]] using the unit's [[Render]] instance:
  * `All` as `*`, a single value as itself, a range as `from-to`.
  */
def renderTerm[A](term: Term[A])(using r: Render[A]): String =
  term match {
    case Term.All             => "*"
    case Term.Single(value)   => r.render(value)
    case Term.Range(from, to) => s"${r.render(from)}-${r.render(to)}"
  }

/** Renders a [[cronh.domain.Field]] as its comma-separated cron form. */
def renderField[A](field: Field[A])(using Render[A]): String =
  field.terms.map(term => renderTerm(term)).mkString(",")
