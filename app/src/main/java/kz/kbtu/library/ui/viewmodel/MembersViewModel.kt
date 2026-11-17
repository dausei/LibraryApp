package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.LoanEntry
import kz.kbtu.library.model.Member
import kz.kbtu.library.model.MemberResult
import kz.kbtu.library.model.MemberStatus
import kz.kbtu.library.ui.Messenger

data class MemberRow(
    val status: MemberStatus,
    val borrowed: List<LoanEntry>,
    val reservations: Int
) {
    val member: Member get() = status.member
}

// The add / rename dialog. memberId == null means a new member.
data class MemberEditor(
    val memberId: Int? = null,
    val name: String = "",
    val error: String? = null
)

data class MembersUiState(
    val rows: List<MemberRow> = emptyList(),
    val editor: MemberEditor? = null,
    val now: Long = 0L,
    val isLoading: Boolean = true
)

class MembersViewModel(
    private val repository: LibraryRepository,
    private val messenger: Messenger
) : ViewModel() {

    private val actions = LoanActions(repository, messenger)
    private val editor = MutableStateFlow<MemberEditor?>(null)

    val uiState: StateFlow<MembersUiState> = combine(repository.library, editor) { snapshot, editor ->
        val activeLoans = snapshot.activeLoanEntries()
        MembersUiState(
            rows = snapshot.members.map { member ->
                MemberRow(
                    status = snapshot.statusOf(member),
                    borrowed = activeLoans.filter { it.loan.memberId == member.id },
                    reservations = snapshot.reservations.count { it.memberId == member.id }
                )
            },
            editor = editor,
            now = snapshot.now,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MembersUiState())

    fun startAdding() { editor.value = MemberEditor() }

    fun startRenaming(member: Member) { editor.value = MemberEditor(member.id, member.name) }

    fun onEditorNameChange(name: String) = editor.update { it?.copy(name = name.take(40), error = null) }

    fun dismissEditor() { editor.value = null }

    // Closes the dialog on success, otherwise shows the problem under the text field
    fun saveEditor() {
        val current = editor.value ?: return
        viewModelScope.launch {
            val result = if (current.memberId == null) {
                repository.addMember(current.name)
            } else {
                repository.renameMember(current.memberId, current.name)
            }
            when (result) {
                is MemberResult.Saved -> {
                    editor.value = null
                    messenger.show(
                        if (current.memberId == null) "Member \"${result.member.name}\" registered"
                        else "Renamed to \"${result.member.name}\""
                    )
                }
                MemberResult.EmptyName -> editor.update { it?.copy(error = "Enter a name first") }
                MemberResult.Duplicate -> editor.update { it?.copy(error = "A member with this name already exists") }
                MemberResult.NotFound -> {
                    editor.value = null
                    messenger.show("This member no longer exists")
                }
            }
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            messenger.show(
                when (val result = repository.deleteMember(member.id)) {
                    DeleteResult.Deleted -> "Deleted member \"${member.name}\""
                    is DeleteResult.HasActiveLoans -> "${member.name} still has ${result.count} borrowed. Return them first"
                    else -> "This member no longer exists"
                }
            )
        }
    }

    fun returnItem(itemId: Int) {
        viewModelScope.launch { actions.returnItem(itemId) }
    }

    fun payFines(memberId: Int) {
        viewModelScope.launch { actions.payFines(memberId) }
    }
}
