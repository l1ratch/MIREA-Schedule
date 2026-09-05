package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugConfig {
    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    private val _networkDelayMs = MutableStateFlow(0L)
    val networkDelayMs: StateFlow<Long> = _networkDelayMs.asStateFlow()

    private val _detailedLogging = MutableStateFlow(false)
    val detailedLogging: StateFlow<Boolean> = _detailedLogging.asStateFlow()

    fun setOfflineSimulated(enabled: Boolean) {
        _isOfflineSimulated.value = enabled
    }

    fun setNetworkDelay(delayMs: Long) {
        _networkDelayMs.value = delayMs
    }

    fun setDetailedLogging(enabled: Boolean) {
        _detailedLogging.value = enabled
    }
}
