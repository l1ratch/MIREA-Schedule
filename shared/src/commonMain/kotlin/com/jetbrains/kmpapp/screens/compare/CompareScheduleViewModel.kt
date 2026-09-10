package com.jetbrains.kmpapp.screens.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.defaultBells
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class CompareCell(
    val lessons: List<Lesson>,
    val isDifferent: Boolean
) {
    val isEmpty: Boolean get() = lessons.isEmpty()
}

data class CompareRow(
    val bellNumber: Int,
    val startTime: String,
    val endTime: String,
    val cells: List<CompareCell>
)

data class CompareDay(
    val date: LocalDate,
    val rows: List<CompareRow>
)

data class ScheduleComparison(
    val targets: List<ScheduleTarget>,
    val days: List<CompareDay>,
    val differencesCount: Int,
    val matchesCount: Int
)

class CompareScheduleViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    val savedTargets: StateFlow<List<ScheduleTarget>> = repository.savedTargets
    val cachedLessons: StateFlow<Map<Int, List<Lesson>>> = repository.cachedLessons

    private val _selectedTargetIds = MutableStateFlow<List<Int>>(emptyList())
    val selectedTargetIds: StateFlow<List<Int>> = _selectedTargetIds.asStateFlow()

    private val _selectedWeekStart = MutableStateFlow(DateUtils.getWeekDates(DateUtils.today()).first())
    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart.asStateFlow()

    val selectedTargets: StateFlow<List<ScheduleTarget>> = combine(
        savedTargets,
        _selectedTargetIds
    ) { targets, ids ->
        targets.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val comparison: StateFlow<ScheduleComparison> = combine(
        savedTargets,
        cachedLessons,
        _selectedTargetIds,
        _selectedWeekStart
    ) { targets, cache, ids, weekStart ->
        val chosen = targets.filter { it.id in ids }
        buildComparison(chosen, cache, weekStart)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ScheduleComparison(emptyList(), emptyList(), 0, 0)
    )

    init {
        val targets = repository.savedTargets.value
        if (targets.isNotEmpty()) {
            _selectedTargetIds.value = targets.take(2).map { it.id }
        }
    }

    fun toggleTarget(targetId: Int) {
        _selectedTargetIds.update { ids ->
            if (targetId in ids) ids - targetId else ids + targetId
        }
    }

    fun nextWeek() {
        _selectedWeekStart.value = _selectedWeekStart.value.plus(DatePeriod(days = 7))
    }

    fun previousWeek() {
        _selectedWeekStart.value = _selectedWeekStart.value.minus(DatePeriod(days = 7))
    }

    fun goToCurrentWeek() {
        _selectedWeekStart.value = DateUtils.getWeekDates(DateUtils.today()).first()
    }

    fun refreshAll() {
        for (target in selectedTargets.value) {
            repository.refreshTarget(target)
        }
    }

    private fun buildComparison(
        targets: List<ScheduleTarget>,
        cache: Map<Int, List<Lesson>>,
        weekStart: LocalDate
    ): ScheduleComparison {
        if (targets.isEmpty()) return ScheduleComparison(targets, emptyList(), 0, 0)

        val weekDates = DateUtils.getWeekDates(weekStart)
        val groupedByDay = targets.map { target ->
            (cache[target.id] ?: emptyList()).filter { it.date in weekDates }
        }

        val days = mutableListOf<CompareDay>()
        var differencesCount = 0
        var matchesCount = 0

        for (date in weekDates) {
            val bellsByTarget = groupedByDay.map { lessonsForTarget ->
                lessonsForTarget.filter { it.date == date }.groupBy { it.bellNumber }
            }
            val upperBell = bellsByTarget.flatMap { it.keys }.maxOrNull() ?: continue

            val rows = (1..upperBell).map { bell ->
                val cells = bellsByTarget.map { bellMap ->
                    val lessons = bellMap[bell]
                        ?.sortedWith(compareBy({ it.startTime }, { it.subject }))
                        ?: emptyList<Lesson>()
                    CompareCell(lessons = lessons, isDifferent = false)
                }
                val bellInfo = defaultBells.firstOrNull { it.number == bell }
                CompareRow(
                    bellNumber = bell,
                    startTime = bellInfo?.startTime ?: "—",
                    endTime = bellInfo?.endTime ?: "—",
                    cells = cells
                )
            }

            val processedRows = rows.map { row ->
                val signatures = row.cells.map { cellSignature(it.lessons) }
                val nonEmpty = signatures.filterNotNull()
                matchesCount += nonEmpty.size
                val hasEmpty = signatures.any { it == null }
                val allSame = !hasEmpty && nonEmpty.toSet().size <= 1
                if (nonEmpty.isEmpty() || allSame) {
                    row
                } else {
                    val counts = nonEmpty.groupingBy { it }.eachCount()
                    val maxCount = counts.values.maxOrNull() ?: 1
                    val mode = counts.filterValues { it == maxCount }.keys.singleOrNull()

                    val newCells = row.cells.mapIndexed { index, cell ->
                        val signature = signatures[index]
                        val isDifferent = when {
                            signature == null -> true
                            mode == null -> true
                            else -> signature != mode
                        }
                        if (isDifferent) {
                            differencesCount++
                            cell.copy(isDifferent = true)
                        } else {
                            cell
                        }
                    }
                    row.copy(cells = newCells)
                }
            }

            days += CompareDay(date, processedRows)
        }

        return ScheduleComparison(targets, days, differencesCount, matchesCount)
    }

    private fun cellSignature(lessons: List<Lesson>): String? {
        if (lessons.isEmpty()) return null
        return lessons.joinToString("||") { lesson ->
            val teachers = lesson.teachers.map { it.trim().lowercase() }.sorted()
            val classrooms = lesson.classrooms.map { it.trim().lowercase() }.sorted()
            buildString {
                append(lesson.subject.trim().lowercase())
                append('|')
                append(lesson.lessonType.name)
                append('|')
                append(teachers.joinToString(","))
                append('|')
                append(classrooms.joinToString(","))
                append('|')
                append(lesson.startTime)
                append('-')
                append(lesson.endTime)
            }
        }
    }
}