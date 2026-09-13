package com.jetbrains.kmpapp.data.network

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import platform.CFNetwork.CFNetworkCopySystemProxySettings
import platform.CoreFoundation.CFDictionaryGetCount
import platform.CoreFoundation.CFDictionaryGetKeysAndValues
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringGetCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.__CFDictionary
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
    val scopedKey = CFStringCreateWithCString(
        null, "__SCOPED__".cstr.ptr, kCFStringEncodingUTF8
    ) ?: return@memScoped false
    val scoped = CFDictionaryGetValue(dict, scopedKey) ?: return@memScoped false
    val scopedDict = scoped.reinterpret<__CFDictionary>()
    val count = CFDictionaryGetCount(scopedDict).toInt()
    if (count == 0) return@memScoped false

    val keys = allocArray<CPointerVar<CFStringRef>>(count)
    CFDictionaryGetKeysAndValues(scopedDict, keys, null)
    val buf = allocArray<ByteVar>(64)
    val markers = listOf("tap", "tun", "ppp", "ipsec", "utun")
    for (i in 0 until count) {
        val key = keys[i] ?: continue
        if (CFStringGetCString(key, buf, 64L, kCFStringEncodingUTF8)) {
            val name = buf.toKString().lowercase()
            if (markers.any { name.contains(it) }) return@memScoped true
        }
    }
    false
}
