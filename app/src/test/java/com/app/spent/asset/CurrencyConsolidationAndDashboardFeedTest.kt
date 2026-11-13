package com.app.spent.asset

import com.app.spent.data.local.entity.AssetHoldingEntity
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.domain.asset.model.ValuedAsset
import com.app.spent.ui.dashboard.DashboardActivityItem
import com.app.spent.util.CurrencyFormatter
import com.app.spent.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConsolidationAndDashboardFeedTest {

    @Test
    fun testCurrencyFormatterPrefixAndSuffix() {
        // Prefix with space (Bs., ARS)
        assertEquals("Bs. 150.00", CurrencyFormatter.format(150.0, "Bs.", "BOB"))
        assertEquals("ARS 2,500.00", CurrencyFormatter.format(2500.0, "ARS", "ARS"))

        // Standard symbol prefix without space ($, €, £)
        assertEquals("$12.50", CurrencyFormatter.format(12.50, "$", "USD"))
        assertEquals("€99.00", CurrencyFormatter.format(99.0, "€", "EUR"))
        assertEquals("£45.20", CurrencyFormatter.format(45.20, "£", "GBP"))

        // Suffix currency (kr)
        val krFormatted = CurrencyFormatter.format(150.0, "kr", "SEK")
        assertTrue("Expected suffix kr but got: $krFormatted", krFormatted.endsWith("kr") || krFormatted.contains("kr"))
    }

    @Test
    fun testInferCurrencyCodeFromSymbol() {
        assertEquals("BOB", LocaleHelper.inferCurrencyCode("Bs."))
        assertEquals("ARS", LocaleHelper.inferCurrencyCode("ARS"))
        assertEquals("USD", LocaleHelper.inferCurrencyCode("$"))
        assertEquals("EUR", LocaleHelper.inferCurrencyCode("€"))
        assertEquals("BRL", LocaleHelper.inferCurrencyCode("R$"))
        assertEquals("PEN", LocaleHelper.inferCurrencyCode("S/"))
        assertEquals("GBP", LocaleHelper.inferCurrencyCode("£"))
        assertEquals("JPY", LocaleHelper.inferCurrencyCode("¥"))
    }

    @Test
    fun testDashboardActivityItemOrdering() {
        val now = 1000000L

        val tx1 = TransactionEntity(
            id = "tx_1",
            amount = 50.0,
            type = "EXPENSE",
            categoryId = "cat_food",
            note = "Groceries",
            timestamp = now + 100
        )

        val tx2 = TransactionEntity(
            id = "tx_2",
            amount = 20.0,
            type = "EXPENSE",
            categoryId = "cat_transport",
            note = "Bus",
            timestamp = now + 50
        )

        val asset1 = ValuedAsset(
            holding = AssetHoldingEntity(
                id = "asset_eur",
                symbol = "EUR",
                name = "Euro Cash",
                type = "CURRENCY",
                quantity = 100.0,
                createdAt = now + 200
            ),
            effectiveUnitPrice = 1.08,
            totalValuation = 108.0,
            change24hPct = 0.5
        )

        val items: List<DashboardActivityItem> = listOf(
            DashboardActivityItem.TransactionRecord(tx1),
            DashboardActivityItem.TransactionRecord(tx2),
            DashboardActivityItem.AssetHoldingRecord(asset1)
        ).sortedByDescending { it.timestamp }

        // Most recent should be asset1 (now + 200), then tx1 (now + 100), then tx2 (now + 50)
        assertEquals("asset_asset_eur", items[0].id)
        assertTrue(items[0] is DashboardActivityItem.AssetHoldingRecord)

        assertEquals("tx_1", items[1].id)
        assertTrue(items[1] is DashboardActivityItem.TransactionRecord)

        assertEquals("tx_2", items[2].id)
        assertTrue(items[2] is DashboardActivityItem.TransactionRecord)
    }
}
