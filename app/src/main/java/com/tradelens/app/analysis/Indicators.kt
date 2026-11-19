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
}
