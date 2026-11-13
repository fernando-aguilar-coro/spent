package com.app.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "asset_holdings")
data class AssetHoldingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val symbol: String,                          // e.g. "AAPL", "BTC", "EUR"
    val name: String,                            // e.g. "Apple Inc.", "Bitcoin", "Euro"
    val type: String = "STOCK",                  // "STOCK", "CRYPTO", "FIAT", "COMMODITY", "CUSTOM"
    val quantity: Double,                        // Units held e.g. 10.5
    val averageBuyPrice: Double? = null,         // Cost basis per unit in base currency
    val currency: String = "USD",                // Quote currency
    val manualPriceOverride: Double? = null,     // Fallback manual price if market feed is disabled
    val isManualPrice: Boolean = false,          // True if using manual fixed price instead of market
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
