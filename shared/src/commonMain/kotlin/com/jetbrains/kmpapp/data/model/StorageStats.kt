package com.jetbrains.kmpapp.data.model

data class StorageStats(
    val schedulesSizeBytes: Long = 0L,
    val schedulesCount: Int = 0,
    val lessonsCount: Int = 0,
    val targetsSizeBytes: Long = 0L,
    val targetsCount: Int = 0,
    val settingsSizeBytes: Long = 0L,
    val totalSizeBytes: Long = 0L
) {
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes Б"
            bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
            else -> {
                val mb = bytes.toDouble() / (1024 * 1024)
                val rounded = (mb * 10).toInt() / 10.0
                "$rounded МБ"
            }
        }
    }
}
