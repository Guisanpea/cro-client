package com.crypto.request

import cats.implicits._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor}

import scala.collection.immutable.SortedMap

object Json {
  implicit val customCirceConfig: Configuration                      = Configuration.default.withSnakeCaseMemberNames
  implicit val createOrderEncoder: Encoder[CreateOrderRequestParams] = deriveConfiguredEncoder
  implicit val orderDetailEncoder: Encoder[GetOrderDetailParams]     = deriveConfiguredEncoder
  implicit val publicEncoder: Encoder[CroPublicRequest]              = deriveConfiguredEncoder
  implicit val signedEncoder: Encoder[CroSignedRequest]              = deriveConfiguredEncoder
  implicit val signedDecoder: Decoder[CroSignedRequest]              = deriveConfiguredDecoder

  def convertToJsonMapSorted[Params: Encoder](params: Params): SortedMap[String, String] = {
    val everythingAsStringDecoder: Decoder[String] = (c: HCursor) =>
      c.value.fold[String]("", _.toString, _.toString, identity, _.mkString(""), _ => "").asRight

    params.asJson.dropNullValues
      .as[Map[String, String]](Decoder.decodeMap(_.some, everythingAsStringDecoder))
      .map(SortedMap.from(_))
      .fold(error => throw new NoSuchMethodException(s"Request was not serializable due to ${error.show}"), identity)
  }

  def toCroParamsString[Params: Encoder](params: Params): String =
    convertToJsonMapSorted(params)
      .map { case (key, value) => key + value }
      .mkString("")
}
