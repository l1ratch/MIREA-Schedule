package com.jetbrains.kmpapp.screens.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.swipeToDismissBack(
    enabled: Boolean = true,
    edgeWidthPx: Float = 200f,
    thresholdPx: Float = 120f,
    requireEdge: Boolean = true,
    onBack: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var totalDrag by remember { mutableFloatStateOf(0f) }
    var startedAtEdge by remember { mutableStateOf(false) }

    pointerInput(enabled) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startedAtEdge = !requireEdge || (offset.x <= edgeWidthPx)
                totalDrag = 0f
            },
            onDragEnd = {
                if (startedAtEdge && totalDrag > thresholdPx) {
                    onBack()
                }
                startedAtEdge = false
                totalDrag = 0f
            },
            onDragCancel = {
                startedAtEdge = false
                totalDrag = 0f
            },
            onHorizontalDrag = { _, dragAmount ->
                if (startedAtEdge) {
                    totalDrag += dragAmount
                }
            }
        )
    }
}
