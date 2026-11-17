package kz.kbtu.library.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import kz.kbtu.library.model.LoanPolicy.DAY_MS

// Sample data the database starts with. Loan dates are relative to the first launch,
// so the demo always contains an overdue loan, a reservation queue and a fine.
object SeedData {

    fun insertInto(db: SupportSQLiteDatabase, now: Long = System.currentTimeMillis()) {
        fun days(n: Int) = n * DAY_MS

        fun item(id: Int, type: String, title: String, year: Int,
                 author: String? = null, pages: Int? = null, genre: String? = null,
                 issue: Int? = null, minutes: Int? = null) =
            db.execSQL(
                "INSERT INTO items (id, type, title, year, author, pages, genre, issueNumber, minutes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(id, type, title, year, author, pages, genre, issue, minutes)
            )

        item(1, "BOOK", "Clean Code", 2008, "Robert Martin", 464, "Programming")
        item(2, "BOOK", "The Pragmatic Programmer", 1999, "Andrew Hunt", 352, "Programming")
        item(3, "BOOK", "Dune", 1965, "Frank Herbert", 612, "Sci-Fi")
        item(4, "BOOK", "Neuromancer", 1984, "William Gibson", 271, "Sci-Fi")
        item(5, "BOOK", "1984", 1949, "George Orwell", 328, "Dystopia")
        item(6, "MAGAZINE", "National Geographic", 2023, issue = 5)
        item(7, "MAGAZINE", "Wired", 2024, issue = 11)
        item(8, "DVD", "Interstellar", 2014, minutes = 169)
        item(9, "DVD", "The Matrix", 1999, minutes = 136)

        listOf(1 to "Aidar", 2 to "Dana", 3 to "Timur").forEach { (id, name) ->
            db.execSQL("INSERT INTO members (id, name) VALUES (?, ?)", arrayOf<Any?>(id, name))
        }

        fun loan(itemId: Int, memberId: Int, borrowedDaysAgo: Int, loanDays: Int,
                 returnedDaysAgo: Int? = null, fine: Int = 0, finePaid: Boolean = false) {
            val borrowedAt = now - days(borrowedDaysAgo)
            db.execSQL(
                "INSERT INTO loans (itemId, memberId, borrowedAt, dueAt, returnedAt, renewals, fine, finePaid) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
                arrayOf<Any?>(
                    itemId, memberId, borrowedAt, borrowedAt + days(loanDays),
                    returnedDaysAgo?.let { now - days(it) }, fine, if (finePaid) 1 else 0
                )
            )
        }

        // Active loans: Dune is on time, Interstellar is overdue, Wired is on time
        loan(itemId = 3, memberId = 1, borrowedDaysAgo = 10, loanDays = 21)
        loan(itemId = 8, memberId = 2, borrowedDaysAgo = 5, loanDays = 3)
        loan(itemId = 7, memberId = 3, borrowedDaysAgo = 2, loanDays = 7)

        // History: one on-time return, one late return with a paid fine, one with an unpaid fine
        loan(itemId = 9, memberId = 1, borrowedDaysAgo = 20, loanDays = 3, returnedDaysAgo = 18)
        loan(itemId = 1, memberId = 3, borrowedDaysAgo = 35, loanDays = 21, returnedDaysAgo = 11, fine = 300, finePaid = true)
        loan(itemId = 4, memberId = 2, borrowedDaysAgo = 30, loanDays = 21, returnedDaysAgo = 6, fine = 300)

        // Timur waits for Dune, which Aidar has
        db.execSQL(
            "INSERT INTO reservations (itemId, memberId, createdAt, status, readyUntil) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(3, 3, now - days(1), "WAITING", null)
        )
    }
}
