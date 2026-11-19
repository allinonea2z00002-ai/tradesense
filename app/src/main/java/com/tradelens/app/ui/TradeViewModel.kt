package com.tradelens.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradelens.app.data.Asset
import com.tradelens.app.data.Assets
import com.tradelens.app.data.Category
import com.tradelens.app.data.MarketAnalysis
import com.tradelens.app.data.MarketRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TradeUiState(
    val assets: List<Asset> = Assets.all,
    val category: Category? = null,
    val selected: Asset = Assets.all.first(),
    val timeframe: String = "1h",
    val analysis: MarketAnalysis? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class TradeViewModel : ViewModel() {

    private val _state = MutableStateFlow(TradeUiState())
    val state: StateFlow<TradeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null

    val timeframes = listOf("1m", "5m", "15m", "1h", "4h", "1d")

    init {
        load()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                load(silent = true)
            }
        }
    }

    fun selectCategory(category: Category?) {
        _state.value = _state.value.copy(category = category)
    }

    fun selectAsset(asset: Asset) {
        if (asset.id == _state.value.selected.id && _state.value.analysis != null) return
        _state.value = _state.value.copy(selected = asset, analysis = null)
        load()
    }

    fun setTimeframe(timeframe: String) {
        if (timeframe == _state.value.timeframe) return
        _state.value = _state.value.copy(timeframe = timeframe)
        load()
    }

    fun refresh() = load()

    private fun load(silent: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _state.value
            if (!silent) _state.value = current.copy(loading = true, error = null)
            try {
                val result = MarketRepository.getAnalysis(current.selected, current.timeframe)
                _state.value = _state.value.copy(analysis = result, loading = false, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Load korte parini: ${e.message ?: "network error"}"
                )
            }
        }
    }
}
