package com.jetbrains.kmpapp.data.network

// Проверка выполняется в Swift-движке (VpnEngine в iosApp):
// C-биндинги CoreFoundation в Kotlin/Native не импортируют типы
// словарей (__CFDictionary), а в Swift разбор настроек прокси тривиален.
actual fun detectVpnActive(): Boolean = VpnStatus.isActive()
