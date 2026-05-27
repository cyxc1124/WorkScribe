package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchTimeConfig
import club.cyxc.workscribe.data.PunchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class PunchTimeRulesTest {

    private val defaultRules = PunchTimeRules.default()

    @Test
    fun clockInWindow_allowsIn_only() {
        assertEquals(PunchWindow.CLOCK_IN, defaultRules.windowAt(LocalTime.MIDNIGHT))
        assertEquals(PunchWindow.CLOCK_IN, defaultRules.windowAt(LocalTime.of(6, 0)))
        assertEquals(PunchWindow.CLOCK_IN, defaultRules.windowAt(LocalTime.of(11, 59, 59)))
        assertEquals(PunchType.IN, punchTypeAt(defaultRules, LocalTime.of(8, 30)))
    }

    @Test
    fun offHoursWindow_noInToday_allowsMakeupIn() {
        assertEquals(PunchWindow.OFF_HOURS, defaultRules.windowAt(LocalTime.of(16, 0)))
        assertEquals(OffHoursPunchState.MAKEUP_IN, defaultRules.offHoursPunchState(emptyList()))
        assertEquals(
            PunchType.IN,
            punchTypeAt(defaultRules, LocalTime.of(16, 0), todayRecords = emptyList()),
        )
    }

    @Test
    fun offHoursWindow_hasInNoOut_waitsForClockOut() {
        val records = listOf(inRecord())
        assertEquals(OffHoursPunchState.WAIT_FOR_OUT, defaultRules.offHoursPunchState(records))
        assertNull(punchTypeAt(defaultRules, LocalTime.of(14, 0), todayRecords = records))
    }

    @Test
    fun offHoursWindow_hasInAndOut_blocked() {
        val records = listOf(inRecord(), outRecord())
        assertEquals(OffHoursPunchState.BLOCKED, defaultRules.offHoursPunchState(records))
        assertNull(punchTypeAt(defaultRules, LocalTime.of(15, 0), todayRecords = records))
    }

    @Test
    fun offHoursWindow_boundaries() {
        assertEquals(PunchWindow.OFF_HOURS, defaultRules.windowAt(LocalTime.NOON))
        assertEquals(PunchWindow.OFF_HOURS, defaultRules.windowAt(LocalTime.of(15, 0)))
        assertEquals(PunchWindow.OFF_HOURS, defaultRules.windowAt(LocalTime.of(17, 29, 59)))
    }

    @Test
    fun clockOutWindow_allowsOut_always() {
        assertEquals(PunchWindow.CLOCK_OUT, defaultRules.windowAt(LocalTime.of(17, 30)))
        assertEquals(PunchWindow.CLOCK_OUT, defaultRules.windowAt(LocalTime.of(23, 59, 59)))
        assertEquals(PunchType.OUT, punchTypeAt(defaultRules, LocalTime.of(22, 0)))
        assertEquals(PunchType.OUT, punchTypeAt(defaultRules, LocalTime.of(18, 0), todayRecords = emptyList()))
    }

    @Test
    fun punchWindows_applyOnWeekendsAndRestDays() {
        val saturday = java.time.LocalDate.of(2026, 5, 23)
        assertEquals(PunchType.IN, punchTypeOn(defaultRules, saturday, LocalTime.of(9, 0)))
        assertEquals(
            PunchType.IN,
            punchTypeOn(defaultRules, saturday, LocalTime.of(14, 0), todayRecords = emptyList()),
        )
        assertEquals(PunchType.OUT, punchTypeOn(defaultRules, saturday, LocalTime.of(20, 0)))
    }

    @Test
    fun customConfig_usesConfiguredWindows() {
        val config = PunchTimeConfig(
            clockInStartMinutes = 8 * 60,
            clockInEndMinutes = 10 * 60,
            clockOutStartMinutes = 18 * 60,
            clockOutEndMinutes = 20 * 60,
        )
        val rules = PunchTimeRules(config)

        assertEquals(PunchWindow.OFF_HOURS, rules.windowAt(LocalTime.of(7, 0)))
        assertEquals(PunchWindow.CLOCK_IN, rules.windowAt(LocalTime.of(8, 30)))
        assertEquals(PunchWindow.OFF_HOURS, rules.windowAt(LocalTime.of(11, 0)))
        assertEquals(PunchWindow.CLOCK_OUT, rules.windowAt(LocalTime.of(19, 0)))
        assertEquals(PunchWindow.OFF_HOURS, rules.windowAt(LocalTime.of(21, 0)))
        assertEquals(PunchType.IN, punchTypeAt(rules, LocalTime.of(9, 0)))
        assertEquals(PunchType.OUT, punchTypeAt(rules, LocalTime.of(19, 30)))
    }

    @Test
    fun customConfig_dynamicHints() {
        val config = PunchTimeConfig(
            clockInEndMinutes = 13 * 60,
            clockOutStartMinutes = 18 * 60,
        )
        val rules = PunchTimeRules(config)

        assertEquals("18:00 后可打下班卡", rules.waitForOutHint)
        assertEquals("13:00–18:00 为非打卡时段，请稍后再试", rules.offHoursHint)
    }

    private fun punchTypeOn(
        rules: PunchTimeRules,
        date: java.time.LocalDate,
        time: LocalTime,
        todayRecords: List<PunchRecord> = emptyList(),
    ): PunchType? {
        val millis = time.atDate(date).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return rules.punchTypeFor(millis, todayRecords)
    }

    private fun punchTypeAt(
        rules: PunchTimeRules,
        time: LocalTime,
        todayRecords: List<PunchRecord> = emptyList(),
    ): PunchType? {
        val millis = time.atDate(java.time.LocalDate.of(2026, 1, 1))
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return rules.punchTypeFor(millis, todayRecords)
    }

    private fun inRecord() = PunchRecord(id = 1, timestamp = 0, type = PunchType.IN)

    private fun outRecord() = PunchRecord(id = 2, timestamp = 1, type = PunchType.OUT)
}
