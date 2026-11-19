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
        val analysis = Indicators.analyse(candles)
        val news = if (offline) {
            listOf(
                NewsItem(
                    "Offline demo — live news-er jonno internet lagbe",
                    "TradeLens", "Now", "",
                    com.tradelens.app.data.Sentiment.NEUTRAL
                )
            )
        } else {
            MarketApi.fetchNews(asset)
        }
        val score = ScoreEngine.compute(price, analysis, news)
        val (scenarios, stopLoss, target) = ScoreEngine.scenarios(score.total, price, analysis)
        val closes = candles.map { it.close }
        return MarketAnalysis(
            asset = asset,
            price = price,
            change24hPct = change24h,
            candles = candles,
            ema20Series = Indicators.ema(closes, 20),
            analysis = analysis,
            score = score,
            verdict = ScoreEngine.verdict(score.total),
            scenarios = scenarios,
            stopLoss = stopLoss,
            target = target,
            news = news,
            offlineDemo = offline
        )
    }
}
