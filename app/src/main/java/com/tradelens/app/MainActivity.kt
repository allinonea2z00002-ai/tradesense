package com.tradelens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradelens.app.ui.DetailScreen
import com.tradelens.app.ui.HomeScreen
import com.tradelens.app.ui.NewsScreen
import com.tradelens.app.ui.SettingsScreen
import com.tradelens.app.ui.TopDropScreen
import com.tradelens.app.ui.TradeViewModel
import com.tradelens.app.ui.WatchlistScreen
import com.tradelens.app.ui.theme.TradeBg
import com.tradelens.app.ui.theme.TradeCard
import com.tradelens.app.ui.theme.TradeGreen
import com.tradelens.app.ui.theme.TradeLensTheme
import com.tradelens.app.ui.theme.TradeMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradeLensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vm: TradeViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    val s = state.strings
                    val detail = state.detailAsset
                    if (detail != null) {
                        BackHandler { vm.closeDetail() }
                        DetailScreen(state = state, vm = vm)
                    } else {
                        Scaffold(
                            bottomBar = {
                                NavigationBar(containerColor = TradeCard) {
                                    val tabs = listOf(
                                        Triple("home", s.navHome, "⌂"),
                                        Triple("top", s.navTopDrop, "↑↓"),
                                        Triple("news", s.navNews, "≡"),
                                        Triple("watchlist", s.navWatchlist, "☆"),
                                        Triple("settings", s.navSettings, "⚙")
                                    )
                                    tabs.forEach { (id, label, icon) ->
                                        NavigationBarItem(
                                            selected = state.tab == id,
                                            onClick = { vm.selectTab(id) },
                                            icon = { Text(icon, fontSize = 20.sp) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = TradeGreen,
                                                selectedTextColor = TradeGreen,
                                                unselectedIconColor = TradeMuted,
                                                unselectedTextColor = TradeMuted,
                                                indicatorColor = Color(0xFF1B2B4A)
                                            )
                                        )
                                    }
                                }
                            },
                            containerColor = TradeBg
                        ) { padding ->
                            androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxSize().padding(padding)
                            ) {
                                when (state.tab) {
                                    "top" -> TopDropScreen(state = state, vm = vm)
                                    "news" -> NewsScreen(state = state, vm = vm)
                                    "watchlist" -> WatchlistScreen(state = state, vm = vm)
                                    "settings" -> SettingsScreen(state = state, vm = vm)
                                    else -> HomeScreen(state = state, vm = vm)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
