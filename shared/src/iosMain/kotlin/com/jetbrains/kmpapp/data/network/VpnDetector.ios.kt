package com.jetbrains.kmpapp.data.network

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CFNetwork.CFNetworkCopySystemProxySettings
import platform.CoreFoundation.CFDictionaryApplyFunction
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringGetCString
import platform.CoreFoundation.__CFString
import platform.CoreFoundation.kCFStringEncodingUTF8

// ponytail: публичного API «VPN включён» на iOS нет — это канонический
// детектор «AppsFlyer-style» по системным настройкам прокси: ключ
// __SCOPED__ содержит интерфейсы, для которых заданы прокси; у любого
// активного туннеля (WireGuard, OpenVPN, корпоративный NEPacketTunnelProvider)
// там появляется tap/tun/ppp/ipsec/utun-интерфейс. Если эвристика начнёт
// ошибаться — следующий уровень: GeoIP-проверка на сервере.
@OptIn(ExperimentalForeignApi::class)
actual fun detectVpnActive(): Boolean = memScoped {
    val dict = CFNetworkCopySystemProxySettings() ?: return@memScoped false
    val scopedKey = CFStringCreateWithCString(null, "__SCOPED__", kCFStringEncodingUTF8)
        ?: return@memScoped false
    val scoped = CFDictionaryGetValue(dict, scopedKey) ?: return@memScoped false

    // Пишем 1 в flag из C-коллбека, если встретили туннельный интерфейс.
    val flag = alloc<IntVar>()
    CFDictionaryApplyFunction(
        scoped,
        staticCFunction { key, _, ctx ->
            if (key != null && ctx != null) {
                val buf = ByteArray(64)
                val ok = buf.usePinned { pinned ->
                    CFStringGetCString(
                        key.reinterpret<__CFString>(),
                        pinned.addressOf(0), 64L, kCFStringEncodingUTF8
                    )
                }
                if (ok) {
                    val end = buf.indexOf(0).takeIf { it >= 0 } ?: buf.size
                    val name = buf.decodeToString(0, end).lowercase()
                    if (TUNNEL_MARKERS.any { name.contains(it) }) {
                        ctx.reinterpret<IntVar>().pointed.value = 1
                    }
                }
            }
        },
        flag.ptr
    )
    flag.value == 1
}

private val TUNNEL_MARKERS = listOf("tap", "tun", "ppp", "ipsec", "utun")
