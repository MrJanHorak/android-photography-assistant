package com.janhorak.shutterdeck.gear.domain

import com.janhorak.shutterdeck.core.time.parseStructuredDateOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class GearLoanReminderLevel {
    UPCOMING,
    DUE_TODAY,
    OVERDUE,
}

data class GearLoanReminder(
    val dueDate: LocalDate,
    val daysUntilDue: Long,
    val level: GearLoanReminderLevel,
    val message: String,
)

fun calculateGearLoanReminder(
    status: String,
    dueDateText: String,
    today: LocalDate = LocalDate.now(),
    dueSoonWindowDays: Long = 7,
): GearLoanReminder? {
    if (status != "Active" || dueSoonWindowDays < 0) return null

    val dueDate = parseGearLoanDate(dueDateText) ?: return null
    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
    return when {
        daysUntilDue < 0 -> GearLoanReminder(
            dueDate = dueDate,
            daysUntilDue = daysUntilDue,
            level = GearLoanReminderLevel.OVERDUE,
            message = if (daysUntilDue == -1L) {
                "Overdue by 1 day"
            } else {
                "Overdue by ${-daysUntilDue} days"
            },
        )

        daysUntilDue == 0L -> GearLoanReminder(
            dueDate = dueDate,
            daysUntilDue = daysUntilDue,
            level = GearLoanReminderLevel.DUE_TODAY,
            message = "Due today",
        )

        daysUntilDue <= dueSoonWindowDays -> GearLoanReminder(
            dueDate = dueDate,
            daysUntilDue = daysUntilDue,
            level = GearLoanReminderLevel.UPCOMING,
            message = if (daysUntilDue == 1L) {
                "Due tomorrow"
            } else {
                "Due in $daysUntilDue days"
            },
        )

        else -> null
    }
}

fun parseGearLoanDate(text: String): LocalDate? {
    return parseStructuredDateOrNull(text)
}
