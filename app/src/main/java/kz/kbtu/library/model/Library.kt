package kz.kbtu.library.model

import kotlinx.coroutines.delay

class Library {
    private val items = mutableListOf<LibraryItem>()
    private val genres = mutableSetOf<String>()
    private val members = mutableMapOf<Int, Member>()
    private val loans = mutableMapOf<Int, Loan>()

    companion object {
        const val MAX_LOANS = 3
    }

    fun addItem(item: LibraryItem) {
        items.add(item)
        if (item is Book) genres.add(item.genre)
    }

    fun addMember(member: Member) {
        members[member.id] = member
    }

    fun allItems(): List<LibraryItem> = items.toList()
    fun allMembers(): List<Member> = members.values.toList()
    fun findItem(itemId: Int): LibraryItem? = items.find { it.id == itemId }
    fun findMember(memberId: Int): Member? = members[memberId]
    fun nextItemId(): Int = (items.maxOfOrNull { it.id } ?: 0) + 1
    fun nextMemberId(): Int = (members.keys.maxOrNull() ?: 0) + 1
    fun allGenres(): Set<String> = genres.toSet()
    fun isAvailable(itemId: Int): Boolean = itemId !in loans
    fun activeLoans(): List<Loan> = loans.values.toList()

    // Returns the items that match the given condition
    fun search(predicate: (LibraryItem) -> Boolean): List<LibraryItem> =
        items.filter(predicate)

    // Lends an item if the rules allow it
    fun borrow(memberId: Int, itemId: Int): LoanResult {
        if (memberId !in members) return LoanResult.MemberNotFound
        val item = findItem(itemId) ?: return LoanResult.ItemNotFound

        loans[itemId]?.let { return LoanResult.AlreadyBorrowed(it.memberId) }
        if (loans.values.count { it.memberId == memberId } >= MAX_LOANS) {
            return LoanResult.LimitReached(MAX_LOANS)
        }

        val loan = Loan(itemId, memberId, item.loanDays())
        loans[itemId] = loan
        return LoanResult.Success(loan)
    }

    // Same as borrow, but waits a bit like a network call
    suspend fun borrowAsync(memberId: Int, itemId: Int): LoanResult {
        delay(300)
        return borrow(memberId, itemId)
    }

    fun returnItem(itemId: Int): Boolean = loans.remove(itemId) != null

    // Sum of pages of all books
    fun totalPages(): Int =
        items.filterIsInstance<Book>()
            .map { it.pages }
            .fold(0) { sum, pages -> sum + pages }

    // The book with the most pages
    fun longestBook(): Book? =
        items.filterIsInstance<Book>()
            .reduceOrNull { longest, book -> if (book.pages > longest.pages) book else longest }

    fun countByType(): Map<String, Int> =
        items.groupBy { it.type.label }
            .mapValues { (_, list) -> list.size }

    // The oldest items
    fun oldest(n: Int = 3): List<LibraryItem> =
        items.sortedBy { it.year }.take(n)

    // Numbers for the Stats screen
    fun stats(): LibraryStats = LibraryStats(
        totalItems = items.size,
        availableItems = items.count { isAvailable(it.id) },
        itemsOnLoan = loans.size,
        totalPages = totalPages(),
        longestBook = longestBook(),
        countByType = countByType(),
        oldest = oldest(),
        genres = allGenres()
    )
}
