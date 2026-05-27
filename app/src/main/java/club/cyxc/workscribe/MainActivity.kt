package club.cyxc.workscribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import club.cyxc.workscribe.ui.CalendarViewModel
import club.cyxc.workscribe.ui.PunchViewModel
import club.cyxc.workscribe.ui.WorkScribeApp
import club.cyxc.workscribe.ui.theme.WorkScribeTheme

class MainActivity : ComponentActivity() {

    private val punchViewModel: PunchViewModel by viewModels {
        viewModelFactory {
            PunchViewModel(application, (application as WorkScribeApplication).repository)
        }
    }

    private val calendarViewModel: CalendarViewModel by viewModels {
        viewModelFactory {
            CalendarViewModel(application, (application as WorkScribeApplication).repository)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkScribeTheme {
                WorkScribeApp(
                    punchViewModel = punchViewModel,
                    calendarViewModel = calendarViewModel,
                )
            }
        }
    }

    private inline fun <reified VM : ViewModel> viewModelFactory(
        crossinline creator: () -> VM,
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (!VM::class.java.isAssignableFrom(modelClass)) {
                    error("Unknown ViewModel class: ${modelClass.name}")
                }
                return creator() as T
            }
        }
    }
}
