package kz.kbtu.library

import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.ui.viewmodel.ItemFormState
import kz.kbtu.library.ui.viewmodel.ItemFormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Input validation of the create / edit form
class ItemFormValidatorTest {

    private val thisYear = 2026

    private val validBook = ItemFormState(
        type = ItemType.BOOK, title = " Dune ", year = "1965",
        author = "Frank Herbert", pages = "612", genre = ""
    )

    @Test
    fun `an empty form reports every required field of a book`() {
        val errors = ItemFormValidator.validate(ItemFormState(), thisYear)

        assertNotNull(errors.title)
        assertNotNull(errors.year)
        assertNotNull(errors.author)
        assertNotNull(errors.pages)
        assertNull(errors.issue)
        assertNull(errors.minutes)
    }

    @Test
    fun `only the fields of the selected type are checked`() {
        val magazine = ItemFormState(type = ItemType.MAGAZINE, title = "Wired", year = "2024")
        val dvd = ItemFormState(type = ItemType.DVD, title = "Matrix", year = "1999")

        assertNotNull(ItemFormValidator.validate(magazine, thisYear).issue)
        assertNull(ItemFormValidator.validate(magazine, thisYear).author)
        assertNotNull(ItemFormValidator.validate(dvd, thisYear).minutes)
        assertNull(ItemFormValidator.validate(dvd, thisYear).pages)
    }

    @Test
    fun `year must be between 1000 and the current year`() {
        fun yearError(year: String) = ItemFormValidator.validate(validBook.copy(year = year), thisYear).year

        assertNotNull(yearError(""))
        assertNotNull(yearError("999"))
        assertNotNull(yearError("2027"))
        assertNull(yearError("1000"))
        assertNull(yearError("2026"))
    }

    @Test
    fun `numbers are range checked`() {
        assertNotNull(ItemFormValidator.validate(validBook.copy(pages = "0"), thisYear).pages)
        assertNotNull(ItemFormValidator.validate(validBook.copy(pages = "5001"), thisYear).pages)
        assertNull(ItemFormValidator.validate(validBook.copy(pages = "5000"), thisYear).pages)

        val dvd = ItemFormState(type = ItemType.DVD, title = "Matrix", year = "1999")
        assertNotNull(ItemFormValidator.validate(dvd.copy(minutes = "1001"), thisYear).minutes)
        assertNull(ItemFormValidator.validate(dvd.copy(minutes = "136"), thisYear).minutes)
    }

    @Test
    fun `a blank title or author is rejected`() {
        assertNotNull(ItemFormValidator.validate(validBook.copy(title = "   "), thisYear).title)
        assertNotNull(ItemFormValidator.validate(validBook.copy(author = " "), thisYear).author)
    }

    @Test
    fun `a valid book is trimmed and gets the default genre`() {
        val data = ItemFormValidator.toItemData(validBook, thisYear)

        assertEquals(NewItemData.NewBook("Dune", 1965, "Frank Herbert", 612, "General"), data)
    }

    @Test
    fun `a valid magazine and dvd are converted`() {
        val magazine = ItemFormState(type = ItemType.MAGAZINE, title = "Wired", year = "2024", issue = "11")
        val dvd = ItemFormState(type = ItemType.DVD, title = "Matrix", year = "1999", minutes = "136")

        assertEquals(NewItemData.NewMagazine("Wired", 2024, 11), ItemFormValidator.toItemData(magazine, thisYear))
        assertEquals(NewItemData.NewDvd("Matrix", 1999, 136), ItemFormValidator.toItemData(dvd, thisYear))
    }

    @Test
    fun `an invalid form produces no data`() {
        assertNull(ItemFormValidator.toItemData(validBook.copy(pages = ""), thisYear))
    }
}
