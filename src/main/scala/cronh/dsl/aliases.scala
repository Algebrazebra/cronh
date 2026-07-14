package cronh.dsl

import cronh.domain.fieldTypes.DayOfWeek
import cronh.dsl.Time.TimeStringContext

/** Short alias for [[DayOfWeek.Monday]]. */
val Mon: DayOfWeek = DayOfWeek.Monday

/** Short alias for [[DayOfWeek.Tuesday]]. */
val Tue: DayOfWeek = DayOfWeek.Tuesday

/** Short alias for [[DayOfWeek.Wednesday]]. */
val Wed: DayOfWeek = DayOfWeek.Wednesday

/** Short alias for [[DayOfWeek.Thursday]]. */
val Thu: DayOfWeek = DayOfWeek.Thursday

/** Short alias for [[DayOfWeek.Friday]]. */
val Fri: DayOfWeek = DayOfWeek.Friday

/** Short alias for [[DayOfWeek.Saturday]]. */
val Sat: DayOfWeek = DayOfWeek.Saturday

/** Short alias for [[DayOfWeek.Sunday]]. */
val Sun: DayOfWeek = DayOfWeek.Sunday

/** Long alias for [[DayOfWeek.Monday]], e.g. `Monday to Friday`.
  */
val Monday: DayOfWeek = DayOfWeek.Monday

/** Long alias for [[DayOfWeek.Tuesday]]. */
val Tuesday: DayOfWeek = DayOfWeek.Tuesday

/** Long alias for [[DayOfWeek.Wednesday]]. */
val Wednesday: DayOfWeek = DayOfWeek.Wednesday

/** Long alias for [[DayOfWeek.Thursday]]. */
val Thursday: DayOfWeek = DayOfWeek.Thursday

/** Long alias for [[DayOfWeek.Friday]]. */
val Friday: DayOfWeek = DayOfWeek.Friday

/** Long alias for [[DayOfWeek.Saturday]]. */
val Saturday: DayOfWeek = DayOfWeek.Saturday

/** Long alias for [[DayOfWeek.Sunday]]. */
val Sunday: DayOfWeek = DayOfWeek.Sunday

/** Alias for midnight, i.e., 00:00. */
val Midnight: Time = time"00:00"

/** Alias for noon, i.e., 12:00. */
val Noon: Time = time"12:00"

/** Monday through Friday, e.g., `Schedule.daily.on(Weekdays)`. */
val Weekdays: DayOfWeekRange = Range[DayOfWeek](from = Monday, to = Friday)

/** Saturday and Sunday, e.g., `Schedule.daily.on(Weekends)`. */
val Weekends: DayOfWeekRange = Range[DayOfWeek](from = Saturday, to = Sunday)
