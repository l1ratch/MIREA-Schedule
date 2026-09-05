package com.jetbrains.kmpapp.screens.map

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun CampusMapView(
    htmlContent: String,
    modifier: Modifier,
    controller: CampusMapController?,
    onRoomClick: ((String) -> Unit)?
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                setBackgroundColor(0x00000000)
                webViewClient = WebViewClient()

                controller?.let { ctrl ->
                    ctrl.onZoomIn = { evaluateJavascript("window.zoomIn && window.zoomIn();", null) }
                    ctrl.onZoomOut = { evaluateJavascript("window.zoomOut && window.zoomOut();", null) }
                    ctrl.onResetView = { evaluateJavascript("window.resetView && window.resetView();", null) }
                }
            }
        },
        update = { webView ->
            controller?.let { ctrl ->
                ctrl.onZoomIn = { webView.evaluateJavascript("window.zoomIn && window.zoomIn();", null) }
                ctrl.onZoomOut = { webView.evaluateJavascript("window.zoomOut && window.zoomOut();", null) }
                ctrl.onResetView = { webView.evaluateJavascript("window.resetView && window.resetView();", null) }
            }
            webView.loadDataWithBaseURL("https://local.map", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}
