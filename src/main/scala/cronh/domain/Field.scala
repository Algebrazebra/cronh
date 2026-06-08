package cronh.domain

/** A cron field: a non-empty list of [[Term]]s.
  *
  * The non-empty invariant is enforced by the private constructor and the
  * factory methods in the companion object. Redundant terms (duplicates,
  * subsumed ranges) are accepted as valid input; use a future `normalized`
  * extension to canonicalize if needed.
  *
  * Combine two fields with `++` to express cron list syntax (e.g. `1,5-10,15`).
  */
final case class Field[+A] private (terms: ::[Term[A]]) {

  def ++[B >: A](other: Field[B]): Field[B] =
    Field(::(terms.head, terms.tail ++ other.terms))
}

object Field {
  import Term._

  /** Matches all values in the field's domain. Equivalent to `*`. */
  val all: Field[Nothing] =
    Field(::(All, Nil))

  /** Matches a single value. */
  def single[A](value: A): Field[A] =
    Field(::(Single(value), Nil))

  /** Matches all values in the inclusive range `[from, to]`.
    *
    * Requires `from <= to`; throws [[IllegalArgumentException]] otherwise.
    */
  def range[A: Ordering](from: A, to: A): Field[A] = {
    require(
      Ordering[A].lteq(from, to),
      s"Start of range must be <= end of range."
    )
    Field(::(Range(from, to), Nil))
  }

  /** Matches any of the given discrete values.
    *
    * Equivalent to cron list syntax when more than one value is supplied, e.g.
    * `Field.of(Minute(0), Minute(15), Minute(30))` → `0,15,30`.
    */
  def of[A](first: A, rest: A*): Field[A] =
    Field(::(Single(first), rest.toList.map(Single(_))))

  /** Composes multiple [[Term]]s into a single field.
    *
    * Use this when you need to mix term shapes, e.g. a single value alongside
    * a range: `Field.from(Term.Single(Minute(1)), Term.Range(Minute(5), Minute(10)))`.
    * For multiple discrete values, prefer [[of]].
    */
  def from[A](first: Term[A], rest: Term[A]*): Field[A] =
    Field(::(first, rest.toList))

}
