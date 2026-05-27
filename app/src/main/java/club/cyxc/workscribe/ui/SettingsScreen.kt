package club.cyxc.workscribe.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import club.cyxc.workscribe.data.PunchTimeConfig

private enum class TimeField {
    CLOCK_IN_START,
    CLOCK_IN_END,
    CLOCK_OUT_START,
    CLOCK_OUT_END,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onUpdateClockInStart: (Int) -> Unit,
    onUpdateClockInEnd: (Int) -> Unit,
    onUpdateClockOutStart: (Int) -> Unit,
    onUpdateClockOutEnd: (Int) -> Unit,
    onResetToDefaults: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingField by remember { mutableStateOf<TimeField?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("打卡时间设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "自定义上班、下班打卡时段。非打卡时段为上班结束至下班开始之间。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                TimeRuleSection(
                    title = "上班打卡",
                    startLabel = "开始时间",
                    endLabel = "结束时间",
                    startMinutes = uiState.clockInStartMinutes,
                    endMinutes = uiState.clockInEndMinutes,
                    onEditStart = { editingField = TimeField.CLOCK_IN_START },
                    onEditEnd = { editingField = TimeField.CLOCK_IN_END },
                )
            }
            item {
                TimeRuleSection(
                    title = "下班打卡",
                    startLabel = "开始时间",
                    endLabel = "结束时间",
                    startMinutes = uiState.clockOutStartMinutes,
                    endMinutes = uiState.clockOutEndMinutes,
                    onEditStart = { editingField = TimeField.CLOCK_OUT_START },
                    onEditEnd = { editingField = TimeField.CLOCK_OUT_END },
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "非打卡时段",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.offHoursSummary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "由上班结束与下班开始时间自动计算",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (uiState.validationError != null) {
                item {
                    Text(
                        text = uiState.validationError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (uiState.saveMessage != null) {
                item {
                    Text(
                        text = uiState.saveMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onResetToDefaults,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("恢复默认")
                    }
                    Button(
                        onClick = onSave,
                        enabled = uiState.validationError == null && !uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (uiState.isSaving) "保存中…" else "保存")
                    }
                }
            }
        }
    }

    editingField?.let { field ->
        val initialMinutes = when (field) {
            TimeField.CLOCK_IN_START -> uiState.clockInStartMinutes
            TimeField.CLOCK_IN_END -> uiState.clockInEndMinutes
            TimeField.CLOCK_OUT_START -> uiState.clockOutStartMinutes
            TimeField.CLOCK_OUT_END -> uiState.clockOutEndMinutes
        }
        val initialTime = PunchTimeConfig.minutesToLocalTime(initialMinutes)
        val title = when (field) {
            TimeField.CLOCK_IN_START -> "上班打卡开始时间"
            TimeField.CLOCK_IN_END -> "上班打卡结束时间"
            TimeField.CLOCK_OUT_START -> "下班打卡开始时间"
            TimeField.CLOCK_OUT_END -> "下班打卡结束时间"
        }

        WheelTimePickerBottomSheet(
            title = title,
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            onDismiss = { editingField = null },
            onConfirm = { hour, minute ->
                val minutes = hour * 60 + minute
                when (field) {
                    TimeField.CLOCK_IN_START -> onUpdateClockInStart(minutes)
                    TimeField.CLOCK_IN_END -> onUpdateClockInEnd(minutes)
                    TimeField.CLOCK_OUT_START -> onUpdateClockOutStart(minutes)
                    TimeField.CLOCK_OUT_END -> onUpdateClockOutEnd(minutes)
                }
                editingField = null
            },
        )
    }
}

@Composable
private fun TimeRuleSection(
    title: String,
    startLabel: String,
    endLabel: String,
    startMinutes: Int,
    endMinutes: Int,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TimePickerRow(
                label = startLabel,
                minutes = startMinutes,
                onClick = onEditStart,
            )
            TimePickerRow(
                label = endLabel,
                minutes = endMinutes,
                onClick = onEditEnd,
            )
        }
    }
}

@Composable
private fun TimePickerRow(
    label: String,
    minutes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = PunchTimeConfig.formatMinutes(minutes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
