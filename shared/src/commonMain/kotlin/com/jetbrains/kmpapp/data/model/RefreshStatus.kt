package com.jetbrains.kmpapp.data.model

sealed interface RefreshStatus {
    data class Success(val message: String = "Расписание обновлено") : RefreshStatus
    data class Error(val code: AppErrorCode, val message: String = "Ошибка (${code.code})") : RefreshStatus
}
