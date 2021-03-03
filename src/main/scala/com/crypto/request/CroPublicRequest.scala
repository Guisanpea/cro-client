package com.crypto.request

import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import zio.ZIO
import zio.clock.Clock
import zio.random.Random

case class CroPublicRequest(id: Long, method: String, nonce: Long)

object CroPublicRequest {
  val getInstrumentsRequest: ZIO[Clock with Random, Nothing, CroPublicRequest] =
    for {
      id    <- ZIO.accessM[Random](_.get.nextInt.map(_.abs))
      nonce <- ZIO.accessM[Clock](_.get.instant).map(_.toEpochMilli)
    } yield CroPublicRequest(id, "public/get-instruments", nonce)
}
