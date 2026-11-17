package kz.kbtu.library.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.components.LibraryItemCard
import kz.kbtu.library.ui.components.SearchField
import kz.kbtu.library.ui.components.TypeFilterRow
import kz.kbtu.library.ui.dialogs.MemberPickerDialog
import kz.kbtu.library.ui.viewmodel.CatalogUiState

// Search, filters, list of items and the add item button
@Composable
fun CatalogScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (ItemType?) -> Unit,
    onOnlyAvailableChange: (Boolean) -> Unit,
    onAddItem: () -> Unit,
    onBorrow: (memberId: Int, itemId: Int) -> Unit,
    onReturn: (itemId: Int) -> Unit,
    onItemClick: (itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Which item the member picker is open for. Survives rotation.
    var borrowItemId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Local copy of the search text so typing stays smooth
    var queryText by remember { mutableStateOf(state.query) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchField(
                        value = queryText,
                        onValueChange = {
                            queryText = it
                            onQueryChange(it)
                        }
                    )
                    TypeFilterRow(selected = state.typeFilter, onSelected = onTypeFilterChange)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.onlyAvailable,
                            onCheckedChange = onOnlyAvailableChange
                        )
                        Text(
                            text = "Only available",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { onOnlyAvailableChange(!state.onlyAvailable) }
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${state.entries.size} of ${state.totalItems}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.totalItems == 0 -> item(key = "empty-catalog") {
                    EmptyState(
                        title = "The catalog is empty",
                        message = "Tap \"Add item\" to put the first book, magazine or DVD on the shelf."
                    )
                }
                state.entries.isEmpty() -> item(key = "nothing-found") {
                    EmptyState(
                        title = "Nothing found",
                        message = "Try another search text or turn off some filters."
                    )
                }
                else -> items(state.entries, key = { it.item.id }) { entry ->
                    LibraryItemCard(
                        item = entry.item,
                        availability = entry.availability,
                        onClick = { onItemClick(entry.item.id) },
                        onBorrow = { borrowItemId = entry.item.id },
                        onReturn = { onReturn(entry.item.id) },
                        onPickUp = { memberId -> onBorrow(memberId, entry.item.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddItem,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add item") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    borrowItemId?.let { itemId ->
        state.entries.find { it.item.id == itemId }?.let { entry ->
            MemberPickerDialog(
                title = "Borrow \"${entry.item.title}\"",
                description = "Loan period: ${entry.item.loanDays()} days. Who takes it?",
                confirmLabel = "Borrow",
                members = state.members,
                mustBeAbleToBorrow = true,
                onDismiss = { borrowItemId = null },
                onConfirm = { memberId ->
                    onBorrow(memberId, itemId)
                    borrowItemId = null
                }
            )
        }
    }
}
