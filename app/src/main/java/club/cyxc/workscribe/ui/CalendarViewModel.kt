package club.cyxc.workscribe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import club.cyxc.workscribe.data.DayStatusType
import club.cyxc.workscribe.data.PunchRecord
import club.cyxc.workscribe.data.PunchRepository
import club.cyxc.workscribe.util.DayStatusResolver
import club.cyxc.workscribe.util.ResolvedDayStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarDayCell(
    val date: LocalDate,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val status: ResolvedDayStatus?,
    val workDurationMillis: Long,
    val manualStatus: DayStatusType?,
)

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val monthStrip: List<YearMonth> = emptyList(),
    val gridDays: List<CalendarDayCell> = emptyList(),
    val selectedDayRecords: List<PunchRecord> = emptyList(),
    val selectedDayStatus: ResolvedDayStatus? = null,
    val selectedDayManualStatus: DayStatusType? = null,
    val workDurationMillis: Long = 0L,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    application: Application,
    private val repository: PunchRepository,
) : AndroidViewModel(application) {

    private val zoneId = ZoneId.systemDefault()
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())

    private val gridRecords = _currentMonth.flatMapLatest { month ->
        val (gridStart, gridEnd) = monthGridBounds(month)
        val startMillis = gridStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = gridEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        repository.observeRecordsBetween(startMillis, endMillis)
    }

    private val gridDayStatuses = _currentMonth.flatMapLatest { month ->
        val (gridStart, gridEnd) = monthGridBounds(month)
        repository.observeDayStatusesBetween(
            gridStart.toEpochDay(),
            gridEnd.plusDays(1).toEpochDay(),
        )
    }

    private val selectedDayRecords = _selectedDate.flatMapLatest { date ->
        if (date == null) {
            flowOf(emptyList())
        } else {
            repository.observeRecordsForDate(date, zoneId)
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        _selectedDate,
        gridRecords,
        gridDayStatuses,
        selectedDayRecords,
    ) { month, selected, gridRecs, gridStatuses, dayRecs ->
        val recordsByDate = gridRecs.groupBy { record ->
            Instant.ofEpochMilli(record.timestamp).atZone(zoneId).toLocalDate()
        }
        val sortedDayRecs = dayRecs.sortedByDescending { it.timestamp }
        val today = LocalDate.now(zoneId)
        reconcileStaleManualOvertime(recordsByDate, gridStatuses, today)
        val selectedAnchor = durationAnchor(selected, today)
        val selectedIncludeOpen = selected == today
        val selectedManual = selected?.toEpochDay()?.let { gridStatuses[it] }
        val selectedStatus = selected?.let { date ->
            DayStatusResolver.resolve(
                date = date,
                records = sortedDayRecs,
                manualType = selectedManual,
                durationAnchorMillis = selectedAnchor,
                includeOpenSession = selectedIncludeOpen,
            )
        }
        CalendarUiState(
            currentMonth = month,
            selectedDate = selected,
            monthStrip = buildMonthStrip(month),
            gridDays = buildGridDays(
                month = month,
                selected = selected,
                today = today,
                recordsByDate = recordsByDate,
                manualStatuses = gridStatuses,
            ),
            selectedDayRecords = sortedDayRecs,
            selectedDayStatus = selectedStatus,
            selectedDayManualStatus = selectedManual,
            workDurationMillis = DayStatusResolver.workDurationMillis(
                sortedDayRecs,
                selectedAnchor,
                selectedIncludeOpen,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(),
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        val dateMonth = YearMonth.from(date)
        if (dateMonth != _currentMonth.value) {
            _currentMonth.value = dateMonth
        }
    }

    fun selectMonth(month: YearMonth) {
        _currentMonth.value = month
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToToday() {
        val today = LocalDate.now(zoneId)
        _currentMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun setDayStatus(date: LocalDate, type: DayStatusType) {
        viewModelScope.launch {
            repository.setDayStatus(date, type)
        }
    }

    fun clearDayStatus(date: LocalDate) {
        viewModelScope.launch {
            repository.clearDayStatus(date)
        }
    }

    private fun buildGridDays(
        month: YearMonth,
        selected: LocalDate?,
        today: LocalDate,
        recordsByDate: Map<LocalDate, List<PunchRecord>>,
        manualStatuses: Map<Long, DayStatusType>,
    ): List<CalendarDayCell> {
        val (gridStart, _) = monthGridBounds(month)
        val daysInMonth = month.lengthOfMonth()
        val firstDay = month.atDay(1)
        val leadingEmpty = firstDay.dayOfWeek.value - 1
        val totalCells = leadingEmpty + daysInMonth
        val rowCount = (totalCells + 6) / 7
        val cellCount = rowCount * 7

        return (0 until cellCount).map { index ->
            val date = gridStart.plusDays(index.toLong())
            val records = recordsByDate[date].orEmpty()
            val manual = manualStatuses[date.toEpochDay()]
            val anchor = durationAnchor(date, today)
            val includeOpenSession = date == today
            CalendarDayCell(
                date = date,
                dayOfMonth = date.dayOfMonth,
                isCurrentMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isSelected = date == selected,
                status = DayStatusResolver.resolve(
                    date,
                    records,
                    manual,
                    anchor,
                    includeOpenSession,
                ),
                workDurationMillis = DayStatusResolver.workDurationMillis(
                    records,
                    anchor,
                    includeOpenSession,
                ),
                manualStatus = manual,
            )
        }
    }

    private fun buildMonthStrip(center: YearMonth): List<YearMonth> {
        return (-2..2).map { offset -> center.plusMonths(offset.toLong()) }
    }

    private fun reconcileStaleManualOvertime(
        recordsByDate: Map<LocalDate, List<PunchRecord>>,
        manualStatuses: Map<Long, DayStatusType>,
        today: LocalDate,
    ) {
        manualStatuses.forEach { (epochDay, type) ->
            if (type != DayStatusType.OVERTIME) return@forEach
            val date = LocalDate.ofEpochDay(epochDay)
            val records = recordsByDate[date].orEmpty()
            val anchor = durationAnchor(date, today)
            val includeOpenSession = date == today
            if (DayStatusResolver.isStaleManualOvertime(
                    date,
                    records,
                    type,
                    anchor,
                    includeOpenSession,
                )
            ) {
                viewModelScope.launch {
                    repository.clearDayStatus(date)
                }
            }
        }
    }

    private fun durationAnchor(selected: LocalDate?, today: LocalDate): Long {
        if (selected == null) return System.currentTimeMillis()
        return when {
            selected.isAfter(today) -> selected.atStartOfDay(zoneId).toInstant().toEpochMilli()
            selected.isBefore(today) -> {
                selected.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            }
            else -> System.currentTimeMillis()
        }
    }

    companion object {
        fun monthGridBounds(month: YearMonth): Pair<LocalDate, LocalDate> {
            val firstDay = month.atDay(1)
            val leadingEmpty = firstDay.dayOfWeek.value - 1
            val gridStart = firstDay.minusDays(leadingEmpty.toLong())
            val daysInMonth = month.lengthOfMonth()
            val totalCells = leadingEmpty + daysInMonth
            val rowCount = (totalCells + 6) / 7
            val gridEnd = gridStart.plusDays((rowCount * 7 - 1).toLong())
            return gridStart to gridEnd
        }
    }
}
