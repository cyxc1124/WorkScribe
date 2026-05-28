package club.cyxc.workscribe.util

data class MonthStatusStats(
    val workDays: Int = 0,
    val restDays: Int = 0,
    val sickDays: Int = 0,
    val overtimeDays: Int = 0,
) {
    companion object {
        val Empty = MonthStatusStats()
    }
}

object MonthStatusCounter {
    fun count(statuses: Iterable<ResolvedDayStatus?>): MonthStatusStats {
        var work = 0
        var rest = 0
        var sick = 0
        var overtime = 0
        for (status in statuses) {
            when (status) {
                ResolvedDayStatus.WORK -> work++
                ResolvedDayStatus.REST -> rest++
                ResolvedDayStatus.SICK -> sick++
                ResolvedDayStatus.OVERTIME -> overtime++
                null -> Unit
            }
        }
        return MonthStatusStats(
            workDays = work,
            restDays = rest,
            sickDays = sick,
            overtimeDays = overtime,
        )
    }
}
