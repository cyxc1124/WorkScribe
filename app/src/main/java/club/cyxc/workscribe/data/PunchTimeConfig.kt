package club.cyxc.workscribe.data

import java.time.LocalTime

data class PunchTimeConfig(
    val clockInStartMinutes: Int = DEFAULT_CLOCK_IN_START_MINUTES,
    val clockInEndMinutes: Int = DEFAULT_CLOCK_IN_END_MINUTES,
    val clockOutStartMinutes: Int = DEFAULT_CLOCK_OUT_START_MINUTES,
    val clockOutEndMinutes: Int = DEFAULT_CLOCK_OUT_END_MINUTES,
) {
    val clockInStart: LocalTime get() = minutesToLocalTime(clockInStartMinutes)
    val clockInEnd: LocalTime get() = minutesToLocalTime(clockInEndMinutes)
    val clockOutStart: LocalTime get() = minutesToLocalTime(clockOutStartMinutes)
    val clockOutEnd: LocalTime get() = minutesToLocalTime(clockOutEndMinutes)

    fun validate(): String? {
        if (clockInStartMinutes >= clockInEndMinutes) {
            return "上班结束时间须晚于开始时间"
        }
        if (clockOutStartMinutes >= clockOutEndMinutes) {
            return "下班结束时间须晚于开始时间"
        }
        if (clockInEndMinutes > clockOutStartMinutes) {
            return "上班结束时间不能晚于下班开始时间"
        }
        return null
    }

    companion object {
        const val DEFAULT_CLOCK_IN_START_MINUTES = 0
        const val DEFAULT_CLOCK_IN_END_MINUTES = 12 * 60
        const val DEFAULT_CLOCK_OUT_START_MINUTES = 17 * 60 + 30
        const val DEFAULT_CLOCK_OUT_END_MINUTES = 23 * 60 + 59

        val DEFAULT = PunchTimeConfig()

        fun minutesToLocalTime(minutes: Int): LocalTime {
            val hour = minutes / 60
            val minute = minutes % 60
            return LocalTime.of(hour, minute)
        }

        fun localTimeToMinutes(time: LocalTime): Int = time.hour * 60 + time.minute

        fun formatMinutes(minutes: Int): String {
            val time = minutesToLocalTime(minutes)
            return String.format("%02d:%02d", time.hour, time.minute)
        }
    }
}
