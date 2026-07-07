package cronh.dsl

import munit.FunSuite

class TimeTest extends FunSuite {

  // Test: military time works within domain boundaries;
  // Test: military time fails outside of domain boundaries;
  // Test: 12h time fails outside of domain boundaries;
  // Test 12h time doesn't care about capitalization of am/pm
  // Test 12h time doesn't care about leading zeros
  // Test 12h time doesn't need minutes (i.e. 1 pm)
  // Test 12h time doesn't need space between 1 and pm
  // WHAT ABOUT 24:00? What about 12 am / pm and 0 am/ pm?
  // 12 am means midnight, 12 pm means noon
  test(" ") {
    compileErrors("time\"25:00\"")
  }

}
