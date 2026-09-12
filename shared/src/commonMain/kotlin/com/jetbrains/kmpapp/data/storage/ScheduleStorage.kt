package com.jetbrains.kmpapp.data.storage

import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.appicon.AppIconManager
import com.jetbrains.kmpapp.data.notifications.NotificationsManager
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.screens.components.AppTab
import com.jetbrains.kmpapp.theme.ThemeOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val _showEmptyLessons = MutableStateFlow<Boolean>(true)
    val showEmptyLessons: StateFlow<Boolean> = _showEmptyLessons.asStateFlow()

    private val _showLessonProgress = MutableStateFlow<Boolean>(true)
    val showLessonProgress: StateFlow<Boolean> = _showLessonProgress.asStateFlow()

    private val _autoScrollToCurrentLesson = MutableStateFlow<Boolean>(true)
    val autoScrollToCurrentLesson: StateFlow<Boolean> = _autoScrollToCurrentLesson.asStateFlow()

    private val _showAbbreviatedNames = MutableStateFlow<Boolean>(false)
    val showAbbreviatedNames: StateFlow<Boolean> = _showAbbreviatedNames.asStateFlow()

    private val _themeMode = MutableStateFlow<ThemeMode>(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dockTabs = MutableStateFlow<List<AppTab>>(DEFAULT_DOCK_TABS)
    val dockTabs: StateFlow<List<AppTab>> = _dockTabs.asStateFlow()

    private val _themeOverlay = MutableStateFlow(ThemeOverlay.NONE)
    val themeOverlay: StateFlow<ThemeOverlay> = _themeOverlay.asStateFlow()

    val isSakuraTheme: StateFlow<Boolean> = themeOverlay.map { it == ThemeOverlay.SAKURA }
        .stateIn(scope, SharingStarted.Eagerly, false)
    val isCyberpunkTheme: StateFlow<Boolean> = themeOverlay.map { it == ThemeOverlay.CYBERPUNK }
        .stateIn(scope, SharingStarted.Eagerly, false)
    val isMatrixTheme: StateFlow<Boolean> = themeOverlay.map { it == ThemeOverlay.MATRIX }
        .stateIn(scope, SharingStarted.Eagerly, false)
    private val _cheatsAgreed = MutableStateFlow<Boolean?>(null)
    val cheatsAgreed: StateFlow<Boolean?> = _cheatsAgreed.asStateFlow()
    private val _cheatsBlocked = MutableStateFlow(false)
    val cheatsBlocked: StateFlow<Boolean> = _cheatsBlocked.asStateFlow()

    private val _betaChannel = MutableStateFlow(false)
    val betaChannel: StateFlow<Boolean> = _betaChannel.asStateFlow()

    private val _analyticsEnabled = MutableStateFlow(true)
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    // null = согласие ещё не спрашивали: первый вход ИЛИ обновление со старой версии
    private val _analyticsConsent = MutableStateFlow<Boolean?>(null)
    val analyticsConsent: StateFlow<Boolean?> = _analyticsConsent.asStateFlow()

    private val _appIcon = MutableStateFlow(AppIconManager.ICON_DEFAULT)
    val appIcon: StateFlow<String> = _appIcon.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifyMinutesBefore = MutableStateFlow(15)
    val notifyMinutesBefore: StateFlow<Int> = _notifyMinutesBefore.asStateFlow()

    private val _askBeforeNoteDelete = MutableStateFlow(true)
    val askBeforeNoteDelete: StateFlow<Boolean> = _askBeforeNoteDelete.asStateFlow()

    private val _notePages = MutableStateFlow<List<com.jetbrains.kmpapp.data.model.NotePage>>(
        com.jetbrains.kmpapp.data.model.defaultNotePages()
    )
    val notePages: StateFlow<List<com.jetbrains.kmpapp.data.model.NotePage>> = _notePages.asStateFlow()

    private val lastSyncTimes = mutableMapOf<Int, Long>()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        try {
            loadPreferenceFlags()
            loadDockTabsSetting()
            restoreScheduleData()
            loadNotePages()
        } catch (t: Throwable) {
            println("ScheduleStorage: failed to load persisted state: ${t.message}")
        }
    }

    /** Тумблеры и скалярные настройки; отсутствующий или битый ключ = дефолт. */
    private fun loadPreferenceFlags() {
        _themeMode.value = loadThemeModeSetting()
        _showEmptyLessons.value = loadBooleanFlag(KEY_SHOW_EMPTY_LESSONS, true)
        _showLessonProgress.value = loadBooleanFlag(KEY_SHOW_LESSON_PROGRESS, true)
        _autoScrollToCurrentLesson.value = loadBooleanFlag(KEY_AUTO_SCROLL_CURRENT_LESSON, true)
        _showAbbreviatedNames.value = loadBooleanFlag(KEY_SHOW_ABBREVIATED_NAMES, false)
        _themeOverlay.value = loadThemeOverlay()
        _cheatsAgreed.value = nullableFlag(KEY_CHEATS_AGREED)
        _cheatsBlocked.value = loadBooleanFlag(KEY_CHEATS_BLOCKED, false)
        _betaChannel.value = loadBooleanFlag(KEY_BETA_CHANNEL, false)
        _analyticsEnabled.value = loadBooleanFlag(KEY_ANALYTICS_ENABLED, true)
        _analyticsConsent.value = nullableFlag(KEY_ANALYTICS_CONSENT)
        // До первого ответа на диалог согласия ничего не отправляем.
        AppAnalytics.setEnabled(_analyticsEnabled.value && _analyticsConsent.value != null)
        _appIcon.value = platformStorage.getString(KEY_APP_ICON) ?: AppIconManager.ICON_DEFAULT
        _notificationsEnabled.value = loadBooleanFlag(KEY_NOTIFICATIONS_ENABLED, false)
        _notifyMinutesBefore.value =
            platformStorage.getString(KEY_NOTIFY_MINUTES_BEFORE)?.toIntOrNull() ?: 15
        _askBeforeNoteDelete.value = loadBooleanFlag(KEY_ASK_BEFORE_NOTE_DELETE, true)
    }

    private fun loadBooleanFlag(key: String, default: Boolean): Boolean = try {
        val s = platformStorage.getString(key)
        if (s.isNullOrBlank()) default else s.toBooleanStrictOrNull() ?: default
    } catch (_: Throwable) {
        default
    }

    /** null = ключа нет (решение ещё не принимали); битое значение тоже null. */
    private fun nullableFlag(key: String): Boolean? = try {
        platformStorage.getString(key)?.toBooleanStrictOrNull()
    } catch (_: Throwable) {
        null
    }

    private fun loadThemeModeSetting(): ThemeMode = try {
        val s = platformStorage.getString(KEY_APP_THEME)
        if (s.isNullOrBlank()) ThemeMode.SYSTEM else try {
            ThemeMode.valueOf(s)
        } catch (_: Throwable) {
            ThemeMode.SYSTEM
        }
    } catch (_: Throwable) {
        ThemeMode.SYSTEM
    }

    private fun loadDockTabsSetting() {
        try {
            val dockTabsStr = platformStorage.getString(KEY_DOCK_TABS)
            if (!dockTabsStr.isNullOrBlank()) {
                val loaded = dockTabsStr.split(",").mapNotNull { name ->
                    try { AppTab.valueOf(name.trim()) } catch (_: Throwable) { null }
                }
                // ponytail: раньше совпадение со старыми дефолтами принудительно
                // сбрасывалось на новый дефолт — это стирало живой выбор
                // (дока без «Аудиторий» == старый дефолт). Сохранённое доверяем.
                _dockTabs.value = sanitizeDockTabs(loaded)
            } else {
                _dockTabs.value = DEFAULT_DOCK_TABS
            }
        } catch (_: Throwable) {
            _dockTabs.value = DEFAULT_DOCK_TABS
        }
    }

    /** Цели, кэш уроков и выбранная цель. Порядок важен: кэш ДО выбранной цели. */
    private fun restoreScheduleData() {
        // Restore saved targets
        val targets: List<ScheduleTarget> = try {
            val targetsJson = platformStorage.getString(KEY_SAVED_TARGETS)
            if (!targetsJson.isNullOrBlank()) {
                try { json.decodeFromString(targetsJson) } catch (_: Throwable) { emptyList() }
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
        _savedTargets.value = targets

        // IMPORTANT: Restore cached lessons for all targets BEFORE setting selected target!
        val loadedCache = mutableMapOf<Int, List<Lesson>>()
        for (target in targets) {
            try {
                val lessonsJson = platformStorage.getString(KEY_LESSONS_PREFIX + target.id)
                if (!lessonsJson.isNullOrBlank()) {
                    try {
                        val lessons: List<Lesson> = json.decodeFromString(lessonsJson)
                        loadedCache[target.id] = lessons
                    } catch (_: Throwable) {}
                }
                val syncTimeStr = platformStorage.getString(KEY_LAST_SYNC_PREFIX + target.id)
                syncTimeStr?.toLongOrNull()?.let { lastSyncTimes[target.id] = it }
            } catch (_: Throwable) {}
        }
        _cachedLessons.value = loadedCache

        // Now that cached lessons and timestamps are ready, restore selected target!
        try {
            val activeIdStr = platformStorage.getString(KEY_SELECTED_TARGET_ID)
            val activeId = activeIdStr?.toIntOrNull()
            val selected = targets.firstOrNull { it.id == activeId } ?: targets.firstOrNull()
            _selectedTarget.value = selected
        } catch (_: Throwable) {}
    }

    private fun loadNotePages() {
        try {
            val pagesJson = platformStorage.getString(KEY_NOTES)
            val loaded: List<com.jetbrains.kmpapp.data.model.NotePage> = if (!pagesJson.isNullOrBlank()) {
                try {
                    json.decodeFromString(pagesJson)
                } catch (_: Throwable) {
                    com.jetbrains.kmpapp.data.model.defaultNotePages()
                }
            } else {
                com.jetbrains.kmpapp.data.model.defaultNotePages()
            }
            _notePages.value = loaded
        } catch (_: Throwable) {
            _notePages.value = com.jetbrains.kmpapp.data.model.defaultNotePages()
        }
    }

    /** Полная перезапись страниц конспектов; запись на диск с дебаунсом. */
    fun saveNotePages(pages: List<com.jetbrains.kmpapp.data.model.NotePage>) {
        _notePages.value = pages
        scope.launch {
            try {
                platformStorage.saveString(KEY_NOTES, json.encodeToString(pages))
            } catch (e: Exception) {
                println("Failed to persist note pages: ${e.message}")
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

    fun setShowLessonProgress(enabled: Boolean) {
        _showLessonProgress.value = enabled
        scope.launch {
            try {
                platformStorage.saveString(KEY_SHOW_LESSON_PROGRESS, enabled.toString())
            } catch (e: Exception) {
                println("Failed to persist showLessonProgress: ${e.message}")
            }
        }
    }

    fun setAutoScrollToCurrentLesson(enabled: Boolean) {
        _autoScrollToCurrentLesson.value = enabled
        scope.launch {
            try {
                platformStorage.saveString(KEY_AUTO_SCROLL_CURRENT_LESSON, enabled.toString())
            } catch (e: Exception) {
                println("Failed to persist autoScrollToCurrentLesson: ${e.message}")
            }
        }
    }

    fun setShowAbbreviatedNames(enabled: Boolean) {
        _showAbbreviatedNames.value = enabled
        scope.launch {
            try {
                platformStorage.saveString(KEY_SHOW_ABBREVIATED_NAMES, enabled.toString())
            } catch (e: Exception) {
                println("Failed to persist showAbbreviatedNames: ${e.message}")
            }
        }
    }

    fun setThemeOverlay(overlay: ThemeOverlay) {
        _themeOverlay.value = overlay
        scope.launch {
            try {
                platformStorage.saveString(KEY_THEME_OVERLAY, overlay.name)
                platformStorage.saveString(KEY_SAKURA_THEME, (overlay == ThemeOverlay.SAKURA).toString())
                platformStorage.saveString(KEY_CYBERPUNK_THEME, (overlay == ThemeOverlay.CYBERPUNK).toString())
                platformStorage.saveString(KEY_MATRIX_THEME, (overlay == ThemeOverlay.MATRIX).toString())
            } catch (e: Exception) {
                println("Failed to persist theme overlay: ${e.message}")
            }
        }
    }

    fun setCyberpunkTheme(enabled: Boolean) {
        setThemeOverlay(if (enabled) ThemeOverlay.CYBERPUNK else ThemeOverlay.NONE)
    }

    fun setMatrixTheme(enabled: Boolean) {
        setThemeOverlay(if (enabled) ThemeOverlay.MATRIX else ThemeOverlay.NONE)
    }

    fun setCheatsAgreed(agreed: Boolean?) {
        _cheatsAgreed.value = agreed
        scope.launch {
            if (agreed == null) platformStorage.remove(KEY_CHEATS_AGREED)
            else platformStorage.saveString(KEY_CHEATS_AGREED, agreed.toString())
        }
    }

    fun setCheatsBlocked(blocked: Boolean) {
        _cheatsBlocked.value = blocked
        scope.launch { platformStorage.saveString(KEY_CHEATS_BLOCKED, blocked.toString()) }
    }

    fun setBetaChannel(enabled: Boolean) {
        _betaChannel.value = enabled
        scope.launch { platformStorage.saveString(KEY_BETA_CHANNEL, enabled.toString()) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        _analyticsEnabled.value = enabled
        // Ручное включение тумблера = согласие; до ответа на диалог ничего не уходит
        if (enabled) _analyticsConsent.value = _analyticsConsent.value ?: true
        AppAnalytics.setEnabled(enabled && _analyticsConsent.value != null)
        scope.launch { platformStorage.saveString(KEY_ANALYTICS_ENABLED, enabled.toString()) }
    }

    /** Ответ на диалог первого запуска: сразу задаёт и согласие, и тумблер. */
    fun setAnalyticsConsent(accepted: Boolean) {
        _analyticsConsent.value = accepted
        _analyticsEnabled.value = accepted
        AppAnalytics.setEnabled(accepted)
        scope.launch {
            platformStorage.saveString(KEY_ANALYTICS_CONSENT, accepted.toString())
            platformStorage.saveString(KEY_ANALYTICS_ENABLED, accepted.toString())
        }
    }

    /** Выбор иконки приложения; применяется немедленно (iOS), хранится для UI. */
    fun setAppIcon(name: String) {
        _appIcon.value = name
        AppIconManager.apply(name)
        scope.launch { platformStorage.saveString(KEY_APP_ICON, name) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        if (enabled) NotificationsManager.requestAuthorization()
        scope.launch { platformStorage.saveString(KEY_NOTIFICATIONS_ENABLED, enabled.toString()) }
    }

    fun setNotifyMinutesBefore(minutes: Int) {
        _notifyMinutesBefore.value = minutes
        scope.launch { platformStorage.saveString(KEY_NOTIFY_MINUTES_BEFORE, minutes.toString()) }
    }

    fun setAskBeforeNoteDelete(ask: Boolean) {
        _askBeforeNoteDelete.value = ask
        scope.launch { platformStorage.saveString(KEY_ASK_BEFORE_NOTE_DELETE, ask.toString()) }
    }

    fun setSakuraThemeExclusive(enabled: Boolean) {
        setThemeOverlay(if (enabled) ThemeOverlay.SAKURA else ThemeOverlay.NONE)
    }

    private fun loadThemeOverlay(): ThemeOverlay {
        val stored = platformStorage.getString(KEY_THEME_OVERLAY)
            ?.let { runCatching { ThemeOverlay.valueOf(it) }.getOrNull() }
        if (stored != null) return stored
        return when {
            platformStorage.getString(KEY_MATRIX_THEME)?.toBooleanStrictOrNull() == true -> ThemeOverlay.MATRIX
            platformStorage.getString(KEY_CYBERPUNK_THEME)?.toBooleanStrictOrNull() == true -> ThemeOverlay.CYBERPUNK
            platformStorage.getString(KEY_SAKURA_THEME)?.toBooleanStrictOrNull() == true -> ThemeOverlay.SAKURA
            else -> ThemeOverlay.NONE
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
        return try {
            val middle = tabs.filter { !it.isFixed }.distinct().take(3)
            listOf(AppTab.SCHEDULE) + middle + listOf(AppTab.OTHER)
        } catch (_: Throwable) {
            DEFAULT_DOCK_TABS
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

    fun resetAllData() {
        val cheatsAgreedBefore = _cheatsAgreed.value
        val cheatsBlockedBefore = _cheatsBlocked.value
        val betaChannelBefore = _betaChannel.value
        val analyticsEnabledBefore = _analyticsEnabled.value
        val analyticsConsentBefore = _analyticsConsent.value
        val notificationsEnabledBefore = _notificationsEnabled.value
        val notifyMinutesBeforeBefore = _notifyMinutesBefore.value
        val askBeforeNoteDeleteBefore = _askBeforeNoteDelete.value
        platformStorage.clearAll()
        _savedTargets.value = emptyList()
        _selectedTarget.value = null
        _cachedLessons.value = emptyMap()
        _showEmptyLessons.value = true
        _showLessonProgress.value = true
        _autoScrollToCurrentLesson.value = true
        _showAbbreviatedNames.value = false
        _themeMode.value = ThemeMode.SYSTEM
        _dockTabs.value = DEFAULT_DOCK_TABS
        _themeOverlay.value = ThemeOverlay.NONE
        _cheatsAgreed.value = cheatsAgreedBefore
        _cheatsBlocked.value = cheatsBlockedBefore
        _betaChannel.value = betaChannelBefore
        _analyticsEnabled.value = analyticsEnabledBefore
        _analyticsConsent.value = analyticsConsentBefore
        _notificationsEnabled.value = notificationsEnabledBefore
        _notifyMinutesBefore.value = notifyMinutesBeforeBefore
        _askBeforeNoteDelete.value = askBeforeNoteDeleteBefore
        _notePages.value = com.jetbrains.kmpapp.data.model.defaultNotePages()
        lastSyncTimes.clear()
        scope.launch {
            if (cheatsAgreedBefore == null) platformStorage.remove(KEY_CHEATS_AGREED)
            else platformStorage.saveString(KEY_CHEATS_AGREED, cheatsAgreedBefore.toString())
            platformStorage.saveString(KEY_CHEATS_BLOCKED, cheatsBlockedBefore.toString())
            platformStorage.saveString(KEY_BETA_CHANNEL, betaChannelBefore.toString())
            platformStorage.saveString(KEY_ANALYTICS_ENABLED, analyticsEnabledBefore.toString())
            if (analyticsConsentBefore == null) platformStorage.remove(KEY_ANALYTICS_CONSENT)
            else platformStorage.saveString(KEY_ANALYTICS_CONSENT, analyticsConsentBefore.toString())
            platformStorage.saveString(KEY_NOTIFICATIONS_ENABLED, notificationsEnabledBefore.toString())
            platformStorage.saveString(KEY_NOTIFY_MINUTES_BEFORE, notifyMinutesBeforeBefore.toString())
            platformStorage.saveString(KEY_ASK_BEFORE_NOTE_DELETE, askBeforeNoteDeleteBefore.toString())
            platformStorage.saveString(KEY_NOTES, json.encodeToString(_notePages.value))
        }
    }

    fun getStorageStats(): com.jetbrains.kmpapp.data.model.StorageStats {
        return try {
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

            com.jetbrains.kmpapp.data.model.StorageStats(
                schedulesSizeBytes = schedulesBytes,
                schedulesCount = _cachedLessons.value.size,
                lessonsCount = totalLessons,
                targetsSizeBytes = targetsBytes,
                targetsCount = _savedTargets.value.size,
                settingsSizeBytes = settingsBytes,
                totalSizeBytes = total
            )
        } catch (_: Throwable) {
            com.jetbrains.kmpapp.data.model.StorageStats()
        }
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
        private const val KEY_LAST_SYNC_PREFIX = "mirea_last_sync_"
        private const val KEY_SHOW_EMPTY_LESSONS = "mirea_show_empty_lessons"
        private const val KEY_SHOW_LESSON_PROGRESS = "mirea_show_lesson_progress"
        private const val KEY_AUTO_SCROLL_CURRENT_LESSON = "mirea_auto_scroll_current_lesson"
        private const val KEY_SHOW_ABBREVIATED_NAMES = "mirea_show_abbreviated_names"
        private const val KEY_APP_THEME = "mirea_app_theme"
        private const val KEY_DOCK_TABS = "mirea_dock_tabs_order"
        private const val KEY_SAKURA_THEME = "mirea_sakura_theme_secret"
        private const val KEY_CYBERPUNK_THEME = "mirea_cyberpunk_theme_secret"
        private const val KEY_MATRIX_THEME = "mirea_matrix_theme_secret"
        private const val KEY_THEME_OVERLAY = "mirea_theme_overlay"
        private const val KEY_CHEATS_AGREED = "mirea_cheats_agreed"
        private const val KEY_CHEATS_BLOCKED = "mirea_cheats_blocked"
        private const val KEY_BETA_CHANNEL = "mirea_beta_channel"
        private const val KEY_ANALYTICS_ENABLED = "mirea_analytics_enabled"
        private const val KEY_ANALYTICS_CONSENT = "mirea_analytics_consent"
        private const val KEY_APP_ICON = "mirea_app_icon"
        private const val KEY_NOTIFICATIONS_ENABLED = "mirea_notifications_enabled"
        private const val KEY_NOTIFY_MINUTES_BEFORE = "mirea_notify_minutes_before"
        private const val KEY_ASK_BEFORE_NOTE_DELETE = "mirea_ask_before_note_delete"
        private const val KEY_NOTES = "mirea_notes_pages"
        // Дефолт дока для НОВЫХ установок (решение владельца): Существующие
        // пользователи не затрагиваются — их сохранённый док доверяется.
        // «Аудитории» и «Сравнение» добавляются в настройках дока.
        val DEFAULT_DOCK_TABS = listOf(
            AppTab.SCHEDULE,
            AppTab.TASKS,
            AppTab.MAP,
            AppTab.NOTES,
            AppTab.OTHER
        )
    }
}


