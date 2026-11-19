package com.tradelens.app.analysis

import com.tradelens.app.data.Analysis
import com.tradelens.app.data.Candle
import com.tradelens.app.data.Trend
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Indicators {

    fun ema(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val out = ArrayList<Double>(values.size)
        var prev = values.first()
        out.add(prev)
        for (i in 1 until values.size) {
            prev = values[i] * k + prev * (1 - k)
            out.add(prev)
        }
        return out
    }

    fun rsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0
        var gain = 0.0
        var loss = 0.0
        for (i in 1..period) {
            val d = closes[i] - closes[i - 1]
            if (d >= 0) gain += d else loss -= d
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        for (i in period + 1 until closes.size) {
            val d = closes[i] - closes[i - 1]
            val g = if (d > 0) d else 0.0
            val l = if (d < 0) -d else 0.0
            avgGain = (avgGain * (period - 1) + g) / period
            avgLoss = (avgLoss * (period - 1) + l) / period
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100 - 100 / (1 + rs)
    }

    /** Returns (macdLine, signalLine, histogram) for the last candle. */
    fun macd(closes: List<Double>): Triple<Double, Double, Double> {
        if (closes.size < 35) return Triple(0.0, 0.0, 0.0)
        val e12 = ema(closes, 12)
        val e26 = ema(closes, 26)
        val line = e12.zip(e26) { a, b -> a - b }
        val signal = ema(line, 9)
        val lastMacd = line.last()
        val lastSignal = signal.last()
        return Triple(lastMacd, lastSignal, lastMacd - lastSignal)
    }

    fun atr(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < 2) return 0.0
        val trs = ArrayList<Double>()
        for (i in 1 until candles.size) {
            val c = candles[i]
            val prev = candles[i - 1]
            val tr = max(c.high - c.low, max(abs(c.high - prev.close), abs(c.low - prev.close)))
            trs.add(tr)
        }
        if (trs.isEmpty()) return 0.0
        val n = min(period, trs.size)
        return trs.takeLast(n).average()
    }

    fun supportResistance(candles: List<Candle>): Pair<Double, Double> {
        if (candles.isEmpty()) return Pair(0.0, 0.0)
        val recent = candles.takeLast(min(50, candles.size))
        val support = recent.minOf { it.low }
        val resistance = recent.maxOf { it.high }
        return Pair(support, resistance)
    }

    fun detectPattern(candles: List<Candle>): String {
        if (candles.size < 2) return "No pattern"
        val last = candles.last()
        val prev = candles[candles.size - 2]
        val body = abs(last.close - last.open)
        val range = last.high - last.low
        if (range > 0 && body / range < 0.3) {
            val upperWick = last.high - max(last.open, last.close)
            val lowerWick = min(last.open, last.close) - last.low
            if (lowerWick > body * 2 && upperWick < body) return "Pin Bar (bullish)"
            if (upperWick > body * 2 && lowerWick < body) return "Pin Bar (bearish)"
            return "Doji — indecision"
        }
        val prevBearish = prev.close < prev.open
        val prevBullish = prev.close > prev.open
        val lastBullish = last.close > last.open
        val lastBearish = last.close < last.open
        if (prevBearish && lastBullish && last.open <= prev.close && last.close >= prev.open) {
            return "Bullish Engulfing"
        }
        if (prevBullish && lastBearish && last.open >= prev.close && last.close <= prev.open) {
            return "Bearish Engulfing"
        }
        return if (lastBullish) "Bullish candle" else "Bearish candle"
    }

    fun analyse(candles: List<Candle>): Analysis {
        val closes = candles.map { it.close }
        val e20 = ema(closes, 20)
        val e50 = ema(closes, 50)
        val ema20 = e20.lastOrNull() ?: closes.lastOrNull() ?: 0.0
        val ema50 = e50.lastOrNull() ?: ema20
        val price = closes.lastOrNull() ?: 0.0
        val trend = when {
            ema20 > ema50 && price > ema20 -> Trend.UPTREND
            ema20 < ema50 && price < ema20 -> Trend.DOWNTREND
            else -> Trend.SIDEWAYS
        }
        val (macdLine, signal, _) = macd(closes)
        val (support, resistance) = supportResistance(candles)
        val volumes = candles.map { it.volume }
        val recentVol = volumes.takeLast(10).average()
        val prevVol = volumes.dropLast(10).takeLast(10).average()
        val volChange = if (prevVol > 0) (recentVol - prevVol) / prevVol * 100 else 0.0
        return Analysis(
            rsi = rsi(closes),
            macd = macdLine,
            macdSignal = signal,
            ema20 = ema20,
            ema50 = ema50,
            trend = trend,
            support = support,
            resistance = resistance,
            pattern = detectPattern(candles),
            volumeChangePct = volChange,
            atr = atr(candles)
        )
    }

    fun timeframeMillis(timeframe: String): Long = when (timeframe) {
        "1m" -> 60_000L
        "5m" -> 300_000L
        "15m" -> 900_000L
        "1h" -> 3_600_000L
        "4h" -> 14_400_000L
        else -> 86_400_000L
    }

    /**
     * Porer [count] ta candle kemon dekhte hote pare — demo projection, guarantee na.
     * Trend (EMA), RSI momentum, ATR range ar support/resistance diye banano.
     * Deterministic wobble: same candle/timeframe-e protibar eki projection dekhabe.
     */
    fun forecastCandles(
        candles: List<Candle>,
        analysis: Analysis,
        scoreTotal: Int,
        timeframe: String,
        count: Int = 5
    ): List<Candle> {
        if (candles.size < 5) return emptyList()
        val last = candles.last()
        val atr = if (analysis.atr > 0) analysis.atr else last.close * 0.01
        val trendDir = when (analysis.trend) {
            Trend.UPTREND -> 1.0
            Trend.DOWNTREND -> -1.0
            else -> 0.0
        }
        // RSI extreme hole ulto dike tan pore
        val momentum = when {
            analysis.rsi > 75 -> -0.4
            analysis.rsi < 25 -> 0.4
            else -> 0.0
        }
        val dir = (trendDir + momentum).coerceIn(-1.0, 1.0)
        val stepBase = atr * 0.45
        var seed = (last.time xor timeframe.hashCode().toLong()) and 0x7fffffffL
        fun nextRand(): Double {
            seed = (seed * 1103515245 + 12345) and 0x7fffffffL
            return seed / 0x7fffffff.toDouble()
        }
        val stepMs = timeframeMillis(timeframe)
        val strongBreakout = scoreTotal >= 70
        val out = ArrayList<Candle>()
        var prevClose = last.close
        var t = last.time + stepMs
        repeat(count) { i ->
            val damp = 1.0 - i * 0.12
            val drift = dir * stepBase * damp + (nextRand() - 0.5) * atr * 0.5
            val open = prevClose
            var close = open + drift
            // Support/resistance-er baire jete na parle okhanei atke jabe,
            // score strong hole breakout korte pare.
            if (!strongBreakout) {
                if (close > analysis.resistance && analysis.resistance > 0) {
                    close = min(close, analysis.resistance * 1.005)
                }
                if (close < analysis.support && analysis.support > 0) {
                    close = max(close, analysis.support * 0.995)
                }
            }
            val high = maxOf(open, close) + nextRand() * atr * 0.25
            val low = minOf(open, close) - nextRand() * atr * 0.25
            out.add(Candle(t, open, high, low, close, last.volume))
            prevClose = close
            t += stepMs
        }
        return out
    }

    /** দাম বনাম RSI মোমেন্টাম ডাইভারজেন্স — দাম একদিকে, RSI অন্যদিকে গেলে সতর্কতা। */
    fun detectDivergence(candles: List<Candle>): Pair<String, Boolean> {
        val closes = candles.map { it.close }
        if (closes.size < 30) return Pair("ডাইভারজেন্স যাচাই করার মতো ডেটা নেই", false)
        val recent = closes.takeLast(20)
        val prev = closes.dropLast(10).takeLast(20)
        val priceChg = (recent.last() - prev.last()) / prev.last() * 100
        val rsiRecent = rsi(recent)
        val rsiPrev = rsi(prev)
        return when {
            priceChg > 1.5 && rsiRecent < rsiPrev - 5 ->
                Pair("বিয়ারিশ ডাইভারজেন্স — দাম বাড়ছে কিন্তু RSI কমছে, নিচে আসার সতর্কতা।", true)
            priceChg < -1.5 && rsiRecent > rsiPrev + 5 ->
                Pair("বুলিশ ডাইভারজেন্স — দাম কমছে কিন্তু RSI বাড়ছে, বাউন্সের সম্ভাবনা।", false)
            else -> Pair("কোনো ডাইভারজেন্স নেই — দাম আর RSI একই দিকে।", false)
        }
    }

    /** আল্ট্রা অ্যানালিটিক্স — সব ফ্যাক্টর মিলিয়ে ১০০-র মধ্যে আল্ট্রা স্কোর। */
    fun ultraAnalysis(
        analysis: Analysis,
        candles: List<Candle>,
        scoreTotal: Int,
        dailyTrend: Trend?,
        price: Double
    ): com.tradelens.app.data.UltraAnalysis {
        val closes = candles.map { it.close }
        val spreadPct = if (price > 0) kotlin.math.abs(analysis.ema20 - analysis.ema50) / price * 100 else 0.0
        val atrPct = if (price > 0 && analysis.atr > 0) analysis.atr / price * 100 else 0.0
        val ratio = if (atrPct > 0) spreadPct / atrPct else 0.0
        val strengthPct = (ratio * 50).toInt().coerceIn(0, 100)
        val strengthLabel = when {
            strengthPct >= 60 -> "শক্তিশালী"
            strengthPct >= 30 -> "মাঝারি"
            else -> "দুর্বল"
        }
        val volatility = when {
            atrPct < 1.0 -> "কম"
            atrPct < 3.0 -> "স্বাভাবিক"
            else -> "বেশি"
        }
        val (divergenceText, divergenceBearish) = detectDivergence(candles)
        val atr = if (analysis.atr > 0) analysis.atr else price * 0.01
        val breakoutChance = when (analysis.trend) {
            Trend.UPTREND -> {
                val dist = if (analysis.resistance > price) (analysis.resistance - price) / atr else 0.0
                (75 - dist * 20).toInt()
            }
            Trend.DOWNTREND -> {
                val dist = if (price > analysis.support) (price - analysis.support) / atr else 0.0
                (75 - dist * 20).toInt()
            }
            else -> 35
        }.coerceIn(5, 95)
        val daily = dailyTrend ?: analysis.trend
        val confluence = dailyTrend != null && dailyTrend == analysis.trend
        val confluenceScore = if (confluence) 100 else 40
        val divergenceScore = if (divergenceBearish) 20 else 90
        val rsiScore = when {
            analysis.rsi in 40.0..65.0 -> 90
            analysis.rsi < 30 || analysis.rsi > 70 -> 50
            else -> 70
        }
        val ultraScore = (
            strengthPct * 0.25 + breakoutChance * 0.25 +
                confluenceScore * 0.2 + divergenceScore * 0.15 + rsiScore * 0.15
            ).toInt().coerceIn(0, 100)
        val ultraVerdict = when {
            ultraScore >= 70 -> "আল্ট্রা স্ট্রং"
            ultraScore >= 45 -> "মাঝারি"
            else -> "দুর্বল"
        }
        return com.tradelens.app.data.UltraAnalysis(
            trendStrength = strengthLabel,
            trendStrengthPct = strengthPct,
            volatility = volatility,
            divergence = divergenceText,
            divergenceBearish = divergenceBearish,
            breakoutChance = breakoutChance,
            dailyTrend = daily,
            confluence = confluence,
            ultraScore = ultraScore,
            ultraVerdict = ultraVerdict
        )
    }
}
