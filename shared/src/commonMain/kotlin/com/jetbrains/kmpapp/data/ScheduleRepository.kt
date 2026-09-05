package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.api.MireaScheduleApi
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ThemeMode
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
    val showEmptyLessons: StateFlow<Boolean> = storage.showEmptyLessons
    val themeMode: StateFlow<ThemeMode> = storage.themeMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setShowEmptyLessons(enabled: Boolean) {
        storage.setShowEmptyLessons(enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        storage.setThemeMode(mode)
    }

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
                if (target != null) {
                    val hasCached = storage.getLessons(target.id) != null
                    // Silent refresh in background if cache is already present, otherwise show loading
                    refreshSchedule(target, silent = hasCached)
                }
            }
        }
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return try {
            api.search(query)
        } catch (e: Exception) {
            println("MireaScheduleApi.search failed: ${e.message}")
            emptyList()
        }
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        storage.addTarget(target)
        scope.launch {
            refreshSchedule(target, silent = false)
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
            refreshSchedule(current, silent = false)
        }
    }

    private suspend fun refreshSchedule(target: ScheduleTarget, silent: Boolean = false) {
        if (!silent) {
            _isLoading.value = true
            _errorMessage.value = null
        }
        try {
            val ical = api.getIcal(target.type, target.id)
            val parsedLessons = MireaICalParser.parse(ical)
            storage.saveLessons(target.id, parsedLessons)
            _errorMessage.value = null
        } catch (e: Exception) {
            println("refreshSchedule error for ${target.targetTitle}: ${e.message}")
            // Only show user-facing error if we don't have cached lessons
            val hasCached = storage.getLessons(target.id) != null
            if (!hasCached && !silent) {
                _errorMessage.value = e.message ?: "Ошибка загрузки расписания"
            }
        } finally {
            if (!silent) {
                _isLoading.value = false
            }
        }
    }

    fun clearCache() {
        storage.clearCache()
        selectedTarget.value?.let { current ->
            scope.launch {
                refreshSchedule(current, silent = false)
            }
        }
    }
}
