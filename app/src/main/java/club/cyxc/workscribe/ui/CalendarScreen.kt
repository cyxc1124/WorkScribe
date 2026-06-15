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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import club.cyxc.workscribe.util.MonthStatusStats
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
    onSelectYear: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onSetDayStatus: (LocalDate, DayStatusType) -> Unit,
    onClearDayStatus: (LocalDate) -> Unit,
    onSaveNote: (LocalDate, String) -> Unit,
    onOpenMakeupPunch: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }

    if (showNoteEditor && uiState.selectedDate != null) {
        DayNoteEditBottomSheet(
            initialContent = uiState.selectedDayNote.orEmpty(),
            onDismiss = { showNoteEditor = false },
            onSave = { content ->
                onSaveNote(uiState.selectedDate, content)
            },
        )
    }

    if (showYearPicker) {
        WheelYearPickerBottomSheet(
            years = CalendarViewModel.selectableYears,
            initialYear = uiState.currentMonth.year,
            onDismiss = { showYearPicker = false },
            onConfirm = { year ->
                onSelectYear(year)
                showYearPicker = false
            },
        )
    }

    if (showMonthPicker) {
        WheelMonthPickerBottomSheet(
            initialMonth = uiState.currentMonth.monthValue,
            onDismiss = { showMonthPicker = false },
            onConfirm = { month ->
                onSelectMonth(YearMonth.of(uiState.currentMonth.year, month))
                showMonthPicker = false
            },
        )
    }

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
                CalendarMonthNavigator(
                    currentMonth = uiState.currentMonth,
                    onYearClick = { showYearPicker = true },
                    onMonthClick = { showMonthPicker = true },
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onGoToToday = onGoToToday,
                )
            }
            item {
                MonthStatsCard(stats = uiState.monthStats)
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
                item {
                    DayNoteCard(
                        note = uiState.selectedDayNote,
                        onClick = { showNoteEditor = true },
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
                item {
                    DayRecordsCard(records = uiState.selectedDayRecords)
                }
            }
        }
    }
}

@Composable
private fun MonthStatsCard(stats: MonthStatusStats) {
    val parts = buildList {
        add("上班 ${stats.workDays} 天")
        add("休息 ${stats.restDays} 天")
        if (stats.sickDays > 0) add("病假 ${stats.sickDays} 天")
        if (stats.overtimeDays > 0) add("加班 ${stats.overtimeDays} 天")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            text = "本月：${parts.joinToString(separator = " · ")}",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun CalendarMonthNavigator(
    currentMonth: YearMonth,
    onYearClick: () -> Unit,
    onMonthClick: () -> Unit,
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
        DatePickerDropdownChip(
            label = TimeFormatter.formatYear(currentMonth.year),
            onClick = onYearClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        DatePickerDropdownChip(
            label = TimeFormatter.formatMonth(currentMonth.monthValue),
            onClick = onMonthClick,
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一个月",
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onGoToToday) {
            Text("今天")
        }
    }
}

@Composable
private fun DatePickerDropdownChip(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
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
    val colorScheme = MaterialTheme.colorScheme
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
                    cell.isSelected -> colorScheme.primaryContainer
                    colors.background != Color.Transparent -> colors.background
                    else -> colorScheme.surface
                },
            )
            .border(
                width = when {
                    cell.isSelected -> 2.dp
                    cell.isToday -> 1.5.dp
                    else -> 0.5.dp
                },
                color = when {
                    cell.isSelected -> colorScheme.primary
                    cell.isToday -> colorScheme.primary
                    else -> colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                !cell.isCurrentMonth -> colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                cell.isSelected -> colorScheme.onPrimaryContainer
                cell.isToday -> colorScheme.primary
                cell.status != null -> colors.onBackground
                else -> colorScheme.onSurface
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

        if (cell.hasNote) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "日期类型",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                text = if (manualStatus != null) {
                    "已手动设置（点击「自动」恢复推断）"
                } else {
                    "自动：有完整上下班且工时超过 8 小时为加班，否则为上班"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedDayHeader(selectedDate: LocalDate?) {
    if (selectedDate == null) {
        Text(
            text = "选择日期",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = TimeFormatter.formatCalendarDayHeader(selectedDate),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (TimeFormatter.isCalendarDayYearVisible(selectedDate)) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TimeFormatter.formatCalendarDayYear(selectedDate),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        Text(
            text = "打卡详情",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayNoteCard(
    note: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "备注",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = note?.takeIf { it.isNotBlank() } ?: "点击添加备注…",
                style = MaterialTheme.typography.bodyMedium,
                color = if (note.isNullOrBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DayDurationCard(workDurationMillis: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "当日工时",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = TimeFormatter.formatDuration(workDurationMillis),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.End,
            )
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
private fun DayRecordsCard(records: List<PunchRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "打卡记录",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            records.forEachIndexed { index, record ->
                CalendarRecordRow(record = record)
                if (index < records.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarRecordRow(record: PunchRecord) {
    val isClockIn = record.type == PunchType.IN
    val typeLabel = if (isClockIn) "上班" else "下班"
    val icon = if (isClockIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout
    val accentColor = if (isClockIn) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accentColor,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = typeLabel,
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = TimeFormatter.formatRecordTime(record.timestamp),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
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
