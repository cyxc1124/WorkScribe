package club.cyxc.workscribe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import club.cyxc.workscribe.data.PunchSettingsRepository
import club.cyxc.workscribe.data.PunchTimeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val clockInStartMinutes: Int = PunchTimeConfig.DEFAULT_CLOCK_IN_START_MINUTES,
    val clockInEndMinutes: Int = PunchTimeConfig.DEFAULT_CLOCK_IN_END_MINUTES,
    val clockOutStartMinutes: Int = PunchTimeConfig.DEFAULT_CLOCK_OUT_START_MINUTES,
    val clockOutEndMinutes: Int = PunchTimeConfig.DEFAULT_CLOCK_OUT_END_MINUTES,
    val validationError: String? = null,
    val saveMessage: String? = null,
    val isSaving: Boolean = false,
) {
    val offHoursSummary: String
        get() = "${PunchTimeConfig.formatMinutes(clockInEndMinutes)} – " +
            PunchTimeConfig.formatMinutes(clockOutStartMinutes)

    fun toConfig(): PunchTimeConfig = PunchTimeConfig(
        clockInStartMinutes = clockInStartMinutes,
        clockInEndMinutes = clockInEndMinutes,
        clockOutStartMinutes = clockOutStartMinutes,
        clockOutEndMinutes = clockOutEndMinutes,
    )
}

class SettingsViewModel(
    application: Application,
    private val settingsRepository: PunchSettingsRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.configFlow.collect { config ->
                _uiState.update { current ->
                    current.copy(
                        clockInStartMinutes = config.clockInStartMinutes,
                        clockInEndMinutes = config.clockInEndMinutes,
                        clockOutStartMinutes = config.clockOutStartMinutes,
                        clockOutEndMinutes = config.clockOutEndMinutes,
                        validationError = null,
                    )
                }
            }
        }
    }

    fun updateClockInStart(minutes: Int) {
        updateDraft { it.copy(clockInStartMinutes = minutes, saveMessage = null) }
    }

    fun updateClockInEnd(minutes: Int) {
        updateDraft { it.copy(clockInEndMinutes = minutes, saveMessage = null) }
    }

    fun updateClockOutStart(minutes: Int) {
        updateDraft { it.copy(clockOutStartMinutes = minutes, saveMessage = null) }
    }

    fun updateClockOutEnd(minutes: Int) {
        updateDraft { it.copy(clockOutEndMinutes = minutes, saveMessage = null) }
    }

    fun resetToDefaults() {
        updateDraft {
            SettingsUiState(saveMessage = null)
        }
    }

    fun save() {
        val state = _uiState.value
        val validationError = state.toConfig().validate()
        if (validationError != null) {
            _uiState.update { it.copy(validationError = validationError, saveMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null) }
            try {
                settingsRepository.saveConfig(state.toConfig())
                _uiState.update {
                    it.copy(isSaving = false, saveMessage = "已保存")
                }
            } catch (error: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        validationError = error.message,
                    )
                }
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    private fun updateDraft(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { current ->
            val next = transform(current)
            next.copy(validationError = next.toConfig().validate())
        }
    }
}
