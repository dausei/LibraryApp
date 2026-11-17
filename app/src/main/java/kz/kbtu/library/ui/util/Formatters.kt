package kz.kbtu.library.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kz.kbtu.library.model.Loan
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.LoanStatus

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

fun formatDate(epochMillis: Long): String =
    dateFormat.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatMoney(amount: Int): String = "$amount ₸"

fun plural(count: Int, word: String): String = if (count == 1) "$count $word" else "$count ${word}s"

// "Due in 3 days", "Due today", "Overdue by 2 days", "Returned 5 Nov 2026"
fun dueText(loan: Loan, status: LoanStatus, now: Long): String = when (status) {
    LoanStatus.RETURNED -> "Returned ${formatDate(loan.returnedAt ?: loan.dueAt)}"
    LoanStatus.OVERDUE -> "Overdue by ${plural(LoanPolicy.daysLate(loan.dueAt, now), "day")}"
    LoanStatus.ACTIVE -> {
        val left = LoanPolicy.daysLeft(loan.dueAt, now)
        if (left == 0) "Due today" else "Due in ${plural(left, "day")}"
    }
}
