package kz.kbtu.library.model

class Magazine(
    id: Int,
    title: String,
    year: Int,
    val issueNumber: Int
) : LibraryItem(id, title, year) {
    override val type = ItemType.MAGAZINE

    // A magazine can be kept for 7 days
    override fun loanDays(): Int = 7

    override fun details(): String = "Issue #$issueNumber"

    override fun attributes(): List<Pair<String, String>> =
        super.attributes() + ("Issue number" to "#$issueNumber")

    override fun describe(): String {
        return "Magazine: ${super.describe()}, issue #$issueNumber"
    }
}
