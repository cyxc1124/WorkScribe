package club.cyxc.workscribe.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PunchDao {
    @Insert
    suspend fun insert(record: PunchRecord): Long

    @Update
    suspend fun update(record: PunchRecord)

    @Query(
        """
        SELECT * FROM punch_records
        WHERE timestamp >= :dayStart AND timestamp < :dayEnd AND type = :type
        ORDER BY timestamp DESC
        LIMIT 1
        """,
    )
    suspend fun findLatestRecordForDayAndType(
        dayStart: Long,
        dayEnd: Long,
        type: PunchType,
    ): PunchRecord?

    @Query("SELECT * FROM punch_records WHERE timestamp >= :dayStart AND timestamp < :dayEnd ORDER BY timestamp DESC")
    fun observeRecordsForDay(dayStart: Long, dayEnd: Long): Flow<List<PunchRecord>>

    @Query("SELECT * FROM punch_records WHERE timestamp >= :rangeStart AND timestamp < :rangeEnd ORDER BY timestamp ASC")
    fun observeRecordsInRange(rangeStart: Long, rangeEnd: Long): Flow<List<PunchRecord>>

    @Query("SELECT * FROM punch_records ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestRecord(): Flow<PunchRecord?>

    @Query("DELETE FROM punch_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
