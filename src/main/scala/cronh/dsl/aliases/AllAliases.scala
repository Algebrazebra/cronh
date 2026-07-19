package cronh.dsl.aliases

import cronh.dsl.aliases.{MonthAliases, TimeAliases, WeekdayAliases}

/** Named calendar and time aliases for the scheduling DSL. */
object AllAliases extends MonthAliases with WeekdayAliases with TimeAliases
