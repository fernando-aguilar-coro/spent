package com.app.spent.ui.analytics.components

import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.analytics.ChartInterval
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

    fun computeTotalBalancePoints(
        transactions: List<TransactionEntity>,
        interval: ChartInterval,
        locale: Locale = Locale.getDefault()
    ): List<TotalBalancePoint> {
        val sortedTransactions = transactions.sortedBy { it.timestamp }

        fun balanceAt(timestampEnd: Long): Double {
            return sortedTransactions
                .filter { it.timestamp <= timestampEnd && it.type != "SAVING" }
                .sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        }

        fun deltaBetween(startMs: Long, endMs: Long): Double {
            return sortedTransactions
                .filter { it.timestamp in startMs..endMs && it.type != "SAVING" }
                .sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        }

        val now = Calendar.getInstance()
        val earliestTx = sortedTransactions.firstOrNull()?.timestamp ?: now.timeInMillis

        return when (interval) {
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

                val list = mutableListOf<TotalBalancePoint>()
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

                    list.add(
                        TotalBalancePoint(
                            label = label,
                            fullDateLabel = fullDateFormat.format(curr.time),
                            totalBalance = balanceAt(eMs),
                            delta = deltaBetween(sMs, eMs),
                            startMs = sMs,
                            endMs = eMs
                        )
                    )

                    curr.add(Calendar.DAY_OF_YEAR, 1)
                }
                list
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

                val list = mutableListOf<TotalBalancePoint>()
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

                    list.add(
                        TotalBalancePoint(
                            label = weekLabelFormat.format(curr.time),
                            fullDateLabel = dateRange,
                            totalBalance = balanceAt(eMs),
                            delta = deltaBetween(sMs, eMs),
                            startMs = sMs,
                            endMs = eMs
                        )
                    )

                    curr.add(Calendar.WEEK_OF_YEAR, 1)
                }
                list
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

                val list = mutableListOf<TotalBalancePoint>()
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

                    list.add(
                        TotalBalancePoint(
                            label = label,
                            fullDateLabel = fullMonthFormat.format(curr.time),
                            totalBalance = balanceAt(eMs),
                            delta = deltaBetween(sMs, eMs),
                            startMs = sMs,
                            endMs = eMs
                        )
                    )

                    curr.add(Calendar.MONTH, 1)
                }
                list
            }
        }
    }

    fun computeNetSavingsPoints(
        transactions: List<TransactionEntity>,
        interval: ChartInterval,
        locale: Locale = Locale.getDefault()
    ): List<NetSavingsPoint> {
        val sortedTransactions = transactions.sortedBy { it.timestamp }

        fun incomeBetween(startMs: Long, endMs: Long): Double {
            return sortedTransactions
                .filter { it.timestamp in startMs..endMs && it.type == "INCOME" }
                .sumOf { it.amount }
        }

        fun expenseBetween(startMs: Long, endMs: Long): Double {
            return sortedTransactions
                .filter { it.timestamp in startMs..endMs && it.type == "EXPENSE" }
                .sumOf { it.amount }
        }

        val now = Calendar.getInstance()
        val earliestTx = sortedTransactions.firstOrNull()?.timestamp ?: now.timeInMillis

        return when (interval) {
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

                val list = mutableListOf<NetSavingsPoint>()
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
                    val inc = incomeBetween(sMs, eMs)
                    val exp = expenseBetween(sMs, eMs)

                    list.add(
                        NetSavingsPoint(
                            label = label,
                            fullDateLabel = fullDateFormat.format(curr.time),
                            income = inc,
                            expense = exp,
                            net = inc - exp,
                            startMs = sMs,
                            endMs = eMs
                        )
                    )

                    curr.add(Calendar.DAY_OF_YEAR, 1)
                }
                list
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

                val list = mutableListOf<NetSavingsPoint>()
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
                    val inc = incomeBetween(sMs, eMs)
                    val exp = expenseBetween(sMs, eMs)

                    list.add(
                        NetSavingsPoint(
                            label = weekLabelFormat.format(curr.time),
                            fullDateLabel = dateRange,
                            income = inc,
                            expense = exp,
                            net = inc - exp,
                            startMs = sMs,
                            endMs = eMs
                        )
                    )

                    curr.add(Calendar.WEEK_OF_YEAR, 1)
                }
                list
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

                val list = mutableListOf<NetSavingsPoint>()
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
                    val inc = incomeBetween(sMs, eMs)
                    val exp = expenseBetween(sMs, eMs)

                    list.add(
                        NetSavingsPoint(
                            label = label,
                            fullDateLabel = fullMonthFormat.format(curr.time),
                            income = inc,
                            expense = exp,
                            net = inc - exp,
                            startMs = sMs,
                            endMs = eMs
                        )
                    )

                    curr.add(Calendar.MONTH, 1)
                }
                list
            }
        }
    }
}
