package kz.kbtu.library.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.Book
import kz.kbtu.library.model.Dvd
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.Magazine
import kz.kbtu.library.ui.Messenger
import kz.kbtu.library.ui.navigation.Screen

// Create and edit form of an item. The same screen is used for both: the route decides.
class ItemFormViewModel(
    private val repository: LibraryRepository,
    private val messenger: Messenger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editedId: Int? = savedStateHandle.get<Int>(Screen.ItemForm.ARG_ITEM_ID)
        ?.takeIf { it != Screen.ItemForm.NEW }

    private val _state = MutableStateFlow(ItemFormState(itemId = editedId, isLoading = editedId != null))
    val uiState: StateFlow<ItemFormState> = _state.asStateFlow()

    // Tells the screen to close after saving
    private val closeChannel = Channel<Unit>(Channel.BUFFERED)
    val closeEvents: Flow<Unit> = closeChannel.receiveAsFlow()

    init {
        if (editedId != null) {
            viewModelScope.launch {
                val item = repository.getItem(editedId)
                _state.update { if (item == null) it.copy(isLoading = false, notFound = true) else item.toFormState(editedId) }
            }
        }
    }

    // Fields cannot be changed to something invalid, e.g. letters in a number field
    fun onTypeChange(type: ItemType) = _state.update { it.copy(type = type) }
    fun onTitleChange(value: String) = _state.update { it.copy(title = value.take(MAX_TEXT)) }
    fun onYearChange(value: String) = _state.update { it.copy(year = value.digits(4)) }
    fun onAuthorChange(value: String) = _state.update { it.copy(author = value.take(MAX_TEXT)) }
    fun onPagesChange(value: String) = _state.update { it.copy(pages = value.digits(4)) }
    fun onGenreChange(value: String) = _state.update { it.copy(genre = value.take(MAX_TEXT)) }
    fun onIssueChange(value: String) = _state.update { it.copy(issue = value.digits(4)) }
    fun onMinutesChange(value: String) = _state.update { it.copy(minutes = value.digits(4)) }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        val data = ItemFormValidator.toItemData(current)
        if (data == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (editedId == null) {
                repository.addItem(data)
                messenger.show("Added \"${data.title}\" to the catalog")
            } else {
                repository.updateItem(editedId, data)
                messenger.show("Saved changes to \"${data.title}\"")
            }
            closeChannel.send(Unit)
        }
    }

    private fun String.digits(max: Int) = filter(Char::isDigit).take(max)

    private fun LibraryItem.toFormState(id: Int) = ItemFormState(
        itemId = id,
        type = type,
        title = title,
        year = year.toString(),
        author = (this as? Book)?.author.orEmpty(),
        pages = (this as? Book)?.pages?.toString().orEmpty(),
        genre = (this as? Book)?.genre.orEmpty(),
        issue = (this as? Magazine)?.issueNumber?.toString().orEmpty(),
        minutes = (this as? Dvd)?.minutesLong?.toString().orEmpty()
    )

    private companion object {
        const val MAX_TEXT = 80
    }
}
