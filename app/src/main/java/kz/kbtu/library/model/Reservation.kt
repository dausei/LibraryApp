package kz.kbtu.library.model

// WAITING -> READY (item came back, held for the member) -> FULFILLED (member picked it up)
// A reservation can also end as CANCELLED, or when the hold expires.
enum class ReservationStatus { WAITING, READY, FULFILLED, CANCELLED }

data class Reservation(
    val id: Int,
    val itemId: Int,
    val memberId: Int,
    val createdAt: Long,
    val status: ReservationStatus,
    val readyUntil: Long? = null
)
