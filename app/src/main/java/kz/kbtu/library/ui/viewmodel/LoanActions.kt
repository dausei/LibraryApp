package kz.kbtu.library.ui.viewmodel

import kotlinx.coroutines.flow.first
import kz.kbtu.library.data.repository.LibraryRepository
import kz.kbtu.library.model.LibrarySnapshot
import kz.kbtu.library.model.LoanResult
import kz.kbtu.library.model.RenewResult
import kz.kbtu.library.model.ReserveResult
import kz.kbtu.library.model.ReturnResult
import kz.kbtu.library.ui.Messenger
import kz.kbtu.library.ui.util.formatDate
import kz.kbtu.library.ui.util.formatMoney

// Lending actions used by several screens: runs the repository call and tells the user the outcome.
// ViewModels call these from viewModelScope.
class LoanActions(
    private val repository: LibraryRepository,
    private val messenger: Messenger
) {
    // Names for the messages are read after the change, from the same database
    private suspend fun snapshot(): LibrarySnapshot = repository.library.first()

    suspend fun borrow(memberId: Int, itemId: Int) {
        val result = repository.borrow(memberId, itemId)
        val snap = snapshot()
        messenger.show(
            when (result) {
                is LoanResult.Success -> {
                    val who = snap.member(memberId)?.name
                    val what = snap.item(itemId)?.title
                    "$who borrowed \"$what\" until ${formatDate(result.loan.dueAt)}"
                }
                is LoanResult.AlreadyBorrowed -> "Already borrowed by ${snap.member(result.byMemberId)?.name}"
                is LoanResult.HeldForOther -> "Kept for ${snap.member(result.memberId)?.name}, who reserved it"
                is LoanResult.LimitReached -> "Limit reached: at most ${result.limit} items per member"
                is LoanResult.UnpaidFines ->
                    "${snap.member(memberId)?.name} owes ${formatMoney(result.amount)}. Pay the fine on the Members tab first"
                LoanResult.ItemNotFound -> "No such item"
                LoanResult.MemberNotFound -> "No such member"
            }
        )
    }

    suspend fun returnItem(itemId: Int) {
        val result = repository.returnItem(itemId)
        val title = snapshot().item(itemId)?.title
        messenger.show(
            when (result) {
                is ReturnResult.Returned -> buildString {
                    append("Returned \"$title\"")
                    if (result.loan.fine > 0) append(". Late fine: ${formatMoney(result.loan.fine)}")
                    result.nextReader?.let { append(". Kept for ${it.name}") }
                }
                ReturnResult.NotBorrowed -> "This item is not borrowed"
            }
        )
    }

    suspend fun renew(loanId: Int) {
        messenger.show(
            when (val result = repository.renew(loanId)) {
                is RenewResult.Renewed -> "Renewed until ${formatDate(result.loan.dueAt)}"
                is RenewResult.Blocked -> result.reason.message
                RenewResult.NotFound -> "This loan is already closed"
            }
        )
    }

    suspend fun reserve(memberId: Int, itemId: Int) {
        val result = repository.reserve(memberId, itemId)
        val snap = snapshot()
        val who = snap.member(memberId)?.name
        val what = snap.item(itemId)?.title
        messenger.show(
            when (result) {
                is ReserveResult.Reserved -> "$who is number ${result.position} in the queue for \"$what\""
                ReserveResult.ItemIsAvailable -> "\"$what\" is on the shelf, borrow it instead"
                ReserveResult.AlreadyHasIt -> "$who already has \"$what\""
                ReserveResult.AlreadyReserved -> "$who is already in the queue for \"$what\""
                ReserveResult.ItemNotFound -> "No such item"
                ReserveResult.MemberNotFound -> "No such member"
            }
        )
    }

    suspend fun cancelReservation(reservationId: Int) {
        val cancelled = repository.cancelReservation(reservationId)
        messenger.show(if (cancelled) "Reservation cancelled" else "This reservation is already closed")
    }

    suspend fun payFines(memberId: Int) {
        val paid = repository.payFines(memberId)
        val who = snapshot().member(memberId)?.name
        messenger.show(if (paid > 0) "$who paid ${formatMoney(paid)}" else "$who has no unpaid fines")
    }
}
