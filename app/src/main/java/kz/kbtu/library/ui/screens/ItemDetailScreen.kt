package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.Member
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.components.ItemTypeIcon
import kz.kbtu.library.ui.components.SectionTitle
import kz.kbtu.library.ui.components.StatusBadge
import kz.kbtu.library.ui.dialogs.MemberPickerDialog

// Everything about one item with Borrow and Return
@Composable
fun ItemDetailScreen(
    item: LibraryItem?,
    borrower: Member?,
    members: List<Member>,
    loanCountOf: (Member) -> Int,
    maxLoans: Int,
    onBorrow: (memberId: Int) -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (item == null) {
        EmptyState(
            title = "Item not found",
            message = "It may have been removed from the catalog.",
            modifier = modifier
        )
        return
    }

    var showPicker by rememberSaveable { mutableStateOf(false) }

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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.attributes().forEach { (label, value) -> DetailRow(label, value) }
            }
        }

        SectionTitle("Availability")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusBadge(
                    text = if (borrower == null) "Available" else "Borrowed by ${borrower.name}",
                    available = borrower == null
                )
                if (borrower == null) {
                    Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Borrow")
                    }
                } else {
                    OutlinedButton(onClick = onReturn, modifier = Modifier.fillMaxWidth()) {
                        Text("Return to the library")
                    }
                }
            }
        }
    }

    if (showPicker) {
        MemberPickerDialog(
            item = item,
            members = members,
            loanCountOf = loanCountOf,
            maxLoans = maxLoans,
            onDismiss = { showPicker = false },
            onConfirm = { memberId ->
                onBorrow(memberId)
                showPicker = false
            }
        )
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
