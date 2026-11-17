package kz.kbtu.library.model

// Who has an item right now
sealed interface Availability {
    data object Available : Availability

    // queueSize = members waiting for it
    data class OnLoan(
        val loan: Loan,
        val borrower: Member?,
        val status: LoanStatus,
        val queueSize: Int
    ) : Availability

    // Back in the library but kept for the first member in the queue
    data class Held(val reservation: Reservation, val member: Member?) : Availability
}

// A loan together with the item and the member it is about
data class LoanEntry(
    val loan: Loan,
    val item: LibraryItem?,
    val member: Member?,
    val status: LoanStatus,
    val renewBlock: RenewBlock?
) {
    val canRenew: Boolean get() = loan.isActive && renewBlock == null
}

// A reservation with its place in the queue (1 = first)
data class ReservationEntry(
    val reservation: Reservation,
    val item: LibraryItem?,
    val member: Member?,
    val position: Int
)

// Everything stored in the library at one moment. All derived data is computed here.
data class LibrarySnapshot(
    val items: List<LibraryItem> = emptyList(),
    val members: List<Member> = emptyList(),
    val loans: List<Loan> = emptyList(),               // all loans, newest first
    val reservations: List<Reservation> = emptyList(), // WAITING and READY only, oldest first
    val now: Long = 0L
) {
    private val itemsById = items.associateBy { it.id }
    private val membersById = members.associateBy { it.id }
    private val activeLoanByItem = loans.filter { it.isActive }.associateBy { it.itemId }

    fun item(id: Int): LibraryItem? = itemsById[id]
    fun member(id: Int): Member? = membersById[id]

    fun queueOf(itemId: Int): List<Reservation> = reservations.filter { it.itemId == itemId }

    fun availabilityOf(itemId: Int): Availability {
        val queue = queueOf(itemId)
        activeLoanByItem[itemId]?.let { loan ->
            return Availability.OnLoan(loan, member(loan.memberId), loan.status(now), queue.size)
        }
        queue.firstOrNull { it.status == ReservationStatus.READY }?.let { hold ->
            return Availability.Held(hold, member(hold.memberId))
        }
        return Availability.Available
    }

    fun statusOf(member: Member): MemberStatus = MemberStatus(
        member = member,
        activeLoans = loans.count { it.memberId == member.id && it.isActive },
        unpaidFine = loans.filter { it.memberId == member.id && it.hasUnpaidFine }.sumOf { it.fine }
    )

    fun memberStatuses(): List<MemberStatus> = members.map(::statusOf)

    fun loanEntry(loan: Loan): LoanEntry = LoanEntry(
        loan = loan,
        item = item(loan.itemId),
        member = member(loan.memberId),
        status = loan.status(now),
        renewBlock = if (loan.isActive) {
            LoanPolicy.renewBlock(loan, hasQueue = queueOf(loan.itemId).isNotEmpty(), now = now)
        } else null
    )

    fun activeLoanEntries(): List<LoanEntry> =
        loans.filter { it.isActive }.sortedBy { it.dueAt }.map(::loanEntry)

    fun historyEntries(): List<LoanEntry> =
        loans.filter { !it.isActive }.sortedByDescending { it.returnedAt }.map(::loanEntry)

    fun reservationEntries(itemId: Int? = null): List<ReservationEntry> =
        reservations
            .groupBy { it.itemId }
            .filterKeys { itemId == null || it == itemId }
            .flatMap { (id, queue) ->
                queue.mapIndexed { index, reservation ->
                    ReservationEntry(reservation, item(id), member(reservation.memberId), index + 1)
                }
            }
            .sortedBy { it.reservation.createdAt }

    fun stats(): LibraryStats {
        val books = items.filterIsInstance<Book>()
        return LibraryStats(
            totalItems = items.size,
            availableItems = items.count { availabilityOf(it.id) == Availability.Available },
            itemsOnLoan = activeLoanByItem.size,
            totalPages = books.sumOf { it.pages },
            longestBook = books.maxByOrNull { it.pages },
            countByType = items.groupBy { it.type.label }.mapValues { (_, list) -> list.size },
            oldest = items.sortedBy { it.year }.take(3),
            genres = books.map { it.genre }.toSet(),
            totalLoans = loans.size,
            overdueLoans = loans.count { it.status(now) == LoanStatus.OVERDUE },
            openReservations = reservations.size,
            finesOutstanding = loans.filter { it.hasUnpaidFine }.sumOf { it.fine },
            finesCollected = loans.filter { it.fine > 0 && it.finePaid }.sumOf { it.fine },
            mostBorrowed = loans.groupingBy { it.itemId }.eachCount().entries
                .mapNotNull { (id, count) -> item(id)?.let { it to count } }
                .sortedByDescending { it.second }
                .take(3)
        )
    }
}
