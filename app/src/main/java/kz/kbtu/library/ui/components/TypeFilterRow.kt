package kz.kbtu.library.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.ItemType

// Chips: All, Books, Magazines, DVDs
@Composable
fun TypeFilterRow(
    selected: ItemType?,
    onSelected: (ItemType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("All") }
        )
        ItemType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(if (selected == type) null else type) },
                label = { Text(type.label + "s") }
            )
        }
    }
}
