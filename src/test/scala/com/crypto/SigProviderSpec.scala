package com.crypto

import cats.implicits.catsSyntaxOptionId
import com.crypto.request.{CreateOrderRequestParams, CroPrivateRequest}
import faith.knowledge.common.syntax.ChainingSyntax._
import io.circe.syntax.EncoderOps
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SigProviderSpec extends AnyWordSpec with Matchers {
  "A sig provider with a given key and pass" should {
    val apiKey      = "apiKey"
    val secretKey   = "secretKey"
    val sigProvider = new SigProvider(apiKey, secretKey)
    "encode properly a given request" in {
      val instrumentName   = "BTC_CRO"
      val side             = "SELL"
      val `type`           = "MARKET"
      val notional: Double = 10
      val method           = "private/create-order"
      val id               = 20
      val nonce            = 203
      val request = CreateOrderRequestParams(
        instrumentName = instrumentName,
        notional = notional.some,
        side = side,
        `type` = `type`
      ) |> (params => CroPrivateRequest(id = id, method = method, nonce = nonce, params = params.some))
      val noHashSig =
        s"$method$id${apiKey}instrument_name${instrumentName}notional${notional}side${side}type${`type`}$nonce"
      val expectedSig = (
        noHashSig
          |> new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmac
          |> Hex.encodeHexString
      )
      sigProvider.signRequest(request).sig shouldBe expectedSig
    }
  }
}
