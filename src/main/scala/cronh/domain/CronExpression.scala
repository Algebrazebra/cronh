package cronh.domain

/** A POSIX cron expression with five fields.
  *
  * Plain data: the five validated fields and nothing else. The fluent builder's
  * type-level state (which fields have been chosen, in what order) lives on
  * `cronh.dsl.Schedule`, not here — the domain is unit-agnostic data and
  * carries no DSL concern (DESIGN.md §2.1). Two expressions with the same five
  * fields are equal.
  */
final case class CronExpression(
    minute: Field[Minute],
    hour: Field[Hour],
    dayOfMonth: Field[MonthDay],
    month: Field[Month],
    dayOfWeek: Field[DayOfWeek]
)
