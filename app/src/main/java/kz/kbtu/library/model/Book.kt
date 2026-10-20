package kz.kbtu.library.model

class Book(
    id: Int,
    title: String,
    year: Int,
    val author: String,
    val pages: Int,
    val genre: String
) : LibraryItem(id, title, year) {
    override val type = ItemType.BOOK

    // A book can be kept for 21 days
    override fun loanDays(): Int = 21

    override fun details(): String = "$author · $pages pages · $genre"

    override fun attributes(): List<Pair<String, String>> =
        super.attributes() + listOf("Author" to author, "Pages" to pages.toString(), "Genre" to genre)

    override fun matches(query: String): Boolean =
        super.matches(query) ||
            author.contains(query, ignoreCase = true) ||
            genre.contains(query, ignoreCase = true)

    override fun describe(): String {
        return "Book: ${super.describe()} by $author, $pages pages"
    }
}
