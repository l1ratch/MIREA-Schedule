package com.jetbrains.kmpapp.data.storage

import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScheduleStorage {

    private val _savedTargets = MutableStateFlow<List<ScheduleTarget>>(emptyList())
    val savedTargets: StateFlow<List<ScheduleTarget>> = _savedTargets.asStateFlow()

    private val _selectedTarget = MutableStateFlow<ScheduleTarget?>(null)
    val selectedTarget: StateFlow<ScheduleTarget?> = _selectedTarget.asStateFlow()

    private val _cachedLessons = MutableStateFlow<Map<Int, List<Lesson>>>(emptyMap())
    val cachedLessons: StateFlow<Map<Int, List<Lesson>>> = _cachedLessons.asStateFlow()

    fun addTarget(target: ScheduleTarget) {
        _savedTargets.update { list ->
            if (list.any { it.id == target.id }) list
            else list + target
        }
        selectTarget(target)
    }

    fun removeTarget(targetId: Int) {
        _savedTargets.update { list -> list.filter { it.id != targetId } }
        if (_selectedTarget.value?.id == targetId) {
            _selectedTarget.value = _savedTargets.value.firstOrNull()
        }
        _cachedLessons.update { map -> map - targetId }
    }

    fun selectTarget(target: ScheduleTarget?) {
        _selectedTarget.value = target
    }

    fun selectTargetById(targetId: Int) {
        val target = _savedTargets.value.firstOrNull { it.id == targetId }
        if (target != null) {
            _selectedTarget.value = target
        }
    }

    fun saveLessons(targetId: Int, lessons: List<Lesson>) {
        _cachedLessons.update { map ->
            map + (targetId to lessons)
        }
    }

    fun getLessons(targetId: Int): List<Lesson>? {
        return _cachedLessons.value[targetId]
    }

    fun clearCache() {
        _cachedLessons.value = emptyMap()
    }
}
