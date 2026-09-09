package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugConfig {
    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    fun setOfflineSimulated(enabled: Boolean) {
        _isOfflineSimulated.value = enabled
    }

    private val _isMapCoordinatePlaneEnabled = MutableStateFlow(false)
    val isMapCoordinatePlaneEnabled: StateFlow<Boolean> = _isMapCoordinatePlaneEnabled.asStateFlow()

    fun setMapCoordinatePlaneEnabled(enabled: Boolean) {
        _isMapCoordinatePlaneEnabled.value = enabled
    }
}
