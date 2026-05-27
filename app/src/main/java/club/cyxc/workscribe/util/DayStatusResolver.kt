package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.DayStatusType
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchTimeConfig
import java.time.DayOfWeek
import java.time.LocalDate

enum class ResolvedDayStatus {
    WORK,
    SICK,
    OVERTIME,
    REST,
}

/** 日历展示用日期状态推断；不影响主屏打卡，打卡时段仅由 [PunchTimeRules] 决定。 */
object DayStatusResolver {
    private const val STANDARD_WORK_MILLIS = 8L * 60 * 60 * 1000

    fun resolve(
        date: LocalDate,
        records: List<PunchRecord>,
        manualType: DayStatusType?,
        durationAnchorMillis: Long,
        includeOpenSession: Boolean,
        lunchBreakEnabled: Boolean = PunchTimeConfig.DEFAULT_LUNCH_BREAK_ENABLED,
        lunchBreakMinutes: Int = PunchTimeConfig.DEFAULT_LUNCH_BREAK_MINUTES,
    ): ResolvedDayStatus? {
        val auto = resolveAuto(
            date,
            records,
            durationAnchorMillis,
            includeOpenSession,
            lunchBreakEnabled,
            lunchBreakMinutes,
        )

        if (manualType != null) {
            if (manualType == DayStatusType.OVERTIME && auto != ResolvedDayStatus.OVERTIME) {
                return auto
            }
            return manualType.toResolved()
        }

        return auto
    }

    fun workDurationMillis(
        records: List<PunchRecord>,
        durationAnchorMillis: Long,
        includeOpenSession: Boolean,
        lunchBreakEnabled: Boolean = PunchTimeConfig.DEFAULT_LUNCH_BREAK_ENABLED,
        lunchBreakMinutes: Int = PunchTimeConfig.DEFAULT_LUNCH_BREAK_MINUTES,
    ): Long = WorkDurationCalculator.calculate(
        records = records,
        nowMillis = durationAnchorMillis,
        includeOpenSession = includeOpenSession,
        lunchBreakEnabled = lunchBreakEnabled,
        lunchBreakMinutes = lunchBreakMinutes,
    )

    fun isStaleManualOvertime(
        date: LocalDate,
        records: List<PunchRecord>,
        manualType: DayStatusType?,
        durationAnchorMillis: Long,
        includeOpenSession: Boolean,
        lunchBreakEnabled: Boolean = PunchTimeConfig.DEFAULT_LUNCH_BREAK_ENABLED,
        lunchBreakMinutes: Int = PunchTimeConfig.DEFAULT_LUNCH_BREAK_MINUTES,
    ): Boolean {
        if (manualType != DayStatusType.OVERTIME) return false
        val auto = resolveAuto(
            date,
            records,
            durationAnchorMillis,
            includeOpenSession,
            lunchBreakEnabled,
            lunchBreakMinutes,
        )
        return auto != ResolvedDayStatus.OVERTIME
    }

    private fun resolveAuto(
        date: LocalDate,
        records: List<PunchRecord>,
        durationAnchorMillis: Long,
        includeOpenSession: Boolean,
        lunchBreakEnabled: Boolean,
        lunchBreakMinutes: Int,
    ): ResolvedDayStatus? {
        if (records.isNotEmpty()) {
            if (!WorkDurationCalculator.hasCompletedSession(records)) {
                return ResolvedDayStatus.WORK
            }
            val duration = WorkDurationCalculator.calculate(
                records = records,
                nowMillis = durationAnchorMillis,
                includeOpenSession = includeOpenSession,
                lunchBreakEnabled = lunchBreakEnabled,
                lunchBreakMinutes = lunchBreakMinutes,
            )
            return if (duration > STANDARD_WORK_MILLIS) {
                ResolvedDayStatus.OVERTIME
            } else {
                ResolvedDayStatus.WORK
            }
        }

        if (isWeekend(date)) {
            return ResolvedDayStatus.REST
        }

        return null
    }

    private fun DayStatusType.toResolved(): ResolvedDayStatus = when (this) {
        DayStatusType.WORK -> ResolvedDayStatus.WORK
        DayStatusType.SICK -> ResolvedDayStatus.SICK
        DayStatusType.OVERTIME -> ResolvedDayStatus.OVERTIME
        DayStatusType.REST -> ResolvedDayStatus.REST
    }

    private fun isWeekend(date: LocalDate): Boolean {
        val day = date.dayOfWeek
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }
}
