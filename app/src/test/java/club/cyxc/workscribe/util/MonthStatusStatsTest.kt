package club.cyxc.workscribe.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthStatusStatsTest {

    @Test
    fun count_emptyStatuses_returnsZeros() {
        assertEquals(MonthStatusStats.Empty, MonthStatusCounter.count(emptyList()))
    }

    @Test
    fun count_nullStatuses_areIgnored() {
        assertEquals(
            MonthStatusStats.Empty,
            MonthStatusCounter.count(listOf(null, null)),
        )
    }

    @Test
    fun count_mixedStatuses_talliesEachType() {
        val statuses = listOf(
            ResolvedDayStatus.WORK,
            ResolvedDayStatus.WORK,
            ResolvedDayStatus.REST,
            ResolvedDayStatus.SICK,
            ResolvedDayStatus.OVERTIME,
            null,
        )
        assertEquals(
            MonthStatusStats(workDays = 2, restDays = 1, sickDays = 1, overtimeDays = 1),
            MonthStatusCounter.count(statuses),
        )
    }
}
