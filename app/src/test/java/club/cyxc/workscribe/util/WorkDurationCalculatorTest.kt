package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkDurationCalculatorTest {

    private val zoneId = ZoneId.systemDefault()
    private val date = LocalDate.of(2026, 5, 25)

    @Test
    fun completedPair_withDefaultLunch_deductsOneHour() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
            PunchRecord(id = 2, timestamp = at(18, 0), type = PunchType.OUT),
        )
        assertEquals(8 * 60 * 60 * 1000L, WorkDurationCalculator.calculate(records, at(18, 0), false))
    }

    @Test
    fun completedPair_withoutLunch_countsGrossDuration() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
            PunchRecord(id = 2, timestamp = at(18, 0), type = PunchType.OUT),
        )
        assertEquals(
            9 * 60 * 60 * 1000L,
            WorkDurationCalculator.calculate(
                records,
                at(18, 0),
                includeOpenSession = false,
                lunchBreakEnabled = false,
            ),
        )
    }

    @Test
    fun completedPair_shorterThanLunch_doesNotGoNegative() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
            PunchRecord(id = 2, timestamp = at(9, 30), type = PunchType.OUT),
        )
        assertEquals(30 * 60 * 1000L, WorkDurationCalculator.calculate(records, at(9, 30), false))
    }

    @Test
    fun pastDay_openInOnly_doesNotInflateWhenOpenSessionExcluded() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
        )
        val endOfDay = endOf(date)
        assertEquals(0L, WorkDurationCalculator.calculate(records, endOfDay, includeOpenSession = false))
    }

    @Test
    fun pastDay_openInOnly_inflatesToAnchorWhenOpenSessionIncluded() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
        )
        val endOfDay = endOf(date)
        assertEquals(
            endOfDay - at(9, 0) - 60 * 60 * 1000L,
            WorkDurationCalculator.calculate(records, endOfDay, includeOpenSession = true),
        )
    }

    @Test
    fun today_openIn_countsToNowWithLunchDeduction() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
        )
        val now = at(18, 0)
        assertEquals(
            now - at(9, 0) - 60 * 60 * 1000L,
            WorkDurationCalculator.calculate(records, now, includeOpenSession = true),
        )
    }

    @Test
    fun orphanOut_only_hasZeroDuration() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(18, 0), type = PunchType.OUT),
        )
        assertEquals(0L, WorkDurationCalculator.calculate(records, at(23, 59), false))
    }

    @Test
    fun multiplePairs_withLunchGap_doesNotDoubleDeduct() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
            PunchRecord(id = 2, timestamp = at(12, 0), type = PunchType.OUT),
            PunchRecord(id = 3, timestamp = at(13, 0), type = PunchType.IN),
            PunchRecord(id = 4, timestamp = at(18, 0), type = PunchType.OUT),
        )
        assertEquals(8 * 60 * 60 * 1000L, WorkDurationCalculator.calculate(records, at(18, 0), false))
    }

    @Test
    fun customLunchDuration_isRespected() {
        val records = listOf(
            PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
            PunchRecord(id = 2, timestamp = at(18, 0), type = PunchType.OUT),
        )
        assertEquals(
            8 * 60 * 60 * 1000L + 30 * 60 * 1000L,
            WorkDurationCalculator.calculate(
                records,
                at(18, 0),
                includeOpenSession = false,
                lunchBreakEnabled = true,
                lunchBreakMinutes = 30,
            ),
        )
    }

    @Test
    fun hasCompletedSession_requiresBothInAndOut() {
        assertEquals(
            false,
            WorkDurationCalculator.hasCompletedSession(
                listOf(PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN)),
            ),
        )
        assertEquals(
            false,
            WorkDurationCalculator.hasCompletedSession(
                listOf(PunchRecord(id = 1, timestamp = at(18, 0), type = PunchType.OUT)),
            ),
        )
        assertEquals(
            true,
            WorkDurationCalculator.hasCompletedSession(
                listOf(
                    PunchRecord(id = 1, timestamp = at(9, 0), type = PunchType.IN),
                    PunchRecord(id = 2, timestamp = at(18, 0), type = PunchType.OUT),
                ),
            ),
        )
    }

    private fun at(hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()

    private fun endOf(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
}
