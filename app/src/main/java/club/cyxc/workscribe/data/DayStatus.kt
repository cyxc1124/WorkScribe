package club.cyxc.workscribe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_statuses")
data class DayStatus(
    @PrimaryKey
    val dateEpochDay: Long,
    val type: DayStatusType,
)
