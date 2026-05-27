package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.DayStatusType
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayStatusResolverTest {

    private val zoneId = ZoneId.systemDefault()
    private val saturday = LocalDate.of(2026, 5, 23) // Saturday
    private val monday = LocalDate.of(2026, 5, 25)

    @Test
    fun weekendWithoutRecords_showsRestForCalendarOnly() {
        assertEquals(
            ResolvedDayStatus.REST,
            DayStatusResolver.resolve(
                date = saturday,
                records = emptyList(),
                manualType = null,
                durationAnchorMillis = anchorFor(saturday),
            ),
        )
    }

    @Test
    fun weekendWithPunchRecords_showsWorkOrOvertime() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(saturday, 9, 0), type = PunchType.IN),
            PunchRecord(id = 2, timestamp = at(saturday, 17, 0), type = PunchType.OUT),
        )
        assertEquals(
            ResolvedDayStatus.WORK,
            DayStatusResolver.resolve(
                date = saturday,
                records = records,
                manualType = null,
                durationAnchorMillis = anchorFor(saturday),
            ),
        )
    }

    @Test
    fun manualRestWithoutRecords_showsRestForCalendarOnly() {
        assertEquals(
            ResolvedDayStatus.REST,
            DayStatusResolver.resolve(
                date = monday,
                records = emptyList(),
                manualType = DayStatusType.REST,
                durationAnchorMillis = anchorFor(monday),
            ),
        )
    }

    @Test
    fun weekdayWithoutRecords_hasNoAutoStatus() {
        assertNull(
            DayStatusResolver.resolve(
                date = monday,
                records = emptyList(),
                manualType = null,
                durationAnchorMillis = anchorFor(monday),
            ),
        )
    }

    private fun anchorFor(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

    private fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
}
