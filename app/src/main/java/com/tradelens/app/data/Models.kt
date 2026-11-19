package com.tradelens.app.data

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class Category { CRYPTO, FOREX, GOLD, STOCK }

data class Asset(
    val id: String,
    val name: String,
    val symbol: String,
    val category: Category,
    val binanceSymbol: String? = null,
    val yahooSymbol: String,
    val decimals: Int = 2
)

enum class Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

data class NewsItem(
    val title: String,
    val source: String,
    val timeText: String,
    val url: String,
    val sentiment: Sentiment
)

enum class Trend { UPTREND, DOWNTREND, SIDEWAYS }

data class Analysis(
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val ema20: Double,
    val ema50: Double,
    val trend: Trend,
    val support: Double,
    val resistance: Double,
    val pattern: String,
    val volumeChangePct: Double,
    val atr: Double
)

data class ScoreBreakdown(
    val trendPoints: Int,
    val rsiPoints: Int,
    val supportPoints: Int,
    val newsPoints: Int,
    val volumePoints: Int,
    val macdPoints: Int,
    val emaPoints: Int
) {
    val total: Int
        get() = trendPoints + rsiPoints + supportPoints + newsPoints + volumePoints + macdPoints + emaPoints
}

enum class Verdict { STRONG_BUY, WAIT, AVOID }

data class Scenario(
    val name: String,
    val probability: Int,
    val target: Double,
    val description: String
)

data class ForecastCandle(
    val candle: Candle,
    val label: String
)

/** Porer candle-gula kemon hote pare — eta prediction na, trend/ATR/score diye banano demo projection. */
data class Forecast(
    val candles: List<Candle>,
    val direction: String,
    val directionText: String,
    val movePct: Double,
    val targetPrice: Double,
    val perCandleText: String,
    val reasons: List<String>,
    val action: String
)

/** আল্ট্রা অ্যানালিটিক্স — ট্রেন্ডের শক্তি, ডাইভারজেন্স, ভোলাটিলিটি, ব্রেকআউট সম্ভাবনা, মাল্টি-টাইমফ্রেম মিল। */
data class UltraAnalysis(
    val trendStrength: String,
    val trendStrengthPct: Int,
    val volatility: String,
    val divergence: String,
    val divergenceBearish: Boolean,
    val breakoutChance: Int,
    val dailyTrend: Trend,
    val confluence: Boolean,
    val ultraScore: Int,
    val ultraVerdict: String
)

data class MarketAnalysis(
    val asset: Asset,
    val price: Double,
    val change24hPct: Double,
    val candles: List<Candle>,
    val ema20Series: List<Double>,
    val analysis: Analysis,
    val score: ScoreBreakdown,
    val verdict: Verdict,
    val scenarios: List<Scenario>,
    val stopLoss: Double,
    val target: Double,
    val news: List<NewsItem>,
    val offlineDemo: Boolean,
    val forecast: Forecast,
    val ultra: UltraAnalysis
)
