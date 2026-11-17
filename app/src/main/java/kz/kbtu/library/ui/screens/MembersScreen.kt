package kz.kbtu.library.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.library.model.Member
import kz.kbtu.library.ui.components.ConfirmDialog
import kz.kbtu.library.ui.components.EmptyState
import kz.kbtu.library.ui.components.MemberCard
import kz.kbtu.library.ui.components.SectionTitle
import kz.kbtu.library.ui.dialogs.MemberEditorDialog
import kz.kbtu.library.ui.viewmodel.MembersUiState

// Register, rename and delete members, see what they borrowed and pay their fines
@Composable
fun MembersScreen(
    state: MembersUiState,
    onAddMember: () -> Unit,
    onRenameMember: (Member) -> Unit,
    onEditorNameChange: (String) -> Unit,
    onSaveEditor: () -> Unit,
    onDismissEditor: () -> Unit,
    onDeleteMember: (Member) -> Unit,
    onReturn: (itemId: Int) -> Unit,
    onPayFines: (memberId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // The member the delete dialog is open for. Survives rotation.
    var deleteMemberId by rememberSaveable { mutableStateOf<Int?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isLoading -> item(key = "loading") {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.rows.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        title = "No members yet",
                        message = "Register a member to start lending items."
                    )
                }
                else -> {
                    item(key = "title") { SectionTitle("${state.rows.size} registered members") }
                    items(state.rows, key = { it.member.id }) { row ->
                        MemberCard(
                            row = row,
                            now = state.now,
                            onReturn = onReturn,
                            onPayFines = { onPayFines(row.member.id) },
                            onRename = { onRenameMember(row.member) },
                            onDelete = { deleteMemberId = row.member.id },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddMember,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add member") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    state.editor?.let { editor ->
        MemberEditorDialog(
            editor = editor,
            onNameChange = onEditorNameChange,
            onSave = onSaveEditor,
            onDismiss = onDismissEditor
        )
    }

    deleteMemberId?.let { id ->
        state.rows.find { it.member.id == id }?.let { row ->
            ConfirmDialog(
                title = "Delete \"${row.member.name}\"?",
                message = "The member and their loan history will be removed. This cannot be undone.",
                confirmLabel = "Delete",
                onConfirm = {
                    onDeleteMember(row.member)
                    deleteMemberId = null
                },
                onDismiss = { deleteMemberId = null }
            )
        }
    }
}
