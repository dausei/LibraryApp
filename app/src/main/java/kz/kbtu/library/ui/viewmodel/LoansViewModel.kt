package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.LoanEntry
import kz.kbtu.library.model.ReservationEntry
import kz.kbtu.library.ui.Messenger

data class LoansUiState(
    val active: List<LoanEntry> = emptyList(),
    val reservations: List<ReservationEntry> = emptyList(),
    val history: List<LoanEntry> = emptyList(),
    val now: Long = 0L,
    val isLoading: Boolean = true
)

// The lending desk: open loans, the reservation queue and the history of returned loans
class LoansViewModel(
    repository: LibraryRepository,
    messenger: Messenger
) : ViewModel() {

    private val actions = LoanActions(repository, messenger)

    init {
        // Holds nobody picked up in time go to the next member in the queue
        viewModelScope.launch { repository.releaseExpiredHolds() }
    }

    val uiState: StateFlow<LoansUiState> = repository.library.map { snapshot ->
        LoansUiState(
            active = snapshot.activeLoanEntries(),
            reservations = snapshot.reservationEntries(),
            history = snapshot.historyEntries(),
            now = snapshot.now,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansUiState())

    fun renew(loanId: Int) {
        viewModelScope.launch { actions.renew(loanId) }
    }

    fun returnItem(itemId: Int) {
        viewModelScope.launch { actions.returnItem(itemId) }
    }

    fun cancelReservation(reservationId: Int) {
        viewModelScope.launch { actions.cancelReservation(reservationId) }
    }

    // The member whose turn it is takes the item home
    fun pickUp(entry: ReservationEntry) {
        viewModelScope.launch { actions.borrow(entry.reservation.memberId, entry.reservation.itemId) }
    }
}
