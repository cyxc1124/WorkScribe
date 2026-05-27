package club.cyxc.workscribe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.cyxc.workscribe.data.DayStatusType
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchType
import club.cyxc.workscribe.util.ResolvedDayStatus
import club.cyxc.workscribe.util.TimeFormatter
import java.time.LocalDate
import java.time.YearMonth

private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

private val WorkGreen = Color(0xFF43A047)
private val WorkGreenBg = Color(0xFFE8F5E9)
private val SickRed = Color(0xFFE53935)
private val SickRedBg = Color(0xFFFFEBEE)
private val OvertimeYellow = Color(0xFFF9A825)
private val OvertimeYellowBg = Color(0xFFFFF8E1)
private val RestBlue = Color(0xFF1E88E5)
private val RestBlueBg = Color(0xFFE3F2FD)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onSetDayStatus: (LocalDate, DayStatusType) -> Unit,
    onClearDayStatus: (LocalDate) -> Unit,
    onOpenMakeupPunch: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("打卡日历") },
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthStrip(
                    currentMonth = uiState.currentMonth,
                    monthStrip = uiState.monthStrip,
                    onSelectMonth = onSelectMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onGoToToday = onGoToToday,
                )
            }
            item {
                CalendarGrid(
                    gridDays = uiState.gridDays,
                    onSelectDate = onSelectDate,
                )
            }
            item {
                ColorLegend()
            }
            item {
                SelectedDayHeader(selectedDate = uiState.selectedDate)
            }
            if (uiState.selectedDate != null) {
                item {
                    DayStatusSelector(
                        selectedStatus = uiState.selectedDayStatus,
                        manualStatus = uiState.selectedDayManualStatus,
                        onSelect = { type ->
                            onSetDayStatus(uiState.selectedDate, type)
                        },
                        onClear = { onClearDayStatus(uiState.selectedDate) },
                    )
                }
                if (!uiState.selectedDate.isAfter(LocalDate.now())) {
                    item {
                        OutlinedButton(
                            onClick = { onOpenMakeupPunch(uiState.selectedDate) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("补卡")
                        }
                    }
                }
            }
            if (uiState.selectedDate == null) {
                item {
                    EmptyDayHint(message = "请选择日期查看详情")
                }
            } else if (uiState.selectedDayRecords.isEmpty()) {
                item {
                    EmptyDayHint(message = "这一天还没有打卡记录")
                }
            } else {
                item {
                    DayDurationCard(workDurationMillis = uiState.workDurationMillis)
                }
                items(uiState.selectedDayRecords, key = { it.id }) { record ->
                    CalendarRecordItem(record = record)
                }
            }
        }
    }
}

@Composable
private fun MonthStrip(
    currentMonth: YearMonth,
    monthStrip: List<YearMonth>,
    onSelectMonth: (YearMonth) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上一个月",
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            monthStrip.forEach { month ->
                val isCurrent = month == currentMonth
                Text(
                    text = if (isCurrent) {
                        TimeFormatter.formatMonthStripLabelFull(month)
                    } else {
                        TimeFormatter.formatMonthStripLabel(month)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectMonth(month) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = if (isCurrent) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                )
            }
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一个月",
            )
        }
        OutlinedButton(onClick = onGoToToday) {
            Text("今天")
        }
    }
}

@Composable
private fun CalendarGrid(
    gridDays: List<CalendarDayCell>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val rowCount = gridDays.size / 7

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                WeekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            for (row in 0 until rowCount) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (column in 0 until 7) {
                        val cell = gridDays[row * 7 + column]
                        DayCell(
                            cell = cell,
                            onClick = { onSelectDate(cell.date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarDayCell,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = statusColors(cell.status)
    val showHours = cell.workDurationMillis > 0 &&
        (cell.status == ResolvedDayStatus.WORK || cell.status == ResolvedDayStatus.OVERTIME)
    val showStatusBlock = cell.status == ResolvedDayStatus.SICK ||
        cell.status == ResolvedDayStatus.REST ||
        (cell.status == ResolvedDayStatus.OVERTIME && cell.workDurationMillis == 0L)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    cell.isSelected -> colors.background.copy(alpha = 0.95f)
                    colors.background != Color.Transparent -> colors.background
                    else -> MaterialTheme.colorScheme.surface
                },
            )
            .border(
                width = when {
                    cell.isSelected -> 2.dp
                    cell.isToday -> 1.5.dp
                    else -> 0.5.dp
                },
                color = when {
                    cell.isSelected -> colors.accent
                    cell.isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(3.dp),
    ) {
        Text(
            text = cell.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (cell.isToday || cell.isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                cell.status != null -> colors.onBackground
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (showHours) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(4.dp),
                color = colors.accent.copy(alpha = 0.85f),
            ) {
                Text(
                    text = TimeFormatter.formatHoursBadge(cell.workDurationMillis),
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }

        if (showStatusBlock) {
            StatusBlock(
                status = cell.status!!,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        } else if (cell.status == ResolvedDayStatus.OVERTIME && cell.workDurationMillis > 0) {
            Text(
                text = "加班",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (cell.status == ResolvedDayStatus.WORK && cell.workDurationMillis == 0L) {
            Text(
                text = "上班",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = colors.accent,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusBlock(
    status: ResolvedDayStatus,
    modifier: Modifier = Modifier,
) {
    val colors = statusColors(status)
    val (label, icon) = statusLabelAndIcon(status)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = colors.accent.copy(alpha = 0.9f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ColorLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LegendItem(color = WorkGreen, label = "上班")
        LegendItem(color = SickRed, label = "病假")
        LegendItem(color = OvertimeYellow, label = "加班")
        LegendItem(color = RestBlue, label = "休息")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayStatusSelector(
    selectedStatus: ResolvedDayStatus?,
    manualStatus: DayStatusType?,
    onSelect: (DayStatusType) -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "日期类型",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DayStatusType.entries.forEach { type ->
                    val resolved = type.toResolved()
                    FilterChip(
                        selected = manualStatus == type ||
                            (manualStatus == null && selectedStatus == resolved),
                        onClick = { onSelect(type) },
                        label = { Text(statusLabel(type)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = statusColors(resolved).background,
                            selectedLabelColor = statusColors(resolved).accent,
                        ),
                    )
                }
                FilterChip(
                    selected = manualStatus == null,
                    onClick = onClear,
                    label = { Text("自动") },
                )
            }
            Text(
                text = "自动：有打卡记录为上班/加班，周末无记录为休息（不影响打卡）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedDayHeader(selectedDate: LocalDate?) {
    Text(
        text = if (selectedDate != null) {
            TimeFormatter.formatDate(selectedDate)
        } else {
            "选择日期"
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DayDurationCard(workDurationMillis: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "当日工时",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = TimeFormatter.formatDuration(workDurationMillis),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EmptyDayHint(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarRecordItem(record: PunchRecord) {
    val isClockIn = record.type == PunchType.IN
    val typeLabel = if (isClockIn) "上班" else "下班"
    val icon = if (isClockIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout
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
                shape = RoundedCornerShape(50),
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
            Column {
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
        }
    }
}

private data class StatusColors(
    val background: Color,
    val accent: Color,
    val onBackground: Color,
)

@Composable
private fun statusColors(status: ResolvedDayStatus?): StatusColors {
    return when (status) {
        ResolvedDayStatus.WORK -> StatusColors(WorkGreenBg, WorkGreen, WorkGreen)
        ResolvedDayStatus.SICK -> StatusColors(SickRedBg, SickRed, SickRed)
        ResolvedDayStatus.OVERTIME -> StatusColors(OvertimeYellowBg, OvertimeYellow, OvertimeYellow)
        ResolvedDayStatus.REST -> StatusColors(RestBlueBg, RestBlue, RestBlue)
        null -> StatusColors(
            Color.Transparent,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun statusLabel(type: DayStatusType): String = when (type) {
    DayStatusType.WORK -> "上班"
    DayStatusType.SICK -> "病假"
    DayStatusType.OVERTIME -> "加班"
    DayStatusType.REST -> "休息"
}

private fun statusLabelAndIcon(status: ResolvedDayStatus): Pair<String, ImageVector> = when (status) {
    ResolvedDayStatus.WORK -> "上班" to Icons.Default.Work
    ResolvedDayStatus.SICK -> "病假" to Icons.Default.LocalHospital
    ResolvedDayStatus.OVERTIME -> "加班" to Icons.Default.MoreTime
    ResolvedDayStatus.REST -> "休息" to Icons.Default.BeachAccess
}

private fun DayStatusType.toResolved(): ResolvedDayStatus = when (this) {
    DayStatusType.WORK -> ResolvedDayStatus.WORK
    DayStatusType.SICK -> ResolvedDayStatus.SICK
    DayStatusType.OVERTIME -> ResolvedDayStatus.OVERTIME
    DayStatusType.REST -> ResolvedDayStatus.REST
}
