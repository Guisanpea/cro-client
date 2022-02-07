package scalable.market.broker.cryptocom

import squants.market.Currency

case class CroConfig(apiKey: String, privateKey: String, stableAssets: List[Currency])
