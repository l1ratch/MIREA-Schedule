package com.jetbrains.kmpapp.data.update

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual fun startPlatformUpdate(browserUrl: String, apkUrl: String?) {
    UIApplication.sharedApplication.openURL(NSURL(string = browserUrl))
}
