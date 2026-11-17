package kz.kbtu.library.data.repository

import kz.kbtu.library.data.local.ItemEntity
import kz.kbtu.library.data.local.LoanEntity
import kz.kbtu.library.data.local.MemberEntity
import kz.kbtu.library.data.local.ReservationEntity
import kz.kbtu.library.model.Book
import kz.kbtu.library.model.Dvd
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.LibraryItem
import kz.kbtu.library.model.Loan
import kz.kbtu.library.model.Magazine
import kz.kbtu.library.model.Member
import kz.kbtu.library.model.NewItemData
import kz.kbtu.library.model.Reservation

// Database rows <-> domain objects

fun ItemEntity.toDomain(): LibraryItem = when (type) {
    ItemType.BOOK -> Book(id, title, year, author.orEmpty(), pages ?: 0, genre ?: "General")
    ItemType.MAGAZINE -> Magazine(id, title, year, issueNumber ?: 0)
    ItemType.DVD -> Dvd(id, title, year, minutes ?: 0)
}

// id = 0 lets Room generate a new id
fun NewItemData.toEntity(id: Int = 0): ItemEntity = when (this) {
    is NewItemData.NewBook -> ItemEntity(
        id = id, type = ItemType.BOOK, title = title, year = year,
        author = author, pages = pages, genre = genre
    )
    is NewItemData.NewMagazine -> ItemEntity(
        id = id, type = ItemType.MAGAZINE, title = title, year = year, issueNumber = issueNumber
    )
    is NewItemData.NewDvd -> ItemEntity(
        id = id, type = ItemType.DVD, title = title, year = year, minutes = minutesLong
    )
}

// The form data of an existing item, used to fill the edit form
fun LibraryItem.toNewItemData(): NewItemData = when (this) {
    is Book -> NewItemData.NewBook(title, year, author, pages, genre)
    is Magazine -> NewItemData.NewMagazine(title, year, issueNumber)
    is Dvd -> NewItemData.NewDvd(title, year, minutesLong)
    else -> error("Unknown item type")
}

fun MemberEntity.toDomain() = Member(id, name)

fun LoanEntity.toDomain() = Loan(id, itemId, memberId, borrowedAt, dueAt, returnedAt, renewals, fine, finePaid)

fun ReservationEntity.toDomain() = Reservation(id, itemId, memberId, createdAt, status, readyUntil)
