# TradeLens — MarketSense

TradeLens sudhu chart dekhabe na — sob kichu miliye 100-er moddhe ekta score diye
bolbe market-er por ki hote pare. Kono app-i 100% sothik predict korte pare na —
eta sudhu decision nite help korbe, taka laganor decision tomar nijer.

## Naam
**TradeLens** (age MarketSense naam-eo daka hoyechilo)

## Ki ki ache
1. **Live Price + Chart** — BTC, ETH, Gold, Forex, Stock. Live candlestick chart,
   timeframe: 1m, 5m, 15m, 1h, 4h, 1D.
2. **Online News** — CoinDesk RSS theke oi asset-er latest news, sentiment
   green (positive) / red (negative) / grey (neutral).
3. **Price Action** — Trend up/down, pin bar, engulfing, doji auto detect.
4. **Indicator** — RSI (14), MACD (12,26,9), EMA 20/50, Volume, ATR.
5. **Support / Resistance** — Chart-e auto line, support-er kache bounce,
   resistance vanghle upore jaoar chance.
6. **Logic / Score (main)** — 100-er moddhe score:
   Trend +20, RSI +15, Support +20, News +15, Volume +10, MACD +10, EMA +10.
   70+ = Strong Buy, 40–70 = Wait, 40-er niche = Avoid / Sell.
7. **Er por ki hote pare** — 3 scenario probability diye:
   Bullish / Sideways / Bearish, sathe Target ar Stop Loss (ATR diye).

## Data source (kono API key lage na)
- Crypto: Binance public API — `/api/v3/klines` ar `/api/v3/ticker/24hr`
- Forex / Gold / Stock: Yahoo Finance chart API (`query1.finance.yahoo.com`)
- News: CoinDesk RSS
- Internet na thakle app offline demo data dekhabe, "Offline demo" likhe.

## Build (GitHub Actions)
Workflow file-ti Muse push korte pare na (GitHub App workflow likhte pare na),
tai `.github/workflows/android-build.yml` nije GitHub web-e banao — file-ta
niche `WORKFLOW.md` file-e deoa ache. Tarpor Actions → Build APK → artifact
`tradelens-apk` download koro.

## Project
- Package: `com.tradelens.app`
- Kotlin + Jetpack Compose + Material 3
- minSdk 26, targetSdk 34, compileSdk 34
- Internet permission lage (live price/news-er jonno)

## Disclaimer
Eta financial advice na. Score ar scenario sudhu ekta hisab, guarantee na.
News hothat kharap ashle market ulta jete pare.
