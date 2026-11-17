package kz.kbtu.library

import kz.kbtu.library.model.Book
import kz.kbtu.library.model.Dvd
import kz.kbtu.library.model.LibrarySnapshot
import kz.kbtu.library.model.Loan
import kz.kbtu.library.model.LoanPolicy.DAY_MS
import kz.kbtu.library.model.Magazine
import kz.kbtu.library.model.Member
import kz.kbtu.library.model.Reservation
import kz.kbtu.library.model.ReservationStatus

const val NOW = 1_800_000_000_000L

// Small library used by the tests:
//   Clean Code (1) is borrowed by Aidar and Dana waits for it
//   Dune (2) is borrowed by Aidar and is overdue
//   Neuromancer (3), Wired (4), Interstellar (5) are on the shelf
fun sampleSnapshot(
    loans: List<Loan> = listOf(
        Loan(id = 1, itemId = 1, memberId = 1, borrowedAt = NOW - 2 * DAY_MS, dueAt = NOW + 19 * DAY_MS),
        Loan(id = 2, itemId = 2, memberId = 1, borrowedAt = NOW - 30 * DAY_MS, dueAt = NOW - 9 * DAY_MS),
        Loan(
            id = 3, itemId = 3, memberId = 2, borrowedAt = NOW - 40 * DAY_MS, dueAt = NOW - 19 * DAY_MS,
            returnedAt = NOW - 16 * DAY_MS, fine = 300
        )
    ),
    reservations: List<Reservation> = listOf(
        Reservation(id = 1, itemId = 1, memberId = 2, createdAt = NOW - DAY_MS, status = ReservationStatus.WAITING)
    )
) = LibrarySnapshot(
    items = listOf(
        Book(1, "Clean Code", 2008, "Robert Martin", 464, "Programming"),
        Book(2, "Dune", 1965, "Frank Herbert", 612, "Sci-Fi"),
        Book(3, "Neuromancer", 1984, "William Gibson", 271, "Sci-Fi"),
        Magazine(4, "Wired", 2024, 11),
        Dvd(5, "Interstellar", 2014, 169)
    ),
    members = listOf(Member(1, "Aidar"), Member(2, "Dana")),
    loans = loans,
    reservations = reservations,
    now = NOW
)
