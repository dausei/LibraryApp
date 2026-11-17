package kz.kbtu.library.model

// Immutable on purpose: who borrowed what is stored in the loans table
data class Member(
    val id: Int,
    val name: String
)
