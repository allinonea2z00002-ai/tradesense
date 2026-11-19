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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable
fun HomeScreen(state: TradeUiState, vm: TradeViewModel) {
    val filtered = if (state.category == null) state.assets
    else state.assets.filter { it.category == state.category }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("TradeLens", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Chart + News + Indicator miliye score — er por ki hote pare",
                color = TradeMuted, fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    CategoryChip("Sob", state.category == null) { vm.selectCategory(null) }
                }
                items(Category.entries) { c ->
                    CategoryChip(c.name, state.category == c) { vm.selectCategory(c) }
                }
            }
        }
        items(filtered) { asset ->
            AssetRow(asset = asset, selected = asset.id == state.selected.id) {
                vm.selectAsset(asset)
            }
        }
        item {
            val a = state.analysis
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TradeGreen)
                }
            } else if (a != null) {
                DetailCard(analysis = a, state = state, vm = vm)
            }
            state.error?.let {
                Text(it, color = TradeRed, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
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
private fun AssetRow(asset: Asset, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF1B2B4A) else TradeCard
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                    .background(TradeBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    asset.symbol.substringBefore("/").take(2),
                    fontWeight = FontWeight.Bold, color = TradeBlue
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(asset.name, fontWeight = FontWeight.Bold)
                Text(asset.symbol, color = TradeMuted, fontSize = 12.sp)
            }
            Text(if (selected) "▶  Analysis niche" else "Tap koro", color = TradeMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DetailCard(analysis: MarketAnalysis, state: TradeUiState, vm: TradeViewModel) {
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
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(analysis.asset.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(analysis.asset.symbol, color = TradeMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$" + fmtPrice(analysis.price),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            (if (analysis.change24hPct >= 0) "+" else "") +
                                fmt(analysis.change24hPct, 2) + "% (24h)",
                            color = if (analysis.change24hPct >= 0) TradeGreen else TradeRed
                        )
                    }
                }
                if (analysis.offlineDemo) {
                    Text(
                        "Offline demo data — live price-er jonno internet lagbe",
                        color = TradeAmber, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
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
                    resistance = a.resistance
                )
                Text(
                    "— Green line: Support  " + fmtPrice(a.support) +
                        "    — Red line: Resistance  " + fmtPrice(a.resistance) +
                        "    — Blue: EMA20",
                    color = TradeMuted, fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh")
                }
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

        // Scenarios
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Er por ki hote pare? (probability, guarantee na)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
