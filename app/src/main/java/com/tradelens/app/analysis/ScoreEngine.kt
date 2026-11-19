package com.tradelens.app.analysis

import com.tradelens.app.data.Analysis
import com.tradelens.app.data.Candle
import com.tradelens.app.data.NewsItem
import com.tradelens.app.data.Scenario
import com.tradelens.app.data.ScoreBreakdown
import com.tradelens.app.data.Sentiment
import com.tradelens.app.data.Trend
import com.tradelens.app.data.Verdict
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ScoreEngine {

    /**
     * Score out of 100, exactly as specified:
     * Trend +20, RSI +15, Support +20, News +15, Volume +10, MACD +10, EMA +10.
     */
    fun compute(
        price: Double,
        analysis: Analysis,
        news: List<NewsItem>
    ): ScoreBreakdown {
        val trendPoints = when (analysis.trend) {
            Trend.UPTREND -> 20
            Trend.SIDEWAYS -> 10
            Trend.DOWNTREND -> 0
        }
        val rsiPoints = when {
            analysis.rsi in 40.0..65.0 -> 15
            analysis.rsi in 30.0..40.0 -> 10
            analysis.rsi in 65.0..75.0 -> 7
            else -> 3
        }
        val supportPoints = when {
            analysis.support <= 0 || price <= 0 -> 0
            else -> {
                val distPct = abs(price - analysis.support) / price * 100
                val range = analysis.resistance - analysis.support
                when {
                    distPct <= 1.5 -> 20
                    range > 0 && (price - analysis.support) / range < 0.35 -> 14
                    else -> 6
                }
            }
        }
        val pos = news.count { it.sentiment == Sentiment.POSITIVE }
        val neg = news.count { it.sentiment == Sentiment.NEGATIVE }
        val newsPoints = when {
            news.isEmpty() -> 7
            pos > neg -> 15
            pos == neg -> 8
            else -> 2
        }
        val volumePoints = when {
            analysis.volumeChangePct > 10 -> 10
            analysis.volumeChangePct > 0 -> 7
            analysis.volumeChangePct > -10 -> 4
            else -> 1
        }
        val macdPoints = if (analysis.macd > analysis.macdSignal) 10 else 3
        val emaPoints = when {
            analysis.ema20 > analysis.ema50 && price > analysis.ema20 -> 10
            analysis.ema20 > analysis.ema50 -> 6
            else -> 2
        }
        return ScoreBreakdown(
            trendPoints = trendPoints,
            rsiPoints = rsiPoints,
            supportPoints = supportPoints,
            newsPoints = newsPoints,
            volumePoints = volumePoints,
            macdPoints = macdPoints,
            emaPoints = emaPoints
        )
    }

    fun verdict(total: Int): Verdict = when {
        total >= 70 -> Verdict.STRONG_BUY
        total >= 40 -> Verdict.WAIT
        else -> Verdict.AVOID
    }

    fun scenarios(
        total: Int,
        price: Double,
        analysis: Analysis
    ): Triple<List<Scenario>, Double, Double> {
        val atr = if (analysis.atr > 0) analysis.atr else price * 0.01
        val target = if (analysis.resistance > price) analysis.resistance else price + 2 * atr
        val stopLoss = if (analysis.support in 0.0..price && analysis.support > 0) {
            analysis.support
        } else {
            price - 1.5 * atr
        }
        // Probability is a guide, never a guarantee.
        val bullRaw = (total * 0.8 + 10).roundToInt()
        val bull = min(85, max(5, bullRaw))
        val bearRaw = ((100 - total) * 0.35).roundToInt()
        val bear = min(40, max(5, bearRaw))
        val side = max(5, 100 - bull - bear)
        val list = listOf(
            Scenario(
                "Bullish", bull, target,
                "Resistance vangle target-e jete pare."
            ),
            Scenario(
                "Sideways", side, price,
                "Range-er vitore thakte pare."
            ),
            Scenario(
                "Bearish", bear, stopLoss,
                "Support vangle niche jete pare."
            )
        )
        return Triple(list, stopLoss, target)
    }

    fun sentimentOf(title: String): Sentiment {
        val t = title.lowercase()
        val positive = listOf(
            "etf", "approval", "approved", "surge", "rally", "bullish", "record high",
            "growth", "adoption", "partnership", "upgrade", "breakout", "gain", "jump",
            "soar", "buy", "positive", "launch"
        )
        val negative = listOf(
            "crash", "ban", "banned", "hack", "hacked", "dump", "lawsuit", "sec",
            "fraud", "drop", "fall", "plunge", "bearish", "sell-off", "collapse",
            "investigation", "fine", "reject"
        )
        val pos = positive.count { t.contains(it) }
        val neg = negative.count { t.contains(it) }
        return when {
            pos > neg -> Sentiment.POSITIVE
            neg > pos -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }
}
