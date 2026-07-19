// Realistic cronh schedules.
//
// Illustrative only — this directory is not compiled by the build, but every
// expression below is mirrored by an acceptance test in src/test (see
// cronh.dsl.ExamplesTest), so the expected strings stay honest.

import cronh.domain.fieldTypes.Month
import cronh.dsl.*
import cronh.render.*

object Schedules {

  // Nightly backup at half past two in the morning.
  val nightlyBackup = Schedule.daily.at(2.h).at(30.min)
  // nightlyBackup.toCron == "30 2 * * *"

  // Morning report on weekdays at 9 AM.
  val morningReport = Schedule.weekdays.at(9.h).at(0.min)
  // morningReport.toCron == "0 9 * * 1-5"

  // Stand-up reminder on Monday and Friday at noon.
  val standupReminder = Schedule.on(Mon, Fri).at(12.h).at(0.min)
  // standupReminder.toCron == "0 12 * * 1,5"

  // Payroll on the 1st and 15th at 6 AM.
  val payroll = Schedule.onThe(1.st, 15.th).at(6.h).at(0.min)
  // payroll.toCron == "0 6 1,15 * *"

  // Weekend batch job at 8 AM.
  val weekendBatch = Schedule.weekends.at(8.h).at(0.min)
  // weekendBatch.toCron == "0 8 * * 6,0"

  // Summer newsletter: June and July, every day at 9 AM.
  val summerNewsletter = Schedule.in(Month.June, Month.July).daily.at(9.h).at(0.min)
  // summerNewsletter.toCron == "0 9 * 6,7 *"

  // Mid-June deadline reminder: June 15th at 9 AM.
  val deadlineReminder = Schedule.in(Month.June).onThe(15.th).at(9.h).at(0.min)
  // deadlineReminder.toCron == "0 9 15 6 *"

  // Cleanup on the 1st of the month OR any Friday, at 4:30 AM.
  // Classic cron ORs the two day fields; cronh makes you spell the "or".
  val cleanup = Schedule.onThe(1.st).orOn(Fri).at(4.h).at(30.min)
  // cleanup.toCron == "30 4 1 * 5"

  // Human-readable rendering for logs and UIs:
  // morningReport.humanReadable == "At 9:00 AM, on weekdays"
  // cleanup.humanReadable == "At 4:30 AM, on day 1 of the month or on Friday"

  // These do not compile — the phase types reject them:
  // Schedule.at(9.h).at(0.min)                    // pick a recurrence first
  // Schedule.daily.at(9.h).at(0.min).at(14.h)     // time already set
  // Schedule.on(Mon).onThe(1.st)                  // spell the OR: .orOnThe(1.st)
  // Schedule.daily.in(Month.June)                 // months come first
  // 25.h                                          // Hour must be between 0 and 23
}
