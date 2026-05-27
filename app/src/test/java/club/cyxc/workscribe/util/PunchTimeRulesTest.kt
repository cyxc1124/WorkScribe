package club.cyxc.workscribe.util

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
        assertEquals(PunchType.IN, PunchTimeRules.punchTypeFor(LocalTime.of(8, 30)))
    }

    @Test
    fun offHoursWindow_blocksPunch() {
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.NOON))
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.of(15, 0)))
        assertEquals(PunchWindow.OFF_HOURS, PunchTimeRules.windowAt(LocalTime.of(17, 29, 59)))
        assertNull(PunchTimeRules.punchTypeFor(LocalTime.of(14, 0)))
    }

    @Test
    fun clockOutWindow_allowsOut_only() {
        assertEquals(PunchWindow.CLOCK_OUT, PunchTimeRules.windowAt(LocalTime.of(17, 30)))
        assertEquals(PunchWindow.CLOCK_OUT, PunchTimeRules.windowAt(LocalTime.of(20, 0)))
        assertEquals(PunchWindow.CLOCK_OUT, PunchTimeRules.windowAt(LocalTime.of(23, 59, 59)))
        assertEquals(PunchType.OUT, PunchTimeRules.punchTypeFor(LocalTime.of(22, 0)))
    }

    private fun PunchTimeRules.punchTypeFor(time: LocalTime): PunchType? {
        return when (windowAt(time)) {
            PunchWindow.CLOCK_IN -> PunchType.IN
            PunchWindow.CLOCK_OUT -> PunchType.OUT
            PunchWindow.OFF_HOURS -> null
        }
    }
}
