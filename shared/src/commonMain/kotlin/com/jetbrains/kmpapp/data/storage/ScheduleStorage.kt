package com.jetbrains.kmpapp.data.storage

import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScheduleStorage(
    private val platformStorage: PlatformStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _savedTargets = MutableStateFlow<List<ScheduleTarget>>(emptyList())
    val savedTargets: StateFlow<List<ScheduleTarget>> = _savedTargets.asStateFlow()

    private val _selectedTarget = MutableStateFlow<ScheduleTarget?>(null)
    val selectedTarget: StateFlow<ScheduleTarget?> = _selectedTarget.asStateFlow()

    private val _cachedLessons = MutableStateFlow<Map<Int, List<Lesson>>>(emptyMap())
    val cachedLessons: StateFlow<Map<Int, List<Lesson>>> = _cachedLessons.asStateFlow()

    private val _showEmptyLessons = MutableStateFlow<Boolean>(false)
    val showEmptyLessons: StateFlow<Boolean> = _showEmptyLessons.asStateFlow()

    private val _themeMode = MutableStateFlow<ThemeMode>(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        scope.launch {
            try {
                // Restore theme mode setting
                val themeStr = platformStorage.getString(KEY_APP_THEME)
                if (!themeStr.isNullOrBlank()) {
                    _themeMode.value = try {
                        ThemeMode.valueOf(themeStr)
                    } catch (_: Exception) {
                        ThemeMode.SYSTEM
                    }
                }

                // Restore show empty lessons setting
                val showEmptyStr = platformStorage.getString(KEY_SHOW_EMPTY_LESSONS)
                if (!showEmptyStr.isNullOrBlank()) {
                    _showEmptyLessons.value = showEmptyStr.toBooleanStrictOrNull() ?: false
                }

                // Restore saved targets
                val targetsJson = platformStorage.getString(KEY_SAVED_TARGETS)
                val targets: List<ScheduleTarget> = if (!targetsJson.isNullOrBlank()) {
                    json.decodeFromString(targetsJson)
                } else {
                    emptyList()
                }
                _savedTargets.value = targets

                // Restore active target
                val activeIdStr = platformStorage.getString(KEY_SELECTED_TARGET_ID)
                val activeId = activeIdStr?.toIntOrNull()
                val selected = targets.firstOrNull { it.id == activeId } ?: targets.firstOrNull()
                _selectedTarget.value = selected

                // Restore cached lessons for all saved targets
                val loadedCache = mutableMapOf<Int, List<Lesson>>()
                for (target in targets) {
                    val lessonsJson = platformStorage.getString(KEY_LESSONS_PREFIX + target.id)
                    if (!lessonsJson.isNullOrBlank()) {
                        try {
                            val lessons: List<Lesson> = json.decodeFromString(lessonsJson)
                            loadedCache[target.id] = lessons
                        } catch (_: Exception) {}
                    }
                }
                _cachedLessons.value = loadedCache
            } catch (e: Exception) {
                println("ScheduleStorage: failed to load persisted state: ${e.message}")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch {
            try {
                platformStorage.saveString(KEY_APP_THEME, mode.name)
            } catch (e: Exception) {
                println("Failed to persist themeMode: ${e.message}")
            }
        }
    }

    fun setShowEmptyLessons(enabled: Boolean) {
        _showEmptyLessons.value = enabled
        scope.launch {
            try {
                platformStorage.saveString(KEY_SHOW_EMPTY_LESSONS, enabled.toString())
            } catch (e: Exception) {
                println("Failed to persist showEmptyLessons: ${e.message}")
            }
        }
    }



    fun addTarget(target: ScheduleTarget) {
        _savedTargets.update { list ->
            if (list.any { it.id == target.id }) list
            else list + target
        }
        selectTarget(target)
        persistTargets()
    }

    fun removeTarget(targetId: Int) {
        _savedTargets.update { list -> list.filter { it.id != targetId } }
        if (_selectedTarget.value?.id == targetId) {
            _selectedTarget.value = _savedTargets.value.firstOrNull()
            persistSelectedTargetId(_selectedTarget.value?.id)
        }
        _cachedLessons.update { map -> map - targetId }
        platformStorage.remove(KEY_LESSONS_PREFIX + targetId)
        persistTargets()
    }

    fun selectTarget(target: ScheduleTarget?) {
        _selectedTarget.value = target
        persistSelectedTargetId(target?.id)
    }

    fun selectTargetById(targetId: Int) {
        val target = _savedTargets.value.firstOrNull { it.id == targetId }
        if (target != null) {
            selectTarget(target)
        }
    }

    fun saveLessons(targetId: Int, lessons: List<Lesson>) {
        _cachedLessons.update { map ->
            map + (targetId to lessons)
        }
        scope.launch {
            try {
                platformStorage.saveString(KEY_LESSONS_PREFIX + targetId, json.encodeToString(lessons))
            } catch (e: Exception) {
                println("Failed to persist lessons for $targetId: ${e.message}")
            }
        }
    }

    fun getLessons(targetId: Int): List<Lesson>? {
        return _cachedLessons.value[targetId]
    }

    fun clearCache() {
        _cachedLessons.value = emptyMap()
        for (target in _savedTargets.value) {
            platformStorage.remove(KEY_LESSONS_PREFIX + target.id)
        }
    }

    fun getStorageStats(): com.jetbrains.kmpapp.data.model.StorageStats {
        var schedulesBytes = 0L
        var totalLessons = 0
        for ((_, lessons) in _cachedLessons.value) {
            totalLessons += lessons.size
        }
        for (target in _savedTargets.value) {
            val str = platformStorage.getString(KEY_LESSONS_PREFIX + target.id)
            if (str != null) {
                schedulesBytes += str.encodeToByteArray().size
            }
        }

        val targetsStr = platformStorage.getString(KEY_SAVED_TARGETS)
        val targetsBytes = targetsStr?.encodeToByteArray()?.size?.toLong() ?: 0L

        var settingsBytes = 0L
        platformStorage.getString(KEY_SELECTED_TARGET_ID)?.let { settingsBytes += it.encodeToByteArray().size }
        platformStorage.getString(KEY_SHOW_EMPTY_LESSONS)?.let { settingsBytes += it.encodeToByteArray().size }
        platformStorage.getString(KEY_APP_THEME)?.let { settingsBytes += it.encodeToByteArray().size }

        val total = schedulesBytes + targetsBytes + settingsBytes

        return com.jetbrains.kmpapp.data.model.StorageStats(
            schedulesSizeBytes = schedulesBytes,
            schedulesCount = _cachedLessons.value.size,
            lessonsCount = totalLessons,
            targetsSizeBytes = targetsBytes,
            targetsCount = _savedTargets.value.size,
            settingsSizeBytes = settingsBytes,
            totalSizeBytes = total
        )
    }

    private fun persistTargets() {
        scope.launch {
            try {
                platformStorage.saveString(KEY_SAVED_TARGETS, json.encodeToString(_savedTargets.value))
            } catch (e: Exception) {
                println("Failed to persist targets: ${e.message}")
            }
        }
    }

    private fun persistSelectedTargetId(id: Int?) {
        scope.launch {
            if (id != null) {
                platformStorage.saveString(KEY_SELECTED_TARGET_ID, id.toString())
            } else {
                platformStorage.remove(KEY_SELECTED_TARGET_ID)
            }
        }
    }

    companion object {
        private const val KEY_SAVED_TARGETS = "mirea_saved_targets"
        private const val KEY_SELECTED_TARGET_ID = "mirea_selected_target_id"
        private const val KEY_LESSONS_PREFIX = "mirea_lessons_"
        private const val KEY_SHOW_EMPTY_LESSONS = "mirea_show_empty_lessons"
        private const val KEY_APP_THEME = "mirea_app_theme"
    }
}


