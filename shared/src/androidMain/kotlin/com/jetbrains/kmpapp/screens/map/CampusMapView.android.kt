package com.jetbrains.kmpapp.screens.map

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun CampusMapView(
    htmlContent: String,
    modifier: Modifier,
    controller: CampusMapController?,
    onRoomClick: ((String) -> Unit)?
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef?.destroy()
                    webViewRef = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.onPause()
            webViewRef?.pauseTimers()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                webViewClient = WebViewClient()

                controller?.let { ctrl ->
                    ctrl.onZoomIn = { evaluateJavascript("window.zoomIn && window.zoomIn();", null) }
                    ctrl.onZoomOut = { evaluateJavascript("window.zoomOut && window.zoomOut();", null) }
                    ctrl.onResetView = { evaluateJavascript("window.resetView && window.resetView();", null) }
                }
                webViewRef = this
            }
        },
        update = { webView ->
            controller?.let { ctrl ->
                ctrl.onZoomIn = { webView.evaluateJavascript("window.zoomIn && window.zoomIn();", null) }
                ctrl.onZoomOut = { webView.evaluateJavascript("window.zoomOut && window.zoomOut();", null) }
                ctrl.onResetView = { webView.evaluateJavascript("window.resetView && window.resetView();", null) }
            }
            if (webView.tag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL("https://local.map", htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}
