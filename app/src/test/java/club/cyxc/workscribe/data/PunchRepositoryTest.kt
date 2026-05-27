package club.cyxc.workscribe.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PunchRepositoryTest {

    private val zoneId = ZoneId.systemDefault()
    private val date = LocalDate.of(2026, 5, 25)

    @Test
    fun makeupPunch_noExistingRecord_insertsNew() = runBlocking {
        val dao = FakePunchDao()
        val repository = PunchRepository(dao, FakeDayStatusDao())

        val timestamp = at(9, 0)
        assertNull(repository.makeupPunch(PunchType.IN, timestamp, zoneId))

        assertEquals(1, dao.records.size)
        assertEquals(timestamp, dao.records.single().timestamp)
        assertEquals(PunchType.IN, dao.records.single().type)
    }

    @Test
    fun makeupPunch_sameTypeSameDay_updatesExisting() = runBlocking {
        val dao = FakePunchDao()
        val repository = PunchRepository(dao, FakeDayStatusDao())
        val original = PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN)
        dao.records.add(original)

        val corrected = at(8, 30)
        assertNull(repository.makeupPunch(PunchType.IN, corrected, zoneId))

        assertEquals(1, dao.records.size)
        assertEquals(1L, dao.records.single().id)
        assertEquals(corrected, dao.records.single().timestamp)
    }

    @Test
    fun makeupPunch_outWhenInExists_insertsOut() = runBlocking {
        val dao = FakePunchDao()
        val repository = PunchRepository(dao, FakeDayStatusDao())
        dao.records.add(PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN))

        val outTime = at(18, 0)
        assertNull(repository.makeupPunch(PunchType.OUT, outTime, zoneId))

        assertEquals(2, dao.records.size)
        assertEquals(PunchType.OUT, dao.records.last().type)
        assertEquals(outTime, dao.records.last().timestamp)
    }

    @Test
    fun makeupPunch_sameTypeSameDay_updatesLatestWhenDuplicatesExist() = runBlocking {
        val dao = FakePunchDao()
        val repository = PunchRepository(dao, FakeDayStatusDao())
        dao.records.add(PunchRecord(id = 1, timestamp = at(8, 0), type = PunchType.IN))
        dao.records.add(PunchRecord(id = 2, timestamp = at(9, 0), type = PunchType.IN))

        val corrected = at(8, 45)
        assertNull(repository.makeupPunch(PunchType.IN, corrected, zoneId))

        assertEquals(2, dao.records.size)
        assertEquals(corrected, dao.records.find { it.id == 2L }!!.timestamp)
        assertEquals(at(8, 0), dao.records.find { it.id == 1L }!!.timestamp)
    }

    @Test
    fun makeupPunch_clearsStaleDayStatus() = runBlocking {
        val dao = FakePunchDao()
        val dayStatusDao = FakeDayStatusDao()
        dayStatusDao.upsert(
            DayStatus(
                dateEpochDay = date.toEpochDay(),
                type = DayStatusType.OVERTIME,
            ),
        )
        val repository = PunchRepository(dao, dayStatusDao)

        assertNull(repository.makeupPunch(PunchType.IN, at(9, 0), zoneId))

        assertEquals(null, dayStatusDao.statuses[date.toEpochDay()])
    }

    private fun at(hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
}

private class FakePunchDao : PunchDao {
    val records = mutableListOf<PunchRecord>()
    private var nextId = 1L

    override suspend fun insert(record: PunchRecord): Long {
        val id = if (record.id == 0L) nextId++ else record.id
        records.add(record.copy(id = id))
        return id
    }

    override suspend fun update(record: PunchRecord) {
        val index = records.indexOfFirst { it.id == record.id }
        check(index >= 0) { "Record ${record.id} not found" }
        records[index] = record
    }

    override suspend fun findLatestRecordForDayAndType(
        dayStart: Long,
        dayEnd: Long,
        type: PunchType,
    ): PunchRecord? = records
        .filter { it.timestamp in dayStart until dayEnd && it.type == type }
        .maxByOrNull { it.timestamp }

    override fun observeRecordsForDay(dayStart: Long, dayEnd: Long): Flow<List<PunchRecord>> =
        flowOf(records.filter { it.timestamp in dayStart until dayEnd })

    override fun observeRecordsInRange(rangeStart: Long, rangeEnd: Long): Flow<List<PunchRecord>> =
        flowOf(records.filter { it.timestamp in rangeStart until rangeEnd })

    override fun observeLatestRecord(): Flow<PunchRecord?> =
        flowOf(records.maxByOrNull { it.timestamp })

    override suspend fun deleteById(id: Long) {
        records.removeAll { it.id == id }
    }
}

private class FakeDayStatusDao : DayStatusDao {
    val statuses = mutableMapOf<Long, DayStatusType>()

    override suspend fun upsert(status: DayStatus) {
        statuses[status.dateEpochDay] = status.type
    }

    override suspend fun deleteByDate(dateEpochDay: Long) {
        statuses.remove(dateEpochDay)
    }

    override fun observeInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DayStatus>> =
        flowOf(
            statuses
                .filterKeys { it in startEpochDay until endEpochDay }
                .map { (epochDay, type) -> DayStatus(epochDay, type) },
        )
}
