// Realistic cronh schedules.
//
// Illustrative only — this directory is not compiled by the build, but every
// expression below is mirrored by an acceptance test in src/test (see
// cronh.dsl.ScheduleTest and cronh.render.HumanReadableTest), so the expected
// strings stay honest.

import cronh.domain.Month
import cronh.dsl.*
import cronh.render.*

object Schedules {

  // Nightly backup at half past two in the morning.
  val nightlyBackup = Schedule.daily.at(2.h, 30.m)
  // nightlyBackup.toCron == "30 2 * * *"

  // Business-hours health check: every hour from 9 to 17, Monday to Friday.
  val healthCheck = Schedule.weekdays.between(9.h, 17.h)
  // healthCheck.toCron == "0 9-17 * * 1-5"

  // Stand-up reminder on Monday and Friday at noon.
  val standupReminder = Schedule.on(Mon, Fri).at(noon)
  // standupReminder.toCron == "0 12 * * 1,5"

  // Mid-week deploy window, expressed as a natural range: Tue to Thu at 9 AM.
  val deployWindow = Schedule.daily.on(Tue to Thu).at(9.h)
  // deployWindow.toCron == "0 9 * * 2-4"

  // Payroll on the 1st and 15th at 6 AM.
  val payroll = Schedule.onThe(1.dom, 15.dom).at(6.h)
  // payroll.toCron == "0 6 1,15 * *"

  // Weekend batch job at 8 AM.
  val weekendBatch = Schedule.weekends.at(8.h)
  // weekendBatch.toCron == "0 8 * * 6,0"

  // Quarterly-ish report: first of the month at midnight, summer months only.
  // Coarse→fine: months, then day-of-month, then time.
  val summerReport =
    Schedule.in(Month.June, Month.July).onThe(1.dom).at(midnight)
  // summerReport.toCron == "0 0 1 6,7 *"

  // Human-readable rendering for logs and UIs:
  // standupReminder.humanReadable == "At 12:00 PM, on Monday and Friday"

  // These do not compile — the phantom types reject them:
  // Schedule.daily.at(9.h).at(14.h)   // hour set twice
  // Schedule.on(Mon).onThe(1.dom)     // day-of-week vs day-of-month conflict
  // Schedule.daily.at(9.h).in(June)   // coarse field after a finer one
  // 25.h                              // Hour must be between 0 and 23
}
