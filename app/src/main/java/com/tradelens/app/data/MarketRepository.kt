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
        val forecast = buildForecast(asset, timeframe, candles, analysis, score.total, price, news, target, stopLoss)
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
            offlineDemo = offline,
            forecast = forecast
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
            "UP" -> "Uporer dike jete pare"
            "DOWN" -> "Nicher dike jete pare"
            else -> "Sideways thakte pare"
        }
        val perCandleText = "$timeframe candle: porer 5 ta candle $directionText — " +
            "mot ${if (movePct >= 0) "+" else ""}${"%.2f".format(movePct)}% porjonto, " +
            "target $${"%,.2f".format(target)}"
        val reasons = ArrayList<String>()
        when (analysis.trend) {
            Trend.UPTREND -> reasons.add("Trend up — EMA20 EMA50-er upore, dam EMA20-er upore ache.")
            Trend.DOWNTREND -> reasons.add("Trend down — EMA20 EMA50-er niche, dam chap-e ache.")
            Trend.SIDEWAYS -> reasons.add("Trend sideways — kono ek dike jor nei, range-er vitore ghurche.")
        }
        when {
            analysis.rsi < 30 -> reasons.add("RSI ${"%.0f".format(analysis.rsi)} — oversold, ekhane bounce korte pare.")
            analysis.rsi > 70 -> reasons.add("RSI ${"%.0f".format(analysis.rsi)} — overbought, niche ashar chance ache.")
            else -> reasons.add("RSI ${"%.0f".format(analysis.rsi)} — neutral zone, upore jawar jayga ache.")
        }
        if (analysis.support > 0 && price > 0) {
            val distSupport = (price - analysis.support) / price * 100
            val distResistance = (analysis.resistance - price) / price * 100
            if (distSupport in 0.0..2.0) reasons.add("Dam support-er khub kache — ekhane theke bounce korte pare.")
            if (distResistance in 0.0..2.0) reasons.add("Dam resistance-er kache — vanghle upore jabe, na vanghle niche asbe.")
        }
        val pos = news.count { it.sentiment == Sentiment.POSITIVE }
        val neg = news.count { it.sentiment == Sentiment.NEGATIVE }
        when {
            pos > neg -> reasons.add("News positive — valo khobor dam-ke upore tante pare.")
            neg > pos -> reasons.add("News negative — kharap khobor dam-ke niche namate pare.")
        }
        when {
            analysis.volumeChangePct > 10 -> reasons.add("Volume barche — move-ta strong hote pare.")
            analysis.volumeChangePct < -10 -> reasons.add("Volume komche — move durbol hote pare.")
        }
        val action = when (ScoreEngine.verdict(scoreTotal)) {
            Verdict.STRONG_BUY -> "Score $scoreTotal+: ekhon kena jay, kintu Stop Loss $${"%,.2f".format(stopLoss)}-e rakho ar ekbare sob taka lagiyo na."
            Verdict.WAIT -> "Ekhon wait koro — $${"%,.2f".format(target)} vangle kinba, ar $${"%,.2f".format(stopLoss)}-er niche gele kena thik hobe na."
            Verdict.AVOID -> "Ekhon kino na — dam niche jawar chance beshi. Support-e ashle abar dekho."
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
