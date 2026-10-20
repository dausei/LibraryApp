package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.LibraryStats
import kz.kbtu.library.ui.components.ItemTypeIcon
import kz.kbtu.library.ui.components.SectionTitle
import kz.kbtu.library.ui.components.StatCard

// Statistics computed from the catalog
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    stats: LibraryStats,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = stats.totalItems.toString(),
                    label = "Items in catalog",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = stats.availableItems.toString(),
                    label = "Available now",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = stats.itemsOnLoan.toString(),
                    label = "On loan",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "%,d".format(stats.totalPages),
                    label = "Pages in all books",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SectionTitle("Longest book") }
        item {
            StatsSectionCard {
                val book = stats.longestBook
                if (book == null) {
                    Text("No books yet", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ItemTypeIcon(type = book.type)
                        Column {
                            Text(book.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${book.author} · ${book.pages} pages",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("Items by type") }
        item {
            StatsSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.countByType.forEach { (typeName, count) ->
                        Column {
                            Row {
                                Text(typeName, modifier = Modifier.weight(1f))
                                Text(count.toString(), style = MaterialTheme.typography.titleMedium)
                            }
                            LinearProgressIndicator(
                                progress = { count / stats.totalItems.toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("Three oldest items") }
        item {
            StatsSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.oldest.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ItemTypeIcon(type = item.type, size = 40.dp)
                            Text(item.title, modifier = Modifier.weight(1f))
                            Text(
                                item.year.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("Book genres") }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.genres.sorted().forEach { genre ->
                    AssistChip(onClick = {}, label = { Text(genre) })
                }
            }
        }
    }
}

@Composable
private fun StatsSectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}
