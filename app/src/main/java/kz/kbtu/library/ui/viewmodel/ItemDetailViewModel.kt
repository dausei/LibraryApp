package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.LoanEntry
import kz.kbtu.library.model.MemberStatus
import kz.kbtu.library.model.ReservationEntry
import kz.kbtu.library.ui.Messenger
import kz.kbtu.library.ui.navigation.Screen

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: LibraryItem? = null, // null after loading means the item does not exist (any more)
    val availability: Availability = Availability.Available,
    val loan: LoanEntry? = null,
    val queue: List<ReservationEntry> = emptyList(),
    val members: List<MemberStatus> = emptyList(),
    val now: Long = 0L
)

class ItemDetailViewModel(
    private val repository: LibraryRepository,
    private val messenger: Messenger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The id comes from the navigation route
    private val itemId: Int = checkNotNull(savedStateHandle[Screen.ItemDetail.ARG_ITEM_ID])

    private val actions = LoanActions(repository, messenger)

    val uiState: StateFlow<ItemDetailUiState> = repository.library.map { snapshot ->
        val availability = snapshot.availabilityOf(itemId)
        ItemDetailUiState(
            isLoading = false,
            item = snapshot.item(itemId),
            availability = availability,
            loan = (availability as? Availability.OnLoan)?.let { snapshot.loanEntry(it.loan) },
            queue = snapshot.reservationEntries(itemId),
            members = snapshot.memberStatuses(),
            now = snapshot.now
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemDetailUiState())

    // Tells the screen to go back after the item was deleted
    private val closeChannel = Channel<Unit>(Channel.BUFFERED)
    val closeEvents: Flow<Unit> = closeChannel.receiveAsFlow()

    fun borrow(memberId: Int) {
        viewModelScope.launch { actions.borrow(memberId, itemId) }
    }

    fun returnItem() {
        viewModelScope.launch { actions.returnItem(itemId) }
    }

    fun renew(loanId: Int) {
        viewModelScope.launch { actions.renew(loanId) }
    }

    fun reserve(memberId: Int) {
        viewModelScope.launch { actions.reserve(memberId, itemId) }
    }

    fun cancelReservation(reservationId: Int) {
        viewModelScope.launch { actions.cancelReservation(reservationId) }
    }

    fun deleteItem() {
        val title = uiState.value.item?.title
        viewModelScope.launch {
            when (repository.deleteItem(itemId)) {
                DeleteResult.Deleted -> {
                    messenger.show("Deleted \"$title\"")
                    closeChannel.send(Unit)
                }
                DeleteResult.OnLoan -> messenger.show("Return \"$title\" before deleting it")
                else -> closeChannel.send(Unit)
            }
        }
    }
}
