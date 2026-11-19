package com.tradelens.app.analysis

import android.graphics.Bitmap
import com.tradelens.app.data.Candle
import kotlin.math.abs
import kotlin.math.max

/**
 * ফটো অ্যানালিটিক্স — চার্টের ছবির সবুজ/লাল পিক্সেল বিশ্লেষণ করে
 * পরের ক্যান্ডেল কোন দিকে যাবে আর কত বড় হতে পারে, তার ধারণা দেয়।
 */
data class PhotoAnalysis(
    val greenPct: Int,
    val redPct: Int,
    val bullishPct: Int,
    val direction: String,
    val directionText: String,
    val avgBodyPct: Float,
    val confidence: Int
)

object PhotoAnalyzer {

    fun analyze(bitmap: Bitmap): PhotoAnalysis {
        val targetW = 240
        val scale = targetW.toFloat() / bitmap.width
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        val pixels = IntArray(targetW * targetH)
        scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        var green = 0
        var red = 0
        var greenRight = 0
        var redRight = 0
        var greenLeft = 0
        var redLeft = 0
        val runs = ArrayList<Int>()
        for (x in 0 until targetW) {
            var run = 0
            for (y in 0 until targetH) {
                val p = pixels[y * targetW + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val isGreen = g > 90 && g > r + 35 && g > b + 20
                val isRed = r > 90 && r > g + 35 && r > b + 20
                if (isGreen || isRed) {
                    run++
                    if (isGreen) {
                        green++
                        if (x >= targetW / 2) greenRight++ else greenLeft++
                    } else {
                        red++
                        if (x >= targetW / 2) redRight++ else redLeft++
                    }
                } else {
                    if (run >= 3) runs.add(run)
                    run = 0
                }
            }
            if (run >= 3) runs.add(run)
        }
        val total = green + red
        val greenShare = if (total > 0) green * 100.0 / total else 50.0
        val rightShare = if (greenRight + redRight > 0) {
            greenRight * 100.0 / (greenRight + redRight)
        } else {
            50.0
        }
        val leftShare = if (greenLeft + redLeft > 0) {
            greenLeft * 100.0 / (greenLeft + redLeft)
        } else {
            50.0
        }
        // ডানদিকের মোমেন্টাম — সাম্প্রতিক ক্যান্ডেলগুলো কোন দিকে
        val momentum = rightShare - leftShare
        val bullish = (greenShare + momentum * 0.5).coerceIn(0.0, 100.0).toInt()
        val direction = when {
            bullish >= 60 -> "UP"
            bullish <= 40 -> "DOWN"
            else -> "SIDEWAYS"
        }
        val directionText = when (direction) {
            "UP" -> "উপরের দিকে যেতে পারে"
            "DOWN" -> "নিচের দিকে যেতে পারে"
            else -> "সাইডওয়েজ থাকতে পারে"
        }
        // গড় ক্যান্ডেল বডির উচ্চতা (ছবির উচ্চতার শতাংশ) — পরের ক্যান্ডেলের সাইজের ধারণা
        val avgBody = if (runs.isNotEmpty()) runs.average().toFloat() else targetH * 0.04f
        val avgBodyPct = (avgBody / targetH * 100).coerceIn(0.5f, 30f)
        val confidence = (50 + abs(bullish - 50)).coerceIn(40, 95)
        return PhotoAnalysis(
            greenPct = greenShare.toInt(),
            redPct = (100 - greenShare).toInt(),
            bullishPct = bullish,
            direction = direction,
            directionText = directionText,
            avgBodyPct = avgBodyPct,
            confidence = confidence
        )
    }
}

/**
 * স্ক্রিন ক্যান্ডেল অ্যানালিটিক্স — স্ক্রিনে এখন যা ক্যান্ডেল দেখা যাচ্ছে,
 * শুধু সেগুলো বিশ্লেষণ করে প্যাটার্ন, ফলাফল আর পরের ক্যান্ডেলের ধারণা দেয়।
 */
data class ScreenAnalysis(
    val greenCount: Int,
    val redCount: Int,
    val windowChangePct: Double,
    val patternText: String,
    val verdictText: String,
    val nextCandle: Candle?,
    val confidence: Int
)

object ScreenAnalyzer {

    fun analyze(candles: List<Candle>, timeframe: String): ScreenAnalysis {
        if (candles.isEmpty()) {
            return ScreenAnalysis(0, 0, 0.0, "ক্যান্ডেল নেই", "—", null, 0)
        }
        val green = candles.count { it.close >= it.open }
        val red = candles.size - green
        val first = candles.first().close
        val change = if (first > 0) (candles.last().close - first) / first * 100 else 0.0
        val last = candles.last()
        val body = abs(last.close - last.open)
        val upperWick = last.high - max(last.open, last.close)
        val last3 = candles.takeLast(3)
        val patternText = when {
            last3.size == 3 && last3.all { it.close > it.open } ->
                "শেষ ৩টি ক্যান্ডেল সবুজ — বুলিশ মোমেন্টাম শক্তিশালী।"
            last3.size == 3 && last3.all { it.close < it.open } ->
                "শেষ ৩টি ক্যান্ডেল লাল — বিয়ারিশ মোমেন্টাম শক্তিশালী।"
            upperWick > body * 2 && body > 0 ->
                "শেষ ক্যান্ডেলে লম্বা উপরের উইক — উপরে রিজেকশন হয়েছে, নিচে আসতে পারে।"
            last.close > last.open && body > 0 ->
                "শেষ ক্যান্ডেল সবুজ — ক্রেতারা এগিয়ে।"
            last.close < last.open && body > 0 ->
                "শেষ ক্যান্ডেল লাল — বিক্রেতারা এগিয়ে।"
            else -> "কোনো স্পষ্ট প্যাটার্ন নেই — মিশ্র মুভ।"
        }
        val analysis = Indicators.analyse(candles)
        val score = ScoreEngine.compute(last.close, analysis, emptyList()).total
        val next = Indicators.forecastCandles(candles, analysis, score, timeframe, 1).firstOrNull()
        val verdictText = when {
            score >= 70 -> "স্ক্রিনের ক্যান্ডেল বলছে: স্ট্রং বাই"
            score >= 40 -> "স্ক্রিনের ক্যান্ডেল বলছে: অপেক্ষা করুন"
            else -> "স্ক্রিনের ক্যান্ডেল বলছে: এড়িয়ে চলুন"
        }
        val greenShare = if (candles.isNotEmpty()) green * 100 / candles.size else 50
        val confidence = (55 + abs(greenShare - 50)).coerceIn(40, 95)
        return ScreenAnalysis(green, red, change, patternText, verdictText, next, confidence)
    }
}
