package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.Library
import kz.kbtu.library.ui.LibraryUiState
import kz.kbtu.library.ui.components.MemberCard
import kz.kbtu.library.ui.components.SectionTitle

// Register members and see what they borrowed
@Composable
fun MembersScreen(
    state: LibraryUiState,
    onAddMember: (name: String) -> Boolean,
    onReturn: (itemId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var newName by rememberSaveable { mutableStateOf("") }

    fun submit() {
        if (onAddMember(newName)) newName = ""
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "add-member") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New member name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { submit() }) { Text("Add") }
            }
        }

        item(key = "title") { SectionTitle("${state.members.size} registered members") }

        items(state.members, key = { it.id }) { member ->
            MemberCard(
                member = member,
                borrowedItems = state.itemsBorrowedBy(member.id),
                maxLoans = Library.MAX_LOANS,
                onReturn = { item -> onReturn(item.id) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
