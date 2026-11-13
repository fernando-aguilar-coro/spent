package com.app.spent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.MarketPriceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    // Asset Holdings
    @Query("SELECT * FROM asset_holdings ORDER BY createdAt DESC")
    fun getHoldingsFlow(): Flow<List<AssetHoldingEntity>>

    @Query("SELECT * FROM asset_holdings WHERE id = :id")
    suspend fun getHoldingById(id: String): AssetHoldingEntity?

    @Query("SELECT DISTINCT symbol FROM asset_holdings")
    suspend fun getAllSymbols(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHolding(holding: AssetHoldingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<AssetHoldingEntity>)

    @Update
    suspend fun updateHolding(holding: AssetHoldingEntity)

    @Query("DELETE FROM asset_holdings WHERE id = :id")
    suspend fun deleteHoldingById(id: String)

    @Query("DELETE FROM asset_holdings")
    suspend fun deleteAllHoldings()

    // Market Prices Cache
    @Query("SELECT * FROM market_prices")
    fun getMarketPricesFlow(): Flow<List<MarketPriceEntity>>

    @Query("SELECT * FROM market_prices WHERE symbol = :symbol")
    suspend fun getMarketPrice(symbol: String): MarketPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrices(prices: List<MarketPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrice(price: MarketPriceEntity)

    @Query("DELETE FROM market_prices WHERE symbol = :symbol")
    suspend fun deletePrice(symbol: String)

    @Query("DELETE FROM market_prices")
    suspend fun deleteAllPrices()
}

