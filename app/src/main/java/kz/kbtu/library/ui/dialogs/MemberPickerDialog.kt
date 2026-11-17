package kz.kbtu.library.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.MemberStatus
import kz.kbtu.library.ui.util.formatMoney

// Choose a member. When mustBeAbleToBorrow is true, members who reached the limit
// or owe a fine are shown but cannot be selected.
@Composable
fun MemberPickerDialog(
    title: String,
    description: String,
    confirmLabel: String,
    members: List<MemberStatus>,
    mustBeAbleToBorrow: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (memberId: Int) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (members.isEmpty()) {
                Text("There are no members yet. Register one on the Members tab first.")
            } else {
                Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    members.forEach { status ->
                        val enabled = !mustBeAbleToBorrow || status.canBorrow
                        val problem = when {
                            status.unpaidFine > 0 -> "owes ${formatMoney(status.unpaidFine)}"
                            status.atLimit -> "limit reached"
                            else -> null
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedId == status.member.id,
                                    enabled = enabled,
                                    onClick = { selectedId = status.member.id },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedId == status.member.id, onClick = null, enabled = enabled)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = status.member.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${status.activeLoans} of ${LoanPolicy.MAX_LOANS} loans" +
                                        (problem?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (problem != null) MaterialTheme.colorScheme.error
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
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
