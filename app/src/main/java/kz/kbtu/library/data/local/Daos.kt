package kz.kbtu.library.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun find(id: Int): ItemEntity?

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Int)
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun find(id: Int): MemberEntity?

    @Query("SELECT COUNT(*) FROM members WHERE name = :name COLLATE NOCASE AND id != :exceptId")
    suspend fun countWithName(name: String, exceptId: Int): Int

    @Insert
    suspend fun insert(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun delete(id: Int)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY borrowedAt DESC, id DESC")
    fun observeAll(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun find(id: Int): LoanEntity?

    @Query("SELECT * FROM loans WHERE itemId = :itemId AND returnedAt IS NULL LIMIT 1")
    suspend fun activeLoanOfItem(itemId: Int): LoanEntity?

    @Query("SELECT COUNT(*) FROM loans WHERE memberId = :memberId AND returnedAt IS NULL")
    suspend fun activeCount(memberId: Int): Int

    @Query("SELECT COALESCE(SUM(fine), 0) FROM loans WHERE memberId = :memberId AND fine > 0 AND finePaid = 0")
    suspend fun unpaidFines(memberId: Int): Int

    // Marks all fines of the member as paid
    @Query("UPDATE loans SET finePaid = 1 WHERE memberId = :memberId AND fine > 0 AND finePaid = 0")
    suspend fun payFines(memberId: Int)

    @Insert
    suspend fun insert(loan: LoanEntity): Long

    @Update
    suspend fun update(loan: LoanEntity)
}

@Dao
interface ReservationDao {
    // Open = still waiting or ready for pickup
    @Query("SELECT * FROM reservations WHERE status IN ('WAITING', 'READY') ORDER BY createdAt, id")
    fun observeOpen(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun find(id: Int): ReservationEntity?

    @Query("SELECT * FROM reservations WHERE itemId = :itemId AND status IN ('WAITING', 'READY') ORDER BY createdAt, id")
    suspend fun openForItem(itemId: Int): List<ReservationEntity>

    @Query("SELECT * FROM reservations WHERE memberId = :memberId AND status = 'READY'")
    suspend fun readyForMember(memberId: Int): List<ReservationEntity>

    @Query("SELECT * FROM reservations WHERE status = 'READY' AND readyUntil < :now")
    suspend fun expiredHolds(now: Long): List<ReservationEntity>

    @Insert
    suspend fun insert(reservation: ReservationEntity): Long

    @Update
    suspend fun update(reservation: ReservationEntity)
}
