package faith.knowledge.common

import munit.FunSuite

class CurrenciesSpec extends FunSuite {
  test("Currencies apply should generate a currency if no one is found") {
    val santiCoin = Currencies("SCN")
    assertEquals(santiCoin.symbol, "SCN")
    assertEquals(santiCoin.code, "SCN")
    assertEquals(santiCoin.name, "SCN")
    assertEquals(santiCoin.formatDecimals, 15)
  }
}
