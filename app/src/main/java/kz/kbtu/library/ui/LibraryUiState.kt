package kz.kbtu.library.ui

import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.LibraryStats
import kz.kbtu.library.model.Loan
import kz.kbtu.library.model.Member

// Everything the UI needs to draw the screens
data class LibraryUiState(
    val items: List<LibraryItem> = emptyList(),
    val visibleItems: List<LibraryItem> = emptyList(),
    val members: List<Member> = emptyList(),
    val loans: Map<Int, Loan> = emptyMap(),
    val stats: LibraryStats = LibraryStats(),

    val query: String = "",
    val typeFilter: ItemType? = null,
    val onlyAvailable: Boolean = false,

    val isBusy: Boolean = false,
    val message: String? = null
) {
    fun borrowerOf(itemId: Int): Member? {
        val loan = loans[itemId] ?: return null
        return members.find { it.id == loan.memberId }
    }

    fun itemsBorrowedBy(memberId: Int): List<LibraryItem> =
        items.filter { loans[it.id]?.memberId == memberId }
}
