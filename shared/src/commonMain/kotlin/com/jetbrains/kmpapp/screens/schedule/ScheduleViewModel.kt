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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
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

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedLessonForDetail = MutableStateFlow<Lesson?>(null)
    val selectedLessonForDetail: StateFlow<Lesson?> = _selectedLessonForDetail.asStateFlow()

    val datesWithLessons: StateFlow<Set<LocalDate>> = repository.currentLessons
        .combine(MutableStateFlow(Unit)) { lessons, _ ->
            lessons.map { it.date }.toSet()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val daySlots: StateFlow<List<ScheduleSlot>> = combine(
        repository.currentLessons,
        _selectedDate,
        repository.showEmptyLessons
    ) { lessons, date, showEmpty ->
        val forDay = lessons.filter { it.date == date }
        if (forDay.isEmpty()) {
            emptyList()
        } else {
            val bellMap = forDay.groupBy { it.bellNumber }
            val minBell = forDay.minOf { it.bellNumber }
            val maxBell = forDay.maxOf { it.bellNumber }

            if (!showEmpty) {
                bellMap.entries.sortedBy { it.key }.map { (bellNum, items) ->
                    val first = items.first()
                    ScheduleSlot.Active(
                        bellNumber = bellNum,
                        startTime = first.startTime,
                        endTime = first.endTime,
                        lessons = items
                    )
                }
            } else {
                val result = mutableListOf<ScheduleSlot>()
                val upperBell = maxOf(maxBell, 1)
                for (b in 1..upperBell) {
                    val items = bellMap[b]
                    if (!items.isNullOrEmpty()) {
                        val first = items.first()
                        result.add(
                            ScheduleSlot.Active(
                                bellNumber = b,
                                startTime = first.startTime,
                                endTime = first.endTime,
                                lessons = items
                            )
                        )
                    } else {
                        val bellInfo = defaultBells.firstOrNull { it.number == b }
                        result.add(
                            ScheduleSlot.Empty(
                                bellNumber = b,
                                startTime = bellInfo?.startTime ?: "—",
                                endTime = bellInfo?.endTime ?: "—"
                            )
                        )
                    }
                }
                result
            }

        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plus(DatePeriod(days = 1))
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minus(DatePeriod(days = 1))
    }

    fun selectLessonForDetail(lesson: Lesson?) {
        _selectedLessonForDetail.value = lesson
    }

    fun selectTarget(target: ScheduleTarget) {
        repository.selectTarget(target)
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        repository.addAndSelectTarget(target)
    }

    fun refresh() {
        repository.refreshCurrentSchedule()
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return repository.search(query)
    }
}
