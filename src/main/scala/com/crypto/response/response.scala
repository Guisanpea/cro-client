package com.crypto

import com.crypto.response.GetInstrumentsResult.Instruments
import com.crypto.response.GetOrderDetailResult.{OrderInfo, TradeList}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

package object response {

  implicit def responseDecoder[R: Decoder]: Decoder[CroResponse[R]] = deriveConfiguredDecoder
  implicit val errorDecoder: Decoder[CroErrorResponse]              = deriveConfiguredDecoder
  implicit val instrumentsDecoder: Decoder[Instruments]             = deriveConfiguredDecoder
  implicit val getInstrumentsDecoder: Decoder[GetInstrumentsResult] = deriveConfiguredDecoder
  implicit val getOrderDetailDecoder: Decoder[GetOrderDetailResult] = deriveConfiguredDecoder
  implicit val orderInfoDecoder: Decoder[OrderInfo]                 = deriveConfiguredDecoder
  implicit val tradeListDecoder: Decoder[TradeList]                 = deriveConfiguredDecoder
  implicit val createOrderDecoder: Decoder[CreateOrderResult]       = deriveConfiguredDecoder

}
