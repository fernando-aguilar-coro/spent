package com.app.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_prices")
data class MarketPriceEntity(
    @PrimaryKey val symbol: String,             // e.g. "AAPL", "BTC", "EUR"
    val priceInBaseCurrency: Double,            // Market price normalized to base currency
    val change24hPct: Double = 0.0,             // 24h market fluctuation percentage
    val high24h: Double? = null,
    val low24h: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
