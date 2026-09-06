package com.jetbrains.kmpapp.data.power

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class PlatformPowerManager actual constructor() {
    private val _isLowPowerMode = MutableStateFlow(false)
    actual val isLowPowerMode: StateFlow<Boolean> = _isLowPowerMode.asStateFlow()

    private val _isAppInForeground = MutableStateFlow(true)
    actual val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private var runningActivities = 0

    init {
        val context = AndroidContextProvider.context
        if (context != null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            _isLowPowerMode.value = powerManager?.isPowerSaveMode ?: false

            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    _isLowPowerMode.value = powerManager?.isPowerSaveMode ?: false
                }
            }
            try {
                context.registerReceiver(receiver, filter)
            } catch (_: Throwable) {}

            val app = context.applicationContext as? Application
            app?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    runningActivities++
                    _isAppInForeground.value = runningActivities > 0
                }

                override fun onActivityStopped(activity: Activity) {
                    runningActivities = (runningActivities - 1).coerceAtLeast(0)
                    _isAppInForeground.value = runningActivities > 0
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        }
    }
}
