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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tradelens.app.data.Trend
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

private fun categoryLabel(c: Category, s: UiText): String = when (c) {
    Category.CRYPTO -> s.catCrypto
    Category.FOREX -> s.catForex
    Category.GOLD -> s.catGold
    Category.STOCK -> s.catStock
}

// ================= HOME — কয়েনের লিস্ট =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    val filtered = if (state.category == null) state.assets
    else state.assets.filter { it.category == state.category }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TradeLens", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(s.appSubtitle, color = TradeMuted, fontSize = 12.sp)
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
                        s.disclaimer,
                        modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = TradeAmber
                    )
                }
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        CategoryChip(s.all, state.category == null) { vm.selectCategory(null) }
                    }
                    items(Category.entries) { c ->
                        CategoryChip(categoryLabel(c, s), state.category == c) { vm.selectCategory(c) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(s.tapHint, color = TradeMuted, fontSize = 13.sp)
            }
            items(filtered) { asset ->
                val quote = state.quotes[asset.id]
                AssetRow(
                    asset = asset,
                    price = quote?.first,
                    change24h = quote?.second,
                    starred = state.watchlist.contains(asset.id),
                    onStar = { vm.toggleWatchlist(asset.id) }
                ) { vm.openDetail(asset) }
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
fun AssetRow(
    asset: Asset,
    price: Double?,
    change24h: Double?,
    starred: Boolean = false,
    onStar: (() -> Unit)? = null,
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
            if (onStar != null) {
                TextButton(onClick = onStar) {
                    Text(
                        if (starred) "★" else "☆",
                        color = if (starred) TradeAmber else TradeMuted, fontSize = 20.sp
                    )
                }
            } else {
                Text("  ›", color = TradeMuted, fontSize = 20.sp)
            }
        }
    }
}

// ================= টপ / ড্রপ =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopDropScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    val byId = state.assets.associateBy { it.id }
    val ranked = state.quotes.entries.sortedByDescending { it.value.second }
    val gainers = ranked.take(5).mapNotNull { byId[it.key] }
    val losers = ranked.takeLast(5).reversed().mapNotNull { byId[it.key] }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.navTopDrop, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TradeBg)
            )
        },
        containerColor = TradeBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            if (state.quotes.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = TradeGreen) }
                }
            } else {
                item {
                    Text(s.topGainers, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TradeGreen)
                }
                items(gainers) { asset ->
                    val q = state.quotes[asset.id]
                    AssetRow(asset, q?.first, q?.second) { vm.openDetail(asset) }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(s.topLosers, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TradeRed)
                }
                items(losers) { asset ->
                    val q = state.quotes[asset.id]
                    AssetRow(asset, q?.first, q?.second) { vm.openDetail(asset) }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ================= নিউজ =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.latestNews, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { vm.loadAllNews() }) {
                        Text(s.refresh, color = TradeGreen, fontWeight = FontWeight.Bold)
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
            item { Spacer(Modifier.height(4.dp)) }
            if (state.allNews.isEmpty()) {
                item { Text(s.noNews, color = TradeMuted) }
            }
            items(state.allNews) { n -> NewsRow(n, s) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ================= ওয়াচলিস্ট =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    val saved = state.assets.filter { state.watchlist.contains(it.id) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.navWatchlist, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TradeBg)
            )
        },
        containerColor = TradeBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            if (saved.isEmpty()) {
                item { Text(s.watchlistEmpty, color = TradeMuted) }
            }
            items(saved) { asset ->
                val q = state.quotes[asset.id]
                AssetRow(
                    asset = asset,
                    price = q?.first,
                    change24h = q?.second,
                    starred = true,
                    onStar = { vm.toggleWatchlist(asset.id) }
                ) { vm.openDetail(asset) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ================= সেটিংস =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settingsTitle, fontWeight = FontWeight.Bold) },
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
                Spacer(Modifier.height(4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(s.languageTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { vm.setLanguage("bn") },
                                label = { Text("বাংলা") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (state.lang == "bn") TradeGreen else TradeCard,
                                    labelColor = if (state.lang == "bn") Color.Black else Color.White
                                )
                            )
                            AssistChip(
                                onClick = { vm.setLanguage("en") },
                                label = { Text("English") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (state.lang == "en") TradeGreen else TradeCard,
                                    labelColor = if (state.lang == "en") Color.Black else Color.White
                                )
                            )
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.aboutTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(s.aboutText, color = TradeMuted)
                        Text(s.refreshNote, color = TradeMuted)
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A00))) {
                    Text(
                        s.disclaimer,
                        modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = TradeAmber
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ================= ডিটেইল — একটি কয়েনের পুরো উইন্ডো =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    val analysis = state.analysis
    val asset = state.detailAsset ?: state.selected
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(asset.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { vm.closeDetail() }) {
                        Text(s.back, color = TradeGreen, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.refresh() }) {
                        Text(s.refresh, color = TradeGreen, fontWeight = FontWeight.Bold)
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
    val s = state.strings
    val a = analysis.analysis
    val verdictText = when (analysis.verdict) {
        Verdict.STRONG_BUY -> s.verdictStrongBuy
        Verdict.WAIT -> s.verdictWait
        Verdict.AVOID -> s.verdictAvoid
    }
    val verdictColor = when (analysis.verdict) {
        Verdict.STRONG_BUY -> TradeGreen
        Verdict.WAIT -> TradeAmber
        Verdict.AVOID -> TradeRed
    }
    // Chart zoom state — কতটা ক্যান্ডেল দেখাবে, আর কতটা পিছনে গেছে
    var visibleCount by remember { mutableIntStateOf(60) }
    var startIndex by remember { mutableIntStateOf(0) }
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
                    s.offlineDemo,
                    color = TradeAmber, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            }
        }

        // Chart + timeframe + zoom + demo
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${s.chartTitle} — ${state.timeframe}",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { vm.toggleDemo() },
                        label = { Text(if (state.demoMode) s.demoChart else s.liveChart) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (state.demoMode) TradeAmber else TradeCard,
                            labelColor = if (state.demoMode) Color.Black else Color.White
                        )
                    )
                }
                if (state.demoMode) {
                    Text(s.demoBadge, color = TradeAmber, fontSize = 12.sp)
                }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("জুম:", color = TradeMuted, fontSize = 13.sp)
                    TextButton(onClick = { visibleCount = (visibleCount + 10).coerceAtMost(100) }) {
                        Text("－", color = TradeGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$visibleCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextButton(onClick = { visibleCount = (visibleCount - 10).coerceAtLeast(15) }) {
                        Text("＋", color = TradeGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { visibleCount = 60; startIndex = 0 }) {
                        Text("⟲ রিসেট", color = TradeGreen, fontSize = 13.sp)
                    }
                }
                CandleChart(
                    candles = analysis.candles,
                    ema20 = analysis.ema20Series,
                    support = a.support,
                    resistance = a.resistance,
                    forecast = analysis.forecast.candles,
                    visibleCount = visibleCount,
                    startIndex = startIndex,
                    onPan = { drag ->
                        val maxStart = (analysis.candles.size - visibleCount).coerceAtLeast(0)
                        startIndex = (startIndex - (drag / 12f).toInt()).coerceIn(0, maxStart)
                    }
                )
                Text(s.chartLegend, color = TradeMuted, fontSize = 11.sp)
                Text(s.forecastLegend, color = TradeAmber, fontSize = 11.sp)
                Text(
                    "চার্টে আঙুল টেনে আগের ক্যান্ডেল দেখুন • ＋ চাপলে জুম ইন, － চাপলে জুম আউট",
                    color = TradeMuted, fontSize = 11.sp
                )
            }
        }

        // Forecast — পরের ক্যান্ডেলগুলো
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.forecastTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val f = analysis.forecast
                val dirColor = when (f.direction) {
                    "UP" -> TradeGreen
                    "DOWN" -> TradeRed
                    else -> TradeAmber
                }
                Text(
                    f.directionText + "  •  " +
                        (if (f.movePct >= 0) "+" else "") + fmt(f.movePct, 2) + "% পর্যন্ত",
                    fontWeight = FontWeight.Bold, color = dirColor, fontSize = 16.sp
                )
                Text(f.perCandleText, color = TradeMuted, fontSize = 13.sp)
                var prev = analysis.price
                f.candles.forEachIndexed { i, c ->
                    val chg = if (prev > 0) (c.close - prev) / prev * 100 else 0.0
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "ক্যান্ডেল ${i + 1} (${state.timeframe})",
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
                Text(s.forecastNote, color = TradeMuted, fontSize = 11.sp)
            }
        }

        // Ultra Analytics
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val u = analysis.ultra
                val ultraColor = when {
                    u.ultraScore >= 70 -> TradeGreen
                    u.ultraScore >= 45 -> TradeAmber
                    else -> TradeRed
                }
                Text(s.ultraTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${u.ultraScore}/100",
                        fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ultraColor
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(u.ultraVerdict, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ultraColor)
                }
                LinearProgressIndicator(
                    progress = u.ultraScore / 100f,
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = ultraColor,
                    trackColor = Color(0xFF26324A)
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(s.trendStrength, color = TradeMuted, modifier = Modifier.weight(1f))
                    Text("${u.trendStrength} (${u.trendStrengthPct}%)", fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = u.trendStrengthPct / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = TradeBlue,
                    trackColor = Color(0xFF26324A)
                )
                IndicatorRow(s.volatility, u.volatility)
                Row(Modifier.fillMaxWidth()) {
                    Text(s.breakoutChance, color = TradeMuted, modifier = Modifier.weight(1f))
                    Text("${u.breakoutChance}%", fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = u.breakoutChance / 100f,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = TradeAmber,
                    trackColor = Color(0xFF26324A)
                )
                val dailyText = when (u.dailyTrend) {
                    Trend.UPTREND -> "আপ"
                    Trend.DOWNTREND -> "ডাউন"
                    Trend.SIDEWAYS -> "সাইডওয়েজ"
                }
                IndicatorRow(s.dailyTrend, dailyText)
                Text(
                    s.confluence + ": " + if (u.confluence) {
                        "হ্যাঁ — ১ ঘণ্টা আর ১ দিনের ট্রেন্ড একই দিকে, সিগন্যাল শক্তিশালী।"
                    } else {
                        "না — টাইমফ্রেমগুলো একমত নয়, সাবধানে থাকুন।"
                    }
                )
                Text(
                    s.divergence + ": " + u.divergence,
                    color = if (u.divergenceBearish) TradeRed else TradeMuted
                )
            }
        }

        // Score
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
                        Text(s.scoreHint, color = TradeMuted, fontSize = 12.sp)
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
                ScoreRow("সাপোর্টের কাছে", analysis.score.supportPoints, 20)
                ScoreRow("নিউজ সেন্টিমেন্ট", analysis.score.newsPoints, 15)
                ScoreRow("ভলিউম (${fmt(a.volumeChangePct, 0)}%)", analysis.score.volumePoints, 10)
                ScoreRow("MACD", analysis.score.macdPoints, 10)
                ScoreRow("EMA 20/50", analysis.score.emaPoints, 10)
            }
        }

        // এখন কী অবস্থা
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.situationTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val trendText = when (a.trend) {
                    Trend.UPTREND -> "ট্রেন্ড আপ — দাম উপরে যাওয়ার পথে আছে।"
                    Trend.DOWNTREND -> "ট্রেন্ড ডাউন — দাম নিচে যাওয়ার পথে আছে।"
                    Trend.SIDEWAYS -> "ট্রেন্ড সাইডওয়েজ — দাম একটি রেঞ্জের ভিতরে ঘুরছে।"
                }
                Text(trendText)
                Text("প্রাইস অ্যাকশন: ${a.pattern}")
                Text(
                    "RSI ${fmt(a.rsi, 1)} — " +
                        if (a.rsi < 30) "ওভারসোল্ড, বাউন্স করতে পারে।"
                        else if (a.rsi > 70) "ওভারবট, নিচে আসার চান্স।"
                        else "নিউট্রাল জোন।"
                )
                Text(
                    "সাপোর্ট $${fmtPrice(a.support)} আর রেজিস্ট্যান্স $${fmtPrice(a.resistance)}-এর মাঝখানে দাম $${fmtPrice(analysis.price)}-এ আছে।"
                )
                Text("টার্গেট: $${fmtPrice(analysis.target)}  •  স্টপ লস: $${fmtPrice(analysis.stopLoss)}")
            }
        }

        // সামনে কী হতে পারে
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.nextTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                analysis.scenarios.forEach { sc ->
                    Column {
                        Row(Modifier.fillMaxWidth()) {
                            Text(sc.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${sc.probability}%", fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = sc.probability / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = when (sc.name) {
                                "Bullish" -> TradeGreen
                                "Bearish" -> TradeRed
                                else -> TradeAmber
                            },
                            trackColor = Color(0xFF26324A)
                        )
                        Text(sc.description + " টার্গেট: $" + fmtPrice(sc.target), color = TradeMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        // কী কারণে হতে পারে
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.whyTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                analysis.forecast.reasons.forEach { r ->
                    Text("•  $r")
                }
            }
        }

        // কী করা উচিত
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12233D))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.actionTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TradeGreen)
                Text(analysis.forecast.action)
                Text(
                    "টার্গেট: $${fmtPrice(analysis.target)}  •  স্টপ লস: $${fmtPrice(analysis.stopLoss)}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "টাকা লাগানোর সিদ্ধান্ত আপনার নিজের — অ্যাপ শুধু সাহায্য করবে।",
                    color = TradeMuted, fontSize = 12.sp
                )
            }
        }

        // Indicators
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.indicatorsTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                IndicatorRow("RSI (14)", fmt(a.rsi, 1) + if (a.rsi < 30) " — ওভারসোল্ড" else if (a.rsi > 70) " — ওভারবট" else " — নিউট্রাল")
                IndicatorRow("MACD", fmt(a.macd, 2) + " / সিগন্যাল " + fmt(a.macdSignal, 2))
                IndicatorRow("EMA 20 / 50", fmtPrice(a.ema20) + " / " + fmtPrice(a.ema50))
                IndicatorRow("সাপোর্ট / রেজিস্ট্যান্স", fmtPrice(a.support) + " / " + fmtPrice(a.resistance))
                IndicatorRow("প্রাইস অ্যাকশন", a.pattern)
                IndicatorRow("ATR", fmtPrice(a.atr))
                IndicatorRow("টার্গেট / স্টপ লস", fmtPrice(analysis.target) + " / " + fmtPrice(analysis.stopLoss))
            }
        }

        // News
        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.latestNews, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (analysis.news.isEmpty()) {
                    Text(s.noNews, color = TradeMuted)
                }
                analysis.news.forEach { n -> NewsRow(n, s) }
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
private fun NewsRow(n: NewsItem, s: UiText) {
    val color = when (n.sentiment) {
        Sentiment.POSITIVE -> TradeGreen
        Sentiment.NEGATIVE -> TradeRed
        Sentiment.NEUTRAL -> TradeMuted
    }
    val tag = when (n.sentiment) {
        Sentiment.POSITIVE -> s.tagPositive
        Sentiment.NEGATIVE -> s.tagNegative
        Sentiment.NEUTRAL -> s.tagNeutral
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B1220)).padding(10.dp)
    ) {
        Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("$tag  •  ${n.source}", color = color, fontSize = 12.sp)
    }
}
