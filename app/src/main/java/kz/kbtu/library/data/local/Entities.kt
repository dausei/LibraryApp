package kz.kbtu.library.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kz.kbtu.library.model.ItemType
import kz.kbtu.library.model.ReservationStatus

// One table for all item types: columns that do not apply to a type stay null
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: ItemType,
    val title: String,
    val year: Int,
    val author: String? = null,
    val pages: Int? = null,
    val genre: String? = null,
    val issueNumber: Int? = null,
    val minutes: Int? = null
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

// Deleting an item or a member removes their loans too
@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("itemId"), Index("memberId")]
)
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val memberId: Int,
    val borrowedAt: Long,
    val dueAt: Long,
    val returnedAt: Long? = null,
    val renewals: Int = 0,
    val fine: Int = 0,
    val finePaid: Boolean = false
)

@Entity(
    tableName = "reservations",
    foreignKeys = [
        ForeignKey(ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MemberEntity::class, parentColumns = ["id"], childColumns = ["memberId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("itemId"), Index("memberId")]
)
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val memberId: Int,
    val createdAt: Long,
    val status: ReservationStatus,
    val readyUntil: Long? = null
)
