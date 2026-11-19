package com.tradelens.app.data

import com.tradelens.app.analysis.ScoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Live data, no API key needed.
 * Crypto: Binance public API. Forex / Gold / Stock: Yahoo Finance chart API.
 * News: CoinDesk RSS. If the network fails, callers fall back to demo candles.
 */
object MarketApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String? = try {
        val req = Request.Builder().url(url)
            .header("User-Agent", "TradeLens/1.0")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    } catch (_: Exception) {
        null
    }

    fun binanceInterval(timeframe: String): String = timeframe

    fun yahooInterval(timeframe: String): Pair<String, String> = when (timeframe) {
        "1m" -> Pair("1d", "1m")
        "5m" -> Pair("5d", "5m")
        "15m" -> Pair("5d", "15m")
        "1h" -> Pair("5d", "1h")
        "4h" -> Pair("1mo", "1h")
        else -> Pair("6mo", "1d")
    }

    suspend fun fetchCandles(asset: Asset, timeframe: String): List<Candle> =
        withContext(Dispatchers.IO) {
            val binance = asset.binanceSymbol
            if (binance != null) {
                val body = get(
                    "https://api.binance.com/api/v3/klines?symbol=$binance" +
                        "&interval=${binanceInterval(timeframe)}&limit=100"
                )
                if (body != null) {
                    val arr = JSONArray(body)
                    val out = ArrayList<Candle>()
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONArray(i)
                        out.add(
                            Candle(
                                time = r.getLong(0),
                                open = r.getString(1).toDouble(),
                                high = r.getString(2).toDouble(),
                                low = r.getString(3).toDouble(),
                                close = r.getString(4).toDouble(),
                                volume = r.getString(5).toDouble()
                            )
                        )
                    }
                    if (out.size >= 20) return@withContext out
                }
            }
            val (range, interval) = yahooInterval(timeframe)
            val body = get(
                "https://query1.finance.yahoo.com/v8/finance/chart/${asset.yahooSymbol}" +
                    "?range=$range&interval=$interval"
            )
            if (body != null) {
                val parsed = parseYahoo(body)
                if (parsed.size >= 20) return@withContext parsed
            }
            emptyList()
        }

    private fun parseYahoo(body: String): List<Candle> {
        return try {
            val chart = JSONObject(body).getJSONObject("chart")
            val result = chart.getJSONArray("result").getJSONObject(0)
            val timestamps = result.getJSONArray("timestamp")
            val quote = result.getJSONObject("indicators")
                .getJSONArray("quote").getJSONObject(0)
            val opens = quote.getJSONArray("open")
            val highs = quote.getJSONArray("high")
            val lows = quote.getJSONArray("low")
            val closes = quote.getJSONArray("close")
            val volumes = quote.getJSONArray("volume")
            val out = ArrayList<Candle>()
            for (i in 0 until timestamps.length()) {
                if (opens.isNull(i) || closes.isNull(i)) continue
                out.add(
                    Candle(
                        time = timestamps.getLong(i) * 1000,
                        open = opens.getDouble(i),
                        high = highs.getDouble(i),
                        low = lows.getDouble(i),
                        close = closes.getDouble(i),
                        volume = if (volumes.isNull(i)) 0.0 else volumes.getDouble(i)
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchTicker(asset: Asset): Pair<Double, Double> =
        withContext(Dispatchers.IO) {
            val binance = asset.binanceSymbol
            if (binance != null) {
                val body = get("https://api.binance.com/api/v3/ticker/24hr?symbol=$binance")
                if (body != null) {
                    val obj = JSONObject(body)
                    return@withContext Pair(
                        obj.getString("lastPrice").toDouble(),
                        obj.optString("priceChangePercent", "0").toDouble()
                    )
                }
            }
            val body = get(
                "https://query1.finance.yahoo.com/v8/finance/chart/${asset.yahooSymbol}" +
                    "?range=5d&interval=1d"
            )
            if (body != null) {
                val candles = parseYahoo(body)
                if (candles.size >= 2) {
                    val last = candles.last().close
                    val prev = candles[candles.size - 2].close
                    val pct = if (prev != 0.0) (last - prev) / prev * 100 else 0.0
                    return@withContext Pair(last, pct)
                }
            }
            Pair(0.0, 0.0)
        }

    suspend fun fetchNews(asset: Asset): List<NewsItem> =
        withContext(Dispatchers.IO) {
            val body = get("https://www.coindesk.com/arc/outboundfeeds/rss/")
                ?: return@withContext emptyList()
            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(StringReader(body))
                val out = ArrayList<NewsItem>()
                var title = ""
                var link = ""
                var inItem = false
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT && out.size < 20) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            val name = parser.name
                            if (name == "item") {
                                inItem = true
                                title = ""
                                link = ""
                            } else if (inItem && name == "title") {
                                title = parser.nextText()
                            } else if (inItem && name == "link") {
                                link = parser.nextText()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "item" && title.isNotBlank()) {
                                inItem = false
                                val keywords = listOf(
                                    asset.name.lowercase(),
                                    asset.symbol.substringBefore("/").lowercase(),
                                    "bitcoin", "crypto", "market"
                                )
                                val lower = title.lowercase()
                                if (keywords.any { lower.contains(it) } || asset.category == Category.CRYPTO) {
                                    out.add(
                                        NewsItem(
                                            title = title,
                                            source = "CoinDesk",
                                            timeText = "Latest",
                                            url = link,
                                            sentiment = ScoreEngine.sentimentOf(title)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
                out.take(8)
            } catch (_: Exception) {
                emptyList()
            }
        }

    /** Deterministic offline demo candles so the UI still works without internet. */
    fun demoCandles(asset: Asset): List<Candle> {
        val seed = abs(asset.id.hashCode()).toLong()
        val random = Random(seed)
        var price = when (asset.id) {
            "btc" -> 95000.0
            "eth" -> 3300.0
            "xau" -> 2650.0
            "eurusd" -> 1.08
            else -> 100.0
        }
        val out = ArrayList<Candle>()
        var t = System.currentTimeMillis() - 100L * 3_600_000L
        repeat(100) {
            val drift = (random.nextDouble() - 0.48) * price * 0.004
            val open = price
            val close = open + drift
            val high = maxOf(open, close) + abs(sin(it.toDouble())) * price * 0.001
            val low = minOf(open, close) - abs(sin(it * 1.7)) * price * 0.001
            out.add(Candle(t, open, high, low, close, 1000 + random.nextDouble() * 5000))
            price = close
            t += 3_600_000L
        }
        return out
    }
}
