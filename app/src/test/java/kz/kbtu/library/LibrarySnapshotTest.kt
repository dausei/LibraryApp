package kz.kbtu.library

import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.LibrarySnapshot
import kz.kbtu.library.model.Loan
import kz.kbtu.library.model.LoanPolicy.DAY_MS
import kz.kbtu.library.model.LoanStatus
import kz.kbtu.library.model.RenewBlock
import kz.kbtu.library.model.Reservation
import kz.kbtu.library.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Everything the screens derive from the stored data
class LibrarySnapshotTest {

    private val snapshot = sampleSnapshot()

    @Test
    fun `an item without an open loan is available`() {
        assertEquals(Availability.Available, snapshot.availabilityOf(4))
        // Neuromancer was returned, so it is available again
        assertEquals(Availability.Available, snapshot.availabilityOf(3))
    }

    @Test
    fun `a borrowed item reports the borrower, the status and the queue`() {
        val onLoan = snapshot.availabilityOf(1) as Availability.OnLoan

        assertEquals("Aidar", onLoan.borrower?.name)
        assertEquals(LoanStatus.ACTIVE, onLoan.status)
        assertEquals(1, onLoan.queueSize)
    }

    @Test
    fun `an overdue loan is reported as overdue`() {
        val onLoan = snapshot.availabilityOf(2) as Availability.OnLoan

        assertEquals(LoanStatus.OVERDUE, onLoan.status)
    }

    @Test
    fun `a returned item with a ready reservation is held for that member`() {
        val ready = Reservation(
            id = 1, itemId = 3, memberId = 2, createdAt = NOW,
            status = ReservationStatus.READY, readyUntil = NOW + 2 * DAY_MS
        )

        val held = sampleSnapshot(reservations = listOf(ready)).availabilityOf(3)

        assertTrue(held is Availability.Held)
        assertEquals("Dana", (held as Availability.Held).member?.name)
    }

    @Test
    fun `member status counts open loans and unpaid fines only`() {
        val aidar = snapshot.statusOf(snapshot.member(1)!!)
        val dana = snapshot.statusOf(snapshot.member(2)!!)

        assertEquals(2, aidar.activeLoans)
        assertEquals(0, aidar.unpaidFine)
        assertTrue(aidar.canBorrow)

        assertEquals(0, dana.activeLoans)
        assertEquals(300, dana.unpaidFine)
        assertEquals(false, dana.canBorrow)
    }

    @Test
    fun `a member at the loan limit cannot borrow`() {
        val loans = (1..3).map {
            Loan(it, itemId = it, memberId = 1, borrowedAt = NOW, dueAt = NOW + DAY_MS)
        }

        val status = sampleSnapshot(loans = loans).statusOf(snapshot.member(1)!!)

        assertTrue(status.atLimit)
        assertEquals(false, status.canBorrow)
    }

    @Test
    fun `active loans are sorted by due date and history by return date`() {
        assertEquals(listOf(2, 1), snapshot.activeLoanEntries().map { it.loan.id })
        assertEquals(listOf(3), snapshot.historyEntries().map { it.loan.id })
    }

    @Test
    fun `a loan with someone waiting cannot be renewed`() {
        val withQueue = snapshot.activeLoanEntries().first { it.loan.id == 1 }
        val overdue = snapshot.activeLoanEntries().first { it.loan.id == 2 }

        assertEquals(RenewBlock.RESERVED, withQueue.renewBlock)
        assertEquals(RenewBlock.OVERDUE, overdue.renewBlock)
        assertEquals(false, withQueue.canRenew)
    }

    @Test
    fun `queue positions start at one per item`() {
        val second = Reservation(2, itemId = 1, memberId = 1, createdAt = NOW, status = ReservationStatus.WAITING)

        val entries = sampleSnapshot(reservations = snapshot.reservations + second).reservationEntries(itemId = 1)

        assertEquals(listOf(1, 2), entries.map { it.position })
        assertEquals(listOf("Dana", "Aidar"), entries.map { it.member?.name })
    }

    @Test
    fun `stats are computed from the items, loans and reservations`() {
        val stats = snapshot.stats()

        assertEquals(5, stats.totalItems)
        assertEquals(3, stats.availableItems)
        assertEquals(2, stats.itemsOnLoan)
        assertEquals(464 + 612 + 271, stats.totalPages)
        assertEquals("Dune", stats.longestBook?.title)
        assertEquals(mapOf("Book" to 3, "Magazine" to 1, "DVD" to 1), stats.countByType)
        assertEquals(listOf("Dune", "Neuromancer", "Clean Code"), stats.oldest.map { it.title })
        assertEquals(setOf("Programming", "Sci-Fi"), stats.genres)
        assertEquals(3, stats.totalLoans)
        assertEquals(1, stats.overdueLoans)
        assertEquals(1, stats.openReservations)
        assertEquals(300, stats.finesOutstanding)
        assertEquals(0, stats.finesCollected)
    }

    @Test
    fun `most borrowed items are ordered by number of loans`() {
        val more = snapshot.loans + Loan(
            id = 4, itemId = 3, memberId = 1, borrowedAt = NOW - 5 * DAY_MS, dueAt = NOW, returnedAt = NOW
        )

        val top = sampleSnapshot(loans = more).stats().mostBorrowed

        assertEquals("Neuromancer", top.first().first.title)
        assertEquals(2, top.first().second)
    }

    @Test
    fun `an empty library has empty stats and no longest book`() {
        val stats = LibrarySnapshot().stats()

        assertEquals(0, stats.totalItems)
        assertNull(stats.longestBook)
    }
}
