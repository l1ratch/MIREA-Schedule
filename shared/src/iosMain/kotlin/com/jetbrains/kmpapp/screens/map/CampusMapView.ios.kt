package com.jetbrains.kmpapp.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIColor
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
    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                opaque = false
                backgroundColor = UIColor.clearColor
                scrollView.backgroundColor = UIColor.clearColor

                controller?.let { ctrl ->
                    ctrl.onZoomIn = { evaluateJavaScript("window.zoomIn && window.zoomIn();", null) }
                    ctrl.onZoomOut = { evaluateJavaScript("window.zoomOut && window.zoomOut();", null) }
                    ctrl.onResetView = { evaluateJavaScript("window.resetView && window.resetView();", null) }
                }
            }
        },
        update = { webView ->
            controller?.let { ctrl ->
                ctrl.onZoomIn = { webView.evaluateJavaScript("window.zoomIn && window.zoomIn();", null) }
                ctrl.onZoomOut = { webView.evaluateJavaScript("window.zoomOut && window.zoomOut();", null) }
                ctrl.onResetView = { webView.evaluateJavaScript("window.resetView && window.resetView();", null) }
            }
            webView.loadHTMLString(htmlContent, baseURL = null)
        },
        modifier = modifier
    )
}
