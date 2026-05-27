package club.cyxc.workscribe.util

import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType

object WorkDurationCalculator {
    fun calculate(records: List<PunchRecord>, nowMillis: Long = System.currentTimeMillis()): Long {
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

        lastIn?.let { inTime ->
            total += nowMillis - inTime
        }

        return total.coerceAtLeast(0)
    }

    fun isWorking(records: List<PunchRecord>): Boolean {
        val latest = records.maxByOrNull { it.timestamp } ?: return false
        return latest.type == PunchType.IN
    }
}
