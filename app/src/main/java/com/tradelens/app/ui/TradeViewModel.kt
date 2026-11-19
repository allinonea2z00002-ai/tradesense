package com.tradelens.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradelens.app.data.Asset
import com.tradelens.app.data.Assets
import com.tradelens.app.data.Category
import com.tradelens.app.data.MarketAnalysis
import com.tradelens.app.data.MarketApi
import com.tradelens.app.data.MarketRepository
import com.tradelens.app.data.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TradeUiState(
    val assets: List<Asset> = Assets.all,
    val category: Category? = null,
    val selected: Asset = Assets.all.first(),
    val detailAsset: Asset? = null,
    val tab: String = "home",
    val lang: String = "bn",
    val strings: UiText = StringsBn,
    val quotes: Map<String, Pair<Double, Double>> = emptyMap(),
    val watchlist: Set<String> = emptySet(),
    val allNews: List<NewsItem> = emptyList(),
    val timeframe: String = "1h",
    val analysis: MarketAnalysis? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class TradeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("tradelens", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        TradeUiState(
            lang = prefs.getString("lang", "bn") ?: "bn",
            watchlist = prefs.getStringSet("watchlist", emptySet())?.toSet() ?: emptySet()
        ).let { it.copy(strings = stringsFor(it.lang)) }
    )
    val state: StateFlow<TradeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var refreshJob: Job? = null

    val timeframes = listOf("1m", "5m", "15m", "1h", "4h", "1d")

    init {
        load()
        loadQuotes()
        loadAllNews()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                load(silent = true)
                loadQuotes()
            }
        }
    }

    /** Sob asset-er live price + 24h change, list-e dekhanor jonno. */
    private fun loadQuotes() {
        viewModelScope.launch {
            val deferred = Assets.all.map { asset ->
                async(Dispatchers.IO) { asset.id to MarketApi.fetchTicker(asset) }
            }
            val map = deferred.awaitAll().toMap()
            _state.value = _state.value.copy(quotes = map)
        }
    }

    /** Sob coin-er news ek jaygay — News tab-er jonno. */
    fun loadAllNews() {
        viewModelScope.launch {
            val news = withContext(Dispatchers.IO) { MarketApi.fetchNews(Assets.all.first()) }
            _state.value = _state.value.copy(allNews = news)
        }
    }

    fun selectTab(tab: String) {
        _state.value = _state.value.copy(tab = tab)
        if (tab == "news") loadAllNews()
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("lang", lang).apply()
        _state.value = _state.value.copy(lang = lang, strings = stringsFor(lang))
    }

    fun toggleWatchlist(assetId: String) {
        val current = _state.value.watchlist.toMutableSet()
        if (current.contains(assetId)) current.remove(assetId) else current.add(assetId)
        prefs.edit().putStringSet("watchlist", current).apply()
        _state.value = _state.value.copy(watchlist = current)
    }

    /** Coin-e tap korle tar pura detail window khulbe. */
    fun openDetail(asset: Asset) {
        _state.value = _state.value.copy(detailAsset = asset, selected = asset, analysis = null)
        load()
    }

    fun closeDetail() {
        _state.value = _state.value.copy(detailAsset = null)
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
                    error = "${_state.value.strings.loadFailed}: ${e.message ?: "network error"}"
                )
            }
        }
    }
}
