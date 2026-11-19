package com.tradelens.app.data

import com.tradelens.app.analysis.Indicators
import com.tradelens.app.analysis.ScoreEngine

object MarketRepository {

    suspend fun getAnalysis(asset: Asset, timeframe: String): MarketAnalysis {
        var candles = MarketApi.fetchCandles(asset, timeframe)
        var offline = false
        if (candles.size < 20) {
            candles = MarketApi.demoCandles(asset)
            offline = true
        }
        val (price, change24h) = if (offline) {
            Pair(candles.last().close, 0.0)
        } else {
            val ticker = MarketApi.fetchTicker(asset)
            if (ticker.first > 0) ticker else Pair(candles.last().close, 0.0)
        }
        val news = if (offline) {
            listOf(
                NewsItem(
                    "অফলাইন ডেমো — লাইভ নিউজের জন্য ইন্টারনেট লাগবে",
                    "TradeLens", "এখন", "",
                    com.tradelens.app.data.Sentiment.NEUTRAL
                )
            )
        } else {
            MarketApi.fetchNews(asset)
        }
        // ১ দিনের ট্রেন্ড — মাল্টি-টাইমফ্রেম মিল (confluence) যাচাইয়ের জন্য
        val dailyCandles = MarketApi.fetchCandles(asset, "1d")
        val dailyTrend = if (dailyCandles.size >= 20) {
            Indicators.analyse(dailyCandles).trend
        } else {
            null
        }
        return buildAnalysis(asset, timeframe, candles, price, change24h, news, offline, dailyTrend)
    }

    /** দেওয়া ক্যান্ডেল দিয়ে পুরো বিশ্লেষণ বানায় — লাইভ আর ডেমো দুই জায়গাতেই ব্যবহৃত হয়। */
    fun buildAnalysis(
        asset: Asset,
        timeframe: String,
        candles: List<Candle>,
        price: Double,
        change24hPct: Double,
        news: List<NewsItem>,
        offline: Boolean,
        dailyTrend: Trend?
    ): MarketAnalysis {
        val analysis = Indicators.analyse(candles)
        val score = ScoreEngine.compute(price, analysis, news)
        val (scenarios, stopLoss, target) = ScoreEngine.scenarios(score.total, price, analysis)
        val closes = candles.map { it.close }
        val forecast = buildForecast(asset, timeframe, candles, analysis, score.total, price, news, target, stopLoss)
        val ultra = Indicators.ultraAnalysis(analysis, candles, score.total, dailyTrend, price)
        return MarketAnalysis(
            asset = asset,
            price = price,
            change24hPct = change24hPct,
            candles = candles,
            ema20Series = Indicators.ema(closes, 20),
            analysis = analysis,
            score = score,
            verdict = ScoreEngine.verdict(score.total),
            scenarios = scenarios,
            stopLoss = stopLoss,
            target = target,
            news = news,
            offlineDemo = offline,
            forecast = forecast,
            ultra = ultra
        )
    }

    private fun buildForecast(
        asset: Asset,
        timeframe: String,
        candles: List<Candle>,
        analysis: Analysis,
        scoreTotal: Int,
        price: Double,
        news: List<NewsItem>,
        target: Double,
        stopLoss: Double
    ): Forecast {
        val projected = Indicators.forecastCandles(candles, analysis, scoreTotal, timeframe, 5)
        val lastProjected = projected.lastOrNull()?.close ?: price
        val movePct = if (price > 0) (lastProjected - price) / price * 100 else 0.0
        val direction = when {
            movePct > 0.15 -> "UP"
            movePct < -0.15 -> "DOWN"
            else -> "SIDEWAYS"
        }
        val directionText = when (direction) {
            "UP" -> "উপরের দিকে যেতে পারে"
            "DOWN" -> "নিচের দিকে যেতে পারে"
            else -> "সাইডওয়েজ থাকতে পারে"
        }
        val perCandleText = "$timeframe ক্যান্ডেল: পরের ৫টি ক্যান্ডেল $directionText — " +
            "মোট ${if (movePct >= 0) "+" else ""}${"%.2f".format(movePct)}% পর্যন্ত, " +
            "টার্গেট $${"%,.2f".format(target)}"
        val reasons = ArrayList<String>()
        when (analysis.trend) {
            Trend.UPTREND -> reasons.add("ট্রেন্ড আপ — EMA20 EMA50-এর উপরে, দাম EMA20-এর উপরে আছে।")
            Trend.DOWNTREND -> reasons.add("ট্রেন্ড ডাউন — EMA20 EMA50-এর নিচে, দাম চাপে আছে।")
            Trend.SIDEWAYS -> reasons.add("ট্রেন্ড সাইডওয়েজ — কোনো একদিকে জোর নেই, রেঞ্জের ভিতরে ঘুরছে।")
        }
        when {
            analysis.rsi < 30 -> reasons.add("RSI ${"%.0f".format(analysis.rsi)} — ওভারসোল্ড, এখানে বাউন্স করতে পারে।")
            analysis.rsi > 70 -> reasons.add("RSI ${"%.0f".format(analysis.rsi)} — ওভারবট, নিচে আসার চান্স আছে।")
            else -> reasons.add("RSI ${"%.0f".format(analysis.rsi)} — নিউট্রাল জোন, উপরে যাওয়ার জায়গা আছে।")
        }
        if (analysis.support > 0 && price > 0) {
            val distSupport = (price - analysis.support) / price * 100
            val distResistance = (analysis.resistance - price) / price * 100
            if (distSupport in 0.0..2.0) reasons.add("দাম সাপোর্টের খুব কাছে — এখান থেকে বাউন্স করতে পারে।")
            if (distResistance in 0.0..2.0) reasons.add("দাম রেজিস্ট্যান্সের কাছে — ভাঙলে উপরে যাবে, না ভাঙলে নিচে আসবে।")
        }
        val pos = news.count { it.sentiment == Sentiment.POSITIVE }
        val neg = news.count { it.sentiment == Sentiment.NEGATIVE }
        when {
            pos > neg -> reasons.add("নিউজ পজিটিভ — ভালো খবর দামকে উপরে টানতে পারে।")
            neg > pos -> reasons.add("নিউজ নেগেটিভ — খারাপ খবর দামকে নিচে নামাতে পারে।")
        }
        when {
            analysis.volumeChangePct > 10 -> reasons.add("ভলিউম বাড়ছে — মুভটি শক্তিশালী হতে পারে।")
            analysis.volumeChangePct < -10 -> reasons.add("ভলিউম কমছে — মুভ দুর্বল হতে পারে।")
        }
        val action = when (ScoreEngine.verdict(scoreTotal)) {
            Verdict.STRONG_BUY -> "স্কোর $scoreTotal+: এখন কেনা যায়, কিন্তু স্টপ লস $${"%,.2f".format(stopLoss)}-এ রাখুন আর একবারে সব টাকা লাগাবেন না।"
            Verdict.WAIT -> "এখন অপেক্ষা করুন — $${"%,.2f".format(target)} ভাঙলে কিনবেন, আর $${"%,.2f".format(stopLoss)}-এর নিচে গেলে কেনা ঠিক হবে না।"
            Verdict.AVOID -> "এখন কিনবেন না — দাম নিচে যাওয়ার চান্স বেশি। সাপোর্টে আসলে আবার দেখুন।"
        }
        return Forecast(
            candles = projected,
            direction = direction,
            directionText = directionText,
            movePct = movePct,
            targetPrice = target,
            perCandleText = perCandleText,
            reasons = reasons,
            action = action
        )
    }
}
