package cronh.domain

import cronh.domain.fieldTypes.{DayOfWeek, Hour, Minute, Month, DayOfMonth}

/** A cron expression with five fields. */
final case class CronExpression(
    minute: Field[Minute],
    hour: Field[Hour],
    dayOfMonth: Field[DayOfMonth],
    month: Field[Month],
    dayOfWeek: Field[DayOfWeek]
)
