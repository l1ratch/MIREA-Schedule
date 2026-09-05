package com.jetbrains.kmpapp.screens.other

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ScheduleTargetType
import com.jetbrains.kmpapp.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import com.jetbrains.kmpapp.data.update.AppUpdateChecker
import com.jetbrains.kmpapp.data.update.UpdateCheckResult
import kotlinx.coroutines.launch

enum class TargetSortOrder(val displayName: String) {
    TITLE_ASC("По названию (А → Я / 0 → 9)"),
    TITLE_DESC("По названию (Я → А / 9 → 0)"),
    RECENT("Сначала новые"),
    OLDEST("Сначала старые")
}

enum class OtherSubScreen {
    ROOT,
    MANAGE_SCHEDULES,
    SETTINGS,
    DATA_AND_CACHE,
    ABOUT,
    DEBUG_SETTINGS
}

class OtherViewModel(
    private val repository: ScheduleRepository,
    private val updateChecker: AppUpdateChecker
) : ViewModel() {

    val savedTargets: StateFlow<List<ScheduleTarget>> = repository.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = repository.selectedTarget
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val showEmptyLessons: StateFlow<Boolean> = repository.showEmptyLessons
    val themeMode: StateFlow<ThemeMode> = repository.themeMode

    private val _activeSubScreen = MutableStateFlow(OtherSubScreen.ROOT)
    val activeSubScreen: StateFlow<OtherSubScreen> = _activeSubScreen.asStateFlow()

    fun navigateToSubScreen(subScreen: OtherSubScreen) {
        _activeSubScreen.value = subScreen
    }

    fun resetToRoot() {
        _activeSubScreen.value = OtherSubScreen.ROOT
    }

    private val _storageStats = MutableStateFlow(repository.getStorageStats())
    val storageStats: StateFlow<com.jetbrains.kmpapp.data.model.StorageStats> = _storageStats.asStateFlow()

    fun refreshStorageStats() {
        _storageStats.value = repository.getStorageStats()
    }

    private val _contributors = MutableStateFlow<List<com.jetbrains.kmpapp.data.model.GitHubContributor>>(
        listOf(
            com.jetbrains.kmpapp.data.model.GitHubContributor(
                login = "l1ratch",
                htmlUrl = "https://github.com/l1ratch",
                avatarUrl = "https://avatars.githubusercontent.com/u/103525164?v=4",
                contributions = 14,
                role = "Создатель и ведущий разработчик"
            )
        )
    )
    val contributors: StateFlow<List<com.jetbrains.kmpapp.data.model.GitHubContributor>> = _contributors.asStateFlow()

    private val _isLoadingContributors = MutableStateFlow(false)
    val isLoadingContributors: StateFlow<Boolean> = _isLoadingContributors.asStateFlow()

    fun loadContributors() {
        viewModelScope.launch {
            _isLoadingContributors.value = true
            val fetched = updateChecker.fetchContributors()
            if (fetched.isNotEmpty()) {
                val staticLead = com.jetbrains.kmpapp.data.model.GitHubContributor(
                    login = "l1ratch",
                    htmlUrl = "https://github.com/l1ratch",
                    avatarUrl = "https://avatars.githubusercontent.com/u/103525164?v=4",
                    contributions = 14,
                    role = "Создатель и ведущий разработчик"
                )
                val otherContributors = fetched.filterNot { it.login.equals("l1ratch", ignoreCase = true) }
                _contributors.value = listOf(staticLead) + otherContributors
            }
            _isLoadingContributors.value = false
        }
    }

    private val _updateResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateResult: StateFlow<UpdateCheckResult?> = _updateResult.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow<String?>(null)
    val updateStatusMessage: StateFlow<String?> = _updateStatusMessage.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateStatusMessage.value = null
            val result = updateChecker.checkForUpdates()
            _updateResult.value = result
            _isCheckingUpdate.value = false
            if (result != null && !result.hasUpdate) {
                _updateStatusMessage.value = "У вас установлена последняя версия (${result.currentVersion})"
            } else if (result == null) {
                _updateStatusMessage.value = "Не удалось проверить обновления"
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateResult.value = null
        _updateStatusMessage.value = null
    }

    // Search, filter, and sort state for ManageSchedulesScreen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<ScheduleTargetType?>(null)
    val filterType: StateFlow<ScheduleTargetType?> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(TargetSortOrder.TITLE_ASC)
    val sortOrder: StateFlow<TargetSortOrder> = _sortOrder.asStateFlow()

    val filteredSavedTargets: StateFlow<List<ScheduleTarget>> = combine(
        repository.savedTargets,
        _searchQuery,
        _filterType,
        _sortOrder
    ) { list, query, filter, sort ->
        var result = list
        if (filter != null) {
            result = result.filter { it.type == filter }
        }
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            result = result.filter {
                it.targetTitle.contains(trimmed, ignoreCase = true) ||
                it.fullTitle.contains(trimmed, ignoreCase = true)
            }
        }
        when (sort) {
            TargetSortOrder.TITLE_ASC -> result.sortedBy { it.targetTitle.lowercase() }
            TargetSortOrder.TITLE_DESC -> result.sortedByDescending { it.targetTitle.lowercase() }
            TargetSortOrder.RECENT -> result
            TargetSortOrder.OLDEST -> result.reversed()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: ScheduleTargetType?) {
        _filterType.value = type
    }

    fun setSortOrder(order: TargetSortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortDirection() {
        _sortOrder.value = when (_sortOrder.value) {
            TargetSortOrder.TITLE_ASC -> TargetSortOrder.TITLE_DESC
            TargetSortOrder.TITLE_DESC -> TargetSortOrder.TITLE_ASC
            TargetSortOrder.RECENT -> TargetSortOrder.OLDEST
            TargetSortOrder.OLDEST -> TargetSortOrder.RECENT
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setShowEmptyLessons(enabled: Boolean) {
        repository.setShowEmptyLessons(enabled)
    }

    fun selectTarget(target: ScheduleTarget) {
        repository.selectTarget(target)
    }

    fun removeTarget(targetId: Int) {
        repository.removeTarget(targetId)
    }

    fun refreshSchedule() {
        repository.refreshCurrentSchedule()
    }

    fun clearCache() {
        repository.clearCache()
        _storageStats.value = repository.getStorageStats()
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return repository.search(query)
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        repository.addAndSelectTarget(target)
    }
}
