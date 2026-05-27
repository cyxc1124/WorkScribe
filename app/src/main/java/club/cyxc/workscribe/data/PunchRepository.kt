package club.cyxc.workscribe.data

import club.cyxc.workscribe.util.MakeupPunchValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class PunchRepository(
    private val dao: PunchDao,
    private val dayStatusDao: DayStatusDao,
) {

    fun observeTodayRecords(): Flow<List<PunchRecord>> {
        val (start, end) = dayBounds(LocalDate.now())
        return dao.observeRecordsForDay(start, end)
    }

    fun observeLatestRecord(): Flow<PunchRecord?> = dao.observeLatestRecord()

    fun observeRecordsForDate(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<PunchRecord>> {
        val (start, end) = dayBounds(date, zoneId)
        return dao.observeRecordsForDay(start, end)
    }

    fun observeRecordsInMonth(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<PunchRecord>> {
        val (start, end) = monthBounds(yearMonth, zoneId)
        return observeRecordsBetween(start, end)
    }

    fun observeRecordsBetween(rangeStart: Long, rangeEnd: Long): Flow<List<PunchRecord>> {
        return dao.observeRecordsInRange(rangeStart, rangeEnd)
    }

    suspend fun punch(type: PunchType) {
        dao.insert(PunchRecord(timestamp = System.currentTimeMillis(), type = type))
    }

    suspend fun makeupPunch(type: PunchType, timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        MakeupPunchValidator.validate(timestamp, zoneId)?.let { return it }
        dao.insert(PunchRecord(timestamp = timestamp, type = type))
        return null
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    fun observeDayStatusesInMonth(
        yearMonth: YearMonth,
    ): Flow<Map<Long, DayStatusType>> {
        val startEpochDay = yearMonth.atDay(1).toEpochDay()
        val endEpochDay = yearMonth.plusMonths(1).atDay(1).toEpochDay()
        return observeDayStatusesBetween(startEpochDay, endEpochDay)
    }

    fun observeDayStatusesBetween(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<Map<Long, DayStatusType>> {
        return dayStatusDao.observeInRange(startEpochDay, endEpochDay).map { statuses ->
            statuses.associate { it.dateEpochDay to it.type }
        }
    }

    suspend fun setDayStatus(date: LocalDate, type: DayStatusType) {
        dayStatusDao.upsert(
            DayStatus(
                dateEpochDay = date.toEpochDay(),
                type = type,
            ),
        )
    }

    suspend fun clearDayStatus(date: LocalDate) {
        dayStatusDao.deleteByDate(date.toEpochDay())
    }

    companion object {
        fun dayBounds(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
            val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return start to end
        }

        fun monthBounds(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
            val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return start to end
        }
    }
}
