package cronh.dsl.aliases

import cronh.domain.fieldTypes.Month
import cronh.dsl.Range
import cronh.dsl.MonthRange

/** Alias for May. */
val May: Month = Month.May

/** Short alias for January. */
val Jan: Month = Month.January

/** Short alias for February. */
val Feb: Month = Month.February

/** Short alias for March. */
val Mar: Month = Month.March

/** Short alias for April. */
val Apr: Month = Month.April

/** Short alias for June. */
val Jun: Month = Month.June

/** Short alias for July. */
val Jul: Month = Month.July

/** Short alias for August. */
val Aug: Month = Month.August

/** Short alias for September. */
val Sep: Month = Month.September

/** Short alias for October. */
val Oct: Month = Month.October

/** Short alias for November. */
val Nov: Month = Month.November

/** Short alias for December. */
val Dec: Month = Month.December

/** Long alias for January. */
val January: Month = Month.January

/** Long alias for February. */
val February: Month = Month.February

/** Long alias for March. */
val March: Month = Month.March

/** Long alias for April. */
val April: Month = Month.April

/** Long alias for June. */
val June: Month = Month.June

/** Long alias for July. */
val July: Month = Month.July

/** Long alias for August. */
val August: Month = Month.August

/** Long alias for September. */
val September: Month = Month.September

/** Long alias for October. */
val October: Month = Month.October

/** Long alias for November. */
val November: Month = Month.November

/** Long alias for December. */
val December: Month = Month.December

/** First calendar quarter from January to March. */
val CQ1: MonthRange = Range[Month](from = Jan, to = Mar)

/** Second calendar quarter from April to June. */
val CQ2: MonthRange = Range[Month](from = Apr, to = Jun)

/** Third calendar quarter from July to September. */
val CQ3: MonthRange = Range[Month](from = Jul, to = Sep)

/** Fourth calendar quarter from October to December. */
val CQ4: MonthRange = Range[Month](from = Oct, to = Dec)
