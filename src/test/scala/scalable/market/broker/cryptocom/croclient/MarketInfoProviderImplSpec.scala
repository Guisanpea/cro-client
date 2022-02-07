package scalable.market.broker.cryptocom.croclient

import cats.effect.IO
import cats.implicits._
import com.crypto.response.InstrumentResult
import munit.CatsEffectSuite
import scalable.market.broker.cryptocom.croclient.MarketInfoProviderImplSpec._
import scalable.market.broker.cryptocom.{CroConfig, MarketInfoProviderImpl}
import scalable.market.common.Currencies.{CRO, USDT}
import scalable.market.common.Instrument
import scalable.market.common.syntax.ChainingSyntax.scalaUtilChainingOps
import squants.market.{BTC, EUR}

class MarketInfoProviderImplSpec extends CatsEffectSuite {

  test("instruments should returns the instruments on the market") {
    provider.instruments.assertEquals(Set(btcEur, btcUsd, croEur, croUsd, usdCro))
  }

  test("isValidMarket should return true if the pair exists in the infoMap") {
    provider.isTradingInstrument(btcUsd).assert
  }

  test("isValidMarket should return true if the reverse instrument exists in the infoMap") {
    val usdBtc = Instrument(USDT, BTC)
    provider.isTradingInstrument(usdBtc).assert
  }

  test("isValidMarket should return false if not the instrument not the reverse exists in the infoMap") {
    val btcCro = Instrument(BTC, CRO)
    provider.isTradingInstrument(btcCro).map(!_).assert
  }

  test("getMarketFor should return an instrument in case that it exists in the infoMap") {
    provider.getInstrumentFor("BTC_USDT").assertEquals(btcUsd.some)
  }

  test("getMarketFor should return nothing in case that it does not exists in the infoMap") {
    provider.getInstrumentFor("DOESNT_EXIST").assertEquals(None)
  }

  test("round should floor the decimals to amount of quantityDecimals specified in the infomap") {
    val quantityDecimals = 2
    val ioInfoMap = Map(btcUsd -> InstrumentResult(btcUsd, BTC, USDT, 5, quantityDecimals = quantityDecimals, true))
      .|>(IO.pure)
    val provider = new MarketInfoProviderImpl(ioInfoMap, croConfigStub)
    provider.round(btcUsd, BigDecimal("1.98765")).assertEquals(BigDecimal("1.98"))
  }

  test(
    "findIntermediaryCurrency should return a stable asset if there are STABLE_BASE and QUOTE_STABLE (buy, buy) markets"
  ) {
    val map      = Map(usdBtc -> usdBtcResult, croUsd -> croUsdResult).pure[IO]
    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  test(
    "findIntermediaryCurrency should return a stable asset if there are STABLE_BASE and STABLE_QUOTE (buy, sell) markets"
  ) {
    val map      = Map(usdBtc -> usdBtcResult, usdCro -> usdCroResult).pure[IO]
    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  test(
    "findIntermediaryCurrency should return a stable asset if there are BASE_STABLE and QUOTE_STABLE (sell, buy) markets"
  ) {
    val map      = Map(btcUsd -> btcUsdResult, croUsd -> croUsdResult).pure[IO]
    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  test(
    "findIntermediaryCurrency should return a stable asset if there are BASE_STABLE and STABLE_QUOTE (sell, sell) markets"
  ) {
    val map      = Map(btcUsd -> btcUsdResult, usdCro -> usdCroResult).pure[IO]
    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  // format: off
  test(
    "findIntermediaryCurrency should prefer STABLE1_BASE and QUOTE_STABLE1 (buy, buy) over STABLE2_BASE and STABLE2_QUOTE (buy, sell)"
  ) {
    val map = Map(
        usdBtc -> usdBtcResult, croUsd -> croUsdResult,
        eurBtc -> eurBtcResult, eurCro -> eurCroResult
      ).pure[IO]
      
    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT, EUR)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  test(
    "findIntermediaryCurrency should prefer STABLE1_BASE and STABLE1_QUOTE (buy, sell) over BASE_STABLE2 and QUOTE_STABLE2 (sell, buy)"
  ) {
    val map = Map(
      usdBtc -> usdBtcResult, usdCro -> usdCroResult,
      btcEur -> btcEurResult, croEur -> croEurResult
    ).pure[IO]

    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }

  test(
    "findIntermediaryCurrency should prefer BASE_STABLE1 and QUOTE_STABLE1 (sell, buy) over BASE_STABLE2 and STABLE2_QUOTE (sell, sell)"
  ) {
    val map = Map(
      btcUsd -> btcUsdResult, croUsd -> croUsdResult,
      btcEur -> btcEurResult, eurCro -> eurCroResult
    ).pure[IO]

    val provider = new MarketInfoProviderImpl(map, CroConfig("apiKey", "privateKey", List(USDT)))
    provider.findIntermediaryStableCurrency(BTC, CRO).assertEquals(USDT.some)
  }
}

object MarketInfoProviderImplSpec {
  val irrelevantInt     = 5
  val irrelevantBoolean = false

  val btcEur = Instrument(BTC, EUR)
  val btcUsd = Instrument(BTC, USDT)
  val croEur = Instrument(CRO, EUR)
  val croUsd = Instrument(CRO, USDT)
  val eurBtc = Instrument(EUR, BTC)
  val eurCro = Instrument(EUR, CRO)
  val usdCro = Instrument(USDT, CRO)
  val usdBtc = Instrument(USDT, BTC)

  val btcEurResult = genResultFrom(btcUsd)
  val btcUsdResult = genResultFrom(btcUsd)
  val croEurResult = genResultFrom(croEur)
  val croUsdResult = genResultFrom(croUsd)
  val eurBtcResult = genResultFrom(eurBtc)
  val eurCroResult = genResultFrom(eurCro)
  val usdCroResult = genResultFrom(usdCro)
  val usdBtcResult = genResultFrom(usdBtc)
  val infoMap = Map(
    btcEur -> btcEurResult,
    croEur -> croEurResult,
    btcUsd -> btcUsdResult,
    croUsd -> croUsdResult,
    usdCro -> usdCroResult
  )
  val ioInfoMap     = IO.pure(infoMap)
  val croConfigStub = CroConfig("apiKey", "privateKey", List.empty)
  val provider      = new MarketInfoProviderImpl(ioInfoMap, croConfigStub)

  def genResultFrom(instrument: Instrument) = {
    val Instrument(base, quote) = instrument
    InstrumentResult(instrument, base, quote, irrelevantInt, irrelevantInt, irrelevantBoolean)
  }
}
