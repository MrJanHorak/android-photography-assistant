package com.janhorak.shutterdeck.gear.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GearLoanRemindersTest {

    private val today = LocalDate.of(2026, 5, 30)

    @Test
    fun blankOrInvalidDueDateReturnsNull() {
        assertNull(calculateGearLoanReminder("Active", "", today))
        assertNull(calculateGearLoanReminder("Active", "05/30/2026", today))
    }

    @Test
    fun nonActiveStatusDoesNotTriggerReminder() {
        assertNull(calculateGearLoanReminder("Returned", "2026-05-31", today))
        assertNull(calculateGearLoanReminder("Lost", "2026-05-31", today))
    }

    @Test
    fun overdueLoanReturnsOverdueReminder() {
        val reminder = calculateGearLoanReminder("Active", "2026-05-27", today)!!

        assertEquals(GearLoanReminderLevel.OVERDUE, reminder.level)
        assertEquals(-3L, reminder.daysUntilDue)
        assertEquals("Overdue by 3 days", reminder.message)
    }

    @Test
    fun dueTodayReturnsDueTodayReminder() {
        val reminder = calculateGearLoanReminder("Active", "2026-05-30", today)!!

        assertEquals(GearLoanReminderLevel.DUE_TODAY, reminder.level)
        assertEquals(0L, reminder.daysUntilDue)
        assertEquals("Due today", reminder.message)
    }

    @Test
    fun upcomingLoanWithinReminderWindowReturnsReminder() {
        val reminder = calculateGearLoanReminder("Active", "2026-06-01", today)!!

        assertEquals(GearLoanReminderLevel.UPCOMING, reminder.level)
        assertEquals(2L, reminder.daysUntilDue)
        assertEquals("Due in 2 days", reminder.message)
    }

    @Test
    fun dueDateOutsideReminderWindowReturnsNull() {
        assertNull(calculateGearLoanReminder("Active", "2026-06-15", today))
    }

    @Test
    fun parseGearLoanDateAcceptsIsoDateOnly() {
        assertEquals(LocalDate.of(2026, 5, 30), parseGearLoanDate("2026-05-30"))
        assertNull(parseGearLoanDate("2026/05/30"))
    }
}
