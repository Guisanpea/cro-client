package com.crypto.request

case class CroSignedRequest(
  id: Long,
  apiKey: String,
  sig: String,
  method: String,
  nonce: Long,
  params: Map[String, String]
)
