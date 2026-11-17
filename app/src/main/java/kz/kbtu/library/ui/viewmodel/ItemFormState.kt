package kz.kbtu.library.ui.viewmodel

import java.time.Year
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.NewItemData

// What the user typed in the create / edit form. Numbers stay Strings while typing.
data class ItemFormState(
    val itemId: Int? = null, // null = creating a new item
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isSaving: Boolean = false,
    val type: ItemType = ItemType.BOOK,
    val title: String = "",
    val year: String = "",
    val author: String = "",
    val pages: String = "",
    val genre: String = "",
    val issue: String = "",
    val minutes: String = "",
    val showErrors: Boolean = false // errors appear after the first attempt to save
) {
    val isEditing: Boolean get() = itemId != null
}

// null = the field is fine
data class ItemFormErrors(
    val title: String? = null,
    val year: String? = null,
    val author: String? = null,
    val pages: String? = null,
    val issue: String? = null,
    val minutes: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(title, year, author, pages, issue, minutes).any { it != null }
}

object ItemFormValidator {
    const val MIN_YEAR = 1000
    const val MAX_PAGES = 5000
    const val MAX_MINUTES = 1000

    fun validate(state: ItemFormState, currentYear: Int = Year.now().value): ItemFormErrors {
        val year = state.year.toIntOrNull()
        return ItemFormErrors(
            title = "Title is required".takeIf { state.title.isBlank() },
            year = "Enter a year from $MIN_YEAR to $currentYear".takeIf { year == null || year !in MIN_YEAR..currentYear },
            author = "Author is required".takeIf { state.type == ItemType.BOOK && state.author.isBlank() },
            pages = "Enter 1 to $MAX_PAGES".takeIf {
                state.type == ItemType.BOOK && state.pages.toIntOrNull()?.let { it in 1..MAX_PAGES } != true
            },
            issue = "Enter a number from 1".takeIf {
                state.type == ItemType.MAGAZINE && state.issue.toIntOrNull()?.let { it >= 1 } != true
            },
            minutes = "Enter 1 to $MAX_MINUTES".takeIf {
                state.type == ItemType.DVD && state.minutes.toIntOrNull()?.let { it in 1..MAX_MINUTES } != true
            }
        )
    }

    // The data to store, or null if the form has errors
    fun toItemData(state: ItemFormState, currentYear: Int = Year.now().value): NewItemData? {
        if (validate(state, currentYear).hasErrors) return null
        val title = state.title.trim()
        val year = state.year.toInt()
        return when (state.type) {
            ItemType.BOOK -> NewItemData.NewBook(
                title, year, state.author.trim(), state.pages.toInt(), state.genre.trim().ifBlank { "General" }
            )
            ItemType.MAGAZINE -> NewItemData.NewMagazine(title, year, state.issue.toInt())
            ItemType.DVD -> NewItemData.NewDvd(title, year, state.minutes.toInt())
        }
    }
}
