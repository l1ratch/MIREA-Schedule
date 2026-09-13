package com.jetbrains.kmpapp.data.network

/**
 * Детектор активного VPN на уровне системы. Смысл: серверы МИРЭА
 * (включая API расписания) доступны только с IP России — при включённом
 * VPN обновление/добавление расписания ломается, UI показывает плашку.
 *
 * Android — TRANSPORT_VPN у активной сети (actual в androidMain).
 * iOS — публичного API нет, поэтому actual в iosMain делегирует
 * Swift-движку (канонический «AppsFlyer-style» разбор
 * CFNetworkCopySystemProxySettings -> __SCOPED__).
 */
expect fun detectVpnActive(): Boolean

/** Мост к Swift-движку (регистрируется в iOSApp). */
object VpnStatus {
    interface Engine {
        fun isVpnActive(): Boolean
    }

    private var engine: Engine? = null

    fun setEngine(newEngine: Engine) {
        engine = newEngine
    }

    fun isActive(): Boolean = engine?.isVpnActive() ?: false
}
