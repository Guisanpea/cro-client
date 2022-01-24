package com.crypto.request

import cats.effect.std.Random
import cats.effect.{Clock, IO}

case class CroPublicRequest(id: Long, method: String, nonce: Long)

object CroPublicRequest {
  val getInstrumentsRequest: IO[CroPublicRequest] =
    for {
      id    <- Random.scalaUtilRandom[IO].map(_.betweenLong(0, Long.MaxValue)).flatten
      nonce <- Clock[IO].realTime.map(_.toMillis)
    } yield CroPublicRequest(id, "public/get-instruments", nonce)
}
