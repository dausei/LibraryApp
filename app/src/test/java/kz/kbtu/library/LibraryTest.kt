package kz.kbtu.library

import kotlinx.coroutines.test.runTest
import kz.kbtu.library.model.Book
import kz.kbtu.library.model.Dvd
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.Library
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.Magazine
import kz.kbtu.library.model.Member
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests of the library rules
class LibraryTest {

    private lateinit var library: Library

    @Before
    fun setUp() {
        library = Library().apply {
            addItem(Book(1, "Clean Code", 2008, "Robert Martin", 464, "Programming"))
            addItem(Book(2, "Dune", 1965, "Frank Herbert", 612, "Sci-Fi"))
            addItem(Book(3, "Neuromancer", 1984, "William Gibson", 271, "Sci-Fi"))
            addItem(Magazine(4, "Wired", 2024, 11))
            addItem(Dvd(5, "Interstellar", 2014, 169))
            addMember(Member(1, "Aidar"))
            addMember(Member(2, "Dana"))
        }
    }

    @Test
    fun `borrow succeeds and uses the loan period of the item type`() {
        val result = library.borrow(memberId = 1, itemId = 5)

        assertTrue(result is LoanResult.Success)
        assertEquals(3, (result as LoanResult.Success).loan.days)
        assertFalse(library.isAvailable(5))
    }

    @Test
    fun `borrowing an item that is already taken fails`() {
        library.borrow(1, 1)

        val result = library.borrow(2, 1)

        assertEquals(LoanResult.AlreadyBorrowed(byMemberId = 1), result)
    }

    @Test
    fun `a member cannot hold more than MAX_LOANS items`() {
        library.borrow(1, 1)
        library.borrow(1, 2)
        library.borrow(1, 3)

        val result = library.borrow(1, 4)

        assertEquals(LoanResult.LimitReached(Library.MAX_LOANS), result)
    }

    @Test
    fun `unknown member or item is reported`() {
        assertEquals(LoanResult.MemberNotFound, library.borrow(99, 1))
        assertEquals(LoanResult.ItemNotFound, library.borrow(1, 99))
    }

    @Test
    fun `returning an item makes it available again`() {
        library.borrow(1, 2)

        assertTrue(library.returnItem(2))
        assertTrue(library.isAvailable(2))
        assertFalse(library.returnItem(2))
    }

    @Test
    fun `borrowAsync gives the same result as borrow`() = runTest {
        val result = library.borrowAsync(2, 4)

        assertTrue(result is LoanResult.Success)
        assertFalse(library.isAvailable(4))
    }

    @Test
    fun `search finds a book by author and ignores case`() {
        val found = library.search { it.matches("herbert") }

        assertEquals(listOf("Dune"), found.map { it.title })
    }

    @Test
    fun `stats are computed from the catalog`() {
        library.borrow(1, 1)

        val stats = library.stats()

        assertEquals(5, stats.totalItems)
        assertEquals(4, stats.availableItems)
        assertEquals(1, stats.itemsOnLoan)
        assertEquals(464 + 612 + 271, stats.totalPages)
        assertEquals("Dune", stats.longestBook?.title)
        assertEquals(mapOf("Book" to 3, "Magazine" to 1, "DVD" to 1), stats.countByType)
        assertEquals(listOf("Dune", "Neuromancer", "Clean Code"), stats.oldest.map { it.title })
        assertEquals(setOf("Programming", "Sci-Fi"), stats.genres)
    }

    @Test
    fun `longest book is null when there are no books`() {
        assertNull(Library().longestBook())
    }

    @Test
    fun `each item type reports its own attributes for the detail screen`() {
        val book = library.findItem(1)!!.attributes().toMap()
        assertEquals("Robert Martin", book["Author"])
        assertEquals("464", book["Pages"])
        assertEquals("21 days", book["Loan period"])

        val dvd = library.findItem(5)!!.attributes().toMap()
        assertEquals("169 min", dvd["Length"])
        assertEquals("3 days", dvd["Loan period"])
        assertFalse("Author" in dvd)
    }

    @Test
    fun `item types are exposed for the UI`() {
        assertEquals(ItemType.BOOK, library.findItem(1)?.type)
        assertEquals(ItemType.MAGAZINE, library.findItem(4)?.type)
        assertEquals(ItemType.DVD, library.findItem(5)?.type)
    }
}
