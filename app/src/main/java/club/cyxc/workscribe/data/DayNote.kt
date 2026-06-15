package club.cyxc.workscribe.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_notes")
data class DayNote(
    @PrimaryKey
    val dateEpochDay: Long,
    val content: String,
    val updatedAt: Long,
)
