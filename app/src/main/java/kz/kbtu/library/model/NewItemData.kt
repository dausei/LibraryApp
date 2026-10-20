package kz.kbtu.library.model

// Data from the add item form, the id is assigned later
sealed interface NewItemData {
    val title: String
    val year: Int

    data class NewBook(
        override val title: String,
        override val year: Int,
        val author: String,
        val pages: Int,
        val genre: String
    ) : NewItemData

    data class NewMagazine(
        override val title: String,
        override val year: Int,
        val issueNumber: Int
    ) : NewItemData

    data class NewDvd(
        override val title: String,
        override val year: Int,
        val minutesLong: Int
    ) : NewItemData
}

// Turns form data into a real item
fun NewItemData.toItem(id: Int): LibraryItem = when (this) {
    is NewItemData.NewBook -> Book(id, title, year, author, pages, genre)
    is NewItemData.NewMagazine -> Magazine(id, title, year, issueNumber)
    is NewItemData.NewDvd -> Dvd(id, title, year, minutesLong)
}
