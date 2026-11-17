package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.ReservationEntry
import kz.kbtu.library.ui.components.ActiveLoanCard
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.components.HistoryLoanCard
import kz.kbtu.library.ui.components.ReservationCard
import kz.kbtu.library.ui.viewmodel.LoansUiState

private enum class LoansTab(val title: String) { ACTIVE("Active"), QUEUE("Queue"), HISTORY("History") }

// The lending desk: open loans with renewals, the reservation queue and the history of returns
@Composable
fun LoansScreen(
    state: LoansUiState,
    onRenew: (loanId: Int) -> Unit,
    onReturn: (itemId: Int) -> Unit,
    onPickUp: (ReservationEntry) -> Unit,
    onCancelReservation: (reservationId: Int) -> Unit,
    onItemClick: (itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = LoansTab.entries
    val counts = listOf(state.active.size, state.reservations.size, state.history.size)

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text("${tab.title} (${counts[index]})") }
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tabs[selectedTab]) {
                LoansTab.ACTIVE ->
                    if (state.active.isEmpty()) {
                        item(key = "empty-active") {
                            EmptyState("Nothing on loan", "Borrow an item from the catalog and it appears here.")
                        }
                    } else {
                        items(state.active, key = { "loan-${it.loan.id}" }) { entry ->
                            ActiveLoanCard(
                                entry = entry,
                                now = state.now,
                                onClick = { onItemClick(entry.loan.itemId) },
                                onRenew = { onRenew(entry.loan.id) },
                                onReturn = { onReturn(entry.loan.itemId) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                LoansTab.QUEUE ->
                    if (state.reservations.isEmpty()) {
                        item(key = "empty-queue") {
                            EmptyState("No reservations", "Reserve a borrowed item from its detail screen to join the queue.")
                        }
                    } else {
                        items(state.reservations, key = { "reservation-${it.reservation.id}" }) { entry ->
                            ReservationCard(
                                entry = entry,
                                onClick = { onItemClick(entry.reservation.itemId) },
                                onPickUp = { onPickUp(entry) },
                                onCancel = { onCancelReservation(entry.reservation.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                LoansTab.HISTORY ->
                    if (state.history.isEmpty()) {
                        item(key = "empty-history") {
                            EmptyState("No history yet", "Returned loans are kept here, together with their fines.")
                        }
                    } else {
                        items(state.history, key = { "history-${it.loan.id}" }) { entry ->
                            HistoryLoanCard(
                                entry = entry,
                                now = state.now,
                                onClick = { onItemClick(entry.loan.itemId) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
            }
        }
    }
}
