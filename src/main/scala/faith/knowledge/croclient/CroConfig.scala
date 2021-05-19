package faith.knowledge.croclient

import squants.market.Currency

case class CroConfig(apiKey: String, privateKey: String, stableAssets: List[Currency])
