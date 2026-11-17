package kz.kbtu.library.data.repository

import kotlinx.coroutines.flow.Flow
import kz.kbtu.library.model.DeleteResult
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.LibrarySnapshot
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.MemberResult
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.model.RenewResult
import kz.kbtu.library.model.ReserveResult
import kz.kbtu.library.model.ReturnResult

// The only way the UI layer reaches the stored data
interface LibraryRepository {

    // Everything in the library. Emits again after every change in the database.
    val library: Flow<LibrarySnapshot>

    // Items
    suspend fun getItem(id: Int): LibraryItem?
    suspend fun addItem(data: NewItemData): Int
    suspend fun updateItem(id: Int, data: NewItemData)
    suspend fun deleteItem(id: Int): DeleteResult

    // Members
    suspend fun addMember(name: String): MemberResult
    suspend fun renameMember(id: Int, name: String): MemberResult
    suspend fun deleteMember(id: Int): DeleteResult
    suspend fun payFines(memberId: Int): Int

    // Loans
    suspend fun borrow(memberId: Int, itemId: Int): LoanResult
    suspend fun returnItem(itemId: Int): ReturnResult
    suspend fun renew(loanId: Int): RenewResult

    // Reservations
    suspend fun reserve(memberId: Int, itemId: Int): ReserveResult
    suspend fun cancelReservation(reservationId: Int): Boolean
    suspend fun releaseExpiredHolds()
}
