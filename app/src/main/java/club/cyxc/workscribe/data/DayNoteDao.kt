package club.cyxc.workscribe.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayNoteDao {
    @Query("SELECT content FROM day_notes WHERE dateEpochDay = :dateEpochDay")
    fun observeContent(dateEpochDay: Long): Flow<String?>

    @Query(
        "SELECT * FROM day_notes WHERE dateEpochDay >= :startEpochDay AND dateEpochDay < :endEpochDay",
    )
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DayNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: DayNote)

    @Query("DELETE FROM day_notes WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteByDate(dateEpochDay: Long)
}
