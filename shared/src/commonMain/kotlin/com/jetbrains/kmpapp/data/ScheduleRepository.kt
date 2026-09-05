package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.api.MireaScheduleApi
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.parser.MireaICalParser
import com.jetbrains.kmpapp.data.storage.ScheduleStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleRepository(
    private val api: MireaScheduleApi,
    private val storage: ScheduleStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val savedTargets: StateFlow<List<ScheduleTarget>> = storage.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = storage.selectedTarget

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val currentLessons: StateFlow<List<Lesson>> = combine(
        storage.selectedTarget,
        storage.cachedLessons
    ) { selected, cache ->
        if (selected == null) emptyList()
        else cache[selected.id] ?: emptyList()
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            storage.selectedTarget.collect { target ->
                if (target != null && storage.getLessons(target.id) == null) {
                    refreshSchedule(target)
                }
            }
        }
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return try {
            api.search(query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        storage.addTarget(target)
        scope.launch {
            refreshSchedule(target)
        }
    }

    fun selectTarget(target: ScheduleTarget) {
        storage.selectTarget(target)
    }

    fun removeTarget(targetId: Int) {
        storage.removeTarget(targetId)
    }

    fun refreshCurrentSchedule() {
        val current = selectedTarget.value ?: return
        scope.launch {
            refreshSchedule(current)
        }
    }

    private suspend fun refreshSchedule(target: ScheduleTarget) {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val ical = api.getIcal(target.type, target.id)
            val parsedLessons = MireaICalParser.parse(ical)
            storage.saveLessons(target.id, parsedLessons)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Ошибка загрузки расписания"
        } finally {
            _isLoading.value = false
        }
    }

    fun clearCache() {
        storage.clearCache()
        selectedTarget.value?.let { current ->
            scope.launch {
                refreshSchedule(current)
            }
        }
    }
}
