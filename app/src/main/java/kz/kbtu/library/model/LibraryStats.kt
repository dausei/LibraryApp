package kz.kbtu.library.model

data class LibraryStats(
    val totalItems: Int = 0,
    val availableItems: Int = 0,
    val itemsOnLoan: Int = 0,
    val totalPages: Int = 0,
    val longestBook: Book? = null,
    val countByType: Map<String, Int> = emptyMap(),
    val oldest: List<LibraryItem> = emptyList(),
    val genres: Set<String> = emptySet()
)
