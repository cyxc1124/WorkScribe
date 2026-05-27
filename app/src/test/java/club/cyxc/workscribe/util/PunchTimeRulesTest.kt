package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class PunchTimeRulesTest {

    @Test
    fun clockInWindow_allowsIn_only() {
        assertEquals(PunchWindow.CLOCK_IN, PunchTimeRules.windowAt(LocalTime.MIDNIGHT))
        assertEquals(PunchWindow.CLOCK_IN, PunchTimeRules.windowAt(LocalTime.of(6, 0)))
        assertEquals(PunchWindow.CLOCK_IN, PunchTimeRules.windowAt(LocalTime.of(11, 59, 59)))
        assertEquals(PunchType.IN, punchTypeAt(LocalTime.of(8, 30)))
    }

    @Test
    fun offHoursWindow_noInToday_allowsMakeupIn() {
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.of(16, 0)))
        assertEquals(OffHoursPunchState.MAKEUP_IN, PunchTimeRules.offHoursPunchState(emptyList()))
        assertEquals(
            PunchType.IN,
            punchTypeAt(LocalTime.of(16, 0), todayRecords = emptyList()),
        )
    }

    @Test
    fun offHoursWindow_hasInNoOut_waitsForClockOut() {
        val records = listOf(inRecord())
        assertEquals(OffHoursPunchState.WAIT_FOR_OUT, PunchTimeRules.offHoursPunchState(records))
        assertNull(punchTypeAt(LocalTime.of(14, 0), todayRecords = records))
    }

    @Test
    fun offHoursWindow_hasInAndOut_blocked() {
        val records = listOf(inRecord(), outRecord())
        assertEquals(OffHoursPunchState.BLOCKED, PunchTimeRules.offHoursPunchState(records))
        assertNull(punchTypeAt(LocalTime.of(15, 0), todayRecords = records))
    }

    @Test
    fun offHoursWindow_boundaries() {
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.NOON))
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.of(15, 0)))
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.of(17, 29, 59)))
    }

    @Test
    fun clockOutWindow_allowsOut_always() {
        assertEquals(PunchWindow.CLOCK_OUT, PunchTimeRules.windowAt(LocalTime.of(17, 30)))
        assertEquals(PunchWindow.CLOCK_OUT, PunchTimeRules.windowAt(LocalTime.of(23, 59, 59)))
        assertEquals(PunchType.OUT, punchTypeAt(LocalTime.of(22, 0)))
        assertEquals(PunchType.OUT, punchTypeAt(LocalTime.of(18, 0), todayRecords = emptyList()))
    }

    private fun punchTypeAt(
        time: LocalTime,
        todayRecords: List<PunchRecord> = emptyList(),
    ): PunchType? {
        val millis = time.atDate(java.time.LocalDate.of(2026, 1, 1))
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return PunchTimeRules.punchTypeFor(millis, todayRecords)
    }

    private fun inRecord() = PunchRecord(id = 1, timestamp = 0, type = PunchType.IN)

    private fun outRecord() = PunchRecord(id = 2, timestamp = 1, type = PunchType.OUT)
}
