package kz.kbtu.library.model

class Dvd(
    id: Int,
    title: String,
    year: Int,
    val minutesLong: Int
) : LibraryItem(id, title, year) {
    override val type = ItemType.DVD

    // A DVD can be kept for 3 days
    override fun loanDays(): Int = 3

    override fun details(): String = "$minutesLong min"

    override fun attributes(): List<Pair<String, String>> =
        super.attributes() + ("Length" to "$minutesLong min")

    override fun describe(): String {
        return "DVD: ${super.describe()}, $minutesLong min"
    }
}
