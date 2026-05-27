package club.cyxc.workscribe.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object MakeupPunchValidator {
    fun validate(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        val now = System.currentTimeMillis()
        if (timestamp > now) {
            return "不能选择未来时间"
        }
        val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        if (date.isAfter(LocalDate.now(zoneId))) {
            return "不能选择未来日期"
        }
        return null
    }
}
