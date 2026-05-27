package club.cyxc.workscribe.data

import androidx.room.TypeConverter

class PunchTypeConverters {
    @TypeConverter
    fun fromPunchType(type: PunchType): String = type.name

    @TypeConverter
    fun toPunchType(value: String): PunchType = PunchType.valueOf(value)

    @TypeConverter
    fun fromDayStatusType(type: DayStatusType): String = type.name

    @TypeConverter
    fun toDayStatusType(value: String): DayStatusType = DayStatusType.valueOf(value)
}
