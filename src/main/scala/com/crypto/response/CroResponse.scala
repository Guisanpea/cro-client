package com.crypto.response

case class CroResponse[Result](id: Long, method: String, code: Int, result: Result)
