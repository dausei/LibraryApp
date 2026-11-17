package kz.kbtu.library.model

// Why a loan cannot be renewed
enum class RenewBlock(val message: String) {
    OVERDUE("Overdue loans cannot be renewed"),
    LIMIT("Renewal limit reached"),
    RESERVED("Someone is waiting for this item")
}

// The lending rules in one place. Pure functions, so they are easy to test.
object LoanPolicy {
    const val MAX_LOANS = 3
    const val MAX_RENEWALS = 2
    const val HOLD_DAYS = 2
    const val FINE_PER_DAY = 100 // tenge for every started day after the due date

    const val DAY_MS = 24L * 60 * 60 * 1000

    fun dueDate(from: Long, loanDays: Int): Long = from + loanDays * DAY_MS

    // Whole days past the due date, a started day counts as a full day
    fun daysLate(dueAt: Long, now: Long): Int =
        if (now <= dueAt) 0 else ((now - dueAt + DAY_MS - 1) / DAY_MS).toInt()

    fun fineFor(dueAt: Long, returnedAt: Long): Int = daysLate(dueAt, returnedAt) * FINE_PER_DAY

    // Whole days until the due date (0 means less than a day left)
    fun daysLeft(dueAt: Long, now: Long): Int = Math.floorDiv(dueAt - now, DAY_MS).toInt()

    fun holdUntil(from: Long): Long = from + HOLD_DAYS * DAY_MS

    fun renewBlock(loan: Loan, hasQueue: Boolean, now: Long): RenewBlock? = when {
        now > loan.dueAt -> RenewBlock.OVERDUE
        loan.renewals >= MAX_RENEWALS -> RenewBlock.LIMIT
        hasQueue -> RenewBlock.RESERVED
        else -> null
    }
}
