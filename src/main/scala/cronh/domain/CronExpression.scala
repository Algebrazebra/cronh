package cronh.domain

/** A POSIX cron expression with five fields. */
final case class CronExpression(
    minute: Field[Minute],
    hour: Field[Hour],
    dayOfMonth: Field[MonthDay],
    month: Field[Month],
    dayOfWeek: Field[DayOfWeek]
)
