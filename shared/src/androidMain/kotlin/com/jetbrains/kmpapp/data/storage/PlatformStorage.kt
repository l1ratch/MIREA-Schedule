package com.jetbrains.kmpapp.data.storage

import android.content.Context
import android.content.SharedPreferences
import java.io.File

actual class PlatformStorage actual constructor() {
    private val prefs: SharedPreferences?
        get() = AndroidContextProvider.context?.getSharedPreferences("mirea_schedule_cache", Context.MODE_PRIVATE)

    private val fallbackDir: File by lazy {
        val baseDir = AndroidContextProvider.context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
        File(baseDir, "mirea_cache").apply { mkdirs() }
    }

    actual fun saveString(key: String, value: String) {
        val p = prefs
        if (p != null) {
            p.edit().putString(key, value).apply()
        } else {
            try {
                File(fallbackDir, sanitizeKey(key)).writeText(value)
            } catch (_: Exception) {}
        }
    }

    actual fun getString(key: String): String? {
        val p = prefs
        if (p != null) {
            return p.getString(key, null)
        }
        return try {
            val file = File(fallbackDir, sanitizeKey(key))
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    actual fun remove(key: String) {
        val p = prefs
        if (p != null) {
            p.edit().remove(key).apply()
        } else {
            try {
                File(fallbackDir, sanitizeKey(key)).delete()
            } catch (_: Exception) {}
        }
    }

    private fun sanitizeKey(key: String): String = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
