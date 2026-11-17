package kz.kbtu.library.model

// A member together with what decides whether they may borrow now
data class MemberStatus(
    val member: Member,
    val activeLoans: Int,
    val unpaidFine: Int
) {
    val atLimit: Boolean get() = activeLoans >= LoanPolicy.MAX_LOANS
    val canBorrow: Boolean get() = !atLimit && unpaidFine == 0
}
