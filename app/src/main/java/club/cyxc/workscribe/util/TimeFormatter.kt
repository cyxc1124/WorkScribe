package club.cyxc.workscribe.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
    private val recordTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)

    fun formatClock(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).format(timeFormatter)
    }

    fun formatDate(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return date.atStartOfDay(zoneId).format(dateFormatter)
    }

    fun formatRecordTime(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).format(recordTimeFormatter)
    }

    fun formatDuration(totalMillis: Long): String {
        val totalSeconds = totalMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> String.format(Locale.CHINA, "%d小时%02d分%02d秒", hours, minutes, seconds)
            minutes > 0 -> String.format(Locale.CHINA, "%d分%02d秒", minutes, seconds)
            else -> String.format(Locale.CHINA, "%d秒", seconds)
        }
    }
}
