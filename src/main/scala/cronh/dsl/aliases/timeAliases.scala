package cronh.dsl.aliases

import cronh.dsl.Time
import cronh.dsl.Time.TimeStringContext

/** Alias for midnight, i.e., 00:00. */
val Midnight: Time = time"00:00"

/** Alias for noon, i.e., 12:00. */
val Noon: Time = time"12:00"
