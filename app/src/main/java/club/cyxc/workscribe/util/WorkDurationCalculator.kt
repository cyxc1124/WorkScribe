package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType

object WorkDurationCalculator {
    /**
     * @param nowMillis Upper bound for an open IN session (typically now for today, end-of-day for past dates).
     * @param includeOpenSession When false, an unmatched IN is ignored (historical / makeup days).
     *                           When true, duration from the last open IN to [nowMillis] is included (today).
     */
    fun calculate(
        records: List<PunchRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        includeOpenSession: Boolean = true,
    ): Long {
        val sorted = records.sortedBy { it.timestamp }
        var total = 0L
        var lastIn: Long? = null

        for (record in sorted) {
            when (record.type) {
                PunchType.IN -> lastIn = record.timestamp
                PunchType.OUT -> {
                    lastIn?.let { inTime ->
                        total += record.timestamp - inTime
                    }
                    lastIn = null
                }
            }
        }

        if (includeOpenSession) {
            lastIn?.let { inTime ->
                total += nowMillis - inTime
            }
        }

        return total.coerceAtLeast(0)
    }

    fun isWorking(records: List<PunchRecord>): Boolean {
        val latest = records.maxByOrNull { it.timestamp } ?: return false
        return latest.type == PunchType.IN
    }
}
