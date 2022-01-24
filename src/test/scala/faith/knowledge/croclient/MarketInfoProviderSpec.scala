package faith.knowledge.croclient

import cats.effect.IO
import cats.implicits._
import com.crypto.response.InstrumentResult
import faith.knowledge.common.Currencies.{CRO, USDT}
import faith.knowledge.common.Instrument
import faith.knowledge.common.syntax.ChainingSyntax.scalaUtilChainingOps
import munit.CatsEffectSuite
import squants.market.{BTC, EUR}

class MarketInfoProviderSpec extends CatsEffectSuite {
  val btcUsd = Instrument(BTC, USDT)
  val croUsd = Instrument(CRO, USDT)
  val btcEur = Instrument(BTC, EUR)
  val croEur = Instrument(CRO, EUR)
  val infoMap = Map(
    btcEur -> InstrumentResult(btcUsd, BTC, EUR, 5, 5, true),
    croEur -> InstrumentResult(croEur, CRO, EUR, 5, 5, true),
    btcUsd -> InstrumentResult(btcUsd, BTC, USDT, 5, 5, true),
    croUsd -> InstrumentResult(croUsd, CRO, USDT, 4, 2, false)
  )
  val ioInfoMap     = IO.pure(infoMap)
  val croConfigStub = CroConfig("apiKey", "privateKey", List.empty)

  test("instruments should returns the instruments on the market") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    provider.instruments.assertEquals(Set(btcUsd, btcEur, croEur, croUsd))
  }

  test("isValidMarket should return true if the pair exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    provider.isValidMarket(btcUsd).assert
  }

  test("isValidMarket should return true if the reverse instrument exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    val usdBtc   = Instrument(USDT, BTC)
    provider.isValidMarket(usdBtc).assert
  }

  test("isValidMarket should return false if not the instrument not the reverse exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    val btcCro   = Instrument(BTC, CRO)
    provider.isValidMarket(btcCro).map(!_).assert
  }

  test("isValidInstrument should return true if the pair exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    provider.isValidInstrument(btcUsd).assert
  }

  test("isValidInstrument should return false if the instrument does not exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    val usdBtc   = Instrument(USDT, BTC)
    provider.isValidInstrument(usdBtc).map(!_).assert
  }

  test("getMarketFor should return an instrument in case that it exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    provider.getMarketFor("BTC_USDT").assertEquals(btcUsd.some)
  }

  test("getMarketFor should return nothing in case that it does not exists in the infoMap") {
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    provider.getMarketFor("DOESNT_EXIST").assertEquals(None)
  }

  test("round should floor the decimals to amount of quantityDecimals specified in the infomap") {
    val quantityDecimals = 2
    val ioInfoMap = Map(btcUsd -> InstrumentResult(btcUsd, BTC, USDT, 5, quantityDecimals = quantityDecimals, true))
      .|>(IO.pure)
    val provider = new MarketInfoProvider(ioInfoMap, croConfigStub)
    provider.round(btcUsd, BigDecimal("1.98765")).assertEquals(BigDecimal("1.98"))
  }

  test(
    "findIntermediaryCurrency should return a stable asset if it there are instruments for which it is quote for base and quote"
  ) {
    val provider = new MarketInfoProvider(ioInfoMap, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  test(
    "findIntermediaryCurrency should return a market currency if it there are instruments for which it is quote for base and quote and no stable is intermediary"
  ) {
    val provider = new MarketInfoProvider(ioInfoMap, CroConfig("apiKey", "privateKey", List.empty))
    provider.findIntermediaryCurrency(BTC, CRO).assertEquals(EUR.some)
  }

  test(
    "findIntermediaryCurrency should return none if it there is are no instruments for which it is quote for base and quote"
  ) {
    val provider = new MarketInfoProvider(ioInfoMap, CroConfig("apiKey", "privateKey", List.empty))
    provider.findIntermediaryCurrency(CRO, USDT).assertEquals(None)
  }
}
