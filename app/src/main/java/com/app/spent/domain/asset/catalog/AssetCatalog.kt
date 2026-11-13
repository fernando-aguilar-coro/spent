package com.app.spent.domain.asset.catalog

import com.app.spent.domain.asset.model.AssetType

data class CatalogAsset(
    val symbol: String,
    val name: String,
    val type: AssetType,
    val description: String? = null
)

/**
 * Verified Asset Catalog providing instant search across Currencies, Cryptos, and Stocks/ETFs
 * powered by live market feeds (ExchangeRate-API, CoinCap, Yahoo Finance).
 */
object AssetCatalog {

    val ASSETS: List<CatalogAsset> = listOf(
        // === Fiat Currencies (ExchangeRate-API) ===
        CatalogAsset("BOB", "Bolivian Boliviano", AssetType.FIAT, "Bolivia • Bs."),
        CatalogAsset("ARS", "Argentine Peso", AssetType.FIAT, "Argentina • $ / ARS"),
        CatalogAsset("USD", "US Dollar", AssetType.FIAT, "United States • $"),
        CatalogAsset("EUR", "Euro", AssetType.FIAT, "European Union • €"),
        CatalogAsset("BRL", "Brazilian Real", AssetType.FIAT, "Brazil • R$"),
        CatalogAsset("PEN", "Peruvian Sol", AssetType.FIAT, "Peru • S/"),
        CatalogAsset("COP", "Colombian Peso", AssetType.FIAT, "Colombia • $"),
        CatalogAsset("CLP", "Chilean Peso", AssetType.FIAT, "Chile • $"),
        CatalogAsset("UYU", "Uruguayan Peso", AssetType.FIAT, "Uruguay • $"),
        CatalogAsset("MXN", "Mexican Peso", AssetType.FIAT, "Mexico • $"),
        CatalogAsset("GBP", "British Pound", AssetType.FIAT, "United Kingdom • £"),
        CatalogAsset("CAD", "Canadian Dollar", AssetType.FIAT, "Canada • CA$"),
        CatalogAsset("AUD", "Australian Dollar", AssetType.FIAT, "Australia • A$"),
        CatalogAsset("JPY", "Japanese Yen", AssetType.FIAT, "Japan • ¥"),
        CatalogAsset("CHF", "Swiss Franc", AssetType.FIAT, "Switzerland • CHF"),
        CatalogAsset("SEK", "Swedish Krona", AssetType.FIAT, "Sweden • kr"),
        CatalogAsset("NOK", "Norwegian Krone", AssetType.FIAT, "Norway • kr"),
        CatalogAsset("DKK", "Danish Krone", AssetType.FIAT, "Denmark • kr"),
        CatalogAsset("PLN", "Polish Zloty", AssetType.FIAT, "Poland • zł"),
        CatalogAsset("CNY", "Chinese Yuan", AssetType.FIAT, "China • ¥"),
        CatalogAsset("INR", "Indian Rupee", AssetType.FIAT, "India • ₹"),
        CatalogAsset("KRW", "South Korean Won", AssetType.FIAT, "South Korea • ₩"),
        CatalogAsset("VES", "Venezuelan Bolívar", AssetType.FIAT, "Venezuela • Bs."),
        CatalogAsset("PYG", "Paraguayan Guaraní", AssetType.FIAT, "Paraguay • ₲"),
        CatalogAsset("CRC", "Costa Rican Colón", AssetType.FIAT, "Costa Rica • ₡"),

        // === Cryptocurrencies (CoinCap / CoinGecko) ===
        CatalogAsset("BTC", "Bitcoin", AssetType.CRYPTO, "Digital Gold"),
        CatalogAsset("ETH", "Ethereum", AssetType.CRYPTO, "Smart Contracts"),
        CatalogAsset("SOL", "Solana", AssetType.CRYPTO, "High Performance L1"),
        CatalogAsset("BNB", "BNB", AssetType.CRYPTO, "Binance Ecosystem"),
        CatalogAsset("XRP", "XRP", AssetType.CRYPTO, "Ripple Payments"),
        CatalogAsset("ADA", "Cardano", AssetType.CRYPTO, "PoS Blockchain"),
        CatalogAsset("DOGE", "Dogecoin", AssetType.CRYPTO, "Meme Currency"),
        CatalogAsset("DOT", "Polkadot", AssetType.CRYPTO, "Interoperability"),
        CatalogAsset("AVAX", "Avalanche", AssetType.CRYPTO, "Subnet Platform"),
        CatalogAsset("LINK", "Chainlink", AssetType.CRYPTO, "Decentralized Oracle"),
        CatalogAsset("MATIC", "Polygon", AssetType.CRYPTO, "Ethereum Scaling"),
        CatalogAsset("LTC", "Litecoin", AssetType.CRYPTO, "Peer-to-Peer Cash"),
        CatalogAsset("TRX", "TRON", AssetType.CRYPTO, "Network Utility"),
        CatalogAsset("NEAR", "NEAR Protocol", AssetType.CRYPTO, "Sharded L1"),
        CatalogAsset("UNI", "Uniswap", AssetType.CRYPTO, "DEX Governance"),

        // === Stocks & ETFs (Yahoo Finance) ===
        CatalogAsset("AAPL", "Apple Inc.", AssetType.STOCK, "Consumer Tech"),
        CatalogAsset("MSFT", "Microsoft Corporation", AssetType.STOCK, "Cloud & Software"),
        CatalogAsset("GOOGL", "Alphabet Inc.", AssetType.STOCK, "Search & AI"),
        CatalogAsset("AMZN", "Amazon.com Inc.", AssetType.STOCK, "E-commerce & AWS"),
        CatalogAsset("TSLA", "Tesla Inc.", AssetType.STOCK, "EVs & Clean Energy"),
        CatalogAsset("NVDA", "NVIDIA Corporation", AssetType.STOCK, "AI & GPUs"),
        CatalogAsset("META", "Meta Platforms Inc.", AssetType.STOCK, "Social & VR"),
        CatalogAsset("VOO", "Vanguard S&P 500 ETF", AssetType.STOCK, "US Large Cap Index"),
        CatalogAsset("SPY", "SPDR S&P 500 ETF Trust", AssetType.STOCK, "S&P 500 Benchmark"),
        CatalogAsset("QQQ", "Invesco QQQ Trust", AssetType.STOCK, "Nasdaq-100 Tech ETF"),
        CatalogAsset("AMD", "Advanced Micro Devices", AssetType.STOCK, "Semiconductors"),
        CatalogAsset("NFLX", "Netflix Inc.", AssetType.STOCK, "Streaming Entertainment"),
        CatalogAsset("DIS", "The Walt Disney Company", AssetType.STOCK, "Media & Parks")
    )

    fun search(query: String): List<CatalogAsset> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return ASSETS
        return ASSETS.filter {
            it.symbol.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true)
        }
    }

    fun findBySymbol(symbol: String): CatalogAsset? {
        val sym = symbol.trim().uppercase()
        return ASSETS.find { it.symbol.equals(sym, ignoreCase = true) }
    }
}
