package kz.kbtu.library.model

// Data from the item form, the id is assigned by the database
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
