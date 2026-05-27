package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class PunchWindow {
    CLOCK_IN,
    CLOCK_OUT,
    OFF_HOURS,
}

/** 非打卡时段（12:00–17:30）下的可操作状态 */
enum class OffHoursPunchState {
    /** 今日尚无上班记录，可补打上班卡 */
    MAKEUP_IN,
    /** 已上班，须等到 17:30 再打下班卡 */
    WAIT_FOR_OUT,
    /** 今日打卡已齐或其他情况，不可操作 */
    BLOCKED,
}

/**
 * 打卡时段：由当前时钟与今日记录共同决定；周末与手动「休息」日均适用，不因日历状态拦截。
 * - 00:00–12:00（不含 12:00）：上班
 * - 12:00–17:30（不含 17:30）：非打卡；若今日尚无上班记录则允许补打上班卡
 * - 17:30–24:00（不含次日 0:00）：下班（无上班记录也可打下班卡）
 */
object PunchTimeRules {
    private val CLOCK_IN_END = LocalTime.NOON
    private val CLOCK_OUT_START = LocalTime.of(17, 30)

    const val MAKEUP_IN_LABEL = "补打上班卡"
    const val WAIT_FOR_OUT_LABEL = "等待下班打卡"
    const val WAIT_FOR_OUT_HINT = "17:30 后可打下班卡"
    const val OFF_HOURS_HINT = "12:00–17:30 为非打卡时段，请稍后再试"
    const val OFF_HOURS_BUTTON = "非打卡时段"

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

    fun hasInToday(todayRecords: List<PunchRecord>): Boolean =
        todayRecords.any { it.type == PunchType.IN }

    fun hasOutToday(todayRecords: List<PunchRecord>): Boolean =
        todayRecords.any { it.type == PunchType.OUT }

    fun offHoursPunchState(todayRecords: List<PunchRecord>): OffHoursPunchState {
        return when {
            !hasInToday(todayRecords) -> OffHoursPunchState.MAKEUP_IN
            !hasOutToday(todayRecords) -> OffHoursPunchState.WAIT_FOR_OUT
            else -> OffHoursPunchState.BLOCKED
        }
    }

    fun punchTypeFor(
        millis: Long,
        todayRecords: List<PunchRecord> = emptyList(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PunchType? {
        return when (windowAt(millis, zoneId)) {
            PunchWindow.CLOCK_IN -> PunchType.IN
            PunchWindow.CLOCK_OUT -> PunchType.OUT
            PunchWindow.OFF_HOURS -> when (offHoursPunchState(todayRecords)) {
                OffHoursPunchState.MAKEUP_IN -> PunchType.IN
                else -> null
            }
        }
    }
}
