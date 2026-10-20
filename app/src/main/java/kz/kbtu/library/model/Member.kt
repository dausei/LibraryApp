package kz.kbtu.library.model

// Immutable on purpose: who borrowed what is stored in Library.loans
data class Member(
    val id: Int,
    val name: String
)
