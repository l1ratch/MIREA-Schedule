package com.jetbrains.kmpapp.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class CampusMapController(
    var onZoomIn: (() -> Unit)? = null,
    var onZoomOut: (() -> Unit)? = null,
    var onResetView: (() -> Unit)? = null
) {
    fun zoomIn() = onZoomIn?.invoke()
    fun zoomOut() = onZoomOut?.invoke()
    fun resetView() = onResetView?.invoke()
}

@Composable
expect fun CampusMapView(
    htmlContent: String,
    modifier: Modifier = Modifier,
    controller: CampusMapController? = null,
    onRoomClick: ((String) -> Unit)? = null
)
