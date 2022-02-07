package scalable.market.broker.cryptocom.croclient

import cats.effect.IO
import cats.implicits._
import com.crypto.CryptoComApi
import com.crypto.response.{Balance, CroResponse, GetOrderDetailResult, OrderInfo}
import munit.CatsEffectSuite
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import scalable.market.broker.cryptocom.{CroConfig, ExchangeClient, ExchangeClientImpl, MarketInfoProvider}
import scalable.market.common.Currencies.CRO
import scalable.market.common.Instrument
import squants.market._

class ExchangeClientImplSpec extends CatsEffectSuite with MockitoSugar with ArgumentMatchersSugar {
  import ExchangeClientImplSpec._
  val fixture = FunFixture[(CryptoComApi, MarketInfoProvider, ExchangeClient)](
    setup = { _ =>
      val stubApi: CryptoComApi = mock[CryptoComApi]
      when(stubApi.getAccountSummary).thenReturn(balancesList.pure[IO])
      val stubProvider: MarketInfoProvider = mock[MarketInfoProvider]
      val exchangeClient: ExchangeClient   = ExchangeClientImpl(stubApi, stubProvider, config)

      (stubApi, stubProvider, exchangeClient)
    },
    teardown = _ => ()
  )

  fixture.test("getBalances should return balances from accountSummary") {
    case (apiStub, marketInfoStub, exchangeClient) =>
      // GIVEN
      when(marketInfoStub.instruments).thenReturn(instrumentSet.pure[IO])
      // WHEN THEN
      exchangeClient.balances
        .assertEquals(balancesList.map(b => Money(b.balance, b.currency)))
  }

  fixture.test("exchange should buy the full instrument if base and quote form a valid instrument") {
    case (apiStub, marketInfoStub, exchangeClient) =>
      implicit val (api, market, exchange) = (apiStub, marketInfoStub, exchangeClient)
      //GIVEN
      buyRequestOrderReturns(exchangeResponse)
      instrumentAndOppositeIsValid(croUsd)
      val croBalance = balancesList.find(_.currency == CRO).get.available
      //WHEN
      val result = exchangeClient
        .exchange("CRO", "USD")
      //THEN
      result
        .<*(buyOrderIsCalledWith(usdCro, croBalance.toDouble))
        .assertEquals(Money(exchangeResponse.result.orderInfo.quantity.get, USD))
  }

  fixture.test("exchange should buy the opposite instrument if it forms a valid instrument") {
    case (apiStub, marketInfoStub, exchangeClient) =>
      implicit val (api, market, exchange) = (apiStub, marketInfoStub, exchangeClient)
      //GIVEN
      instrumentIsNotValid(croUsd)
      instrumentIsValid(usdCro)
      buyRequestOrderReturns(exchangeResponse)
      val croBalance = balancesList.find(_.currency == CRO).get.available
      //WHEN
      val result = exchangeClient
        .exchange("CRO", "USD")
      //THEN
      result
        .<*(IO(verify(apiStub).requestBuyOrder(usdCro, croBalance.toDouble)))
        .assertEquals(Money(exchangeResponse.result.orderInfo.quantity.get, USD))
  }

  fixture.test("exchange should sell using intermediary if there is a valid intermediary") {
    case (apiStub, marketInfoStub, exchangeClient) =>
      implicit val (api, market, exchange) = (apiStub, marketInfoStub, exchangeClient)
      //Given
      instrumentAndOppositeNotValid(btcCro)
      instrumentAndOppositeIsValid(btcUsd)
      instrumentAndOppositeIsValid(usdCro)
      stableIsValidIntermediary(base = CRO, quote = BTC, stable = USD)
      buyRequestOrderReturns(exchangeResponse)
      // WHEN
      val result = exchangeClient
        .exchange("CRO", "BTC")
      // THEN
      result
        .<*(buyOrderIsCalledWith(instrument = usdCro, amountToSell = croBalance.toDouble))
        .<*(buyOrderIsCalledWith(instrument = btcUsd, amountToSell = usdBalance.toDouble))
        .assertEquals(Money(exchangeResponse.result.orderInfo.quantity.get, BTC))
  }

  /*
  fixture.test("exchange should sell using intermediary") {
    case (apiStub, marketInfoStub, exchangeClient) =>
      // GIVEN BTC_CRO and CRO_BTC not valid instruments
      when(marketInfoStub.isValidInstrument(eqTo(btcCro))).thenAnswer(false.pure[IO])
      when(marketInfoStub.isValidInstrument(eqTo(croBtc))).thenAnswer(false.pure[IO])
      // AND USD as a valid sell intermediary between both
      when(marketInfoStub.findInter(eqTo(CRO), eqTo(BTC))).thenAnswer(USD.some.pure[IO])
      when(apiStub.requestSellOrder(any, any)).thenReturn(exchangeResponse.pure[IO])
      // WHEN Exchanging CRO_BTC
      val result = exchangeClient
        .exchange("CRO", "BTC")
      // THEN I sell all CRO_USD and then all USD_BTC
      result
        .<*(IO(verify(apiStub).requestSellOrder(croUsd, croBalance.toDouble)))
        .<*(IO(verify(apiStub).requestSellOrder(usdBtc, usdBalance.toDouble)))
        .assertEquals(Money(exchangeResponse.result.orderInfo.quantity.get, BTC))
  }
   */

  def instrumentAndOppositeIsValid(instrument: Instrument)(implicit marketInfoStub: MarketInfoProvider) = {
    when(marketInfoStub.isTradingInstrument(eqTo(instrument))).thenAnswer(true.pure[IO])
    when(marketInfoStub.isTradingInstrument(eqTo(instrument.opposite))).thenAnswer(true.pure[IO])
  }

  def instrumentIsValid(instrument: Instrument)(implicit marketInfoStub: MarketInfoProvider) =
    when(marketInfoStub.isTradingInstrument(eqTo(instrument))).thenAnswer(true.pure[IO])

  def instrumentIsNotValid(instrument: Instrument)(implicit marketInfoStub: MarketInfoProvider) =
    when(marketInfoStub.isTradingInstrument(eqTo(instrument))).thenAnswer(false.pure[IO])

  def instrumentAndOppositeNotValid(instrument: Instrument)(implicit marketInfoStub: MarketInfoProvider) = {
    when(marketInfoStub.isTradingInstrument(eqTo(instrument))).thenAnswer(false.pure[IO])
    when(marketInfoStub.isTradingInstrument(eqTo(instrument.opposite))).thenAnswer(false.pure[IO])
  }

  def stableIsValidIntermediary(base: Currency, quote: Currency, stable: Currency)(implicit
    marketInfoStub: MarketInfoProvider
  ) = {
    when(marketInfoStub.findIntermediaryStableCurrency(eqTo(base), eqTo(quote))).thenReturn(stable.some.pure[IO])
  }

  def sellRequestOrderReturns(response: CroResponse[GetOrderDetailResult])(implicit apiStub: CryptoComApi) =
    when(apiStub.requestSellOrder(any, any)).thenReturn(response.pure[IO])

  def buyRequestOrderReturns(response: CroResponse[GetOrderDetailResult])(implicit apiStub: CryptoComApi) =
    when(apiStub.requestBuyOrder(any, any)).thenReturn(response.pure[IO])

  def sellOrderIsCalledWith(instrument: Instrument, amountToSell: Double)(implicit apiStub: CryptoComApi) =
    IO(verify(apiStub).requestSellOrder(instrument, amountToSell))

  def buyOrderIsCalledWith(instrument: Instrument, amountToSell: Double)(implicit apiStub: CryptoComApi) =
    IO(verify(apiStub).requestBuyOrder(instrument, amountToSell))

}

object ExchangeClientImplSpec {
  val config = CroConfig("apiKey", "privateKey", List(USD, EUR))
  val usdBtc = Instrument(USD, BTC)
  val usdCro = Instrument(USD, CRO)
  val croBtc = Instrument(CRO, BTC)
  val croUsd = Instrument(CRO, USD)
  val btcUsd = Instrument(BTC, USD)
  val btcCro = Instrument(BTC, CRO)
  val balancesList =
    List(Balance(90, 80, 70, 60, CRO), Balance(10, 20, 30, 40, USD), Balance(100, 1000, 10000, 100000, BTC))
  val croBalance = balancesList.find(_.currency == CRO).get.available
  val usdBalance = balancesList.find(_.currency == USD).get.available
  val btcBalance = balancesList.find(_.currency == BTC).get.available

  val exchangeResponse: CroResponse[GetOrderDetailResult] = CroResponse(
    1L,
    "method/sell",
    200,
    GetOrderDetailResult(
      tradeList = List.empty,
      orderInfo = OrderInfo(
        "status",
        None,
        "side",
        2.0.some,
        1.0.some,
        "orderId",
        "clientOid".some,
        100L,
        200L,
        "sell",
        "instrument",
        200.0.some,
        300.0.some,
        402,
        "CRO",
        "timeInForce",
        "execInst"
      )
    )
  )

  var instrumentSet: Set[Instrument] = Set(Instrument(CRO, USD), Instrument(CRO, BTC))

}
