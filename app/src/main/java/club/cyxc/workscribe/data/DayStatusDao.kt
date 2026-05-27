package club.cyxc.workscribe.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayStatusDao {
    @Query(
        "SELECT * FROM day_statuses WHERE dateEpochDay >= :startEpochDay AND dateEpochDay < :endEpochDay",
    )
    fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DayStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: DayStatus)

    @Query("DELETE FROM day_statuses WHERE dateEpochDay = :dateEpochDay")
    suspend fun deleteByDate(dateEpochDay: Long)
}
