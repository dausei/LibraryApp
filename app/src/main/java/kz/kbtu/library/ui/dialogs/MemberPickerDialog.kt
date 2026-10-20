package kz.kbtu.library.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.Member

// Choose who borrows the item
@Composable
fun MemberPickerDialog(
    item: LibraryItem,
    members: List<Member>,
    loanCountOf: (Member) -> Int,
    maxLoans: Int,
    onDismiss: () -> Unit,
    onConfirm: (memberId: Int) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrow \"${item.title}\"") },
        text = {
            if (members.isEmpty()) {
                Text("There are no members yet. Register one on the Members tab first.")
            } else {
                Column(modifier = Modifier.selectableGroup()) {
                    Text(
                        text = "Loan period: ${item.loanDays()} days. Who takes it?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    members.forEach { member ->
                        val count = loanCountOf(member)
                        val full = count >= maxLoans
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedId == member.id,
                                    onClick = { selectedId = member.id },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedId == member.id, onClick = null)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(member.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "$count of $maxLoans loans" + if (full) " · limit reached" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (full) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedId != null,
                onClick = { selectedId?.let(onConfirm) }
            ) { Text("Borrow") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
