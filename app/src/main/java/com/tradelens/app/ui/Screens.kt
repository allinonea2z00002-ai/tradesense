package com.tradelens.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradelens.app.data.Asset
import com.tradelens.app.data.Category
import com.tradelens.app.data.MarketAnalysis
import com.tradelens.app.data.NewsItem
import com.tradelens.app.data.Sentiment
import com.tradelens.app.data.Verdict
import com.tradelens.app.ui.theme.TradeAmber
import com.tradelens.app.ui.theme.TradeBg
import com.tradelens.app.ui.theme.TradeBlue
import com.tradelens.app.ui.theme.TradeCard
import com.tradelens.app.ui.theme.TradeGreen
import com.tradelens.app.ui.theme.TradeMuted
import com.tradelens.app.ui.theme.TradeRed
import java.util.Locale

fun fmt(value: Double, decimals: Int): String =
    String.format(Locale.US, "%,.${decimals}f", value)

fun fmtPrice(value: Double): String =
    if (value >= 1000) String.format(Locale.US, "%,.2f", value)
    else if (value >= 10) String.format(Locale.US, "%.2f", value)
    else String.format(Locale.US, "%.4f", value)

// ================= HOME — coin-er list =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: TradeUiState, vm: TradeViewModel) {
    val filtered = if (state.category == null) state.assets
    else state.assets.filter { it.category == state.category }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TradeLens", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            "Chart + News + Score — er por ki hote pare",
                            color = TradeMuted, fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TradeBg)
            )
        },
        containerColor = TradeBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A00)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Sotto kotha: kono app-i 100% sothik predict korte pare na. " +
                            "Eta sudhu decision nite help korbe.",
                        modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = TradeAmber
                    )
                }
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        CategoryChip("Sob", state.category == null) { vm.selectCategory(null) }
                    }
                    items(Category.entries) { c ->
                        CategoryChip(c.name, state.category == c) { vm.selectCategory(c) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Coin-e tap koro — tar pura detail window khulbe", color = TradeMuted, fontSize = 13.sp)
            }
            items(filtered) { asset ->
                val quote = state.quotes[asset.id]
                AssetRow(asset = asset, price = quote?.first, change24h = quote?.second) {
                    vm.openDetail(asset)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) TradeGreen else TradeCard,
            labelColor = if (selected) Color.Black else Color.White
        )
    )
}

@Composable
private fun AssetRow(
    asset: Asset,
    price: Double?,
    change24h: Double?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TradeCard)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                    .background(TradeBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    asset.symbol.substringBefore("/").take(2),
                    fontWeight = FontWeight.Bold, color = TradeBlue, fontSize = 17.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(asset.symbol, color = TradeMuted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (price != null) "$" + fmtPrice(price) else "...",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                if (change24h != null) {
                    Text(
                        (if (change24h >= 0) "+" else "") + fmt(change24h, 2) + "%",
                        color = if (change24h >= 0) TradeGreen else TradeRed,
                        fontSize = 12.sp
                    )
                }
            }
            Text("  ›", color = TradeMuted, fontSize = 20.sp)
        }
    }
}

// ================= DETAIL — ekta coin-er pura window =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(state: TradeUiState, vm: TradeViewModel) {
    val analysis = state.analysis
    val asset = state.detailAsset ?: state.selected
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(asset.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { vm.closeDetail() }) {
                        Text("‹ Back", color = TradeGreen, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.refresh() }) {
                        Text("↻ Refresh", color = TradeGreen, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TradeBg)
            )
        },
        containerColor = TradeBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (state.loading && analysis == null) {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TradeGreen)
                    }
                } else if (analysis != null) {
                    DetailContent(analysis = analysis, state = state, vm = vm)
                }
                state.error?.let {
                    Text(it, color = TradeRed, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailContent(analysis: MarketAnalysis, state: TradeUiState, vm: TradeViewModel) {
    val a = analysis.analysis
    val verdictText = when (analysis.verdict) {
        Verdict.STRONG_BUY -> "Strong Buy"
        Verdict.WAIT -> "Wait"
        Verdict.AVOID -> "Avoid / Sell"
    }
    val verdictColor = when (analysis.verdict) {
        Verdict.STRONG_BUY -> TradeGreen
        Verdict.WAIT -> TradeAmber
        Verdict.AVOID -> TradeRed
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Price header
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(analysis.asset.symbol, color = TradeMuted, fontSize = 13.sp)
                    Text(
                        "$" + fmtPrice(analysis.price),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        (if (analysis.change24hPct >= 0) "+" else "") +
                            fmt(analysis.change24hPct, 2) + "% (24h)",
                        color = if (analysis.change24hPct >= 0) TradeGreen else TradeRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${analysis.score.total}/100",
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, color = verdictColor
                    )
                    Text(verdictText, fontWeight = FontWeight.Bold, color = verdictColor)
                }
            }
            if (analysis.offlineDemo) {
                Text(
                    "Offline demo data — live price-er jonno internet lagbe",
                    color = TradeAmber, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            }
        }

        // Chart + timeframe
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp)) {
                Text("Chart — ${state.timeframe} candle", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(vm.timeframes) { tf ->
                        AssistChip(
                            onClick = { vm.setTimeframe(tf) },
                            label = { Text(tf) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (tf == state.timeframe) TradeGreen else TradeCard,
                                labelColor = if (tf == state.timeframe) Color.Black else Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CandleChart(
                    candles = analysis.candles,
                    ema20 = analysis.ema20Series,
                    support = a.support,
                    resistance = a.resistance,
                    forecast = analysis.forecast.candles
                )
                Text(
                    "— Sobuj line: Support  " + fmtPrice(a.support) +
                        "    — Lal line: Resistance  " + fmtPrice(a.resistance) +
                        "    — Nil: EMA20",
                    color = TradeMuted, fontSize = 11.sp
                )
                Text(
                    "Daan diker halka (dashed) candle-gula forecast — porer candle kemon hote pare. " +
                        "Holud dashed line: forecast-e kothay jete pare.",
                    color = TradeAmber, fontSize = 11.sp
                )
            }
        }

        // Forecast — porer candle-gula
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Porer candle-gula kemon hote pare?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val f = analysis.forecast
                val dirColor = when (f.direction) {
                    "UP" -> TradeGreen
                    "DOWN" -> TradeRed
                    else -> TradeAmber
                }
                Text(
                    f.directionText + "  •  " +
                        (if (f.movePct >= 0) "+" else "") + fmt(f.movePct, 2) + "% porjonto",
                    fontWeight = FontWeight.Bold, color = dirColor, fontSize = 16.sp
                )
                Text(f.perCandleText, color = TradeMuted, fontSize = 13.sp)
                var prev = analysis.price
                f.candles.forEachIndexed { i, c ->
                    val chg = if (prev > 0) (c.close - prev) / prev * 100 else 0.0
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "Candle ${i + 1} (${state.timeframe})",
                            modifier = Modifier.weight(1f), color = TradeMuted
                        )
                        Text(
                            "$" + fmtPrice(c.close) + "  " +
                                (if (c.close >= c.open) "↑" else "↓") + " " +
                                (if (chg >= 0) "+" else "") + fmt(chg, 2) + "%",
                            fontWeight = FontWeight.Bold,
                            color = if (c.close >= c.open) TradeGreen else TradeRed
                        )
                    }
                    prev = c.close
                }
                Text(
                    "Eta demo projection — trend, RSI ar ATR diye banano. Guarantee na.",
                    color = TradeMuted, fontSize = 11.sp
                )
            }
        }

        // Score — main jinish
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${analysis.score.total}/100",
                        fontSize = 34.sp, fontWeight = FontWeight.Bold, color = verdictColor
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(verdictText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = verdictColor)
                        Text("Score 70+ Strong Buy • 40–70 Wait • 40- Avoid", color = TradeMuted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = analysis.score.total / 100f,
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = verdictColor,
                    trackColor = Color(0xFF26324A)
                )
                Spacer(Modifier.height(12.dp))
                ScoreRow("Trend (${a.trend})", analysis.score.trendPoints, 20)
                ScoreRow("RSI (${fmt(a.rsi, 1)})", analysis.score.rsiPoints, 15)
                ScoreRow("Support-er kache", analysis.score.supportPoints, 20)
                ScoreRow("News sentiment", analysis.score.newsPoints, 15)
                ScoreRow("Volume (${fmt(a.volumeChangePct, 0)}%)", analysis.score.volumePoints, 10)
                ScoreRow("MACD", analysis.score.macdPoints, 10)
                ScoreRow("EMA 20/50", analysis.score.emaPoints, 10)
            }
        }

        // Ekhon ki obostha
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ekhon ki obostha?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val trendText = when (a.trend) {
                    com.tradelens.app.data.Trend.UPTREND -> "Trend up — dam upore jawar pothe ache."
                    com.tradelens.app.data.Trend.DOWNTREND -> "Trend down — dam niche jawar pothe ache."
                    com.tradelens.app.data.Trend.SIDEWAYS -> "Trend sideways — dam ekta range-er vitore ghurche."
                }
                Text(trendText)
                Text("Price Action: ${a.pattern}")
                Text(
                    "RSI ${fmt(a.rsi, 1)} — " +
                        if (a.rsi < 30) "oversold, bounce korte pare."
                        else if (a.rsi > 70) "overbought, niche ashar chance."
                        else "neutral zone."
                )
                Text(
                    "Support $${fmtPrice(a.support)} ar Resistance $${fmtPrice(a.resistance)}-er majhkhane dam $${fmtPrice(analysis.price)}-e ache."
                )
                Text("Target: $${fmtPrice(analysis.target)}  •  Stop Loss: $${fmtPrice(analysis.stopLoss)}")
            }
        }

        // Samne ki hote pare
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Samne ki hote pare? (probability, guarantee na)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                analysis.scenarios.forEach { s ->
                    Column {
                        Row(Modifier.fillMaxWidth()) {
                            Text(s.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${s.probability}%", fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = s.probability / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = when (s.name) {
                                "Bullish" -> TradeGreen
                                "Bearish" -> TradeRed
                                else -> TradeAmber
                            },
                            trackColor = Color(0xFF26324A)
                        )
                        Text(s.description + " Target: $" + fmtPrice(s.target), color = TradeMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        // Keno hote pare
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ki karone hote pare?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                analysis.forecast.reasons.forEach { r ->
                    Text("•  $r")
                }
            }
        }

        // Ki kora uchit
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12233D))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ki kora uchit?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TradeGreen)
                Text(analysis.forecast.action)
                Text(
                    "Target: $${fmtPrice(analysis.target)}  •  Stop Loss: $${fmtPrice(analysis.stopLoss)}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Taka laganor decision tomar nijer — app sudhu help korbe.",
                    color = TradeMuted, fontSize = 12.sp
                )
            }
        }

        // Indicators
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Indicator", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                IndicatorRow("RSI (14)", fmt(a.rsi, 1) + if (a.rsi < 30) " — Oversold" else if (a.rsi > 70) " — Overbought" else " — Neutral")
                IndicatorRow("MACD", fmt(a.macd, 2) + " / Signal " + fmt(a.macdSignal, 2))
                IndicatorRow("EMA 20 / 50", fmtPrice(a.ema20) + " / " + fmtPrice(a.ema50))
                IndicatorRow("Support / Resistance", fmtPrice(a.support) + " / " + fmtPrice(a.resistance))
                IndicatorRow("Price Action", a.pattern)
                IndicatorRow("ATR", fmtPrice(a.atr))
                IndicatorRow("Target / Stop Loss", fmtPrice(analysis.target) + " / " + fmtPrice(analysis.stopLoss))
            }
        }

        // News
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Latest News", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (analysis.news.isEmpty()) {
                    Text("Kono news pai ni.", color = TradeMuted)
                }
                analysis.news.forEach { n -> NewsRow(n) }
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, points: Int, max: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text("$points / $max", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IndicatorRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TradeMuted, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NewsRow(n: NewsItem) {
    val color = when (n.sentiment) {
        Sentiment.POSITIVE -> TradeGreen
        Sentiment.NEGATIVE -> TradeRed
        Sentiment.NEUTRAL -> TradeMuted
    }
    val tag = when (n.sentiment) {
        Sentiment.POSITIVE -> "● Positive"
        Sentiment.NEGATIVE -> "● Negative"
        Sentiment.NEUTRAL -> "● Neutral"
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B1220)).padding(10.dp)
    ) {
        Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("$tag  •  ${n.source}", color = color, fontSize = 12.sp)
    }
}
