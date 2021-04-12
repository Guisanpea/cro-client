package faith.knowledge.common

import squants.market.Currency

object Money {
  object USDT  extends Currency("USDT", "Tether", "₮", 2)
  object CRO extends Currency("CRO", "Crypto.org Coin", "cro", 15)
}
