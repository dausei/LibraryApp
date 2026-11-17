package kz.kbtu.library.data.repository

import androidx.room.withTransaction
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kz.kbtu.library.data.local.LibraryDatabase
import kz.kbtu.library.data.local.LoanEntity
import kz.kbtu.library.data.local.MemberEntity
import kz.kbtu.library.data.local.ReservationEntity
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.LibrarySnapshot
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.Member
import kz.kbtu.library.model.MemberResult
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.model.RenewResult
import kz.kbtu.library.model.ReservationStatus
import kz.kbtu.library.model.ReserveResult
import kz.kbtu.library.model.ReturnResult

// Room-backed repository. Every operation that touches several tables runs in one transaction,
// so the data is never left half changed.
class RoomLibraryRepository(
    private val db: LibraryDatabase,
    private val clock: Clock = Clock.systemDefaultZone()
) : LibraryRepository {

    private val itemDao = db.itemDao()
    private val memberDao = db.memberDao()
    private val loanDao = db.loanDao()
    private val reservationDao = db.reservationDao()

    override val library: Flow<LibrarySnapshot> = combine(
        itemDao.observeAll(),
        memberDao.observeAll(),
        loanDao.observeAll(),
        reservationDao.observeOpen()
    ) { items, members, loans, reservations ->
        LibrarySnapshot(
            items = items.map { it.toDomain() },
            members = members.map { it.toDomain() },
            loans = loans.map { it.toDomain() },
            reservations = reservations.map { it.toDomain() },
            now = clock.millis()
        )
    }

    // ---- Items ----

    override suspend fun getItem(id: Int): LibraryItem? = itemDao.find(id)?.toDomain()

    override suspend fun addItem(data: NewItemData): Int = itemDao.insert(data.toEntity()).toInt()

    override suspend fun updateItem(id: Int, data: NewItemData) = itemDao.update(data.toEntity(id))

    override suspend fun deleteItem(id: Int): DeleteResult = db.withTransaction {
        if (itemDao.find(id) == null) return@withTransaction DeleteResult.NotFound
        if (loanDao.activeLoanOfItem(id) != null) return@withTransaction DeleteResult.OnLoan
        itemDao.delete(id) // loans and reservations of the item are removed by the foreign keys
        DeleteResult.Deleted
    }

    // ---- Members ----

    override suspend fun addMember(name: String): MemberResult {
        val clean = name.trim()
        if (clean.isEmpty()) return MemberResult.EmptyName
        return db.withTransaction {
            if (memberDao.countWithName(clean, exceptId = 0) > 0) return@withTransaction MemberResult.Duplicate
            val id = memberDao.insert(MemberEntity(name = clean)).toInt()
            MemberResult.Saved(Member(id, clean))
        }
    }

    override suspend fun renameMember(id: Int, name: String): MemberResult {
        val clean = name.trim()
        if (clean.isEmpty()) return MemberResult.EmptyName
        return db.withTransaction {
            val member = memberDao.find(id) ?: return@withTransaction MemberResult.NotFound
            if (memberDao.countWithName(clean, exceptId = id) > 0) return@withTransaction MemberResult.Duplicate
            memberDao.update(member.copy(name = clean))
            MemberResult.Saved(Member(id, clean))
        }
    }

    override suspend fun deleteMember(id: Int): DeleteResult = db.withTransaction {
        if (memberDao.find(id) == null) return@withTransaction DeleteResult.NotFound
        val active = loanDao.activeCount(id)
        if (active > 0) return@withTransaction DeleteResult.HasActiveLoans(active)

        // Items kept for this member go to the next one in the queue
        val now = clock.millis()
        reservationDao.readyForMember(id).forEach { hold ->
            reservationDao.update(hold.copy(status = ReservationStatus.CANCELLED, readyUntil = null))
            promoteNextInQueue(hold.itemId, now)
        }
        memberDao.delete(id) // the member's loan history and reservations go with them
        DeleteResult.Deleted
    }

    // Returns the paid amount
    override suspend fun payFines(memberId: Int): Int = db.withTransaction {
        val amount = loanDao.unpaidFines(memberId)
        if (amount > 0) loanDao.payFines(memberId)
        amount
    }

    // ---- Loans ----

    override suspend fun borrow(memberId: Int, itemId: Int): LoanResult = db.withTransaction {
        releaseExpiredHoldsInTransaction()
        val now = clock.millis()

        if (memberDao.find(memberId) == null) return@withTransaction LoanResult.MemberNotFound
        val item = itemDao.find(itemId)?.toDomain() ?: return@withTransaction LoanResult.ItemNotFound

        loanDao.activeLoanOfItem(itemId)?.let { return@withTransaction LoanResult.AlreadyBorrowed(it.memberId) }

        // An item kept for another member cannot be taken
        val queue = reservationDao.openForItem(itemId)
        val hold = queue.firstOrNull { it.status == ReservationStatus.READY }
        if (hold != null && hold.memberId != memberId) {
            return@withTransaction LoanResult.HeldForOther(hold.memberId)
        }

        val fines = loanDao.unpaidFines(memberId)
        if (fines > 0) return@withTransaction LoanResult.UnpaidFines(fines)
        if (loanDao.activeCount(memberId) >= LoanPolicy.MAX_LOANS) {
            return@withTransaction LoanResult.LimitReached(LoanPolicy.MAX_LOANS)
        }

        val loan = LoanEntity(
            itemId = itemId,
            memberId = memberId,
            borrowedAt = now,
            dueAt = LoanPolicy.dueDate(now, item.loanDays())
        )
        val id = loanDao.insert(loan).toInt()

        // The member's own reservation for this item is done now
        queue.filter { it.memberId == memberId }.forEach {
            reservationDao.update(it.copy(status = ReservationStatus.FULFILLED))
        }
        LoanResult.Success(loan.copy(id = id).toDomain())
    }

    override suspend fun returnItem(itemId: Int): ReturnResult = db.withTransaction {
        val now = clock.millis()
        val loan = loanDao.activeLoanOfItem(itemId) ?: return@withTransaction ReturnResult.NotBorrowed

        val returned = loan.copy(returnedAt = now, fine = LoanPolicy.fineFor(loan.dueAt, now))
        loanDao.update(returned)

        val next = promoteNextInQueue(itemId, now)
        ReturnResult.Returned(returned.toDomain(), next?.let { memberDao.find(it.memberId)?.toDomain() })
    }

    override suspend fun renew(loanId: Int): RenewResult = db.withTransaction {
        val now = clock.millis()
        val loan = loanDao.find(loanId)
        if (loan == null || loan.returnedAt != null) return@withTransaction RenewResult.NotFound
        val item = itemDao.find(loan.itemId)?.toDomain() ?: return@withTransaction RenewResult.NotFound

        val hasQueue = reservationDao.openForItem(loan.itemId).isNotEmpty()
        LoanPolicy.renewBlock(loan.toDomain(), hasQueue, now)?.let {
            return@withTransaction RenewResult.Blocked(it)
        }

        val renewed = loan.copy(
            dueAt = LoanPolicy.dueDate(loan.dueAt, item.loanDays()),
            renewals = loan.renewals + 1
        )
        loanDao.update(renewed)
        RenewResult.Renewed(renewed.toDomain())
    }

    // ---- Reservations ----

    override suspend fun reserve(memberId: Int, itemId: Int): ReserveResult = db.withTransaction {
        releaseExpiredHoldsInTransaction()
        val now = clock.millis()

        if (memberDao.find(memberId) == null) return@withTransaction ReserveResult.MemberNotFound
        if (itemDao.find(itemId) == null) return@withTransaction ReserveResult.ItemNotFound

        val loan = loanDao.activeLoanOfItem(itemId)
        if (loan?.memberId == memberId) return@withTransaction ReserveResult.AlreadyHasIt

        val queue = reservationDao.openForItem(itemId)
        if (queue.any { it.memberId == memberId }) return@withTransaction ReserveResult.AlreadyReserved
        // Nothing to wait for: the item is on the shelf and nobody holds it
        if (loan == null && queue.isEmpty()) return@withTransaction ReserveResult.ItemIsAvailable

        reservationDao.insert(
            ReservationEntity(itemId = itemId, memberId = memberId, createdAt = now, status = ReservationStatus.WAITING)
        )
        ReserveResult.Reserved(position = queue.size + 1)
    }

    override suspend fun cancelReservation(reservationId: Int): Boolean = db.withTransaction {
        val reservation = reservationDao.find(reservationId)
        if (reservation == null || reservation.status !in OPEN) return@withTransaction false

        reservationDao.update(reservation.copy(status = ReservationStatus.CANCELLED, readyUntil = null))
        // The hold is free again, so the next member in the queue gets the item
        if (reservation.status == ReservationStatus.READY) {
            promoteNextInQueue(reservation.itemId, clock.millis())
        }
        true
    }

    override suspend fun releaseExpiredHolds() = db.withTransaction { releaseExpiredHoldsInTransaction() }

    // A ready item that nobody picked up in time goes to the next member
    private suspend fun releaseExpiredHoldsInTransaction() {
        val now = clock.millis()
        reservationDao.expiredHolds(now).forEach { hold ->
            reservationDao.update(hold.copy(status = ReservationStatus.CANCELLED, readyUntil = null))
            promoteNextInQueue(hold.itemId, now)
        }
    }

    // First waiting reservation of the item becomes READY for the hold period
    private suspend fun promoteNextInQueue(itemId: Int, now: Long): ReservationEntity? {
        val next = reservationDao.openForItem(itemId).firstOrNull { it.status == ReservationStatus.WAITING }
            ?: return null
        val ready = next.copy(status = ReservationStatus.READY, readyUntil = LoanPolicy.holdUntil(now))
        reservationDao.update(ready)
        return ready
    }

    private companion object {
        val OPEN = setOf(ReservationStatus.WAITING, ReservationStatus.READY)
    }
}
