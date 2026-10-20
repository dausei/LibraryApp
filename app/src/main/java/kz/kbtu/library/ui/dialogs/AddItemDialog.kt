package kz.kbtu.library.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Year
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.NewItemData

// Form for a new book, magazine or DVD
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (NewItemData) -> Unit
) {
    // Form fields survive rotation
    var type by rememberSaveable { mutableStateOf(ItemType.BOOK) }
    var title by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var pages by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var issue by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val currentYear = remember { Year.now().value }

    // Validation of the fields
    val titleOk = title.isNotBlank()
    val yearValue = year.toIntOrNull()?.takeIf { it in 1000..currentYear }
    val authorOk = author.isNotBlank()
    val pagesValue = pages.toIntOrNull()?.takeIf { it in 1..5000 }
    val issueValue = issue.toIntOrNull()?.takeIf { it >= 1 }
    val minutesValue = minutes.toIntOrNull()?.takeIf { it in 1..1000 }

    fun buildItem(): NewItemData? {
        if (!titleOk || yearValue == null) return null
        return when (type) {
            ItemType.BOOK ->
                if (authorOk && pagesValue != null) {
                    NewItemData.NewBook(title.trim(), yearValue, author.trim(), pagesValue, genre.trim().ifBlank { "General" })
                } else null
            ItemType.MAGAZINE -> issueValue?.let { NewItemData.NewMagazine(title.trim(), yearValue, it) }
            ItemType.DVD -> minutesValue?.let { NewItemData.NewDvd(title.trim(), yearValue, it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                FormField("Title", title, { title = it }, error = "Title is required".takeIf { submitted && !titleOk })
                FormField(
                    "Year", year, { year = it },
                    error = "Enter a year from 1000 to $currentYear".takeIf { submitted && yearValue == null },
                    numeric = true, maxLength = 4
                )
                when (type) {
                    ItemType.BOOK -> {
                        FormField("Author", author, { author = it }, error = "Author is required".takeIf { submitted && !authorOk })
                        FormField(
                            "Pages", pages, { pages = it },
                            error = "Enter 1 to 5000".takeIf { submitted && pagesValue == null },
                            numeric = true, maxLength = 4
                        )
                        FormField("Genre (optional)", genre, { genre = it })
                    }
                    ItemType.MAGAZINE -> FormField(
                        "Issue number", issue, { issue = it },
                        error = "Enter a number".takeIf { submitted && issueValue == null },
                        numeric = true, maxLength = 4
                    )
                    ItemType.DVD -> FormField(
                        "Length in minutes", minutes, { minutes = it },
                        error = "Enter 1 to 1000".takeIf { submitted && minutesValue == null },
                        numeric = true, maxLength = 4
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                buildItem()?.let(onConfirm)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    numeric: Boolean = false,
    maxLength: Int = 60
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.length <= maxLength && (!numeric || new.all(Char::isDigit))) onValueChange(new)
        },
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth()
    )
}
