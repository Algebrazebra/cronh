package cronh.dsl

import cronh.domain.fieldTypes.{Hour, Minute}
import cronh.dsl.Time.TimeStringContext
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*
import cronh.domain.Generators.{given_Arbitrary_Hour, given_Arbitrary_Minute}

class TimeTest extends ScalaCheckSuite {

  property("Valid 24h time can be created from domain boundaries") = forAll {
    (h: Hour, m: Minute) =>
      val zeroPaddedHour = "%02d".format(h.value)
      val zeroPaddedMinute = "%02d".format(m.value)
      val time = time"$zeroPaddedHour:$zeroPaddedMinute"
      assertEquals(time.hour, h)
      assertEquals(time.minute, m)
  }

  test("24:00 is not a valid time and raises a compile time error") {
    compileErrors("time\"24:00\"")
  }

  test("0 am and 0 pm is not a valid time and raises a compile time error") {
    compileErrors("time\"0 am\"")
    compileErrors("time\"0 pm\"")
  }

  test("In 12h time, the space before the meridiem is optional") {
    for {
      h <- 1 to 12
    } yield {
      assert(time"${h}am" == time"$h am")
    }
  }

  test("In 12h time, omitting the minutes is valid to get a round hour") {
    for {
      h <- 1 to 12
    } yield {
      assertEquals(time"${h % 12}:00", time"$h am")
      assertEquals(time"${h % 12 + 12}:00", time"$h pm")
    }
  }

  test("In 12h time, 12 am means midnight and 12 pm means noon") {
    val midnight = time"12:00 am"
    val noon = time"12:00 pm"
    assertEquals(midnight.hour, Hour(0))
    assertEquals(midnight.minute, Minute(0))
    assertEquals(noon.hour, Hour(12))
    assertEquals(noon.minute, Minute(0))
  }

  test("capitalization of am and pm doesn't matter") {
    val amCapitalizations = Seq("am", "AM", "aM", "Am")
    val pmCapitalizations = Seq("pm", "PM", "pM", "Pm")
    for {
      h <- 1 to 12
      am <- amCapitalizations
      pm <- pmCapitalizations
    } yield {
      assertEquals(time"$h $am", time"${h % 12}:00")
      assertEquals(time"$h $pm", time"${h % 12 + 12}:00")
    }
  }

  test("In 12h time, the hour can be padded to two digits") {
    for {
      h <- 1 to 9
    } yield {
      assertEquals(time"${"%02d".format(h)} am", time"$h am")
    }
  }

}
