package com.jetbrains.kmpapp.screens.map

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun CampusMapView(
    htmlContent: String,
    modifier: Modifier,
    controller: CampusMapController?,
    onRoomClick: ((String) -> Unit)?
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadedHtml by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // Safely release reference on disposal without calling destructive global pauseTimers()
            webViewRef?.stopLoading()
            webViewRef = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.allowFileAccess = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                // Disable overview mode to ensure 1:1 CSS pixel ratio matching iOS
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = false

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        println("[CampusMap WebView] onReceivedError: ${error?.description} code: ${error?.errorCode}")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        println("[CampusMap JS] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                        return true
                    }
                }

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
            if (loadedHtml != htmlContent) {
                loadedHtml = htmlContent
                // Use null baseURL to avoid cross-origin / SSL domain resolution restrictions in Chromium
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
        modifier = modifier
    )
}
