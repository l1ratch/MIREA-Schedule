package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.api.MireaScheduleApi
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.LessonDiffItem
import com.jetbrains.kmpapp.data.model.LessonDiffType
import com.jetbrains.kmpapp.data.model.ScheduleDiff
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
import kotlin.time.Clock

class ScheduleRepository(
    private val api: MireaScheduleApi,
    private val storage: ScheduleStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val savedTargets: StateFlow<List<ScheduleTarget>> = storage.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = storage.selectedTarget
    val showEmptyLessons: StateFlow<Boolean> = storage.showEmptyLessons
    val themeMode: StateFlow<ThemeMode> = storage.themeMode
    val dockTabs: StateFlow<List<com.jetbrains.kmpapp.screens.components.AppTab>> = storage.dockTabs

    fun setDockTabs(tabs: List<com.jetbrains.kmpapp.screens.components.AppTab>) {
        storage.setDockTabs(tabs)
    }

    private val _activeDiff = MutableStateFlow<ScheduleDiff?>(null)
    val activeDiff: StateFlow<ScheduleDiff?> = _activeDiff.asStateFlow()

    fun dismissDiff() {
        _activeDiff.value = null
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _refreshStatus = MutableStateFlow<com.jetbrains.kmpapp.data.model.RefreshStatus?>(null)
    val refreshStatus: StateFlow<com.jetbrains.kmpapp.data.model.RefreshStatus?> = _refreshStatus.asStateFlow()

    fun clearRefreshStatus() {
        _refreshStatus.value = null
    }

    fun getStorageStats(): com.jetbrains.kmpapp.data.model.StorageStats = storage.getStorageStats()

    fun setShowEmptyLessons(enabled: Boolean) {
        storage.setShowEmptyLessons(enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        storage.setThemeMode(mode)
    }

    val isSakuraTheme: StateFlow<Boolean> = storage.isSakuraTheme

    fun setSakuraTheme(enabled: Boolean) {
        storage.setSakuraTheme(enabled)
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
                    val cached = storage.getLessons(target.id)
                    val hasCached = cached != null && cached.isNotEmpty()
                    val lastSync = storage.getLastSyncTime(target.id)
                    val now = Clock.System.now().toEpochMilliseconds()
                    val isFresh = (now - lastSync) < 30 * 60 * 1000L // 30 minutes TTL

                    if (!hasCached) {
                        // First load for this target: fetch from network with loading indicator
                        refreshSchedule(target, silent = false)
                    } else if (!isFresh) {
                        // Cache present but older than 30 minutes: silent background refresh
                        refreshSchedule(target, silent = true)
                    }
                    // Otherwise: cache is fresh (< 30 min), do not make network call!
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
            val oldLessons = storage.getLessons(target.id)
            storage.saveLessons(target.id, parsedLessons)
            val now = Clock.System.now().toEpochMilliseconds()
            storage.setLastSyncTime(target.id, now)
            _errorMessage.value = null
            if (!silent) {
                _refreshStatus.value = com.jetbrains.kmpapp.data.model.RefreshStatus.Success()
            }

            if (oldLessons != null && oldLessons.isNotEmpty()) {
                val diff = computeDiff(oldLessons, parsedLessons, target)
                if (diff != null) {
                    _activeDiff.value = diff
                }
            }
        } catch (e: Exception) {
            println("refreshSchedule error for ${target.targetTitle}: ${e.message}")
            val code = com.jetbrains.kmpapp.data.model.AppErrorCode.fromException(e)
            _refreshStatus.value = com.jetbrains.kmpapp.data.model.RefreshStatus.Error(code)
            // Only show user-facing full-screen error if there is NO cached data at all
            val cached = storage.getLessons(target.id)
            val hasCached = cached != null && cached.isNotEmpty()
            if (!hasCached) {
                _errorMessage.value = "Ошибка (${code.code}): ${code.shortTitle}"
            }
        } finally {
            if (!silent) {
                _isLoading.value = false
            }
        }
    }

    private fun computeDiff(
        oldLessons: List<Lesson>,
        newLessons: List<Lesson>,
        target: ScheduleTarget
    ): ScheduleDiff? {
        val oldMap = oldLessons.groupBy { "${it.date}_${it.bellNumber}_${it.subject.trim().lowercase()}" }
        val newMap = newLessons.groupBy { "${it.date}_${it.bellNumber}_${it.subject.trim().lowercase()}" }

        val items = mutableListOf<LessonDiffItem>()

        for ((key, newGroup) in newMap) {
            val oldGroup = oldMap[key]
            if (oldGroup == null) {
                for (lesson in newGroup) {
                    items.add(
                        LessonDiffItem(
                            type = LessonDiffType.ADDED,
                            date = lesson.date,
                            bellNumber = lesson.bellNumber,
                            subject = lesson.subject,
                            description = "Новая пара (${lesson.bellNumber} пара, ауд. ${lesson.classrooms.joinToString().ifEmpty { "—" }})"
                        )
                    )
                }
            } else {
                val oldFirst = oldGroup.first()
                val newFirst = newGroup.first()
                val changes = mutableListOf<String>()
                if (oldFirst.classrooms != newFirst.classrooms) {
                    changes.add("ауд: ${oldFirst.classrooms.joinToString().ifEmpty { "—" }} → ${newFirst.classrooms.joinToString().ifEmpty { "—" }}")
                }
                if (oldFirst.teachers != newFirst.teachers) {
                    changes.add("преп: ${oldFirst.teachers.joinToString().ifEmpty { "—" }} → ${newFirst.teachers.joinToString().ifEmpty { "—" }}")
                }
                if (oldFirst.startTime != newFirst.startTime) {
                    changes.add("время: ${oldFirst.startTime} → ${newFirst.startTime}")
                }
                if (changes.isNotEmpty()) {
                    items.add(
                        LessonDiffItem(
                            type = LessonDiffType.MODIFIED,
                            date = newFirst.date,
                            bellNumber = newFirst.bellNumber,
                            subject = newFirst.subject,
                            description = "${newFirst.bellNumber} пара: ${changes.joinToString(", ")}"
                        )
                    )
                }
            }
        }

        for ((key, oldGroup) in oldMap) {
            if (!newMap.containsKey(key)) {
                for (lesson in oldGroup) {
                    items.add(
                        LessonDiffItem(
                            type = LessonDiffType.CANCELLED,
                            date = lesson.date,
                            bellNumber = lesson.bellNumber,
                            subject = lesson.subject,
                            description = "Отменена (${lesson.bellNumber} пара)"
                        )
                    )
                }
            }
        }

        return if (items.isNotEmpty()) {
            ScheduleDiff(
                targetId = target.id,
                targetTitle = target.targetTitle,
                items = items.sortedWith(compareBy({ it.date }, { it.bellNumber }))
            )
        } else null
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
