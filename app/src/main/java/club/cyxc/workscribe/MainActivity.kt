package club.cyxc.workscribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import club.cyxc.workscribe.ui.PunchScreen
import club.cyxc.workscribe.ui.PunchViewModel
import club.cyxc.workscribe.ui.theme.WorkScribeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PunchViewModel by viewModels {
        val app = application as WorkScribeApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PunchViewModel(application, app.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkScribeTheme {
                val uiState by viewModel.uiState.collectAsState()

                PunchScreen(
                    uiState = uiState,
                    onPunch = viewModel::punch,
                    onDeleteRecord = viewModel::deleteRecord,
                )
            }
        }
    }
}
