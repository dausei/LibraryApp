package kz.kbtu.library

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kz.kbtu.library.data.local.LibraryDatabase
import kz.kbtu.library.data.local.SeedData
import kz.kbtu.library.data.repository.RoomLibraryRepository
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.LoanPolicy.DAY_MS
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.MemberResult
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.model.RenewBlock
import kz.kbtu.library.model.RenewResult
import kz.kbtu.library.model.ReserveResult
import kz.kbtu.library.model.ReservationStatus
import kz.kbtu.library.model.ReturnResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Runs the repository against a real (in-memory) Room database. The clock is controlled by the test.
@RunWith(AndroidJUnit4::class)
class RoomLibraryRepositoryTest {

    private class TestClock(var millis: Long) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = Instant.ofEpochMilli(millis)
        override fun millis(): Long = millis
    }

    private lateinit var db: LibraryDatabase
    private lateinit var clock: TestClock
    private lateinit var repository: RoomLibraryRepository

    // Seed data (see SeedData): items 1-9, members 1 Aidar, 2 Dana (owes 300, Interstellar overdue),
    // 3 Timur. Dune (3) is with Aidar, Timur waits for it.
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        clock = TestClock(1_800_000_000_000L)
        SeedData.insertInto(db.openHelper.writableDatabase, clock.millis())
        repository = RoomLibraryRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun snapshot() = runBlocking { repository.library.first() }

    @Test
    fun itemCrudSurvivesInTheDatabase() = runBlocking {
        val id = repository.addItem(NewItemData.NewMagazine("Forbes", 2025, 3))
        assertEquals("Forbes", repository.getItem(id)?.title)
        assertEquals(10, snapshot().items.size)

        repository.updateItem(id, NewItemData.NewMagazine("Forbes Kazakhstan", 2025, 4))
        assertEquals("Forbes Kazakhstan", repository.getItem(id)?.title)

        assertEquals(DeleteResult.Deleted, repository.deleteItem(id))
        assertNull(repository.getItem(id))
    }

    @Test
    fun anItemOnLoanCannotBeDeleted() = runBlocking {
        assertEquals(DeleteResult.OnLoan, repository.deleteItem(3))
        assertNotNull(repository.getItem(3))
    }

    @Test
    fun membersAreValidatedAndDeletedOnlyWithoutLoans() = runBlocking {
        assertEquals(MemberResult.EmptyName, repository.addMember("  "))
        assertEquals(MemberResult.Duplicate, repository.addMember("aidar"))
        val saved = repository.addMember("Mira") as MemberResult.Saved
        assertEquals(MemberResult.Duplicate, repository.renameMember(saved.member.id, "Dana"))
        assertTrue(repository.renameMember(saved.member.id, "Mira K") is MemberResult.Saved)

        assertEquals(DeleteResult.HasActiveLoans(1), repository.deleteMember(1))
        assertEquals(DeleteResult.Deleted, repository.deleteMember(saved.member.id))
    }

    @Test
    fun borrowEnforcesEveryRule() = runBlocking {
        assertEquals(LoanResult.MemberNotFound, repository.borrow(99, 1))
        assertEquals(LoanResult.ItemNotFound, repository.borrow(1, 99))
        assertEquals(LoanResult.AlreadyBorrowed(byMemberId = 1), repository.borrow(3, 3))
        assertEquals(LoanResult.UnpaidFines(300), repository.borrow(2, 1)) // Dana owes a fine

        val ok = repository.borrow(3, 1) as LoanResult.Success // Timur takes Clean Code (book: 21 days)
        assertEquals(clock.millis() + 21 * DAY_MS, ok.loan.dueAt)
    }

    @Test
    fun aMemberCannotHoldMoreThanTheLimit() = runBlocking {
        // Timur already has Wired; give him two more, the next one is refused
        assertTrue(repository.borrow(3, 1) is LoanResult.Success)
        assertTrue(repository.borrow(3, 2) is LoanResult.Success)
        assertEquals(LoanResult.LimitReached(LoanPolicy.MAX_LOANS), repository.borrow(3, 4))
    }

    @Test
    fun returningALateItemChargesAFineAndPayingClearsIt() = runBlocking {
        // Interstellar is 2 days overdue for Dana
        val result = repository.returnItem(8) as ReturnResult.Returned
        assertEquals(2 * LoanPolicy.FINE_PER_DAY, result.loan.fine)
        assertEquals(ReturnResult.NotBorrowed, repository.returnItem(8))

        val danaOwes = snapshot().statusOf(snapshot().member(2)!!).unpaidFine
        assertEquals(300 + 200, danaOwes)

        assertEquals(500, repository.payFines(2))
        assertEquals(0, snapshot().statusOf(snapshot().member(2)!!).unpaidFine)
        assertTrue(repository.borrow(2, 1) is LoanResult.Success) // can borrow again
    }

    @Test
    fun theQueueMovesWhenTheItemComesBack() = runBlocking {
        // Timur is waiting for Dune; Aidar returns it
        val result = repository.returnItem(3) as ReturnResult.Returned
        assertEquals("Timur", result.nextReader?.name)

        val held = snapshot().availabilityOf(3) as Availability.Held
        assertEquals(ReservationStatus.READY, held.reservation.status)
        assertEquals(clock.millis() + LoanPolicy.HOLD_DAYS * DAY_MS, held.reservation.readyUntil)

        // Nobody else can take it while it is kept for Timur
        assertEquals(LoanResult.HeldForOther(memberId = 3), repository.borrow(1, 3))

        // Timur picks it up: the reservation is fulfilled and the queue is empty
        assertTrue(repository.borrow(3, 3) is LoanResult.Success)
        assertTrue(snapshot().reservations.isEmpty())
        assertTrue(snapshot().availabilityOf(3) is Availability.OnLoan)
    }

    @Test
    fun anUnclaimedHoldExpiresAndGoesToTheNextMember() = runBlocking {
        assertEquals(ReserveResult.Reserved(position = 2), repository.reserve(2, 3)) // Dana is second in line
        repository.returnItem(3)                                                       // Timur is READY

        clock.millis += (LoanPolicy.HOLD_DAYS + 1) * DAY_MS
        repository.releaseExpiredHolds()

        val reservations = snapshot().reservations
        assertEquals(listOf(2), reservations.map { it.memberId })
        assertEquals(ReservationStatus.READY, reservations.single().status)
    }

    @Test
    fun cancellingAReadyReservationPassesTheItemOn() = runBlocking {
        repository.reserve(2, 3)
        repository.returnItem(3) // Timur READY, Dana WAITING
        val timurs = snapshot().reservations.first { it.memberId == 3 }

        assertTrue(repository.cancelReservation(timurs.id))

        assertEquals(ReservationStatus.READY, snapshot().reservations.single().status)
        assertEquals(2, snapshot().reservations.single().memberId)
        assertEquals(false, repository.cancelReservation(timurs.id)) // already closed
    }

    @Test
    fun reservingFollowsTheRules() = runBlocking {
        assertEquals(ReserveResult.ItemIsAvailable, repository.reserve(1, 4))
        assertEquals(ReserveResult.AlreadyHasIt, repository.reserve(1, 3))
        assertEquals(ReserveResult.AlreadyReserved, repository.reserve(3, 3))
        assertEquals(ReserveResult.MemberNotFound, repository.reserve(99, 3))
        assertEquals(ReserveResult.Reserved(position = 2), repository.reserve(2, 3))
    }

    @Test
    fun renewingExtendsTheDueDateUnlessSomethingBlocksIt() = runBlocking {
        val wired = snapshot().loans.first { it.itemId == 7 } // Timur, magazine, 7 days
        val first = repository.renew(wired.id) as RenewResult.Renewed
        assertEquals(wired.dueAt + 7 * DAY_MS, first.loan.dueAt)
        assertEquals(1, first.loan.renewals)

        repository.renew(wired.id)
        assertEquals(RenewResult.Blocked(RenewBlock.LIMIT), repository.renew(wired.id))

        val dune = snapshot().loans.first { it.itemId == 3 && it.isActive }
        assertEquals(RenewResult.Blocked(RenewBlock.RESERVED), repository.renew(dune.id)) // Timur waits

        val overdue = snapshot().loans.first { it.itemId == 8 && it.isActive }
        assertEquals(RenewResult.Blocked(RenewBlock.OVERDUE), repository.renew(overdue.id))
        assertEquals(RenewResult.NotFound, repository.renew(9999))
    }

    @Test
    fun deletingAMemberPassesTheirReadyHoldOn() = runBlocking {
        repository.reserve(2, 3)
        repository.returnItem(3) // Timur READY, Dana WAITING
        repository.returnItem(7) // Timur gives back Wired, so he has no open loans

        assertEquals(DeleteResult.Deleted, repository.deleteMember(3))

        assertEquals(2, snapshot().reservations.single().memberId)
        assertEquals(ReservationStatus.READY, snapshot().reservations.single().status)
    }
}
