package com.jetbrains.kmpapp.data.model

enum class AppErrorCode(
    val code: String,
    val shortTitle: String,
    val description: String
) {
    ERR_NO_NETWORK(
        code = "E-101",
        shortTitle = "Нет сети",
        description = "Отсутствует подключение к интернету"
    ),
    ERR_TIMEOUT(
        code = "E-102",
        shortTitle = "Таймаут",
        description = "Превышено время ожидания ответа сервера МИРЭА"
    ),
    ERR_SERVER_ERROR(
        code = "E-103",
        shortTitle = "Сбой сервера",
        description = "Сервер расписания РТУ МИРЭА временно недоступен (5xx)"
    ),
    ERR_NOT_FOUND(
        code = "E-104",
        shortTitle = "Не найдено",
        description = "Расписание для выбранной группы не найдено"
    ),
    ERR_PARSE_ERROR(
        code = "E-105",
        shortTitle = "Ошибка парсинга",
        description = "Не удалось обработать формат данных iCal"
    ),
    ERR_STORAGE_READ(
        code = "E-201",
        shortTitle = "Ошибка кэша",
        description = "Не удалось прочитать сохраненное расписание"
    ),
    ERR_STORAGE_WRITE(
        code = "E-202",
        shortTitle = "Сбой записи",
        description = "Не удалось сохранить расписание в локальную память"
    ),
    ERR_UPDATE_CHECK(
        code = "E-301",
        shortTitle = "Сбой обновления",
        description = "Не удалось связаться с GitHub для проверки версий"
    ),
    ERR_UNKNOWN(
        code = "E-999",
        shortTitle = "Ошибка",
        description = "Произошла непредвиденная ошибка"
    );

    companion object {
        fun fromException(e: Throwable): AppErrorCode {
            val msg = e.message?.lowercase() ?: ""
            return when {
                msg.contains("timeout") || msg.contains("timed out") -> ERR_TIMEOUT
                msg.contains("network") || msg.contains("connect") || msg.contains("unreachable") || msg.contains("no address") -> ERR_NO_NETWORK
                msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504") -> ERR_SERVER_ERROR
                msg.contains("404") || msg.contains("not found") -> ERR_NOT_FOUND
                msg.contains("parse") || msg.contains("ical") || msg.contains("format") -> ERR_PARSE_ERROR
                else -> ERR_UNKNOWN
            }
        }
    }
}
