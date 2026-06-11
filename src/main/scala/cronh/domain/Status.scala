package cronh.domain

/** Phantom marker tracking whether a [[CronExpression]]'s time of day has been
  * set by the DSL.
  *
  * Never instantiated; exists only at the type level so that setting the time
  * twice (e.g. `.at(9.h).at(14.h)`) is a compile error instead of a silent
  * overwrite. The markers are nested in the companion to avoid shadowing
  * `scala.collection.immutable.Set`.
  */
sealed trait Status

object Status {

  /** The time of day has been explicitly set (e.g. via `.at`). */
  sealed trait Set extends Status

  /** The time of day is still at its default and may be set. */
  sealed trait Unset extends Status
}
