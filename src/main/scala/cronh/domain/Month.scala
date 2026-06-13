package cronh.domain

/** A cron month. */
enum Month private (val value: Int) {
  case January extends Month(1)
  case February extends Month(2)
  case March extends Month(3)
  case April extends Month(4)
  case May extends Month(5)
  case June extends Month(6)
  case July extends Month(7)
  case August extends Month(8)
  case September extends Month(9)
  case October extends Month(10)
  case November extends Month(11)
  case December extends Month(12)
}

object Month {
  given Ordering[Month] = Ordering.by(_.value)

  given DomainBounds[Month] with {
    val domain: IndexedSeq[Month] = Month.values.toIndexedSeq
  }
}
