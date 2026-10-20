package kz.kbtu.library.model

// Every possible outcome of a borrow request
sealed class LoanResult {
    data class Success(val loan: Loan) : LoanResult()
    data class AlreadyBorrowed(val byMemberId: Int) : LoanResult()
    data class LimitReached(val limit: Int) : LoanResult()
    object ItemNotFound : LoanResult()
    object MemberNotFound : LoanResult()
}
