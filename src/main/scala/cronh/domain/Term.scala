package cronh.domain

/** Represents a cron term as per the POSIX grammar. Can be understood as the
  * "shape" of a cron field.
  */
sealed trait Term[+A]

object Term {
  case object All extends Term[Nothing]

  final case class Single[+A](value: A) extends Term[A]

  final case class Range[+A](from: A, to: A) extends Term[A]
}
