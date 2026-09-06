package com.jetbrains.kmpapp.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CampusMapView(
    htmlContent: String,
    modifier: Modifier,
    controller: CampusMapController?,
    onRoomClick: ((String) -> Unit)?
) {
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }
    var loadedHtml by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef = null
        }
    }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                opaque = true
                scrollView.scrollEnabled = false
                scrollView.bounces = false

                controller?.let { ctrl ->
                    ctrl.onZoomIn = { evaluateJavaScript("window.zoomIn && window.zoomIn();", null) }
                    ctrl.onZoomOut = { evaluateJavaScript("window.zoomOut && window.zoomOut();", null) }
                    ctrl.onResetView = { evaluateJavaScript("window.resetView && window.resetView();", null) }
                }
                webViewRef = this
            }
        },
        update = { webView ->
            controller?.let { ctrl ->
                ctrl.onZoomIn = { webView.evaluateJavaScript("window.zoomIn && window.zoomIn();", null) }
                ctrl.onZoomOut = { webView.evaluateJavaScript("window.zoomOut && window.zoomOut();", null) }
                ctrl.onResetView = { webView.evaluateJavaScript("window.resetView && window.resetView();", null) }
            }
            // CRITICAL FIX: Only reload HTML when it actually changes, preventing 100% CPU loops on recompositions
            if (loadedHtml != htmlContent) {
                loadedHtml = htmlContent
                webView.loadHTMLString(htmlContent, baseURL = null)
            }
        },
        modifier = modifier
    )
}
