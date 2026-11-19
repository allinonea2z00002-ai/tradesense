package com.tradelens.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradelens.app.analysis.Indicators
import com.tradelens.app.analysis.PhotoAnalysis
import com.tradelens.app.analysis.PhotoAnalyzer
import com.tradelens.app.analysis.ScreenAnalyzer
import com.tradelens.app.ui.theme.TradeAmber
import com.tradelens.app.ui.theme.TradeBg
import com.tradelens.app.ui.theme.TradeCard
import com.tradelens.app.ui.theme.TradeGreen
import com.tradelens.app.ui.theme.TradeMuted
import com.tradelens.app.ui.theme.TradeRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** পরের ক্যান্ডেলের প্রিভিউ — একটা ক্যান্ডেল এঁকে দেখায়, তার সাইজ সহ। */
@Composable
fun NextCandlePreview(
    bodyRatio: Float,
    wickRatio: Float,
    up: Boolean,
    directionText: String,
    sizeText: String,
    s: UiText,
    modifier: Modifier = Modifier
) {
    val color = if (up) TradeGreen else TradeRed
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.nextCandle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Canvas(Modifier.size(90.dp, 150.dp)) {
            val totalH = size.height * wickRatio.coerceIn(0.1f, 0.95f)
            val bodyH = size.height * bodyRatio.coerceIn(0.05f, 0.8f)
            val cx = size.width / 2f
            val top = (size.height - totalH) / 2f
            val bodyTop = top + (totalH - bodyH) / 2f
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(cx, top),
                end = androidx.compose.ui.geometry.Offset(cx, top + totalH),
                strokeWidth = 3f
            )
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(cx - 16f, bodyTop),
                size = androidx.compose.ui.geometry.Size(32f, max(4f, bodyH))
            )
        }
        Text(directionText, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("${s.candleSize}: $sizeText", color = TradeMuted, fontSize = 12.sp)
    }
}

// ================= 1. ফটো অ্যানালিটিক্স =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAnalyticsScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<PhotoAnalysis?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        analyzing = true
        scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } catch (e: Exception) {
                    null
                }
            }
            bitmap = bmp
            result = bmp?.let { withContext(Dispatchers.Default) { PhotoAnalyzer.analyze(it) } }
            analyzing = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.photoAnalytics, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { vm.closePhotoAnalytics() }) {
                        Text(s.back, color = TradeGreen, fontWeight = FontWeight.Bold)
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
                Spacer(Modifier.height(4.dp))
                if (bitmap == null) {
                    Text(s.pickPhotoHint, color = TradeMuted)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { launcher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = TradeGreen)
                    ) {
                        Text(s.pickPhoto, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.Black)
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.weight(1.6f).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                        Spacer(Modifier.width(12.dp))
                        result?.let { r ->
                            NextCandlePreview(
                                bodyRatio = (r.avgBodyPct / 12f).coerceIn(0.15f, 0.75f),
                                wickRatio = (r.avgBodyPct / 12f * 1.6f).coerceIn(0.2f, 0.9f),
                                up = r.direction == "UP",
                                directionText = r.directionText,
                                sizeText = "${"%.1f".format(r.avgBodyPct)}%",
                                s = s,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (analyzing) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TradeGreen)
                        }
                    }
                    result?.let { r ->
                        Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(s.analysisResult, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                val dirColor = when (r.direction) {
                                    "UP" -> TradeGreen
                                    "DOWN" -> TradeRed
                                    else -> TradeAmber
                                }
                                Text(r.directionText, fontWeight = FontWeight.Bold, color = dirColor, fontSize = 16.sp)
                                Text("সবুজ ক্যান্ডেল: ${r.greenPct}%  •  লাল ক্যান্ডেল: ${r.redPct}%")
                                Text("${s.confidence}: ${r.confidence}%")
                                Text("${s.candleSize}: ${"%.1f".format(r.avgBodyPct)}% — ছবির ক্যান্ডেলগুলোর গড় বডির তুলনায় পরের ক্যান্ডেল এমন বড় হতে পারে।")
                                Text(
                                    "ছবি দেখে ধারণা দেওয়া হয়েছে — আসল সিদ্ধান্ত নেওয়ার আগে লাইভ চার্টও দেখে নিন।",
                                    color = TradeMuted, fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { launcher.launch("image/*") }) {
                        Text(s.pickPhoto, color = TradeGreen, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ================= 2. স্ক্রিন ক্যান্ডেল অ্যানালিটিক্স =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenAnalyticsScreen(state: TradeUiState, vm: TradeViewModel) {
    val s = state.strings
    val candles = state.screenCandles
    val result = remember(candles, state.timeframe) {
        if (candles.isNotEmpty()) ScreenAnalyzer.analyze(candles, state.timeframe) else null
    }
    val visAnalysis = remember(candles) {
        if (candles.isNotEmpty()) Indicators.analyse(candles) else null
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.screenAnalytics, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = { vm.closeScreenAnalytics() }) {
                        Text(s.back, color = TradeGreen, fontWeight = FontWeight.Bold)
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
                Spacer(Modifier.height(4.dp))
                Text(
                    "${s.visibleCandles}: ${candles.size} (${state.timeframe})",
                    color = TradeMuted, fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                if (result != null && visAnalysis != null) {
                    val range = max(
                        candles.maxOf { it.high } - candles.minOf { it.low },
                        result.nextCandle?.let { it.high - it.low } ?: 0.0
                    )
                    val next = result.nextCandle
                    val bodyRatio = if (next != null && range > 0) {
                        (abs(next.close - next.open) / range).toFloat()
                    } else {
                        0.3f
                    }
                    val wickRatio = if (next != null && range > 0) {
                        ((next.high - next.low) / range).toFloat()
                    } else {
                        0.5f
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1.6f)) {
                            CandleChart(
                                candles = candles,
                                ema20 = Indicators.ema(candles.map { it.close }, 20),
                                support = visAnalysis.support,
                                resistance = visAnalysis.resistance,
                                forecast = listOfNotNull(next),
                                visibleCount = candles.size,
                                startIndex = 0
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (next != null) {
                            NextCandlePreview(
                                bodyRatio = bodyRatio,
                                wickRatio = wickRatio,
                                up = next.close >= next.open,
                                directionText = if (next.close >= next.open) "উপরের দিকে যেতে পারে" else "নিচের দিকে যেতে পারে",
                                sizeText = "$" + fmtPrice(next.close),
                                s = s,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = TradeCard)) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(s.analysisResult, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(result.verdictText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TradeGreen)
                            Text("${s.pattern}: ${result.patternText}")
                            Text(
                                "সবুজ: ${result.greenCount}  •  লাল: ${result.redCount}  •  " +
                                    "এই স্ক্রিনে পরিবর্তন: " +
                                    (if (result.windowChangePct >= 0) "+" else "") +
                                    fmt(result.windowChangePct, 2) + "%"
                            )
                            Text("${s.confidence}: ${result.confidence}%")
                            next?.let {
                                Text(
                                    "${s.nextCandle}: $${fmtPrice(it.close)} — " +
                                        "সাইজ ${fmt(abs(it.close - it.open), 2)}",
                                    color = TradeMuted, fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    Text(s.noNews, color = TradeMuted)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
