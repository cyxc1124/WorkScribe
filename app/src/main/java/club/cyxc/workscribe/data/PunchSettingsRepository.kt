package club.cyxc.workscribe.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.punchSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "punch_settings",
)

class PunchSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val configFlow: Flow<PunchTimeConfig> = dataStore.data.map { preferences ->
        PunchTimeConfig(
            clockInStartMinutes = preferences[CLOCK_IN_START] ?: PunchTimeConfig.DEFAULT_CLOCK_IN_START_MINUTES,
            clockInEndMinutes = preferences[CLOCK_IN_END] ?: PunchTimeConfig.DEFAULT_CLOCK_IN_END_MINUTES,
            clockOutStartMinutes = preferences[CLOCK_OUT_START] ?: PunchTimeConfig.DEFAULT_CLOCK_OUT_START_MINUTES,
            clockOutEndMinutes = preferences[CLOCK_OUT_END] ?: PunchTimeConfig.DEFAULT_CLOCK_OUT_END_MINUTES,
        )
    }

    suspend fun saveConfig(config: PunchTimeConfig) {
        val validationError = config.validate()
        require(validationError == null) { validationError ?: "无效的打卡时间设置" }
        dataStore.edit { preferences ->
            preferences[CLOCK_IN_START] = config.clockInStartMinutes
            preferences[CLOCK_IN_END] = config.clockInEndMinutes
            preferences[CLOCK_OUT_START] = config.clockOutStartMinutes
            preferences[CLOCK_OUT_END] = config.clockOutEndMinutes
        }
    }

    companion object {
        private val CLOCK_IN_START = intPreferencesKey("clock_in_start_minutes")
        private val CLOCK_IN_END = intPreferencesKey("clock_in_end_minutes")
        private val CLOCK_OUT_START = intPreferencesKey("clock_out_start_minutes")
        private val CLOCK_OUT_END = intPreferencesKey("clock_out_end_minutes")

        fun from(context: Context): PunchSettingsRepository {
            return PunchSettingsRepository(context.punchSettingsDataStore)
        }
    }
}
