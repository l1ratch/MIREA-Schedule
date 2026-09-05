package com.jetbrains.kmpapp.data.storage

import android.app.ActivityThread
import android.content.Context
import android.content.SharedPreferences

actual class PlatformStorage actual constructor() {
    private val prefs: SharedPreferences? by lazy {
        try {
            val app = ActivityThread.currentApplication()
            app?.getSharedPreferences("mirea_schedule_cache", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    actual fun saveString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun getString(key: String): String? {
        return prefs?.getString(key, null)
    }

    actual fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
}
