package kz.kbtu.library.model

// Base class of books, magazines and DVDs
abstract class LibraryItem(
    val id: Int,
    val title: String,
    val year: Int
) : Borrowable {
    abstract val type: ItemType

    abstract fun details(): String

    // Used by the search field
    open fun matches(query: String): Boolean = title.contains(query, ignoreCase = true)

    // Label and value rows for the details screen
    open fun attributes(): List<Pair<String, String>> = listOf(
        "Type" to type.label,
        "Catalog number" to "#$id",
        "Year" to year.toString(),
        "Loan period" to "${loanDays()} days"
    )

    override fun describe(): String = "[$id] $title ($year)"
}
