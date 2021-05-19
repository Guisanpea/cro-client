package faith.knowledge.common

import faith.knowledge.common.Currencies.USDT
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import squants.market.{BTC, USD}

class InstrumentSpec extends AnyWordSpec with Matchers with TryValues {
  "Instrument from name should return an instrument if valid format is passed" in {
    val instrument = Instrument.fromName("BTC_USDT")
    instrument.success.value.base shouldBe BTC
    instrument.success.value.quote shouldBe USDT
  }

  "Instrument from name should return a failure if invalid format is passed" in {
    val instrument = Instrument.fromName("BTCUSDT")
    instrument.failure.exception.isInstanceOf[IllegalArgumentException]
  }
}
