package com.crypto

import com.crypto.request.{CroPrivateRequest, CroSignedRequest}
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import scalable.market.common.syntax.ChainingSyntax.scalaUtilChainingOps

class SigProvider(apiKey: String, privateKey: String) {
  def signRequest[Params](request: CroPrivateRequest[Params]): CroSignedRequest = {
    val noHashSig = request.method + request.id.toString + apiKey + request.croParamsString + request.nonce.toString

    (new HmacUtils(HmacAlgorithms.HMAC_SHA_256, privateKey).hmac(noHashSig)
      |> Hex.encodeHexString
      |> { hashString =>
        CroSignedRequest(
          id = request.id,
          apiKey = apiKey,
          sig = hashString,
          method = request.method,
          nonce = request.nonce,
          params = request.paramsMap
        )
      })
  }
}
