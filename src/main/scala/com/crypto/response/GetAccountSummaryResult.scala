package com.crypto.response

import com.crypto.response.GetAccountSummaryResult.Balance
import squants.market.Currency

case class GetAccountSummaryResult(account: List[Balance])

object GetAccountSummaryResult {
  case class Balance(
    balance: BigDecimal,
    available: BigDecimal,
    order: BigDecimal,
    stake: BigDecimal,
    currency: Currency
  )
}
