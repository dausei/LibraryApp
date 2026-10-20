package kz.kbtu.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.library.data.DataLoader
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.Library
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.Member
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.model.toItem

// Holds the screen state and handles user actions
class LibraryViewModel : ViewModel() {

    private val library: Library = DataLoader.load()

    // Only the ViewModel changes the state
    private val _uiState = MutableStateFlow(LibraryUiState().refreshed())

    // The UI observes this one
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    // Search and filters
    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query).refreshed() }

    fun onTypeFilterChange(type: ItemType?) = _uiState.update { it.copy(typeFilter = type).refreshed() }

    fun onOnlyAvailableChange(value: Boolean) = _uiState.update { it.copy(onlyAvailable = value).refreshed() }

    // Adds a new item to the catalog
    fun addItem(data: NewItemData) {
        val item = data.toItem(library.nextItemId())
        library.addItem(item)
        _uiState.update { it.copy(message = "Added \"${item.title}\" to the catalog").refreshed() }
    }

    // Returns true if the member was registered
    fun addMember(name: String): Boolean {
        val clean = name.trim()
        val error = when {
            clean.isEmpty() -> "Enter a name first"
            library.allMembers().any { it.name.equals(clean, ignoreCase = true) } ->
                "Member \"$clean\" already exists"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(message = error) }
            return false
        }
        library.addMember(Member(library.nextMemberId(), clean))
        _uiState.update { it.copy(message = "Member \"$clean\" registered").refreshed() }
        return true
    }

    // Runs in viewModelScope, so it is cancelled together with the ViewModel
    fun borrow(memberId: Int, itemId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = library.borrowAsync(memberId, itemId)
            _uiState.update { it.copy(isBusy = false, message = describe(result)).refreshed() }
        }
    }

    fun returnItem(itemId: Int) {
        val title = library.findItem(itemId)?.title
        val message = if (library.returnItem(itemId)) "Returned \"$title\"" else "This item is not borrowed"
        _uiState.update { it.copy(message = message).refreshed() }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    // Rebuilds the derived fields after every change
    private fun LibraryUiState.refreshed(): LibraryUiState {
        val matches: (LibraryItem) -> Boolean = { item ->
            (typeFilter == null || item.type == typeFilter) &&
                (!onlyAvailable || library.isAvailable(item.id)) &&
                (query.isBlank() || item.matches(query.trim()))
        }
        return copy(
            items = library.allItems(),
            visibleItems = library.search(matches),
            members = library.allMembers(),
            loans = library.activeLoans().associateBy { it.itemId },
            stats = library.stats()
        )
    }

    // Turns a loan result into a message for the Snackbar
    private fun describe(result: LoanResult): String = when (result) {
        is LoanResult.Success -> {
            val who = library.findMember(result.loan.memberId)?.name
            val what = library.findItem(result.loan.itemId)?.title
            "$who borrowed \"$what\" for ${result.loan.days} days"
        }
        is LoanResult.AlreadyBorrowed ->
            "Already borrowed by ${library.findMember(result.byMemberId)?.name}"
        is LoanResult.LimitReached -> "Limit reached: at most ${result.limit} items per member"
        LoanResult.ItemNotFound -> "No such item"
        LoanResult.MemberNotFound -> "No such member"
    }
}
