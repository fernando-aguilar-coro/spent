package com.app.spent.recurring

import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.domain.recurring.RecurringRuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecurringRuleEngineTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun testFutureRuleGeneratesZeroOccurrencesToday() {
        // Today is Sept 7, recurring rule starts Sept 17 (in the future)
        val startDate = LocalDate.of(2026, 9, 17).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_future",
            amount = 100.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            startDate = startDate,
            lastExecuted = 0L,
            isActive = true
        )

        val today = LocalDate.of(2026, 9, 7)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertTrue("Future rule should generate zero occurrences today", occurrences.isEmpty())
    }

    @Test
    fun testFutureRuleGeneratesFirstOccurrenceWhenDateArrives() {
        // Rule starts Sept 17, and today is Sept 17
        val startDate = LocalDate.of(2026, 9, 17).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_future_due",
            amount = 100.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            startDate = startDate,
            lastExecuted = 0L,
            isActive = true
        )

        val today = LocalDate.of(2026, 9, 17)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertEquals(1, occurrences.size)
        assertEquals(LocalDate.of(2026, 9, 17), occurrences[0].dueDate)
    }

    @Test
    fun testOptionBMonthEndClampingJanFebMar() {
        // Rule starts on Jan 31
        val startDate = LocalDate.of(2026, 1, 31).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_clamp",
            amount = 50.0,
            categoryId = "cat_general",
            frequency = "MONTHLY",
            startDate = startDate,
            lastExecuted = 0L,
            isActive = true
        )

        // Today is March 31
        val today = LocalDate.of(2026, 3, 31)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertEquals(3, occurrences.size)
        assertEquals(LocalDate.of(2026, 1, 31), occurrences[0].dueDate)
        assertEquals(LocalDate.of(2026, 2, 28), occurrences[1].dueDate)
        assertEquals(LocalDate.of(2026, 3, 31), occurrences[2].dueDate)
    }

    @Test
    fun testWeeklyCadence() {
        // Monday Sept 7, 2026
        val startDate = LocalDate.of(2026, 9, 7).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_weekly",
            amount = 25.0,
            categoryId = "cat_gym",
            frequency = "WEEKLY",
            startDate = startDate,
            lastExecuted = 0L,
            isActive = true
        )

        // Today is Monday Sept 21 (2 weeks later)
        val today = LocalDate.of(2026, 9, 21)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertEquals(3, occurrences.size)
        assertEquals(LocalDate.of(2026, 9, 7), occurrences[0].dueDate)
        assertEquals(LocalDate.of(2026, 9, 14), occurrences[1].dueDate)
        assertEquals(LocalDate.of(2026, 9, 21), occurrences[2].dueDate)
    }

    @Test
    fun testDailyCadence() {
        val startDate = LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_daily",
            amount = 5.0,
            categoryId = "cat_coffee",
            frequency = "DAILY",
            startDate = startDate,
            lastExecuted = 0L,
            isActive = true
        )

        val today = LocalDate.of(2026, 9, 4)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2026, 9, 1), occurrences[0].dueDate)
        assertEquals(LocalDate.of(2026, 9, 2), occurrences[1].dueDate)
        assertEquals(LocalDate.of(2026, 9, 3), occurrences[2].dueDate)
        assertEquals(LocalDate.of(2026, 9, 4), occurrences[3].dueDate)
    }

    @Test
    fun testStoppedRuleGeneratesNothing() {
        val startDate = LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_stopped",
            amount = 5.0,
            categoryId = "cat_coffee",
            frequency = "DAILY",
            startDate = startDate,
            lastExecuted = 0L,
            isActive = false // Stopped
        )

        val today = LocalDate.of(2026, 9, 4)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun testEndDateClampsOccurrences() {
        val startDate = LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endDate = LocalDate.of(2026, 9, 2).atStartOfDay(zone).toInstant().toEpochMilli()
        val rule = RecurringRuleEntity(
            id = "rule_end",
            amount = 5.0,
            categoryId = "cat_coffee",
            frequency = "DAILY",
            startDate = startDate,
            endDate = endDate,
            lastExecuted = 0L,
            isActive = true
        )

        val today = LocalDate.of(2026, 9, 5)
        val occurrences = RecurringRuleEngine.calculatePendingDueDates(rule, today, zone)

        assertEquals(2, occurrences.size)
        assertEquals(LocalDate.of(2026, 9, 1), occurrences[0].dueDate)
        assertEquals(LocalDate.of(2026, 9, 2), occurrences[1].dueDate)
    }
}
