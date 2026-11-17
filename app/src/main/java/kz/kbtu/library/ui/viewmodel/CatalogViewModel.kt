package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.MemberStatus
import kz.kbtu.library.ui.Messenger

// One row of the catalog
data class CatalogEntry(val item: LibraryItem, val availability: Availability)

data class CatalogUiState(
    val entries: List<CatalogEntry> = emptyList(), // after search and filters
    val totalItems: Int = 0,
    val members: List<MemberStatus> = emptyList(),
    val query: String = "",
    val typeFilter: ItemType? = null,
    val onlyAvailable: Boolean = false,
    val isLoading: Boolean = true
)

class CatalogViewModel(
    repository: LibraryRepository,
    messenger: Messenger
) : ViewModel() {

    private val actions = LoanActions(repository, messenger)

    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<ItemType?>(null)
    private val onlyAvailable = MutableStateFlow(false)

    // Database changes and filter changes both end up here
    val uiState: StateFlow<CatalogUiState> = combine(
        repository.library, query, typeFilter, onlyAvailable
    ) { snapshot, query, type, onlyAvailable ->
        val all = snapshot.items.map { CatalogEntry(it, snapshot.availabilityOf(it.id)) }
        CatalogUiState(
            entries = all.filter { entry ->
                (type == null || entry.item.type == type) &&
                    (!onlyAvailable || entry.availability == Availability.Available) &&
                    (query.isBlank() || entry.item.matches(query.trim()))
            },
            totalItems = all.size,
            members = snapshot.memberStatuses(),
            query = query,
            typeFilter = type,
            onlyAvailable = onlyAvailable,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun onQueryChange(value: String) { query.value = value }

    fun onTypeFilterChange(type: ItemType?) { typeFilter.value = type }

    fun onOnlyAvailableChange(value: Boolean) { onlyAvailable.value = value }

    fun borrow(memberId: Int, itemId: Int) {
        viewModelScope.launch { actions.borrow(memberId, itemId) }
    }

    fun returnItem(itemId: Int) {
        viewModelScope.launch { actions.returnItem(itemId) }
    }
}
