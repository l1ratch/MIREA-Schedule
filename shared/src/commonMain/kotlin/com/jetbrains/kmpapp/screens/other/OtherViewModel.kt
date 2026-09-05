package com.jetbrains.kmpapp.screens.other

import androidx.lifecycle.ViewModel
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import kotlinx.coroutines.flow.StateFlow

class OtherViewModel(
    private val repository: ScheduleRepository
) : ViewModel() {

    val savedTargets: StateFlow<List<ScheduleTarget>> = repository.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = repository.selectedTarget
    val isLoading: StateFlow<Boolean> = repository.isLoading

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
    }
}
