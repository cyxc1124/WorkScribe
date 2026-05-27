package club.cyxc.workscribe.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PunchTimeConfigTest {

    @Test
    fun defaultConfig_isValid() {
        assertNull(PunchTimeConfig.DEFAULT.validate())
    }

    @Test
    fun validate_rejectsClockInEndBeforeStart() {
        val config = PunchTimeConfig(
            clockInStartMinutes = 10 * 60,
            clockInEndMinutes = 9 * 60,
        )
        assertEquals("上班结束时间须晚于开始时间", config.validate())
    }

    @Test
    fun validate_rejectsClockOutEndBeforeStart() {
        val config = PunchTimeConfig(
            clockOutStartMinutes = 19 * 60,
            clockOutEndMinutes = 18 * 60,
        )
        assertEquals("下班结束时间须晚于开始时间", config.validate())
    }

    @Test
    fun validate_rejectsClockInEndAfterClockOutStart() {
        val config = PunchTimeConfig(
            clockInEndMinutes = 18 * 60,
            clockOutStartMinutes = 17 * 60,
        )
        assertEquals("上班结束时间不能晚于下班开始时间", config.validate())
    }
}
