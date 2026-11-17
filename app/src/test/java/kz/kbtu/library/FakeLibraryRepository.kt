package kz.kbtu.library

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.LibrarySnapshot
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.Member
import kz.kbtu.library.model.MemberResult
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.model.RenewResult
import kz.kbtu.library.model.ReserveResult
import kz.kbtu.library.model.ReturnResult

// Repository for ViewModel tests: serves a fixed snapshot and records the calls it receives.
// The real rules are in RoomLibraryRepository and are covered by the instrumented tests.
class FakeLibraryRepository(
    snapshot: LibrarySnapshot = sampleSnapshot()
) : LibraryRepository {

    val snapshot = MutableStateFlow(snapshot)
    override val library: Flow<LibrarySnapshot> = this.snapshot

    val calls = mutableListOf<String>()

    var memberResult: MemberResult = MemberResult.Saved(Member(9, "New"))
    var deleteItemResult: DeleteResult = DeleteResult.Deleted
    var deleteMemberResult: DeleteResult = DeleteResult.Deleted
    var borrowResult: LoanResult = LoanResult.ItemNotFound

    override suspend fun getItem(id: Int): LibraryItem? = snapshot.value.item(id)

    override suspend fun addItem(data: NewItemData): Int {
        calls += "addItem:${data.title}"
        return 99
    }

    override suspend fun updateItem(id: Int, data: NewItemData) {
        calls += "updateItem:$id:${data.title}"
    }

    override suspend fun deleteItem(id: Int): DeleteResult {
        calls += "deleteItem:$id"
        return deleteItemResult
    }

    override suspend fun addMember(name: String): MemberResult {
        calls += "addMember:$name"
        return memberResult
    }

    override suspend fun renameMember(id: Int, name: String): MemberResult {
        calls += "renameMember:$id:$name"
        return memberResult
    }

    override suspend fun deleteMember(id: Int): DeleteResult {
        calls += "deleteMember:$id"
        return deleteMemberResult
    }

    override suspend fun payFines(memberId: Int): Int {
        calls += "payFines:$memberId"
        return 300
    }

    override suspend fun borrow(memberId: Int, itemId: Int): LoanResult {
        calls += "borrow:$memberId:$itemId"
        return borrowResult
    }

    override suspend fun returnItem(itemId: Int): ReturnResult {
        calls += "return:$itemId"
        return ReturnResult.NotBorrowed
    }

    override suspend fun renew(loanId: Int): RenewResult {
        calls += "renew:$loanId"
        return RenewResult.NotFound
    }

    override suspend fun reserve(memberId: Int, itemId: Int): ReserveResult {
        calls += "reserve:$memberId:$itemId"
        return ReserveResult.ItemIsAvailable
    }

    override suspend fun cancelReservation(reservationId: Int): Boolean {
        calls += "cancelReservation:$reservationId"
        return true
    }

    override suspend fun releaseExpiredHolds() {
        calls += "releaseExpiredHolds"
    }
}
