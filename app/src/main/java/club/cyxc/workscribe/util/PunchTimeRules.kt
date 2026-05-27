package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class PunchWindow {
    CLOCK_IN,
    CLOCK_OUT,
    OFF_HOURS,
}

/**
 * 打卡时段：仅由当前时钟决定类型，不按「上一条记录」交替或拦截重复打卡。
 * - 00:00–12:00（不含 12:00）：上班
 * - 12:00–17:30（不含 17:30）：非打卡
 * - 17:30–24:00（不含次日 0:00）：下班
 */
object PunchTimeRules {
    private val CLOCK_IN_END = LocalTime.NOON
    private val CLOCK_OUT_START = LocalTime.of(17, 30)

    const val OFF_HOURS_HINT = "12:00–17:30 为非打卡时段，请稍后再试"

    fun windowAt(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): PunchWindow {
        val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
        return windowAt(time)
    }

    fun windowAt(time: LocalTime): PunchWindow {
        return when {
            time >= LocalTime.MIDNIGHT && time < CLOCK_IN_END -> PunchWindow.CLOCK_IN
            time >= CLOCK_IN_END && time < CLOCK_OUT_START -> PunchWindow.OFF_HOURS
            else -> PunchWindow.CLOCK_OUT
        }
    }

    fun punchTypeFor(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): PunchType? {
        return when (windowAt(millis, zoneId)) {
            PunchWindow.CLOCK_IN -> PunchType.IN
            PunchWindow.CLOCK_OUT -> PunchType.OUT
            PunchWindow.OFF_HOURS -> null
        }
    }
}
