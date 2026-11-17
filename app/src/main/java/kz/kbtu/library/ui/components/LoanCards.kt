package kz.kbtu.library.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.LoanEntry
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.LoanStatus
import kz.kbtu.library.model.ReservationEntry
import kz.kbtu.library.model.ReservationStatus
import kz.kbtu.library.ui.util.dueText
import kz.kbtu.library.ui.util.formatDate
import kz.kbtu.library.ui.util.formatMoney

@Composable
private fun LoanCardFrame(
    entry: LoanEntry,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                entry.item?.let { ItemTypeIcon(type = it.type) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.item?.title ?: "Deleted item",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Borrowed by ${entry.member?.name ?: "unknown"} on ${formatDate(entry.loan.borrowedAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

// An open loan: due date, renew and return
@Composable
fun ActiveLoanCard(
    entry: LoanEntry,
    now: Long,
    onClick: () -> Unit,
    onRenew: () -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loan = entry.loan
    LoanCardFrame(entry, modifier, onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(
                text = dueText(loan, entry.status, now),
                tone = if (entry.status == LoanStatus.OVERDUE) BadgeTone.Danger else BadgeTone.Busy,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = "Renewed ${loan.renewals}/${LoanPolicy.MAX_RENEWALS}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Due ${formatDate(loan.dueAt)}" +
                if (entry.status == LoanStatus.OVERDUE) " · fine so far ${formatMoney(LoanPolicy.fineFor(loan.dueAt, now))}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (entry.status == LoanStatus.OVERDUE) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        entry.renewBlock?.let {
            Text(
                text = "Cannot renew: ${it.message.replaceFirstChar(Char::lowercase)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRenew, enabled = entry.canRenew) { Text("Renew") }
            Button(onClick = onReturn) { Text("Return") }
        }
    }
}

// A returned loan with its fine
@Composable
fun HistoryLoanCard(
    entry: LoanEntry,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loan = entry.loan
    LoanCardFrame(entry, modifier, onClick) {
        Text(
            text = dueText(loan, LoanStatus.RETURNED, now) + " · renewed ${loan.renewals}×",
            style = MaterialTheme.typography.bodyMedium
        )
        if (loan.fine > 0) {
            StatusBadge(
                text = "Fine ${formatMoney(loan.fine)} · " + if (loan.finePaid) "paid" else "unpaid",
                tone = if (loan.finePaid) BadgeTone.Warning else BadgeTone.Danger
            )
        } else {
            StatusBadge(text = "Returned on time", tone = BadgeTone.Available)
        }
    }
}

// A place in the queue for an item
@Composable
fun ReservationCard(
    entry: ReservationEntry,
    onClick: () -> Unit,
    onPickUp: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = entry.reservation.status == ReservationStatus.READY
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                entry.item?.let { ItemTypeIcon(type = it.type) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.item?.title ?: "Deleted item",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Reserved by ${entry.member?.name ?: "unknown"} on ${formatDate(entry.reservation.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(
                text = if (ready) "Ready until ${formatDate(entry.reservation.readyUntil ?: 0L)}"
                else "Waiting · place ${entry.position} in the queue",
                tone = if (ready) BadgeTone.Warning else BadgeTone.Busy
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ready) Button(onClick = onPickUp) { Text("Pick up") }
                TextButton(onClick = onCancel) { Text("Cancel reservation") }
            }
        }
    }
}
