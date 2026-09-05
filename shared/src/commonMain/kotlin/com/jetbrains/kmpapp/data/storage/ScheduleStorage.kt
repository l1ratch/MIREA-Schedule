package com.jetbrains.kmpapp.data.storage

import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.screens.components.AppTab
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

    private val _dockTabs = MutableStateFlow<List<AppTab>>(DEFAULT_DOCK_TABS)
    val dockTabs: StateFlow<List<AppTab>> = _dockTabs.asStateFlow()

    private val _isSakuraTheme = MutableStateFlow<Boolean>(false)
    val isSakuraTheme: StateFlow<Boolean> = _isSakuraTheme.asStateFlow()

    private val lastSyncTimes = mutableMapOf<Int, Long>()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
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

            // Restore dock tabs setting
            val dockTabsStr = platformStorage.getString(KEY_DOCK_TABS)
            if (!dockTabsStr.isNullOrBlank()) {
                val loaded = dockTabsStr.split(",").mapNotNull { name ->
                    try { AppTab.valueOf(name.trim()) } catch (_: Exception) { null }
                }
                _dockTabs.value = sanitizeDockTabs(loaded)
            } else {
                _dockTabs.value = DEFAULT_DOCK_TABS
            }

            // Restore sakura theme
            val sakuraStr = platformStorage.getString(KEY_SAKURA_THEME)
            if (!sakuraStr.isNullOrBlank()) {
                _isSakuraTheme.value = sakuraStr.toBooleanStrictOrNull() ?: false
            }

            // Restore saved targets
            val targetsJson = platformStorage.getString(KEY_SAVED_TARGETS)
            val targets: List<ScheduleTarget> = if (!targetsJson.isNullOrBlank()) {
                try { json.decodeFromString(targetsJson) } catch (_: Exception) { emptyList() }
            } else {
                emptyList()
            }
            _savedTargets.value = targets

            // IMPORTANT: Restore cached lessons for all targets BEFORE setting selected target!
            val loadedCache = mutableMapOf<Int, List<Lesson>>()
            for (target in targets) {
                val lessonsJson = platformStorage.getString(KEY_LESSONS_PREFIX + target.id)
                if (!lessonsJson.isNullOrBlank()) {
                    try {
                        val lessons: List<Lesson> = json.decodeFromString(lessonsJson)
                        loadedCache[target.id] = lessons
                    } catch (_: Exception) {}
                }
                val syncTimeStr = platformStorage.getString(KEY_LAST_SYNC_PREFIX + target.id)
                syncTimeStr?.toLongOrNull()?.let { lastSyncTimes[target.id] = it }
            }
            _cachedLessons.value = loadedCache

            // Now that cached lessons and timestamps are ready, restore selected target!
            val activeIdStr = platformStorage.getString(KEY_SELECTED_TARGET_ID)
            val activeId = activeIdStr?.toIntOrNull()
            val selected = targets.firstOrNull { it.id == activeId } ?: targets.firstOrNull()
            _selectedTarget.value = selected
        } catch (e: Exception) {
            println("ScheduleStorage: failed to load persisted state: ${e.message}")
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

    fun setDockTabs(tabs: List<AppTab>) {
        val sanitized = sanitizeDockTabs(tabs)
        _dockTabs.value = sanitized
        scope.launch {
            try {
                platformStorage.saveString(KEY_DOCK_TABS, sanitized.joinToString(",") { it.name })
            } catch (e: Exception) {
                println("Failed to persist dock tabs: ${e.message}")
            }
        }
    }

    private fun sanitizeDockTabs(tabs: List<AppTab>): List<AppTab> {
        val middle = tabs.filter { !it.isFixed }.distinct().take(3)
        return listOf(AppTab.SCHEDULE) + middle + listOf(AppTab.OTHER)
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
        lastSyncTimes.remove(targetId)
        platformStorage.remove(KEY_LESSONS_PREFIX + targetId)
        platformStorage.remove(KEY_LAST_SYNC_PREFIX + targetId)
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

    fun getLastSyncTime(targetId: Int): Long {
        val cached = lastSyncTimes[targetId]
        if (cached != null) return cached
        val str = platformStorage.getString(KEY_LAST_SYNC_PREFIX + targetId)
        val time = str?.toLongOrNull() ?: 0L
        lastSyncTimes[targetId] = time
        return time
    }

    fun setLastSyncTime(targetId: Int, time: Long) {
        lastSyncTimes[targetId] = time
        scope.launch {
            try {
                platformStorage.saveString(KEY_LAST_SYNC_PREFIX + targetId, time.toString())
            } catch (e: Exception) {
                println("Failed to persist lastSyncTime for $targetId: ${e.message}")
            }
        }
    }

    fun clearCache() {
        _cachedLessons.value = emptyMap()
        lastSyncTimes.clear()
        for (target in _savedTargets.value) {
            platformStorage.remove(KEY_LESSONS_PREFIX + target.id)
            platformStorage.remove(KEY_LAST_SYNC_PREFIX + target.id)
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

    fun setSakuraTheme(enabled: Boolean) {
        _isSakuraTheme.value = enabled
        scope.launch {
            platformStorage.saveString(KEY_SAKURA_THEME, enabled.toString())
        }
    }

    companion object {
        private const val KEY_SAVED_TARGETS = "mirea_saved_targets"
        private const val KEY_SELECTED_TARGET_ID = "mirea_selected_target_id"
        private const val KEY_LESSONS_PREFIX = "mirea_lessons_"
        private const val KEY_LAST_SYNC_PREFIX = "mirea_last_sync_"
        private const val KEY_SHOW_EMPTY_LESSONS = "mirea_show_empty_lessons"
        private const val KEY_APP_THEME = "mirea_app_theme"
        private const val KEY_DOCK_TABS = "mirea_dock_tabs_order"
        private const val KEY_SAKURA_THEME = "mirea_sakura_theme_secret"
        val DEFAULT_DOCK_TABS = listOf(AppTab.SCHEDULE, AppTab.FREE_ROOMS, AppTab.TASKS, AppTab.OTHER)
    }
}


