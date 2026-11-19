package com.tradelens.app.data

object Assets {
    val all: List<Asset> = listOf(
        Asset("btc", "Bitcoin", "BTC/USDT", Category.CRYPTO, "BTCUSDT", "BTC-USD", 2),
        Asset("eth", "Ethereum", "ETH/USDT", Category.CRYPTO, "ETHUSDT", "ETH-USD", 2),
        Asset("bnb", "BNB", "BNB/USDT", Category.CRYPTO, "BNBUSDT", "BNB-USD", 2),
        Asset("sol", "Solana", "SOL/USDT", Category.CRYPTO, "SOLUSDT", "SOL-USD", 2),
        Asset("xrp", "XRP", "XRP/USDT", Category.CRYPTO, "XRPUSDT", "XRP-USD", 4),
        Asset("doge", "Dogecoin", "DOGE/USDT", Category.CRYPTO, "DOGEUSDT", "DOGE-USD", 4),
        Asset("xau", "Gold", "XAU/USD", Category.GOLD, null, "GC=F", 2),
        Asset("xag", "Silver", "XAG/USD", Category.GOLD, null, "SI=F", 2),
        Asset("eurusd", "Euro / USD", "EUR/USD", Category.FOREX, null, "EURUSD=X", 4),
        Asset("gbpusd", "Pound / USD", "GBP/USD", Category.FOREX, null, "GBPUSD=X", 4),
        Asset("usdjpy", "USD / Yen", "USD/JPY", Category.FOREX, null, "USDJPY=X", 2),
        Asset("aapl", "Apple", "AAPL", Category.STOCK, null, "AAPL", 2),
        Asset("tsla", "Tesla", "TSLA", Category.STOCK, null, "TSLA", 2),
        Asset("nvda", "NVIDIA", "NVDA", Category.STOCK, null, "NVDA", 2)
    )

    fun byId(id: String): Asset = all.firstOrNull { it.id == id } ?: all.first()
}
