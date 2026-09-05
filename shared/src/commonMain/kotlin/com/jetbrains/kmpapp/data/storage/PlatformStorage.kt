package com.jetbrains.kmpapp.data.storage

expect class PlatformStorage() {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}
