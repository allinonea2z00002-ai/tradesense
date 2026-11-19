package com.tradelens.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tradelens.app.analysis.Indicators
import com.tradelens.app.data.Asset
import com.tradelens.app.data.Assets
import com.tradelens.app.data.Candle
import com.tradelens.app.data.Category
import com.tradelens.app.data.MarketAnalysis
import com.tradelens.app.data.MarketApi
import com.tradelens.app.data.MarketRepository
import com.tradelens.app.data.NewsItem
import com.tradelens.app.data.Sentiment
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
    val demoMode: Boolean = false,
    val photoMode: Boolean = false,
    val screenMode: Boolean = false,
    val screenCandles: List<Candle> = emptyList(),
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
    private var demoJob: Job? = null

    val timeframes = listOf("1m", "5m", "15m", "1h", "4h", "1d")

    /** ডেমো চার্ট — সিমুলেটেড লাইভ ডেটা, প্রতি ৩ সেকেন্ডে নতুন টিক। */
    fun toggleDemo() {
        val on = !_state.value.demoMode
        _state.value = _state.value.copy(demoMode = on)
        if (on) startDemo() else {
            demoJob?.cancel()
            demoJob = null
            load()
        }
    }

    private fun startDemo() {
        demoJob?.cancel()
        loadJob?.cancel()
        demoJob = viewModelScope.launch {
            val asset = _state.value.selected
            val timeframe = _state.value.timeframe
            var candles = MarketApi.demoCandles(asset)
            val demoNews = listOf(
                NewsItem(
                    "ডেমো মোড — সিমুলেটেড ডেটা",
                    "TradeLens", "এখন", "",
                    Sentiment.NEUTRAL
                )
            )
            var tick = 0
            while (true) {
                val dailyTrend = _state.value.analysis?.ultra?.dailyTrend
                val built = MarketRepository.buildAnalysis(
                    asset, timeframe, candles, candles.last().close, 0.0, demoNews, true, dailyTrend
                )
                _state.value = _state.value.copy(analysis = built, loading = false, error = null)
                delay(3000)
                tick++
                val last = candles.last()
                val drift = (kotlin.random.Random.nextDouble() - 0.48) * last.close * 0.002
                val newClose = last.close + drift
                val updated = last.copy(
                    close = newClose,
                    high = maxOf(last.high, newClose),
                    low = minOf(last.low, newClose)
                )
                candles = if (tick % 5 == 0) {
                    (candles + updated.copy(time = last.time + Indicators.timeframeMillis(timeframe))).takeLast(100)
                } else {
                    candles.dropLast(1) + updated
                }
            }
        }
    }

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
    /** ফটো অ্যানালিটিক্স স্ক্রিন খোলা/বন্ধ। */
    fun openPhotoAnalytics() {
        _state.value = _state.value.copy(photoMode = true)
    }

    fun closePhotoAnalytics() {
        _state.value = _state.value.copy(photoMode = false)
    }

    /** স্ক্রিন ক্যান্ডেল অ্যানালিটিক্স — স্ক্রিনে যা ক্যান্ডেল দেখা যাচ্ছে তাই বিশ্লেষণ। */
    fun openScreenAnalytics(candles: List<Candle>) {
        _state.value = _state.value.copy(screenMode = true, screenCandles = candles)
    }

    fun closeScreenAnalytics() {
        _state.value = _state.value.copy(screenMode = false)
    }

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
        if (_state.value.demoMode) {
            startDemo()
            return
        }
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
