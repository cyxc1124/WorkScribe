package club.cyxc.workscribe.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PunchDao {
    @Insert
    suspend fun insert(record: PunchRecord): Long

    @Query("SELECT * FROM punch_records WHERE timestamp >= :dayStart AND timestamp < :dayEnd ORDER BY timestamp DESC")
    fun observeRecordsForDay(dayStart: Long, dayEnd: Long): Flow<List<PunchRecord>>

    @Query("SELECT * FROM punch_records ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestRecord(): Flow<PunchRecord?>

    @Query("DELETE FROM punch_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
