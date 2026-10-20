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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.Library
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.ui.LibraryUiState
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.components.LibraryItemCard
import kz.kbtu.library.ui.components.SearchField
import kz.kbtu.library.ui.components.TypeFilterRow
import kz.kbtu.library.ui.dialogs.AddItemDialog
import kz.kbtu.library.ui.dialogs.MemberPickerDialog

// Search, filters, list of items and the add item button
@Composable
fun CatalogScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (ItemType?) -> Unit,
    onOnlyAvailableChange: (Boolean) -> Unit,
    onAddItem: (NewItemData) -> Unit,
    onBorrow: (memberId: Int, itemId: Int) -> Unit,
    onReturn: (itemId: Int) -> Unit,
    onItemClick: (itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog state survives rotation
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var borrowItemId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Local copy of the search text so typing stays smooth
    var queryText by remember { mutableStateOf(state.query) }

    val listState = rememberLazyListState()

    var knownItemCount by remember { mutableIntStateOf(state.items.size) }
    LaunchedEffect(state.items.size) {
        if (state.items.size > knownItemCount && state.visibleItems.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleItems.size)
        }
        knownItemCount = state.items.size
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
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
                            text = "${state.visibleItems.size} of ${state.items.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.visibleItems.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "Nothing found",
                        message = "Try another search text or turn off some filters."
                    )
                }
            } else {
                items(state.visibleItems, key = { it.id }) { item ->
                    LibraryItemCard(
                        item = item,
                        borrower = state.borrowerOf(item.id),
                        onClick = { onItemClick(item.id) },
                        onBorrow = { borrowItemId = item.id },
                        onReturn = { onReturn(item.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add item") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                onAddItem(newItem)
                showAddDialog = false
            }
        )
    }

    borrowItemId?.let { itemId ->
        state.items.find { it.id == itemId }?.let { item ->
            MemberPickerDialog(
                item = item,
                members = state.members,
                loanCountOf = { member -> state.itemsBorrowedBy(member.id).size },
                maxLoans = Library.MAX_LOANS,
                onDismiss = { borrowItemId = null },
                onConfirm = { memberId ->
                    onBorrow(memberId, itemId)
                    borrowItemId = null
                }
            )
        }
    }
}
