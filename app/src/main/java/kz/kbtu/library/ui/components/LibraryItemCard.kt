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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.Book
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.ui.theme.LibraryTheme
import kz.kbtu.library.ui.util.formatDate

// One row of the catalog
@Composable
fun LibraryItemCard(
    item: LibraryItem,
    availability: Availability,
    onClick: () -> Unit,
    onBorrow: () -> Unit,
    onReturn: () -> Unit,
    onPickUp: (memberId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemTypeIcon(type = item.type)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.details(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.type.label} · ${item.year} · ${item.loanDays()}-day loan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvailabilityBadge(availability, modifier = Modifier.weight(1f, fill = false))
                when (availability) {
                    Availability.Available -> Button(onClick = onBorrow) { Text("Borrow") }
                    is Availability.OnLoan -> OutlinedButton(onClick = onReturn) { Text("Return") }
                    is Availability.Held -> Button(onClick = { onPickUp(availability.reservation.memberId) }) {
                        Text("Pick up")
                    }
                }
            }
            if (availability is Availability.OnLoan) {
                val queue = if (availability.queueSize > 0) " · ${availability.queueSize} waiting" else ""
                Text(
                    text = "Due ${formatDate(availability.loan.dueAt)}$queue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryItemCardPreview() {
    LibraryTheme {
        LibraryItemCard(
            item = Book(1, "Clean Code", 2008, "Robert Martin", 464, "Programming"),
            availability = Availability.Available,
            onClick = {},
            onBorrow = {},
            onReturn = {},
            onPickUp = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
