package faith.knowledge.croclient

import com.crypto.CroApi
import guru.broken.brokers.ExchangeClient

import java.util

class CroClient private (croApi: CroApi, croConfig: CroConfig) extends ExchangeClient {
  override def getBalances = ???

  override def convert(s: String, s1: String) = ???

  override def convert(s: String, s1: String, bigDecimal: java.math.BigDecimal) = ???

  override def exchange(s: String, s1: String) = ???

  override def getTop24(s: String, i: Int, set: util.Set[String]) = ???

  override def isValidPair(s: String, s1: String) = ???
}
