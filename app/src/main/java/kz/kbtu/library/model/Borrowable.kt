package kz.kbtu.library.model

interface Borrowable {
    fun loanDays(): Int
    fun describe(): String
}
