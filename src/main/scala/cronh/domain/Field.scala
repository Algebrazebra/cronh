package cronh.domain

import cronh.domain.fieldTypes.DomainBounds

/** A cron field: a non-empty list of [[Term]]s.
  *
  * The non-empty invariant is enforced by the private constructor and the
  * factory methods in the companion object. Redundant terms (duplicates,
  * subsumed ranges) are accepted as valid input; use the opt-in [[normalized]]
  * extension to canonicalize when needed.
  *
  * Combine two fields with `++` to express cron list syntax (e.g. `1,5-10,15`).
  */
final case class Field[+A] private (terms: ::[Term[A]]) {

  def ++[B >: A](other: Field[B]): Field[B] =
    Field(::(terms.head, terms.tail ::: other.terms))
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
  def range[A: Ordering](from: A, to: A): Field[A] =
    Field(::(Range(from, to), Nil))

  /** Matches any of the given discrete values.
    *
    * Equivalent to cron list syntax when more than one value is supplied, e.g.
    * `Field.of(Minute(0), Minute(15), Minute(30))` → `0,15,30`.
    */
  def of[A](first: A, rest: A*): Field[A] =
    Field(::(Single(first), rest.toList.map(Single(_))))

  /** Composes multiple [[Term]]s into a single field.
    *
    * Use this when you need to mix term shapes, e.g. a single value alongside a
    * range:
    * `Field.from(Term.Single(Minute(1)), Term.Range(Minute(5), Minute(10)))`.
    * For multiple discrete values, prefer [[of]].
    */
  def from[A](first: Term[A], rest: Term[A]*): Field[A] =
    Field(::(first, rest.toList))

  extension [A](field: Field[A]) {

    /** The canonical form of this field under Vixie OR semantics.
      *
      * A field denotes the union of its terms, so duplicates, overlapping or
      * adjacent ranges, and values subsumed by a range are all redundant. This
      * operation removes them:
      *
      *   - any field containing [[Term.All]] collapses to [[Field.all]]
      *   - a field covering the entire domain (e.g. `0-59` for minutes)
      *     collapses to [[Field.all]]
      *   - overlapping, adjacent, and duplicate terms merge into minimal runs,
      *     emitted in ascending order as [[Term.Single]] (run of one) or
      *     [[Term.Range]] (run of two or more)
      *
      * Normalization is opt-in: unnormalized fields are valid and render
      * faithfully (DESIGN.md §4.4).
      */
    def normalized(using ord: Ordering[A], bounds: DomainBounds[A]): Field[A] =
      if (field.terms.contains(Term.All)) all
      else {
        val domain = bounds.domain
        val indexOf = bounds.indexOf
        val covered = field.terms
          .flatMap {
            case Term.All             => domain.indices
            case Term.Single(value)   => indexOf(value) :: Nil
            case Term.Range(from, to) => indexOf(from) to indexOf(to)
          }
          .distinct
          .sorted
        if (covered.size == domain.size) all
        else {
          val runs = covered
            .foldLeft(List.empty[(Int, Int)]) {
              case ((start, end) :: rest, i) if i == end + 1 =>
                (start, i) :: rest
              case (acc, i) => (i, i) :: acc
            }
            .reverse
          val canonical = runs.map {
            case (lo, hi) if lo == hi => Single(domain(lo))
            case (lo, hi)             => Range(domain(lo), domain(hi))
          }
          Field(::(canonical.head, canonical.tail))
        }
      }

    /** True when [[normalized]] would return this field unchanged. */
    def isNormalized(using Ordering[A], DomainBounds[A]): Boolean =
      field == field.normalized
  }

}
