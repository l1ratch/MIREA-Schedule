package com.jetbrains.kmpapp.data.storage

import platform.Foundation.NSUserDefaults

actual class PlatformStorage actual constructor() {
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual fun saveString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    actual fun getString(key: String): String? {
        return userDefaults.stringForKey(key)
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }
}
