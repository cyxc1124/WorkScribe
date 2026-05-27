package club.cyxc.workscribe

import android.app.Application
import club.cyxc.workscribe.data.AppDatabase
import club.cyxc.workscribe.data.PunchRepository
import club.cyxc.workscribe.data.PunchSettingsRepository

class WorkScribeApplication : Application() {
    val repository: PunchRepository by lazy {
        val database = AppDatabase.getInstance(this)
        PunchRepository(database.punchDao(), database.dayStatusDao())
    }

    val settingsRepository: PunchSettingsRepository by lazy {
        PunchSettingsRepository.from(this)
    }
}
