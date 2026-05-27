package club.cyxc.workscribe.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType
import club.cyxc.workscribe.util.OffHoursPunchState
import club.cyxc.workscribe.util.PunchTimeRules
import club.cyxc.workscribe.util.PunchWindow
import club.cyxc.workscribe.util.TimeFormatter
import club.cyxc.workscribe.util.WorkDurationCalculator
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchScreen(
    uiState: PunchUiState,
    onPunch: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val liveDuration = remember(uiState.todayRecords, nowMillis) {
        WorkDurationCalculator.calculate(uiState.todayRecords, nowMillis)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("WorkScribe") },
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
                ClockHeader(today = uiState.today, nowMillis = nowMillis)
            }
            item {
                StatusCard(
                    isWorking = uiState.isWorking,
                    workDurationMillis = liveDuration,
                )
            }
            item {
                PunchButton(
                    nowMillis = nowMillis,
                    todayRecords = uiState.todayRecords,
                    onPunch = onPunch,
                )
            }
            item {
                Text(
                    text = "今日记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (uiState.todayRecords.isEmpty()) {
                item {
                    EmptyRecordsHint()
                }
            } else {
                items(uiState.todayRecords, key = { it.id }) { record ->
                    RecordItem(
                        record = record,
                        onDelete = { onDeleteRecord(record.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockHeader(today: LocalDate, nowMillis: Long) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = TimeFormatter.formatDate(today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = TimeFormatter.formatClock(nowMillis),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatusCard(isWorking: Boolean, workDurationMillis: Long) {
    val containerColor by animateColorAsState(
        targetValue = if (isWorking) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "statusColor",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWorking) "工作中" else "未上班",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "今日工时",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = TimeFormatter.formatDuration(workDurationMillis),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PunchButton(
    nowMillis: Long,
    todayRecords: List<PunchRecord>,
    onPunch: () -> Unit,
) {
    when (PunchTimeRules.windowAt(nowMillis)) {
        PunchWindow.CLOCK_IN -> ActivePunchButton(
            label = "上班打卡",
            icon = Icons.Default.Login,
            containerColor = MaterialTheme.colorScheme.primary,
            onPunch = onPunch,
        )
        PunchWindow.CLOCK_OUT -> ActivePunchButton(
            label = "下班打卡",
            icon = Icons.Default.Logout,
            containerColor = MaterialTheme.colorScheme.tertiary,
            onPunch = onPunch,
        )
        PunchWindow.OFF_HOURS -> OffHoursPunchButton(
            todayRecords = todayRecords,
            onPunch = onPunch,
        )
    }
}

@Composable
private fun ActivePunchButton(
    label: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onPunch: () -> Unit,
) {
    Button(
        onClick = onPunch,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OffHoursPunchButton(
    todayRecords: List<PunchRecord>,
    onPunch: () -> Unit,
) {
    when (PunchTimeRules.offHoursPunchState(todayRecords)) {
        OffHoursPunchState.MAKEUP_IN -> ActivePunchButton(
            label = PunchTimeRules.MAKEUP_IN_LABEL,
            icon = Icons.Default.Login,
            containerColor = MaterialTheme.colorScheme.primary,
            onPunch = onPunch,
        )
        OffHoursPunchState.WAIT_FOR_OUT -> DisabledPunchButton(
            label = PunchTimeRules.WAIT_FOR_OUT_LABEL,
            hint = PunchTimeRules.WAIT_FOR_OUT_HINT,
        )
        OffHoursPunchState.BLOCKED -> DisabledPunchButton(
            label = PunchTimeRules.OFF_HOURS_BUTTON,
            hint = PunchTimeRules.OFF_HOURS_HINT,
        )
    }
}

@Composable
private fun DisabledPunchButton(label: String, hint: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyRecordsHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "今天还没有打卡记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordItem(record: PunchRecord, onDelete: () -> Unit) {
    val isClockIn = record.type == PunchType.IN
    val typeLabel = if (isClockIn) "上班" else "下班"
    val icon = if (isClockIn) Icons.Default.Login else Icons.Default.Logout
    val accentColor = if (isClockIn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = TimeFormatter.formatRecordTime(record.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除记录",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
