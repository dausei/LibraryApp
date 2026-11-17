package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.viewmodel.ItemFormErrors
import kz.kbtu.library.ui.viewmodel.ItemFormState
import kz.kbtu.library.ui.viewmodel.ItemFormValidator

// Form to create a book, magazine or DVD, or to edit an existing one
@Composable
fun ItemFormScreen(
    state: ItemFormState,
    onTypeChange: (ItemType) -> Unit,
    onTitleChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onPagesChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onIssueChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (state.notFound) {
        EmptyState(
            title = "Item not found",
            message = "It may have been removed from the catalog.",
            modifier = modifier
        )
        return
    }

    // Errors are shown only after the first attempt to save
    val errors = if (state.showErrors) ItemFormValidator.validate(state) else ItemFormErrors()

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemType.entries.forEach { option ->
                FilterChip(
                    selected = state.type == option,
                    onClick = { onTypeChange(option) },
                    enabled = !state.isEditing, // the type of an existing item cannot change
                    label = { Text(option.label) }
                )
            }
        }
        FormField("Title", state.title, onTitleChange, error = errors.title, capitalize = true)
        FormField("Year", state.year, onYearChange, error = errors.year, numeric = true)
        when (state.type) {
            ItemType.BOOK -> {
                FormField("Author", state.author, onAuthorChange, error = errors.author, capitalize = true)
                FormField("Pages", state.pages, onPagesChange, error = errors.pages, numeric = true)
                FormField("Genre (optional)", state.genre, onGenreChange, capitalize = true)
            }
            ItemType.MAGAZINE -> FormField("Issue number", state.issue, onIssueChange, error = errors.issue, numeric = true)
            ItemType.DVD -> FormField("Length in minutes", state.minutes, onMinutesChange, error = errors.minutes, numeric = true)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                Text(if (state.isEditing) "Save changes" else "Add to catalog")
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    numeric: Boolean = false,
    capitalize: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
