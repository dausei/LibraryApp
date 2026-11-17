package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.ReservationStatus
import kz.kbtu.library.ui.components.AvailabilityBadge
import kz.kbtu.library.ui.components.ConfirmDialog
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.components.ItemTypeIcon
import kz.kbtu.library.ui.components.SectionTitle
import kz.kbtu.library.ui.dialogs.MemberPickerDialog
import kz.kbtu.library.ui.util.dueText
import kz.kbtu.library.ui.util.formatDate
import kz.kbtu.library.ui.viewmodel.ItemDetailUiState

private enum class PickerMode { BORROW, RESERVE }

// Everything about one item: its fields, who has it, the queue, and all actions on it
@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onBorrow: (memberId: Int) -> Unit,
    onReturn: () -> Unit,
    onRenew: (loanId: Int) -> Unit,
    onReserve: (memberId: Int) -> Unit,
    onCancelReservation: (reservationId: Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val item = state.item
    if (item == null) {
        EmptyState(
            title = "Item not found",
            message = "It may have been removed from the catalog.",
            modifier = modifier
        )
        return
    }

    // Dialogs survive rotation
    var pickerMode by rememberSaveable { mutableStateOf<PickerMode?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemTypeIcon(type = item.type, size = 72.dp)
            Column {
                Text(text = item.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = item.details(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionTitle("Details")
        DetailsCard {
            item.attributes().forEach { (label, value) -> DetailRow(label, value) }
        }

        SectionTitle("Availability")
        DetailsCard {
            AvailabilityBadge(state.availability)
            when (val availability = state.availability) {
                Availability.Available -> {
                    Button(onClick = { pickerMode = PickerMode.BORROW }, modifier = Modifier.fillMaxWidth()) {
                        Text("Borrow")
                    }
                }
                is Availability.OnLoan -> {
                    val loan = state.loan
                    Text(
                        text = "Borrowed ${formatDate(availability.loan.borrowedAt)} · due ${formatDate(availability.loan.dueAt)}" +
                            if (loan != null) " · ${dueText(loan.loan, loan.status, state.now)}" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    loan?.renewBlock?.let {
                        Text(
                            text = "Cannot renew: ${it.message.replaceFirstChar(Char::lowercase)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onReturn, modifier = Modifier.weight(1f)) { Text("Return") }
                        OutlinedButton(
                            onClick = { loan?.let { onRenew(it.loan.id) } },
                            enabled = loan?.canRenew == true,
                            modifier = Modifier.weight(1f)
                        ) { Text("Renew") }
                    }
                    OutlinedButton(onClick = { pickerMode = PickerMode.RESERVE }, modifier = Modifier.fillMaxWidth()) {
                        Text("Reserve for a member")
                    }
                }
                is Availability.Held -> {
                    Text(
                        text = "Waiting for ${availability.member?.name} until " +
                            formatDate(availability.reservation.readyUntil ?: 0L),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { onBorrow(availability.reservation.memberId) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pick up for ${availability.member?.name}") }
                    OutlinedButton(onClick = { pickerMode = PickerMode.RESERVE }, modifier = Modifier.fillMaxWidth()) {
                        Text("Reserve for a member")
                    }
                }
            }
        }

        if (state.queue.isNotEmpty()) {
            SectionTitle("Reservation queue")
            DetailsCard {
                state.queue.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${entry.position}. ${entry.member?.name}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (entry.reservation.status == ReservationStatus.READY) "Ready for pickup"
                                else "Waiting since ${formatDate(entry.reservation.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onCancelReservation(entry.reservation.id) }) { Text("Cancel") }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Edit", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { showDeleteDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Delete", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    pickerMode?.let { mode ->
        MemberPickerDialog(
            title = if (mode == PickerMode.BORROW) "Borrow \"${item.title}\"" else "Reserve \"${item.title}\"",
            description = if (mode == PickerMode.BORROW) "Loan period: ${item.loanDays()} days. Who takes it?"
            else "Who joins the queue? They get the item when it comes back.",
            confirmLabel = if (mode == PickerMode.BORROW) "Borrow" else "Reserve",
            members = state.members,
            mustBeAbleToBorrow = mode == PickerMode.BORROW,
            onDismiss = { pickerMode = null },
            onConfirm = { memberId ->
                if (mode == PickerMode.BORROW) onBorrow(memberId) else onReserve(memberId)
                pickerMode = null
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete \"${item.title}\"?",
            message = "The item and its loan history will be removed from the library. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
