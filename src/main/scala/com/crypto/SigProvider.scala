package com.crypto

import com.crypto.request.{CroPrivateRequest, CroSignedRequest}
import faith.knowledge.common.ChainingSyntax._
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

class SigProvider(apiKey: String, privateKey: String) {
  def signRequest[Params <: Jsonable](request: CroPrivateRequest[Params]): CroSignedRequest = {
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
