package kz.kbtu.library.model

// Every possible outcome of a borrow request
sealed class LoanResult {
    data class Success(val loan: Loan) : LoanResult()
    data class AlreadyBorrowed(val byMemberId: Int) : LoanResult()
    data class HeldForOther(val memberId: Int) : LoanResult()
    data class LimitReached(val limit: Int) : LoanResult()
    data class UnpaidFines(val amount: Int) : LoanResult()
    data object ItemNotFound : LoanResult()
    data object MemberNotFound : LoanResult()
}

sealed class ReturnResult {
    data class Returned(val loan: Loan, val nextReader: Member?) : ReturnResult()
    data object NotBorrowed : ReturnResult()
}

sealed class RenewResult {
    data class Renewed(val loan: Loan) : RenewResult()
    data class Blocked(val reason: RenewBlock) : RenewResult()
    data object NotFound : RenewResult()
}

sealed class ReserveResult {
    data class Reserved(val position: Int) : ReserveResult()
    data object ItemIsAvailable : ReserveResult()
    data object AlreadyHasIt : ReserveResult()
    data object AlreadyReserved : ReserveResult()
    data object ItemNotFound : ReserveResult()
    data object MemberNotFound : ReserveResult()
}

sealed class MemberResult {
    data class Saved(val member: Member) : MemberResult()
    data object EmptyName : MemberResult()
    data object Duplicate : MemberResult()
    data object NotFound : MemberResult()
}

sealed class DeleteResult {
    data object Deleted : DeleteResult()
    data class HasActiveLoans(val count: Int) : DeleteResult()
    data object OnLoan : DeleteResult()
    data object NotFound : DeleteResult()
}
