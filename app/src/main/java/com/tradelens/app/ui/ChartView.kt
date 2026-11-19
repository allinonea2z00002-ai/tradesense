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
import com.tradelens.app.ui.theme.TradeAmber
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
    forecast: List<Candle> = emptyList(),
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        if (candles.isEmpty()) return@Canvas
        val shown = candles.takeLast(55)
        val all = shown + forecast
        val emaShown = if (ema20.size >= shown.size) {
            ema20.takeLast(shown.size)
        } else {
            emptyList()
        }
        var lo = all.minOf { it.low }
        var hi = all.maxOf { it.high }
        if (support > 0) lo = min(lo, support)
        if (resistance > 0) hi = max(hi, resistance)
        if (hi - lo < 1e-9) hi = lo + 1.0
        val pad = (hi - lo) * 0.06
        lo -= pad
        hi += pad
        fun y(price: Double): Float =
            (size.height - ((price - lo) / (hi - lo) * size.height)).toFloat()
        fun x(i: Int): Float = size.width * (i + 0.5f) / all.size

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
        // Forecast target line (last projected close)
        if (forecast.isNotEmpty()) {
            drawLine(
                color = TradeAmber, start = Offset(0f, y(forecast.last().close)),
                end = Offset(size.width, y(forecast.last().close)),
                strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }
        // Divider between real and forecast candles
        if (forecast.isNotEmpty()) {
            val dx = size.width * shown.size / all.size
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(dx, 0f), end = Offset(dx, size.height),
                strokeWidth = 2f, pathEffect = dash
            )
        }
        // Candles: real solid, forecast dashed + translucent
        val bodyW = size.width / all.size * 0.6f
        all.forEachIndexed { i, c ->
            val isForecast = i >= shown.size
            val up = c.close >= c.open
            val base = if (up) TradeGreen else TradeRed
            val color = if (isForecast) base.copy(alpha = 0.45f) else base
            val cx = x(i)
            val wickEffect = if (isForecast) dash else null
            drawLine(color, Offset(cx, y(c.high)), Offset(cx, y(c.low)), strokeWidth = 2f, pathEffect = wickEffect)
            val top = y(max(c.open, c.close))
            val bottom = y(min(c.open, c.close))
            if (isForecast) {
                drawRect(
                    color = color,
                    topLeft = Offset(cx - bodyW / 2, top),
                    size = androidx.compose.ui.geometry.Size(bodyW, max(2f, bottom - top)),
                    style = Stroke(width = 2f, pathEffect = dash)
                )
            } else {
                drawRect(
                    color = color,
                    topLeft = Offset(cx - bodyW / 2, top),
                    size = androidx.compose.ui.geometry.Size(bodyW, max(2f, bottom - top))
                )
            }
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
