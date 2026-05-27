package club.cyxc.workscribe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "punch_records")
data class PunchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: PunchType,
)
