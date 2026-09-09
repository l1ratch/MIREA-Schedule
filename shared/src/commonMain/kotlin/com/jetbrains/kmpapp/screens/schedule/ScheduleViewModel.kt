package com.jetbrains.kmpapp.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleSlot
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.defaultBells
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class ScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    val savedTargets: StateFlow<List<ScheduleTarget>> = repository.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = repository.selectedTarget
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val errorMessage: StateFlow<String?> = repository.errorMessage
    val activeDiff: StateFlow<com.jetbrains.kmpapp.data.model.ScheduleDiff?> = repository.activeDiff
    val refreshStatus: StateFlow<com.jetbrains.kmpapp.data.model.RefreshStatus?> = repository.refreshStatus
    val showLessonProgress: StateFlow<Boolean> = repository.showLessonProgress
    val autoScrollToCurrentLesson: StateFlow<Boolean> = repository.autoScrollToCurrentLesson
    val showAbbreviatedNames: StateFlow<Boolean> = repository.showAbbreviatedNames

    private var lastAutoScrolledDate: LocalDate? = null
    private var lastAutoScrolledTargetId: Int? = null

    fun canAutoScroll(date: LocalDate, targetId: Int?): Boolean {
        return date != lastAutoScrolledDate || targetId != lastAutoScrolledTargetId
    }

    fun markAutoScrolled(date: LocalDate, targetId: Int?) {
        lastAutoScrolledDate = date
        lastAutoScrolledTargetId = targetId
    }

    fun resetAutoScroll() {
        lastAutoScrolledDate = null
        lastAutoScrolledTargetId = null
    }

    private val _currentMinutes = MutableStateFlow(DateUtils.currentTimeMinutes())
    val currentMinutes: StateFlow<Int> = _currentMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            repository.refreshStatus.collect { status ->
                if (status != null) {
                    val delayMs = if (status is com.jetbrains.kmpapp.data.model.RefreshStatus.Error) 3500L else 2500L
                    kotlinx.coroutines.delay(delayMs)
                    repository.clearRefreshStatus()
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                // Power-saving tick: pause or sleep longer when in background
                val isForeground = repository.isLowPowerMode.value.let { lowPower ->
                    // Update current minute
                    _currentMinutes.value = DateUtils.currentTimeMinutes()
                    val sleepTime = if (lowPower) 60_000L else 30_000L
                    kotlinx.coroutines.delay(sleepTime)
                }
            }
        }
    }

    fun dismissStatusBadge() {
        repository.clearRefreshStatus()
    }

    fun dismissDiff() {
        repository.dismissDiff()
    }

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedLessonForDetail = MutableStateFlow<Lesson?>(null)
    val selectedLessonForDetail: StateFlow<Lesson?> = _selectedLessonForDetail.asStateFlow()

    val datesWithLessons: StateFlow<Set<LocalDate>> = repository.currentLessons
        .combine(MutableStateFlow(Unit)) { lessons, _ ->
            lessons.map { it.date }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val dayLessonSummaries: StateFlow<Map<LocalDate, DayLessonSummary>> = repository.currentLessons
        .combine(MutableStateFlow(Unit)) { lessons, _ ->
            lessons.groupBy { it.date }.mapValues { (_, dayLessons) ->
                val orderedTypes = dayLessons.groupBy { it.bellNumber }
                    .entries.sortedBy { it.key }
                    .map { (_, slotLessons) -> slotLessons.first().lessonType }
                DayLessonSummary(lessonTypes = orderedTypes)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val daySlots: StateFlow<List<ScheduleSlot>> = combine(
        repository.currentLessons,
        _selectedDate,
        repository.showEmptyLessons
    ) { lessons, date, showEmpty -> slotsForDate(lessons, date, showEmpty) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLessons: StateFlow<List<Lesson>> = repository.currentLessons
    val showEmptyLessons: StateFlow<Boolean> = repository.showEmptyLessons

    fun slotsForDate(
        date: LocalDate,
        lessons: List<Lesson> = currentLessons.value,
        showEmpty: Boolean = showEmptyLessons.value
    ): List<ScheduleSlot> = slotsForDate(lessons, date, showEmpty)

    private fun slotsForDate(
        lessons: List<Lesson>,
        date: LocalDate,
        showEmpty: Boolean
    ): List<ScheduleSlot> {
        val forDay = lessons.filter { it.date == date }
        if (forDay.isEmpty()) {
            return if (showEmpty && date.dayOfWeek != DayOfWeek.SUNDAY) {
                (1..7).map { bell ->
                    val bellInfo = defaultBells.firstOrNull { it.number == bell }
                    ScheduleSlot.Empty(bell, bellInfo?.startTime ?: "—", bellInfo?.endTime ?: "—")
                }
            } else emptyList()
        }

        val bellMap = forDay.groupBy { it.bellNumber }
        if (!showEmpty) {
            return bellMap.entries.sortedBy { it.key }.map { (bell, items) ->
                val first = items.first()
                ScheduleSlot.Active(bell, first.startTime, first.endTime, items)
            }
        }

        val result = mutableListOf<ScheduleSlot>()
        val upperBell = maxOf(forDay.maxOfOrNull { it.bellNumber } ?: 7, 7)
        for (bell in 1..upperBell) {
            val items = bellMap[bell]
            if (!items.isNullOrEmpty()) {
                val first = items.first()
                result += ScheduleSlot.Active(bell, first.startTime, first.endTime, items)
            } else {
                val bellInfo = defaultBells.firstOrNull { it.number == bell }
                result += ScheduleSlot.Empty(bell, bellInfo?.startTime ?: "—", bellInfo?.endTime ?: "—")
            }
        }
        return result
    }

    fun selectDate(date: LocalDate) {
        if (_selectedDate.value != date) {
            _selectedDate.value = date
            resetAutoScroll()
        }
    }

    fun nextDay() {
        selectDate(_selectedDate.value.plus(DatePeriod(days = 1)))
    }

    fun previousDay() {
        selectDate(_selectedDate.value.minus(DatePeriod(days = 1)))
    }

    fun selectLessonForDetail(lesson: Lesson?) {
        _selectedLessonForDetail.value = lesson
    }

    fun selectTarget(target: ScheduleTarget) {
        repository.selectTarget(target)
        resetAutoScroll()
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        repository.addAndSelectTarget(target)
        resetAutoScroll()
    }

    fun refresh() {
        repository.refreshCurrentSchedule()
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return repository.search(query)
    }
}

data class DayLessonSummary(
    val lessonTypes: List<com.jetbrains.kmpapp.data.model.LessonType> = emptyList()
) {
    val hasLessons: Boolean get() = lessonTypes.isNotEmpty()
}

