package kz.kbtu.library.model

data class LibraryStats(
    val totalItems: Int = 0,
    val availableItems: Int = 0,
    val itemsOnLoan: Int = 0,
    val totalPages: Int = 0,
    val longestBook: Book? = null,
    val countByType: Map<String, Int> = emptyMap(),
    val oldest: List<LibraryItem> = emptyList(),
    val genres: Set<String> = emptySet(),

    // Loan history
    val totalLoans: Int = 0,
    val overdueLoans: Int = 0,
    val openReservations: Int = 0,
    val finesOutstanding: Int = 0,
    val finesCollected: Int = 0,
    val mostBorrowed: List<Pair<LibraryItem, Int>> = emptyList()
)
