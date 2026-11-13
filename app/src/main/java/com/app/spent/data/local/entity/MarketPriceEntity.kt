package com.app.spent.data.local.entity

import androidx.room.Entity

@Entity(tableName = "market_prices", primaryKeys = ["symbol", "baseCurrency"])
data class MarketPriceEntity(
    val symbol: String,                        // e.g. "AAPL", "BTC", "EUR"
    val baseCurrency: String = "USD",          // Currency the price is normalized to
    val priceInBaseCurrency: Double,            // Market price normalized to base currency
    val change24hPct: Double = 0.0,             // 24h market fluctuation percentage
    val high24h: Double? = null,
    val low24h: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
