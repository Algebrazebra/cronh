package cronh.domain

/** Represents a cron term as per the Vixie grammar. Can be understood as the
  * "shape" of a cron field.
  */
sealed trait Term[+A]

object Term {
  case object All extends Term[Nothing]

  final case class Single[+A](value: A) extends Term[A]

  /** An inclusive range `[from, to]`. Construct via [[Range.apply]], which
    * enforces `from <= to`.
    */
  final case class Range[+A] private (from: A, to: A) extends Term[A]

  object Range {

    /** Smart constructor for [[Range]].
      *
      * Requires `from <= to`; throws [[IllegalArgumentException]] otherwise.
      */
    def apply[A: Ordering](from: A, to: A): Range[A] = {
      require(
        Ordering[A].lteq(from, to),
        s"Start of range must be <= end of range."
      )
      new Range(from, to)
    }
  }
}
