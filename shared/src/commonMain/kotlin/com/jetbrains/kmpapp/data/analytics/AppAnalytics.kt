package com.jetbrains.kmpapp.data.analytics

/**
 * Анонимная аналитика (AppMetrica): аудитория, частота разделов, падения.
 *
 * Движок подставляет платформа на старте приложения:
 *  - Android: ScheduleApp.onCreate → AndroidAnalytics (shared/androidMain)
 *  - iOS: iOSApp.init → Swift-класс AppMetricaEngine (iosApp)
 * Падения собираются SDK автоматически после активации.
 *
 * Выключается пользователем тумблером в настройках (mirea_analytics_enabled).
 */
interface AnalyticsEngine {
    fun logEvent(name: String, params: Map<String, String>)
    fun setEnabled(enabled: Boolean)
}

object AppAnalytics {
    /** Публичный ключ AppMetrica — по дизайну системы шьётся в приложение. */
    const val API_KEY = "fc0cde08-05c5-4718-96ee-e9674b8c33e7"

    private var engine: AnalyticsEngine? = null
    private var enabled = true

    fun setEngine(engine: AnalyticsEngine) {
        this.engine = engine
        engine.setEnabled(enabled)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        engine?.setEnabled(value)
    }

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        if (enabled) engine?.logEvent(name, params)
    }
}
