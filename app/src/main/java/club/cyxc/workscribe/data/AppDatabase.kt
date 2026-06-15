package club.cyxc.workscribe.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [PunchRecord::class, DayStatus::class, DayNote::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(PunchTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun punchDao(): PunchDao
    abstract fun dayStatusDao(): DayStatusDao
    abstract fun dayNoteDao(): DayNoteDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS day_statuses (
                        dateEpochDay INTEGER NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS day_notes (
                        dateEpochDay INTEGER NOT NULL PRIMARY KEY,
                        content TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workscribe.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
