package com.app.spent.analytics

import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.analytics.ChartInterval
import com.app.spent.ui.analytics.components.ChartTimelineHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnalyticsScrollableTimelineTest {

    @Test
    fun testComputeTotalBalancePointsDaily() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }

        val tx1 = TransactionEntity(
            id = "tx-1",
            amount = 1000.0,
            type = "INCOME",
            categoryId = "cat_salary",
            note = "Salary",
            timestamp = now - (5L * 24 * 60 * 60 * 1000) // 5 days ago
        )

        val tx2 = TransactionEntity(
            id = "tx-2",
            amount = 250.0,
            type = "EXPENSE",
            categoryId = "cat_food",
            note = "Groceries",
            timestamp = now - (2L * 24 * 60 * 60 * 1000) // 2 days ago
        )

        val points = ChartTimelineHelper.computeTotalBalancePoints(
            transactions = listOf(tx1, tx2),
            interval = ChartInterval.DAY
        )

        // Must have at least 14 days
        assertTrue("Daily points should have at least 14 days", points.size >= 14)

        // Latest point (today) must have totalBalance = 1000 - 250 = 750
        val lastPoint = points.last()
        assertEquals(750.0, lastPoint.totalBalance, 0.001)

        // Point from 5 days ago should have delta = 1000
        val day5Point = points.find { it.startMs <= tx1.timestamp && tx1.timestamp <= it.endMs }
        assertNotNull(day5Point)
        assertEquals(1000.0, day5Point!!.delta, 0.001)

        // Point from 2 days ago should have delta = -250
        val day2Point = points.find { it.startMs <= tx2.timestamp && tx2.timestamp <= it.endMs }
        assertNotNull(day2Point)
        assertEquals(-250.0, day2Point!!.delta, 0.001)
    }

    @Test
    fun testComputeTotalBalancePointsWeekly() {
        val now = System.currentTimeMillis()

        val tx1 = TransactionEntity(
            id = "tx-1",
            amount = 500.0,
            type = "INCOME",
            categoryId = "cat_salary",
            note = "Deposit",
            timestamp = now - (14L * 24 * 60 * 60 * 1000) // 2 weeks ago
        )

        val points = ChartTimelineHelper.computeTotalBalancePoints(
            transactions = listOf(tx1),
            interval = ChartInterval.WEEK
        )

        assertTrue("Weekly points should have at least 8 weeks", points.size >= 8)
        val lastPoint = points.last()
        assertEquals(500.0, lastPoint.totalBalance, 0.001)
    }

    @Test
    fun testComputeTotalBalancePointsMonthly() {
        val now = System.currentTimeMillis()

        val tx1 = TransactionEntity(
            id = "tx-1",
            amount = 2000.0,
            type = "INCOME",
            categoryId = "cat_salary",
            note = "Job",
            timestamp = now - (60L * 24 * 60 * 60 * 1000) // 2 months ago
        )

        val tx2 = TransactionEntity(
            id = "tx-2",
            amount = 400.0,
            type = "EXPENSE",
            categoryId = "cat_rent",
            note = "Rent",
            timestamp = now
        )

        val points = ChartTimelineHelper.computeTotalBalancePoints(
            transactions = listOf(tx1, tx2),
            interval = ChartInterval.MONTH
        )

        assertTrue("Monthly points should have at least 6 months", points.size >= 6)
        val lastPoint = points.last()
        assertEquals(1600.0, lastPoint.totalBalance, 0.001)
        assertEquals(-400.0, lastPoint.delta, 0.001)
    }

    @Test
    fun testComputeNetSavingsPoints() {
        val now = System.currentTimeMillis()

        val txIncome = TransactionEntity(
            id = "inc-1",
            amount = 1500.0,
            type = "INCOME",
            categoryId = "cat_salary",
            note = "Income",
            timestamp = now - (1L * 24 * 60 * 60 * 1000)
        )

        val txExpense = TransactionEntity(
            id = "exp-1",
            amount = 300.0,
            type = "EXPENSE",
            categoryId = "cat_general",
            note = "Expense",
            timestamp = now - (1L * 24 * 60 * 60 * 1000)
        )

        val points = ChartTimelineHelper.computeNetSavingsPoints(
            transactions = listOf(txIncome, txExpense),
            interval = ChartInterval.DAY
        )

        val dayPoint = points.find { it.startMs <= txIncome.timestamp && txIncome.timestamp <= it.endMs }
        assertNotNull(dayPoint)
        assertEquals(1500.0, dayPoint!!.income, 0.001)
        assertEquals(300.0, dayPoint.expense, 0.001)
        assertEquals(1200.0, dayPoint.net, 0.001)
    }

    @Test
    fun testFormatCompactAmount() {
        assertEquals("$500", ChartTimelineHelper.formatCompactAmount(500.0, "$"))
        assertEquals("$1.5k", ChartTimelineHelper.formatCompactAmount(1500.0, "$"))
        assertEquals("-$2.3k", ChartTimelineHelper.formatCompactAmount(-2300.0, "$"))
        assertEquals("$1.0M", ChartTimelineHelper.formatCompactAmount(1_000_000.0, "$"))
    }
}
