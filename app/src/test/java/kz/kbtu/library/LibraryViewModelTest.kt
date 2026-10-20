package kz.kbtu.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.ui.LibraryViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests of the ViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LibraryViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state contains the sample catalog`() {
        val state = viewModel.uiState.value

        assertEquals(9, state.items.size)
        assertEquals(9, state.visibleItems.size)
        assertEquals(3, state.members.size)
        assertEquals("Aidar", state.borrowerOf(3)?.name)
    }

    @Test
    fun `typing a query filters the visible items`() {
        viewModel.onQueryChange("orwell")

        val state = viewModel.uiState.value
        assertEquals(listOf("1984"), state.visibleItems.map { it.title })
        assertEquals(9, state.items.size)
    }

    @Test
    fun `type filter and only-available filter work together`() {
        viewModel.onTypeFilterChange(ItemType.BOOK)
        viewModel.onOnlyAvailableChange(true)

        val titles = viewModel.uiState.value.visibleItems.map { it.title }
        assertEquals(4, titles.size)
        assertFalse("Dune" in titles)
    }

    @Test
    fun `borrow sets busy then reports the result`() = runTest(dispatcher) {
        viewModel.borrow(memberId = 2, itemId = 8)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isBusy)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBusy)
        assertEquals("Dana borrowed \"Interstellar\" for 3 days", state.message)
        assertEquals("Dana", state.borrowerOf(8)?.name)
    }

    @Test
    fun `borrowing a taken item shows an explanation`() = runTest(dispatcher) {
        viewModel.borrow(memberId = 2, itemId = 3)
        advanceUntilIdle()

        assertEquals("Already borrowed by Aidar", viewModel.uiState.value.message)
    }

    @Test
    fun `return makes the item available and message can be dismissed`() {
        viewModel.returnItem(3)
        assertNull(viewModel.uiState.value.borrowerOf(3))
        assertEquals("Returned \"Dune\"", viewModel.uiState.value.message)

        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `adding an item puts it into the catalog and stats`() {
        viewModel.addItem(NewItemData.NewBook("Kotlin in Action", 2017, "Dmitry Jemerov", 360, "Programming"))

        val state = viewModel.uiState.value
        assertEquals(10, state.items.size)
        assertEquals(10, state.stats.totalItems)
        assertEquals("Kotlin in Action", state.items.last().title)
    }

    @Test
    fun `blank and duplicate member names are rejected`() {
        assertFalse(viewModel.addMember("   "))
        assertFalse(viewModel.addMember("dana"))
        assertTrue(viewModel.addMember("  Aliya "))

        assertEquals(listOf("Aidar", "Dana", "Timur", "Aliya"), viewModel.uiState.value.members.map { it.name })
    }
}
