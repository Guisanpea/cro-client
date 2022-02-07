package com.crypto.response

import cats.implicits._
import com.crypto.response.GetTickerResult.{InstrumentFoundResult, InstrumentNotFoundResult}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import scalable.market.common.Currencies
import squants.market.Currency

import scala.util.Success

object Json {
  implicit val customCirceConfig: Configuration                     = Configuration.default.withSnakeCaseMemberNames
  implicit def responseDecoder[R: Decoder]: Decoder[CroResponse[R]] = deriveConfiguredDecoder

  implicit def publicResponseDecoder[R: Decoder]: Decoder[CroPublicResponse[R]] = deriveConfiguredDecoder

  implicit val errorDecoder: Decoder[CroErrorResponse] = deriveConfiguredDecoder

  implicit val currencyDecoder: Decoder[Currency]                   = Decoder.decodeString.emapTry(s => Success(Currencies.apply(s)))
  implicit val instrumentDecoder: Decoder[InstrumentResult]         = deriveConfiguredDecoder
  implicit val getInstrumentsDecoder: Decoder[GetInstrumentsResult] = deriveConfiguredDecoder
  implicit val tradeListDecoder: Decoder[TradeList]                 = deriveConfiguredDecoder
  implicit val orderInfoDecoder: Decoder[OrderInfo]                 = deriveConfiguredDecoder
  implicit val balanceDecoder: Decoder[Balance]                     = deriveConfiguredDecoder

  implicit val getOrderDetailDecoder: Decoder[GetOrderDetailResult]         = deriveConfiguredDecoder
  implicit val createOrderDecoder: Decoder[CreateOrderResult]               = deriveConfiguredDecoder
  implicit val getAllTickersDecoder: Decoder[GetAllTickersResult]           = deriveConfiguredDecoder
  implicit val instrumentNotFoundDecoder: Decoder[InstrumentNotFoundResult] = deriveConfiguredDecoder
  implicit val instrumentFoundDecoder: Decoder[InstrumentFoundResult]       = deriveConfiguredDecoder
  implicit val accountSummaryDecoder: Decoder[GetAccountSummaryResult]      = deriveConfiguredDecoder

  implicit val tickerDecoder: Decoder[Ticker] =
    Decoder.forProduct9("i", "b", "k", "a", "t", "v", "h", "l", "c")(Ticker.apply)

  implicit val getTickerDecoder: Decoder[GetTickerResult] = // format: off
    List[Decoder[GetTickerResult]](
      Decoder[InstrumentNotFoundResult].widen,
      Decoder[InstrumentFoundResult].widen
    ).reduceLeft(_ or _) // format: on

  implicit val candlestickDecoder: Decoder[Candlestick] =
    Decoder.forProduct6("t", "o", "c", "l", "h", "v")(Candlestick.apply)
  implicit val getCandlestickDecoder: Decoder[GetCandlestickResult] =
    deriveConfiguredDecoder[GetCandlestickResult]
}
