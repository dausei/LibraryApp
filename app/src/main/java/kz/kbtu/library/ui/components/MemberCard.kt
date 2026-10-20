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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.Member

// A member with the loan limit bar and borrowed items
@Composable
fun MemberCard(
    member: Member,
    borrowedItems: List<LibraryItem>,
    maxLoans: Int,
    onReturn: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val limitReached = borrowedItems.size >= maxLoans

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
                        text = "${borrowedItems.size} of $maxLoans loans used",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (limitReached) colors.error else colors.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { borrowedItems.size / maxLoans.toFloat() },
                color = if (limitReached) colors.error else colors.primary,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            if (borrowedItems.isEmpty()) {
                Text(
                    text = "Nothing borrowed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                borrowedItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ItemTypeIcon(type = item.type, size = 36.dp)
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onReturn(item) }) { Text("Return") }
                    }
                }
            }
        }
    }
}
