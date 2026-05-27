package club.cyxc.workscribe.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class PunchRepository(private val dao: PunchDao) {

    fun observeTodayRecords(): Flow<List<PunchRecord>> {
        val (start, end) = dayBounds(LocalDate.now())
        return dao.observeRecordsForDay(start, end)
    }

    fun observeLatestRecord(): Flow<PunchRecord?> = dao.observeLatestRecord()

    suspend fun punch(type: PunchType) {
        dao.insert(PunchRecord(timestamp = System.currentTimeMillis(), type = type))
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    companion object {
        fun dayBounds(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
            val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return start to end
        }
    }
}
