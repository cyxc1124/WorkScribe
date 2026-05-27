package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.DayStatusType
import club.cyxc.workscribe.data.PunchRecord
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
    ): ResolvedDayStatus? {
        manualType?.let { return it.toResolved() }

        if (records.isNotEmpty()) {
            val duration = WorkDurationCalculator.calculate(records, durationAnchorMillis)
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

    fun workDurationMillis(
        records: List<PunchRecord>,
        durationAnchorMillis: Long,
    ): Long = WorkDurationCalculator.calculate(records, durationAnchorMillis)

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
