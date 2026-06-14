package cronh.dsl

import cronh.domain.{DayOfWeek, Hour}

/** Short alias for [[cronh.domain.DayOfWeek.Monday]]. */
val Mon: DayOfWeek = DayOfWeek.Monday

/** Short alias for [[cronh.domain.DayOfWeek.Tuesday]]. */
val Tue: DayOfWeek = DayOfWeek.Tuesday

/** Short alias for [[cronh.domain.DayOfWeek.Wednesday]]. */
val Wed: DayOfWeek = DayOfWeek.Wednesday

/** Short alias for [[cronh.domain.DayOfWeek.Thursday]]. */
val Thu: DayOfWeek = DayOfWeek.Thursday

/** Short alias for [[cronh.domain.DayOfWeek.Friday]]. */
val Fri: DayOfWeek = DayOfWeek.Friday

/** Short alias for [[cronh.domain.DayOfWeek.Saturday]]. */
val Sat: DayOfWeek = DayOfWeek.Saturday

/** Short alias for [[cronh.domain.DayOfWeek.Sunday]]. */
val Sun: DayOfWeek = DayOfWeek.Sunday

/** Long alias for [[cronh.domain.DayOfWeek.Monday]], e.g. `Monday to Friday`.
  */
val Monday: DayOfWeek = DayOfWeek.Monday

/** Long alias for [[cronh.domain.DayOfWeek.Tuesday]]. */
val Tuesday: DayOfWeek = DayOfWeek.Tuesday

/** Long alias for [[cronh.domain.DayOfWeek.Wednesday]]. */
val Wednesday: DayOfWeek = DayOfWeek.Wednesday

/** Long alias for [[cronh.domain.DayOfWeek.Thursday]]. */
val Thursday: DayOfWeek = DayOfWeek.Thursday

/** Long alias for [[cronh.domain.DayOfWeek.Friday]]. */
val Friday: DayOfWeek = DayOfWeek.Friday

/** Long alias for [[cronh.domain.DayOfWeek.Saturday]]. */
val Saturday: DayOfWeek = DayOfWeek.Saturday

/** Long alias for [[cronh.domain.DayOfWeek.Sunday]]. */
val Sunday: DayOfWeek = DayOfWeek.Sunday

/** Midnight (hour 0), e.g. `Schedule.daily.at(midnight)`. */
val midnight: Hour = Hour(0)

/** Noon (hour 12), e.g. `Schedule.on(Mon, Fri).at(noon)`. */
val noon: Hour = Hour(12)

/** Monday through Friday, e.g. `Schedule.daily.on(Weekdays)`. */
val Weekdays: WeekdaySelector =
  WeekdaySelector.range(DayOfWeek.Monday, DayOfWeek.Friday)

/** Saturday and Sunday, e.g. `Schedule.daily.on(Weekends)`. */
val Weekends: WeekdaySelector =
  WeekdaySelector(DayOfWeek.Saturday, DayOfWeek.Sunday)
