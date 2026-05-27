package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchTimeConfig
import club.cyxc.workscribe.data.PunchType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class PunchWindow {
    CLOCK_IN,
    CLOCK_OUT,
    OFF_HOURS,
}

/** 非打卡时段下的可操作状态 */
enum class OffHoursPunchState {
    /** 今日尚无上班记录，可补打上班卡 */
    MAKEUP_IN,
    /** 已上班，须等到下班打卡开始时间再打下班卡 */
    WAIT_FOR_OUT,
    /** 今日打卡已齐或其他情况，不可操作 */
    BLOCKED,
}

/**
 * 打卡时段：由当前时钟、自定义规则与今日记录共同决定。
 * - 上班窗口：[clockInStart, clockInEnd)
 * - 非打卡窗口：[clockInEnd, clockOutStart)；若今日尚无上班记录则允许补打上班卡
 * - 下班窗口：[clockOutStart, clockOutEnd]
 */
class PunchTimeRules(
    private val config: PunchTimeConfig = PunchTimeConfig.DEFAULT,
) {
    val makeupInLabel: String = MAKEUP_IN_LABEL
    val waitForOutLabel: String = WAIT_FOR_OUT_LABEL
    val offHoursButton: String = OFF_HOURS_BUTTON

    val waitForOutHint: String
        get() = "${PunchTimeConfig.formatMinutes(config.clockOutStartMinutes)} 后可打下班卡"

    val offHoursHint: String
        get() = "${PunchTimeConfig.formatMinutes(config.clockInEndMinutes)}–" +
            "${PunchTimeConfig.formatMinutes(config.clockOutStartMinutes)} 为非打卡时段，请稍后再试"

    fun windowAt(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): PunchWindow {
        val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
        return windowAt(time)
    }

    fun windowAt(time: LocalTime): PunchWindow {
        val minutes = PunchTimeConfig.localTimeToMinutes(time)
        return when {
            minutes >= config.clockInStartMinutes && minutes < config.clockInEndMinutes -> PunchWindow.CLOCK_IN
            minutes >= config.clockOutStartMinutes && minutes <= config.clockOutEndMinutes -> PunchWindow.CLOCK_OUT
            minutes >= config.clockInEndMinutes && minutes < config.clockOutStartMinutes -> PunchWindow.OFF_HOURS
            else -> PunchWindow.OFF_HOURS
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

    companion object {
        const val MAKEUP_IN_LABEL = "补打上班卡"
        const val WAIT_FOR_OUT_LABEL = "等待下班打卡"
        const val OFF_HOURS_BUTTON = "非打卡时段"

        fun default(): PunchTimeRules = PunchTimeRules(PunchTimeConfig.DEFAULT)
    }
}
