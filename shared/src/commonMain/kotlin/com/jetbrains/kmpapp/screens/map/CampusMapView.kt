package com.jetbrains.kmpapp.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class CampusMapController(
    var onZoomIn: (() -> Unit)? = null,
    var onZoomOut: (() -> Unit)? = null,
    var onResetView: (() -> Unit)? = null,
    var onToggleLayer: ((section: String, show: Boolean) -> Unit)? = null
) {
    fun zoomIn() = onZoomIn?.invoke()
    fun zoomOut() = onZoomOut?.invoke()
    fun resetView() = onResetView?.invoke()
    fun toggleLayer(section: String, show: Boolean) = onToggleLayer?.invoke(section, show)
}

@Composable
expect fun CampusMapView(
    htmlContent: String,
    modifier: Modifier = Modifier,
    controller: CampusMapController? = null,
    onRoomClick: ((String) -> Unit)? = null
)
