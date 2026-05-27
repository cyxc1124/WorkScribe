package club.cyxc.workscribe.data

import androidx.room.TypeConverter

class PunchTypeConverters {
    @TypeConverter
    fun fromPunchType(type: PunchType): String = type.name

    @TypeConverter
    fun toPunchType(value: String): PunchType = PunchType.valueOf(value)
}
