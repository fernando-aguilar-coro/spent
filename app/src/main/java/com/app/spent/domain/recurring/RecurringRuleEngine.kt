package com.app.spent.domain.recurring

import com.app.spent.data.local.entity.RecurringRuleEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class DueOccurrence(
    val dueDate: LocalDate,
    val timestamp: Long
)

object RecurringRuleEngine {

    /**
     * Deterministically calculates all pending due dates and timestamps for a recurring rule
     * that fall on or before [today].
     *
     * Rules:
     * - If [rule.startDate] is strictly in the future relative to [today], returns empty list.
     * - If [rule.lastExecuted] is 0L, the first occurrence is [rule.startDate]'s LocalDate.
     *   Any subsequent occurrences up to [today] are also generated.
     * - If [rule.lastExecuted] > 0L, occurrences start from the next scheduled interval
     *   after [rule.lastExecuted] up to [today].
     * - Respects [rule.endDate]: occurrences after [rule.endDate] are excluded.
     * - Uses Option B month-end clamping (anchorDay preserved across varying month lengths).
     */
    fun calculatePendingDueDates(
        rule: RecurringRuleEntity,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DueOccurrence> {
        if (!rule.isActive) return emptyList()

        val startZdt = Instant.ofEpochMilli(rule.startDate).atZone(zone)
        val startLocalDate = startZdt.toLocalDate()
        val startLocalTime = startZdt.toLocalTime()
        val anchorDay = startLocalDate.dayOfMonth

        // 1. If start date has not arrived yet, nothing is due today
        if (startLocalDate.isAfter(today)) {
            return emptyList()
        }

        val endDateLocalDate = if (rule.endDate != null && rule.endDate > 0) {
            Instant.ofEpochMilli(rule.endDate).atZone(zone).toLocalDate()
        } else null

        val occurrences = mutableListOf<DueOccurrence>()

        val lastRunDate: LocalDate = if (rule.lastExecuted == 0L) {
            if (endDateLocalDate == null || !startLocalDate.isAfter(endDateLocalDate)) {
                occurrences.add(
                    DueOccurrence(
                        dueDate = startLocalDate,
                        timestamp = rule.startDate
                    )
                )
            }
            startLocalDate
        } else {
            Instant.ofEpochMilli(rule.lastExecuted).atZone(zone).toLocalDate()
        }

        // 3. Advance through subsequent intervals up to today
        var currentPointer: LocalDate = lastRunDate

        while (true) {
            val nextDate: LocalDate = when (rule.frequency) {
                "DAILY" -> currentPointer.plusDays(1)
                "WEEKLY" -> currentPointer.plusWeeks(1)
                "BIWEEKLY" -> currentPointer.plusWeeks(2)
                "MONTHLY" -> {
                    val nextYearMonth = YearMonth.from(currentPointer).plusMonths(1)
                    val effectiveDay = minOf(anchorDay, nextYearMonth.lengthOfMonth())
                    nextYearMonth.atDay(effectiveDay)
                }
                else -> {
                    val nextYearMonth = YearMonth.from(currentPointer).plusMonths(1)
                    val effectiveDay = minOf(anchorDay, nextYearMonth.lengthOfMonth())
                    nextYearMonth.atDay(effectiveDay)
                }
            }

            // Cannot generate dates in the future
            if (nextDate.isAfter(today)) {
                break
            }

            // Cannot generate dates past rule's end date
            if (endDateLocalDate != null && nextDate.isAfter(endDateLocalDate)) {
                break
            }

            val nextTimestamp = nextDate.atTime(startLocalTime).atZone(zone).toInstant().toEpochMilli()

            occurrences.add(
                DueOccurrence(
                    dueDate = nextDate,
                    timestamp = nextTimestamp
                )
            )

            currentPointer = nextDate
        }

        return occurrences
    }
}
