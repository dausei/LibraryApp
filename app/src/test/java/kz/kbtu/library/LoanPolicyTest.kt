package kz.kbtu.library

import kz.kbtu.library.model.Loan
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.LoanPolicy.DAY_MS
import kz.kbtu.library.model.LoanStatus
import kz.kbtu.library.model.RenewBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Lending rules: due dates, fines, renewals
class LoanPolicyTest {

    @Test
    fun `due date is the loan period after the start`() {
        assertEquals(NOW + 21 * DAY_MS, LoanPolicy.dueDate(NOW, loanDays = 21))
    }

    @Test
    fun `no fine when returned on or before the due date`() {
        assertEquals(0, LoanPolicy.fineFor(dueAt = NOW, returnedAt = NOW))
        assertEquals(0, LoanPolicy.fineFor(dueAt = NOW, returnedAt = NOW - DAY_MS))
    }

    @Test
    fun `a started late day counts as a full day`() {
        assertEquals(100, LoanPolicy.fineFor(dueAt = NOW, returnedAt = NOW + 1))
        assertEquals(100, LoanPolicy.fineFor(dueAt = NOW, returnedAt = NOW + DAY_MS))
        assertEquals(200, LoanPolicy.fineFor(dueAt = NOW, returnedAt = NOW + DAY_MS + 1))
        assertEquals(300, LoanPolicy.fineFor(dueAt = NOW, returnedAt = NOW + 3 * DAY_MS))
    }

    @Test
    fun `days left rounds down`() {
        assertEquals(3, LoanPolicy.daysLeft(dueAt = NOW + 3 * DAY_MS + 5, now = NOW))
        assertEquals(0, LoanPolicy.daysLeft(dueAt = NOW + 1000, now = NOW))
    }

    @Test
    fun `loan status follows the due date and the return`() {
        val loan = Loan(1, 1, 1, borrowedAt = NOW - DAY_MS, dueAt = NOW + DAY_MS)

        assertEquals(LoanStatus.ACTIVE, loan.status(NOW))
        assertEquals(LoanStatus.OVERDUE, loan.status(NOW + 2 * DAY_MS))
        assertEquals(LoanStatus.RETURNED, loan.copy(returnedAt = NOW).status(NOW + 2 * DAY_MS))
    }

    @Test
    fun `a loan can be renewed when nothing blocks it`() {
        val loan = Loan(1, 1, 1, borrowedAt = NOW - DAY_MS, dueAt = NOW + DAY_MS)

        assertNull(LoanPolicy.renewBlock(loan, hasQueue = false, now = NOW))
    }

    @Test
    fun `renewing is blocked for overdue loans, at the limit, and when someone waits`() {
        val loan = Loan(1, 1, 1, borrowedAt = NOW - DAY_MS, dueAt = NOW + DAY_MS)

        assertEquals(RenewBlock.OVERDUE, LoanPolicy.renewBlock(loan, false, now = NOW + 2 * DAY_MS))
        assertEquals(
            RenewBlock.LIMIT,
            LoanPolicy.renewBlock(loan.copy(renewals = LoanPolicy.MAX_RENEWALS), false, NOW)
        )
        assertEquals(RenewBlock.RESERVED, LoanPolicy.renewBlock(loan, hasQueue = true, now = NOW))
    }
}
