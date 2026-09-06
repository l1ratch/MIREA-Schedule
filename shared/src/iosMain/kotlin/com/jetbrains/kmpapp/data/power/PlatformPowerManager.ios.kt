package com.jetbrains.kmpapp.data.power

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoPowerStateDidChangeNotification
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

actual class PlatformPowerManager actual constructor() {
    private val _isLowPowerMode = MutableStateFlow(false)
    actual val isLowPowerMode: StateFlow<Boolean> = _isLowPowerMode.asStateFlow()

    private val _isAppInForeground = MutableStateFlow(true)
    actual val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    init {
        val processInfo = NSProcessInfo.processInfo
        _isLowPowerMode.value = processInfo.isLowPowerModeEnabled

        val center = NSNotificationCenter.defaultCenter
        val mainQueue = NSOperationQueue.mainQueue

        center.addObserverForName(
            name = NSProcessInfoPowerStateDidChangeNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            _isLowPowerMode.value = NSProcessInfo.processInfo.isLowPowerModeEnabled
        }

        center.addObserverForName(
            name = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            _isAppInForeground.value = false
        }

        center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            _isAppInForeground.value = false
        }

        center.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            _isAppInForeground.value = true
        }

        center.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = mainQueue
        ) { _ ->
            _isAppInForeground.value = true
        }
    }
}
