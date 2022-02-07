package com.crypto

import cats.implicits.catsSyntaxOptionId
import com.crypto.SigProviderSpec._
import com.crypto.SigProvider
import com.crypto.request.Json._
import com.crypto.request.{CreateOrderRequestParams, CroPrivateRequest}
import munit.FunSuite
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import scalable.market.common.syntax.ChainingSyntax.scalaUtilChainingOps

class SigProviderSpec extends FunSuite {
  test("A sig provider with a given key and pass should encode properly a given request") {
    //GIVEN
    val sigProvider = new SigProvider(apiKey, secretKey)
    val request =
      createRequest(instrumentName, side, `type`, notional, method, id, nonce)
    val noHashSig =
      s"${request.method}${request.id}${apiKey}instrument_name${instrumentName}notional${notional}side${side}type${`type`}$nonce"
    //WHEN
    val sig = sigProvider.signRequest(request).sig
    //THEN
    val expectedSig = signHmacSha256(noHashSig)
    assertEquals(sig, expectedSig)
  }

}

object SigProviderSpec {
  val apiKey           = "apiKey"
  val secretKey        = "secretKey"
  val instrumentName   = "BTC_CRO"
  val side             = "SELL"
  val `type`           = "MARKET"
  val notional: Double = 10
  val method           = "private/create-order"
  val id               = 20
  val nonce            = 203

  private def signHmacSha256(string: String) =
    string
      .|>(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmac)
      .|>(Hex.encodeHexString)

  private def createRequest(
    instrumentName: String,
    side: String,
    `type`: String,
    notional: Double,
    method: String,
    id: Int,
    nonce: Int
  ) = {
    val request = CreateOrderRequestParams(
      instrumentName = instrumentName,
      notional = notional.some,
      side = side,
      `type` = `type`
    ) |> (params => CroPrivateRequest(id = id, method = method, nonce = nonce, params = params.some))
    request
  }
}
