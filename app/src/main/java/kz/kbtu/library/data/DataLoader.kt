package kz.kbtu.library.data

import kz.kbtu.library.model.Book
import kz.kbtu.library.model.Dvd
import kz.kbtu.library.model.Library
import kz.kbtu.library.model.Magazine
import kz.kbtu.library.model.Member

// Sample data the app starts with
object DataLoader {

    fun load(): Library {
        val library = Library()

        library.addItem(Book(1, "Clean Code", 2008, "Robert Martin", 464, "Programming"))
        library.addItem(Book(2, "The Pragmatic Programmer", 1999, "Andrew Hunt", 352, "Programming"))
        library.addItem(Book(3, "Dune", 1965, "Frank Herbert", 612, "Sci-Fi"))
        library.addItem(Book(4, "Neuromancer", 1984, "William Gibson", 271, "Sci-Fi"))
        library.addItem(Book(5, "1984", 1949, "George Orwell", 328, "Dystopia"))
        library.addItem(Magazine(6, "National Geographic", 2023, 5))
        library.addItem(Magazine(7, "Wired", 2024, 11))
        library.addItem(Dvd(8, "Interstellar", 2014, 169))
        library.addItem(Dvd(9, "The Matrix", 1999, 136))

        library.addMember(Member(1, "Aidar"))
        library.addMember(Member(2, "Dana"))
        library.addMember(Member(3, "Timur"))

        library.borrow(memberId = 1, itemId = 3)

        return library
    }
}
