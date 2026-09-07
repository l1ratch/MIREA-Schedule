package com.jetbrains.kmpapp.data.power

import kotlinx.coroutines.flow.StateFlow

expect class PlatformPowerManager() {
    val isLowPowerMode: StateFlow<Boolean>
    val isAppInForeground: StateFlow<Boolean>
}
