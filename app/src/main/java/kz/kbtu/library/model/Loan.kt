package kz.kbtu.library.model

enum class LoanStatus(val label: String) {
    ACTIVE("Active"),
    OVERDUE("Overdue"),
    RETURNED("Returned")
}

// One lending of an item. Times are epoch milliseconds.
data class Loan(
    val id: Int,
    val itemId: Int,
    val memberId: Int,
    val borrowedAt: Long,
    val dueAt: Long,
    val returnedAt: Long? = null,
    val renewals: Int = 0,
    val fine: Int = 0,
    val finePaid: Boolean = false
) {
    val isActive: Boolean get() = returnedAt == null
    val hasUnpaidFine: Boolean get() = fine > 0 && !finePaid

    fun status(now: Long): LoanStatus = when {
        returnedAt != null -> LoanStatus.RETURNED
        now > dueAt -> LoanStatus.OVERDUE
        else -> LoanStatus.ACTIVE
    }
}
