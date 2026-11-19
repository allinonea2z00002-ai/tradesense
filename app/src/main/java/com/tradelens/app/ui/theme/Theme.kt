package com.tradelens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TradeGreen = Color(0xFF00E676)
val TradeRed = Color(0xFFFF5252)
val TradeBlue = Color(0xFF38BDF8)
val TradeAmber = Color(0xFFFFB020)
val TradeBg = Color(0xFF0B1220)
val TradeCard = Color(0xFF131E33)
val TradeMuted = Color(0xFF94A3B8)

private val TradeLensColors = darkColorScheme(
    primary = TradeGreen,
    secondary = TradeBlue,
    background = TradeBg,
    surface = TradeCard,
    onBackground = Color.White,
    onSurface = Color.White,
    error = TradeRed
)

@Composable
fun TradeLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TradeLensColors, content = content)
}
