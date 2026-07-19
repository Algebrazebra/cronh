package cronh.domain

import cronh.domain.fieldTypes.{DayOfWeek, Hour, Minute, Month, DayOfMonth}

/** A five-field cron expression in cron order: minute, hour, day of month,
  * month, and day of week.
  */
final case class CronExpression(
    minute: Field[Minute],
    hour: Field[Hour],
    dayOfMonth: Field[DayOfMonth],
    month: Field[Month],
    dayOfWeek: Field[DayOfWeek]
)
