package kz.kbtu.library.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.LoanPolicy
import kz.kbtu.library.model.LoanStatus
import kz.kbtu.library.ui.util.dueText
import kz.kbtu.library.ui.util.formatMoney
import kz.kbtu.library.ui.util.plural
import kz.kbtu.library.ui.viewmodel.MemberRow

// A member with the loan limit bar, the borrowed items and the fines
@Composable
fun MemberCard(
    row: MemberRow,
    now: Long,
    onReturn: (itemId: Int) -> Unit,
    onPayFines: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val member = row.member
    val limitReached = row.status.atLimit

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = member.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${row.status.activeLoans} of ${LoanPolicy.MAX_LOANS} loans used" +
                            if (row.reservations > 0) " · ${plural(row.reservations, "reservation")}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (limitReached) colors.error else colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename ${member.name}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${member.name}")
                }
            }

            LinearProgressIndicator(
                progress = { row.status.activeLoans / LoanPolicy.MAX_LOANS.toFloat() },
                color = if (limitReached) colors.error else colors.primary,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            if (row.status.unpaidFine > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Owes ${formatMoney(row.status.unpaidFine)} · cannot borrow",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onPayFines) { Text("Pay") }
                }
            }

            if (row.borrowed.isEmpty()) {
                Text(
                    text = "Nothing borrowed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                row.borrowed.forEach { entry ->
                    val item = entry.item ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ItemTypeIcon(type = item.type, size = 36.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dueText(entry.loan, entry.status, now),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entry.status == LoanStatus.OVERDUE) colors.error else colors.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { onReturn(item.id) }) { Text("Return") }
                    }
                }
            }
        }
    }
}
