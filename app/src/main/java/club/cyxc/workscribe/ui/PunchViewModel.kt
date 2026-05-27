package club.cyxc.workscribe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchRepository
import club.cyxc.workscribe.data.PunchSettingsRepository
import club.cyxc.workscribe.util.PunchTimeRules
import club.cyxc.workscribe.util.WorkDurationCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PunchUiState(
    val todayRecords: List<PunchRecord> = emptyList(),
    val isWorking: Boolean = false,
    val workDurationMillis: Long = 0L,
    val today: LocalDate = LocalDate.now(),
    val punchTimeRules: PunchTimeRules = PunchTimeRules.default(),
)

class PunchViewModel(
    application: Application,
    private val repository: PunchRepository,
    private val settingsRepository: PunchSettingsRepository,
) : AndroidViewModel(application) {

    val uiState: StateFlow<PunchUiState> = combine(
        repository.observeTodayRecords(),
        settingsRepository.configFlow,
    ) { todayRecords, config ->
        val isWorking = WorkDurationCalculator.isWorking(todayRecords)
        PunchUiState(
            todayRecords = todayRecords.sortedByDescending { it.timestamp },
            isWorking = isWorking,
            workDurationMillis = WorkDurationCalculator.calculate(todayRecords),
            today = LocalDate.now(),
            punchTimeRules = PunchTimeRules(config),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PunchUiState(),
    )

    fun punch() {
        viewModelScope.launch {
            val state = uiState.value
            val type = state.punchTimeRules.punchTypeFor(
                millis = System.currentTimeMillis(),
                todayRecords = state.todayRecords,
            ) ?: return@launch
            repository.punch(type)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
        }
    }
}
