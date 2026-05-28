package club.cyxc.workscribe.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)
    private val calendarDayHeaderFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val calendarWeekdayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.CHINA)
    private val recordTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val yearMonthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

    fun formatClock(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).format(timeFormatter)
    }

    fun formatDate(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return date.atStartOfDay(zoneId).format(dateFormatter)
    }

    /** 日历选中日标题，如「5月25日 周一」。 */
    fun formatCalendarDayHeader(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zoned = date.atStartOfDay(zoneId)
        return "${zoned.format(calendarDayHeaderFormatter)} ${zoned.format(calendarWeekdayFormatter)}"
    }

    fun formatCalendarDayYear(date: LocalDate): String {
        return formatYear(date.year)
    }

    fun formatYear(year: Int): String {
        return String.format(Locale.CHINA, "%d年", year)
    }

    fun formatMonth(month: Int): String {
        return String.format(Locale.CHINA, "%d月", month)
    }

    fun isCalendarDayYearVisible(date: LocalDate): Boolean {
        return date.year != LocalDate.now().year
    }

    fun formatYearMonth(yearMonth: YearMonth): String {
        return yearMonth.atDay(1).format(yearMonthFormatter)
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

    fun formatHoursBadge(totalMillis: Long): String {
        val totalMinutes = totalMillis / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0L || hours > 0) {
            String.format(Locale.CHINA, "%d h", hours)
        } else {
            String.format(Locale.CHINA, "%d h", 1)
        }
    }

    fun formatMonthStripLabel(yearMonth: YearMonth): String {
        return yearMonth.format(DateTimeFormatter.ofPattern("M月", Locale.CHINA))
    }

    fun formatMonthStripLabelFull(yearMonth: YearMonth): String {
        return yearMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA))
    }
}
