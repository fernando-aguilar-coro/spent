package com.app.spent.ui.analytics.components

import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.analytics.ChartInterval
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class TotalBalancePoint(
    val label: String,
    val fullDateLabel: String,
    val totalBalance: Double,
    val delta: Double,
    val startMs: Long,
    val endMs: Long
)

data class NetSavingsPoint(
    val label: String,
    val fullDateLabel: String,
    val income: Double,
    val expense: Double,
    val net: Double = income - expense,
    val startMs: Long,
    val endMs: Long
)

internal data class IntervalSlot(
    val label: String,
    val fullDateLabel: String,
    val sMs: Long,
    val eMs: Long
)

object ChartTimelineHelper {

    fun formatCompactAmount(value: Double, currency: String): String {
        val absVal = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> "$sign$currency%.1fM".format(absVal / 1_000_000)
            absVal >= 1_000 -> "$sign$currency%.1fk".format(absVal / 1_000)
            else -> "$sign$currency%.0f".format(absVal)
        }
    }

    private fun generateIntervalSlots(
        earliestTx: Long,
        interval: ChartInterval,
        locale: Locale
    ): List<IntervalSlot> {
        val slots = mutableListOf<IntervalSlot>()

        when (interval) {
            ChartInterval.DAY -> {
                val dayFormat = SimpleDateFormat("d", locale)
                val monthDayFormat = SimpleDateFormat("MMM d", locale)
                val fullDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", locale)

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = earliestTx
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Window cap: maximum 90 days for daily view to prevent OpenGL/Canvas hardware limits
                val max90DaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -89)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (startCal.before(max90DaysAgo)) {
                    startCal.timeInMillis = max90DaysAgo.timeInMillis
                }

                // Ensure at least 14 days of timeline are present
                val min14DaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -13)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (startCal.after(min14DaysAgo)) {
                    startCal.timeInMillis = min14DaysAgo.timeInMillis
                }

                val endToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val curr = startCal.clone() as Calendar
                while (!curr.after(endToday)) {
                    val sMs = curr.timeInMillis
                    val eCal = (curr.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val eMs = eCal.timeInMillis

                    val isFirstDayOfMonth = curr.get(Calendar.DAY_OF_MONTH) == 1
                    val label = if (isFirstDayOfMonth) monthDayFormat.format(curr.time) else dayFormat.format(curr.time)

                    slots.add(
                        IntervalSlot(
                            label = label,
                            fullDateLabel = fullDateFormat.format(curr.time),
                            sMs = sMs,
                            eMs = eMs
                        )
                    )
                    curr.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            ChartInterval.WEEK -> {
                val weekLabelFormat = SimpleDateFormat("MMM d", locale)
                val fullDateFormat = SimpleDateFormat("MMM d, yyyy", locale)

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = earliestTx
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Window cap: maximum 104 weeks (2 years)
                val max104WeeksAgo = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    add(Calendar.WEEK_OF_YEAR, -103)
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (startCal.before(max104WeeksAgo)) {
                    startCal.timeInMillis = max104WeeksAgo.timeInMillis
                }

                // Ensure at least 8 weeks of timeline
                val min8WeeksAgo = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    add(Calendar.WEEK_OF_YEAR, -7)
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (startCal.after(min8WeeksAgo)) {
                    startCal.timeInMillis = min8WeeksAgo.timeInMillis
                }

                val endThisWeek = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val curr = startCal.clone() as Calendar
                while (!curr.after(endThisWeek)) {
                    val sMs = curr.timeInMillis
                    val eCal = (curr.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val eMs = eCal.timeInMillis

                    val dateRange = "${weekLabelFormat.format(curr.time)} - ${fullDateFormat.format(eCal.time)}"
                    slots.add(
                        IntervalSlot(
                            label = weekLabelFormat.format(curr.time),
                            fullDateLabel = dateRange,
                            sMs = sMs,
                            eMs = eMs
                        )
                    )
                    curr.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            ChartInterval.MONTH -> {
                val monthFormat = SimpleDateFormat("MMM", locale)
                val yearMonthFormat = SimpleDateFormat("MMM ''yy", locale)
                val fullMonthFormat = SimpleDateFormat("MMMM yyyy", locale)

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = earliestTx
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Window cap: maximum 120 months (10 years)
                val max120MonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -119)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (startCal.before(max120MonthsAgo)) {
                    startCal.timeInMillis = max120MonthsAgo.timeInMillis
                }

                // Ensure at least 6 months
                val min6MonthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -5)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (startCal.after(min6MonthsAgo)) {
                    startCal.timeInMillis = min6MonthsAgo.timeInMillis
                }

                val endThisMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val curr = startCal.clone() as Calendar
                while (!curr.after(endThisMonth)) {
                    val sMs = curr.timeInMillis
                    val eCal = (curr.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val eMs = eCal.timeInMillis

                    val isJanuary = curr.get(Calendar.MONTH) == Calendar.JANUARY
                    val label = if (isJanuary) yearMonthFormat.format(curr.time) else monthFormat.format(curr.time)

                    slots.add(
                        IntervalSlot(
                            label = label,
                            fullDateLabel = fullMonthFormat.format(curr.time),
                            sMs = sMs,
                            eMs = eMs
                        )
                    )
                    curr.add(Calendar.MONTH, 1)
                }
            }
        }

        return slots
    }

    /**
     * Computes TotalBalancePoint list in O(N + D) single-pass linear time with prefix sum accumulation.
     */
    fun computeTotalBalancePoints(
        transactions: List<TransactionEntity>,
        interval: ChartInterval,
        locale: Locale = Locale.getDefault()
    ): List<TotalBalancePoint> {
        val nonSaving = transactions.filter { it.type != "SAVING" }.sortedBy { it.timestamp }
        val earliestTx = nonSaving.firstOrNull()?.timestamp ?: System.currentTimeMillis()

        val slots = generateIntervalSlots(earliestTx, interval, locale)
        if (slots.isEmpty()) return emptyList()

        val firstSlotStartMs = slots.first().sMs

        // 1. Single pass to calculate starting balance prior to the window start
        var runningBalance = 0.0
        var txIndex = 0

        while (txIndex < nonSaving.size && nonSaving[txIndex].timestamp < firstSlotStartMs) {
            val tx = nonSaving[txIndex]
            if (tx.type == "INCOME") {
                runningBalance += tx.amount
            } else if (tx.type == "EXPENSE") {
                runningBalance -= tx.amount
            }
            txIndex++
        }

        // 2. Single linear sweep through slots and remaining transactions
        val points = ArrayList<TotalBalancePoint>(slots.size)
        for (slot in slots) {
            var intervalDelta = 0.0
            while (txIndex < nonSaving.size && nonSaving[txIndex].timestamp <= slot.eMs) {
                val tx = nonSaving[txIndex]
                if (tx.type == "INCOME") {
                    intervalDelta += tx.amount
                } else if (tx.type == "EXPENSE") {
                    intervalDelta -= tx.amount
                }
                txIndex++
            }

            runningBalance += intervalDelta
            points.add(
                TotalBalancePoint(
                    label = slot.label,
                    fullDateLabel = slot.fullDateLabel,
                    totalBalance = runningBalance,
                    delta = intervalDelta,
                    startMs = slot.sMs,
                    endMs = slot.eMs
                )
            )
        }

        return points
    }

    /**
     * Computes NetSavingsPoint list in O(N + D) single-pass linear time.
     */
    fun computeNetSavingsPoints(
        transactions: List<TransactionEntity>,
        interval: ChartInterval,
        locale: Locale = Locale.getDefault()
    ): List<NetSavingsPoint> {
        val nonSaving = transactions.filter { it.type != "SAVING" }.sortedBy { it.timestamp }
        val earliestTx = nonSaving.firstOrNull()?.timestamp ?: System.currentTimeMillis()

        val slots = generateIntervalSlots(earliestTx, interval, locale)
        if (slots.isEmpty()) return emptyList()

        val firstSlotStartMs = slots.first().sMs
        var txIndex = 0

        // Skip transactions prior to the visible window
        while (txIndex < nonSaving.size && nonSaving[txIndex].timestamp < firstSlotStartMs) {
            txIndex++
        }

        val points = ArrayList<NetSavingsPoint>(slots.size)
        for (slot in slots) {
            var intervalIncome = 0.0
            var intervalExpense = 0.0

            while (txIndex < nonSaving.size && nonSaving[txIndex].timestamp <= slot.eMs) {
                val tx = nonSaving[txIndex]
                if (tx.type == "INCOME") {
                    intervalIncome += tx.amount
                } else if (tx.type == "EXPENSE") {
                    intervalExpense += tx.amount
                }
                txIndex++
            }

            points.add(
                NetSavingsPoint(
                    label = slot.label,
                    fullDateLabel = slot.fullDateLabel,
                    income = intervalIncome,
                    expense = intervalExpense,
                    net = intervalIncome - intervalExpense,
                    startMs = slot.sMs,
                    endMs = slot.eMs
                )
            )
        }

        return points
    }
}
