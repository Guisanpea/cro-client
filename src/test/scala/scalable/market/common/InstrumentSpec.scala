package scalable.market.common

import munit.FunSuite
import scalable.market.common.Currencies.USDT
import squants.market.BTC

class InstrumentSpec extends FunSuite with TryValues {
  test("Instrument from name should return an instrument if valid format is passed") {
    val instrument = Instrument.fromName("BTC_USDT")
    assertEquals(instrument.success.base, BTC)
    assertEquals(instrument.success.quote, USDT)
  }

  test("Instrument from name should return a failure if invalid format is passed") {
    val instrument = Instrument.fromName("BTCUSDT")
    assert(instrument.failure.isInstanceOf[IllegalArgumentException])
  }
}
