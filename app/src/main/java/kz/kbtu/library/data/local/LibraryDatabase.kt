package kz.kbtu.library.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase.Callback

@Database(
    entities = [ItemEntity::class, MemberEntity::class, LoanEntity::class, ReservationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun memberDao(): MemberDao
    abstract fun loanDao(): LoanDao
    abstract fun reservationDao(): ReservationDao

    companion object {
        fun create(context: Context): LibraryDatabase =
            Room.databaseBuilder(context, LibraryDatabase::class.java, "library.db")
                .addCallback(object : Callback() {
                    // Runs once, when the database file is created: fills it with sample data
                    override fun onCreate(db: SupportSQLiteDatabase) = SeedData.insertInto(db)
                })
                .build()
    }
}
