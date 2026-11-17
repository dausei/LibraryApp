package kz.kbtu.library

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kz.kbtu.library.model.Availability
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.MemberResult
import kz.kbtu.library.ui.Messenger
import kz.kbtu.library.ui.navigation.Screen
import kz.kbtu.library.ui.viewmodel.CatalogViewModel
import kz.kbtu.library.ui.viewmodel.ItemDetailViewModel
import kz.kbtu.library.ui.viewmodel.ItemFormViewModel
import kz.kbtu.library.ui.viewmodel.LoansViewModel
import kz.kbtu.library.ui.viewmodel.MembersViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository()
    // Created lazily: viewModelScope must be built after the rule has set the Main dispatcher
    private val viewModel by lazy { CatalogViewModel(repository, Messenger()) }

    @Test
    fun `the catalog lists every item with its availability`() = runTest {
        collectIn(viewModel.uiState)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.totalItems)
        assertEquals(5, state.entries.size)
        assertTrue(state.entries.first { it.item.id == 1 }.availability is Availability.OnLoan)
        assertEquals(Availability.Available, state.entries.first { it.item.id == 4 }.availability)
    }

    @Test
    fun `search and filters narrow the list`() = runTest {
        collectIn(viewModel.uiState)

        viewModel.onQueryChange("herbert")
        assertEquals(listOf("Dune"), viewModel.uiState.value.entries.map { it.item.title })

        viewModel.onQueryChange("")
        viewModel.onTypeFilterChange(ItemType.DVD)
        assertEquals(listOf("Interstellar"), viewModel.uiState.value.entries.map { it.item.title })

        viewModel.onTypeFilterChange(null)
        viewModel.onOnlyAvailableChange(true)
        assertEquals(setOf("Neuromancer", "Wired", "Interstellar"), viewModel.uiState.value.entries.map { it.item.title }.toSet())
        assertEquals(5, viewModel.uiState.value.totalItems)
    }

    @Test
    fun `the list follows the database without a manual refresh`() = runTest {
        collectIn(viewModel.uiState)

        repository.snapshot.value = repository.snapshot.value.copy(items = emptyList())

        assertEquals(0, viewModel.uiState.value.totalItems)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }

    @Test
    fun `borrow and return go through the repository and tell the user`() = runTest {
        val messenger = Messenger()
        val vm = CatalogViewModel(repository, messenger)

        vm.borrow(memberId = 1, itemId = 4)
        vm.returnItem(itemId = 1)

        assertEquals(listOf("borrow:1:4", "return:1"), repository.calls)
        assertEquals("No such item", messenger.messages.first())
    }
}

class ItemFormViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository()
    private val messenger = Messenger()

    private fun newForm() = ItemFormViewModel(
        repository, messenger, SavedStateHandle(mapOf(Screen.ItemForm.ARG_ITEM_ID to Screen.ItemForm.NEW))
    )

    private fun editForm(id: Int) = ItemFormViewModel(
        repository, messenger, SavedStateHandle(mapOf(Screen.ItemForm.ARG_ITEM_ID to id))
    )

    @Test
    fun `saving an invalid form shows errors and stores nothing`() {
        val vm = newForm()

        vm.save()

        assertTrue(vm.uiState.value.showErrors)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `number fields accept only digits`() {
        val vm = newForm()

        vm.onYearChange("19a6b5x7")

        assertEquals("1965", vm.uiState.value.year)
    }

    @Test
    fun `a valid form creates the item and closes`() = runTest {
        val vm = newForm()
        vm.onTitleChange("Dune")
        vm.onYearChange("1965")
        vm.onAuthorChange("Frank Herbert")
        vm.onPagesChange("612")

        vm.save()

        assertEquals(listOf("addItem:Dune"), repository.calls)
        vm.closeEvents.first()
        assertEquals("Added \"Dune\" to the catalog", messenger.messages.first())
    }

    @Test
    fun `the edit form is filled from the stored item and saves as an update`() = runTest {
        val vm = editForm(2)

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Dune", state.title)
        assertEquals("1965", state.year)
        assertEquals("612", state.pages)

        vm.onTitleChange("Dune Messiah")
        vm.save()

        assertEquals(listOf("updateItem:2:Dune Messiah"), repository.calls)
    }

    @Test
    fun `editing an item that does not exist reports it`() {
        val vm = editForm(404)

        assertTrue(vm.uiState.value.notFound)
        assertFalse(vm.uiState.value.isLoading)
    }
}

class ItemDetailViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository()
    private val messenger = Messenger()

    private fun detail(id: Int) = ItemDetailViewModel(
        repository, messenger, SavedStateHandle(mapOf(Screen.ItemDetail.ARG_ITEM_ID to id))
    )

    @Test
    fun `the state shows the loan and the queue of the item`() = runTest {
        val vm = detail(1)
        collectIn(vm.uiState)

        val state = vm.uiState.value
        assertEquals("Clean Code", state.item?.title)
        assertNotNull(state.loan)
        assertEquals(listOf("Dana"), state.queue.map { it.member?.name })
        assertEquals(2, state.members.size)
    }

    @Test
    fun `a missing item gives a null item, not a crash`() = runTest {
        val vm = detail(404)
        collectIn(vm.uiState)

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.item)
    }

    @Test
    fun `deleting closes the screen, but an item on loan stays`() = runTest {
        val vm = detail(4)
        collectIn(vm.uiState)

        vm.deleteItem()
        vm.closeEvents.first()
        assertEquals("Deleted \"Wired\"", messenger.messages.first())

        repository.deleteItemResult = DeleteResult.OnLoan
        vm.deleteItem()
        assertEquals("Return \"Wired\" before deleting it", messenger.messages.first())
        assertEquals(listOf("deleteItem:4", "deleteItem:4"), repository.calls)
    }

    @Test
    fun `reserve and cancel use the item of the screen`() = runTest {
        val vm = detail(1)

        vm.reserve(memberId = 1)
        vm.cancelReservation(reservationId = 7)

        assertEquals(listOf("reserve:1:1", "cancelReservation:7"), repository.calls)
    }
}

class MembersViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository()
    private val messenger = Messenger()
    private val viewModel by lazy { MembersViewModel(repository, messenger) }

    @Test
    fun `rows show loans and fines of every member`() = runTest {
        collectIn(viewModel.uiState)

        val rows = viewModel.uiState.value.rows
        assertEquals(listOf("Aidar", "Dana"), rows.map { it.member.name })
        assertEquals(2, rows[0].borrowed.size)
        assertEquals(300, rows[1].status.unpaidFine)
        assertEquals(1, rows[1].reservations)
    }

    @Test
    fun `the editor closes after a member was saved`() = runTest {
        collectIn(viewModel.uiState)

        viewModel.startAdding()
        viewModel.onEditorNameChange("Timur")
        assertEquals("Timur", viewModel.uiState.value.editor?.name)
        viewModel.saveEditor()

        assertEquals(listOf("addMember:Timur"), repository.calls)
        assertNull(viewModel.uiState.value.editor)
        assertEquals("Member \"New\" registered", messenger.messages.first())
    }

    @Test
    fun `a duplicate name keeps the editor open with an error`() = runTest {
        collectIn(viewModel.uiState)
        repository.memberResult = MemberResult.Duplicate

        viewModel.startRenaming(repository.snapshot.value.member(1)!!)
        viewModel.onEditorNameChange("Dana")
        viewModel.saveEditor()

        val editor = viewModel.uiState.value.editor
        assertEquals("A member with this name already exists", editor?.error)
        assertEquals(listOf("renameMember:1:Dana"), repository.calls)

        viewModel.onEditorNameChange("Dana2")
        assertNull(viewModel.uiState.value.editor?.error)
    }

    @Test
    fun `a member with loans cannot be deleted`() = runTest {
        repository.deleteMemberResult = DeleteResult.HasActiveLoans(2)

        viewModel.deleteMember(repository.snapshot.value.member(1)!!)

        assertEquals("Aidar still has 2 borrowed. Return them first", messenger.messages.first())
    }
}

class LoansViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository()
    private val viewModel by lazy { LoansViewModel(repository, Messenger()) }

    @Test
    fun `expired holds are released when the screen starts`() {
        viewModel
        assertEquals(listOf("releaseExpiredHolds"), repository.calls)
    }

    @Test
    fun `the three lists are filled from the database`() = runTest {
        collectIn(viewModel.uiState)

        val state = viewModel.uiState.value
        assertEquals(2, state.active.size)
        assertEquals(1, state.reservations.size)
        assertEquals(1, state.history.size)
    }

    @Test
    fun `picking up a reservation borrows the item for the member who reserved it`() = runTest {
        collectIn(viewModel.uiState)
        val entry = viewModel.uiState.value.reservations.single()

        viewModel.pickUp(entry)

        assertTrue("borrow:2:1" in repository.calls)
    }
}

// Keeps a StateFlow produced with stateIn(WhileSubscribed) active for the duration of a test
private fun TestScope.collectIn(flow: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { } }
}
