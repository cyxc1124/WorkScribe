package club.cyxc.workscribe.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate

private enum class AppTab {
    PUNCH,
    CALENDAR,
}

@Composable
fun WorkScribeApp(
    punchViewModel: PunchViewModel,
    calendarViewModel: CalendarViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.PUNCH) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showMakeupPunch by rememberSaveable { mutableStateOf(false) }
    var makeupPunchInitialDate by rememberSaveable { mutableStateOf<String?>(null) }

    if (showMakeupPunch) {
        val initialDate = makeupPunchInitialDate?.let(LocalDate::parse)
            ?: LocalDate.now().minusDays(1)
        MakeupPunchDialog(
            initialDate = initialDate,
            onDismiss = { showMakeupPunch = false },
            onConfirm = { timestamp, type ->
                punchViewModel.makeupPunch(timestamp, type) { error ->
                    if (error == null) {
                        showMakeupPunch = false
                    }
                }
            },
        )
    }

    if (showSettings) {
        val settingsUiState by settingsViewModel.uiState.collectAsState()
        SettingsScreen(
            uiState = settingsUiState,
            onBack = { showSettings = false },
            onUpdateClockInStart = settingsViewModel::updateClockInStart,
            onUpdateClockInEnd = settingsViewModel::updateClockInEnd,
            onUpdateClockOutStart = settingsViewModel::updateClockOutStart,
            onUpdateClockOutEnd = settingsViewModel::updateClockOutEnd,
            onResetToDefaults = settingsViewModel::resetToDefaults,
            onSave = settingsViewModel::save,
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.PUNCH,
                    onClick = { selectedTab = AppTab.PUNCH },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                        )
                    },
                    label = { Text("打卡") },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.CALENDAR,
                    onClick = { selectedTab = AppTab.CALENDAR },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                        )
                    },
                    label = { Text("日历") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.PUNCH -> {
                val punchUiState by punchViewModel.uiState.collectAsState()
                PunchScreen(
                    uiState = punchUiState,
                    onPunch = punchViewModel::punch,
                    onDeleteRecord = punchViewModel::deleteRecord,
                    onOpenSettings = { showSettings = true },
                    onOpenMakeupPunch = {
                        makeupPunchInitialDate = null
                        showMakeupPunch = true
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            AppTab.CALENDAR -> {
                val calendarUiState by calendarViewModel.uiState.collectAsState()
                CalendarScreen(
                    uiState = calendarUiState,
                    onSelectDate = calendarViewModel::selectDate,
                    onSelectMonth = calendarViewModel::selectMonth,
                    onPreviousMonth = calendarViewModel::previousMonth,
                    onNextMonth = calendarViewModel::nextMonth,
                    onGoToToday = calendarViewModel::goToToday,
                    onSetDayStatus = calendarViewModel::setDayStatus,
                    onClearDayStatus = calendarViewModel::clearDayStatus,
                    onOpenMakeupPunch = { date ->
                        makeupPunchInitialDate = date.toString()
                        showMakeupPunch = true
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
