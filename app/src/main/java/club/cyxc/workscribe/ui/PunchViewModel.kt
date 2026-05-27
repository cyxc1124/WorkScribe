package club.cyxc.workscribe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchRepository
import club.cyxc.workscribe.util.PunchTimeRules
import club.cyxc.workscribe.util.WorkDurationCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PunchUiState(
    val todayRecords: List<PunchRecord> = emptyList(),
    val isWorking: Boolean = false,
    val workDurationMillis: Long = 0L,
    val today: LocalDate = LocalDate.now(),
)

class PunchViewModel(
    application: Application,
    private val repository: PunchRepository,
) : AndroidViewModel(application) {

    val uiState: StateFlow<PunchUiState> = repository.observeTodayRecords()
        .map { todayRecords ->
            val isWorking = WorkDurationCalculator.isWorking(todayRecords)
            PunchUiState(
                todayRecords = todayRecords.sortedByDescending { it.timestamp },
                isWorking = isWorking,
                workDurationMillis = WorkDurationCalculator.calculate(todayRecords),
                today = LocalDate.now(),
            )
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PunchUiState(),
    )

    fun punch() {
        viewModelScope.launch {
            val type = PunchTimeRules.punchTypeFor(System.currentTimeMillis()) ?: return@launch
            repository.punch(type)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
        }
    }
}
