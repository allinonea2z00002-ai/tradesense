package com.tradelens.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tradelens.app.data.Candle
import com.tradelens.app.ui.theme.TradeBlue
import com.tradelens.app.ui.theme.TradeGreen
import com.tradelens.app.ui.theme.TradeRed
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandleChart(
    candles: List<Candle>,
    ema20: List<Double>,
    support: Double,
    resistance: Double,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        if (candles.isEmpty()) return@Canvas
        val shown = candles.takeLast(60)
        val emaShown = if (ema20.size >= shown.size) {
            ema20.takeLast(shown.size)
        } else {
            emptyList()
        }
        var lo = shown.minOf { it.low }
        var hi = shown.maxOf { it.high }
        if (support > 0) lo = min(lo, support)
        if (resistance > 0) hi = max(hi, resistance)
        if (hi - lo < 1e-9) hi = lo + 1.0
        val pad = (hi - lo) * 0.06
        lo -= pad
        hi += pad
        fun y(price: Double): Float =
            (size.height - ((price - lo) / (hi - lo) * size.height)).toFloat()
        fun x(i: Int): Float = size.width * (i + 0.5f) / shown.size

        // Support / resistance dashed lines
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        if (support > 0) {
            drawLine(
                color = TradeGreen, start = Offset(0f, y(support)),
                end = Offset(size.width, y(support)), strokeWidth = 2f, pathEffect = dash
            )
        }
        if (resistance > 0) {
            drawLine(
                color = TradeRed, start = Offset(0f, y(resistance)),
                end = Offset(size.width, y(resistance)), strokeWidth = 2f, pathEffect = dash
            )
        }
        // Candles
        val bodyW = size.width / shown.size * 0.6f
        shown.forEachIndexed { i, c ->
            val up = c.close >= c.open
            val color = if (up) TradeGreen else TradeRed
            val cx = x(i)
            drawLine(color, Offset(cx, y(c.high)), Offset(cx, y(c.low)), strokeWidth = 2f)
            val top = y(max(c.open, c.close))
            val bottom = y(min(c.open, c.close))
            drawRect(
                color = color,
                topLeft = Offset(cx - bodyW / 2, top),
                size = androidx.compose.ui.geometry.Size(bodyW, max(2f, bottom - top))
            )
        }
        // EMA20 line
        if (emaShown.size == shown.size) {
            val path = androidx.compose.ui.graphics.Path()
            emaShown.forEachIndexed { i, v ->
                if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
            }
            drawPath(path, color = TradeBlue, style = Stroke(width = 3f))
        }
    }
}
