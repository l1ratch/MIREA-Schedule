package com.jetbrains.kmpapp.data.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.Darwin.freeifaddrs
import platform.Darwin.getifaddrs
import platform.Darwin.ifaddrs
import platform.posix.AF_INET

// ponytail: эвристика — публичного API «VPN включён» на iOS нет. Считаем
// VPN-интерфейсом туннельный интерфейс С IPv4-адресом (системные utun
// без адреса — не VPN). VPN без IPv4-адреса не увидим; если эвристика
// начнёт ошибаться — усиливать через CFNetworkCopySystemProxySettings.
@OptIn(ExperimentalForeignApi::class)
actual fun detectVpnActive(): Boolean = memScoped {
    val ifap = allocPointerTo<ifaddrs>()
    if (getifaddrs(ifap.ptr) != 0) return@memScoped false

    var cursor = ifap.value
    var vpn = false
    while (cursor != null && !vpn) {
        val iface = cursor.pointed
        val name = iface.ifa_name?.toKString() ?: ""
        val hasIpv4 = iface.ifa_addr != null &&
            iface.ifa_addr.pointed.sa_family.toInt() == AF_INET
        if (hasIpv4 && (
                name.startsWith("utun") || name.startsWith("tun") ||
                name.startsWith("ppp") || name.startsWith("ipsec") ||
                name.startsWith("tap")
            )
        ) {
            vpn = true
        }
        cursor = iface.ifa_next
    }
    freeifaddrs(ifap.value)
    vpn
}
