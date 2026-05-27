package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchTimeConfig
import club.cyxc.workscribe.data.PunchType

object WorkDurationCalculator {
    /**
     * @param nowMillis Upper bound for an open IN session (typically now for today, end-of-day for past dates).
     * @param includeOpenSession When false, an unmatched IN is ignored (historical / makeup days).
     *                           When true, duration from the last open IN to [nowMillis] is included (today).
     * @param lunchBreakEnabled When true, deduct [lunchBreakMinutes] from gross duration once per day
     *                          if the work span does not already include a gap at least that long.
     */
    fun calculate(
        records: List<PunchRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        includeOpenSession: Boolean = true,
        lunchBreakEnabled: Boolean = PunchTimeConfig.DEFAULT_LUNCH_BREAK_ENABLED,
        lunchBreakMinutes: Int = PunchTimeConfig.DEFAULT_LUNCH_BREAK_MINUTES,
    ): Long {
        val sorted = records.sortedBy { it.timestamp }
        var gross = 0L
        var lastIn: Long? = null
        var firstIn: Long? = null
        var lastEnd: Long? = null

        for (record in sorted) {
            when (record.type) {
                PunchType.IN -> {
                    if (firstIn == null) {
                        firstIn = record.timestamp
                    }
                    lastIn = record.timestamp
                }
                PunchType.OUT -> {
                    lastIn?.let { inTime ->
                        gross += record.timestamp - inTime
                    }
                    lastEnd = record.timestamp
                    lastIn = null
                }
            }
        }

        if (includeOpenSession) {
            lastIn?.let { inTime ->
                gross += nowMillis - inTime
                if (firstIn == null) {
                    firstIn = inTime
                }
                lastEnd = nowMillis
            }
        }

        return applyLunchDeduction(
            grossMillis = gross.coerceAtLeast(0),
            firstInMillis = firstIn,
            lastEndMillis = lastEnd,
            lunchBreakEnabled = lunchBreakEnabled,
            lunchBreakMinutes = lunchBreakMinutes,
        )
    }

    internal fun applyLunchDeduction(
        grossMillis: Long,
        firstInMillis: Long?,
        lastEndMillis: Long?,
        lunchBreakEnabled: Boolean,
        lunchBreakMinutes: Int,
    ): Long {
        if (!lunchBreakEnabled || lunchBreakMinutes <= 0) {
            return grossMillis
        }
        val lunchMillis = lunchBreakMinutes.toLong() * 60_000
        if (grossMillis <= lunchMillis) {
            return grossMillis
        }
        if (firstInMillis == null || lastEndMillis == null) {
            return grossMillis
        }

        val spanMillis = lastEndMillis - firstInMillis
        val gapMillis = spanMillis - grossMillis
        if (gapMillis >= lunchMillis) {
            return grossMillis
        }

        return grossMillis - lunchMillis
    }

    fun isWorking(records: List<PunchRecord>): Boolean {
        val latest = records.maxByOrNull { it.timestamp } ?: return false
        return latest.type == PunchType.IN
    }

    /** True when the day has at least one clock-in and one clock-out (completed session). */
    fun hasCompletedSession(records: List<PunchRecord>): Boolean {
        val hasIn = records.any { it.type == PunchType.IN }
        val hasOut = records.any { it.type == PunchType.OUT }
        return hasIn && hasOut
    }
}
