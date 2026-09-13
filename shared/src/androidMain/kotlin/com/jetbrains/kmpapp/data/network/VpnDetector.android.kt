package com.jetbrains.kmpapp.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.jetbrains.kmpapp.data.storage.AndroidContextProvider

actual fun detectVpnActive(): Boolean {
    val context = AndroidContextProvider.context ?: return false
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
}
