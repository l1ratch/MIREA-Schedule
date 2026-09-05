package com.jetbrains.kmpapp.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

class ScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    val savedTargets: StateFlow<List<ScheduleTarget>> = repository.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = repository.selectedTarget
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val errorMessage: StateFlow<String?> = repository.errorMessage

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _searchFilter = MutableStateFlow("")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    val datesWithLessons: StateFlow<Set<LocalDate>> = repository.currentLessons
        .combine(MutableStateFlow(Unit)) { lessons, _ ->
            lessons.map { it.date }.toSet()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val dayLessons: StateFlow<List<Lesson>> = combine(
        repository.currentLessons,
        _selectedDate,
        _searchFilter
    ) { lessons, date, filter ->
        val forDay = lessons.filter { it.date == date }
        if (filter.isBlank()) {
            forDay
        } else {
            val q = filter.trim().lowercase()
            forDay.filter {
                it.subject.lowercase().contains(q) ||
                it.teachers.any { t -> t.lowercase().contains(q) } ||
                it.classrooms.any { c -> c.lowercase().contains(q) } ||
                it.lessonType.displayName.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSearchFilter(query: String) {
        _searchFilter.value = query
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
