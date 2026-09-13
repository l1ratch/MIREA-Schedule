package com.jetbrains.kmpapp.screens.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Свайп «назад» от левого края: экран едет за пальцем (как перелистывание
 * дней в расписании), после отпускания либо улетает вправо и вызывает
 * onBack, либо плавно возвращается на место. Один модификатор — все
 * подстраницы приложения.
 */
fun Modifier.swipeToDismissBack(
    enabled: Boolean = true,
    edgeWidthPx: Float = 200f,
    thresholdPx: Float = 120f,
    requireEdge: Boolean = true,
    onBack: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    var dragPx by remember { mutableFloatStateOf(0f) }
    var startedAtEdge by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    pointerInput(enabled) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startedAtEdge = !requireEdge || (offset.x <= edgeWidthPx)
                dragPx = 0f
            },
            onHorizontalDrag = { _, dragAmount ->
                if (startedAtEdge) {
                    dragPx = (dragPx + dragAmount).coerceAtLeast(0f)
                }
            },
            onDragEnd = {
                if (startedAtEdge && dragPx >= thresholdPx) {
                    // Улетает за экран, и только потом переключаем состояние:
                    // уходящий экран остаётся за краем, AnimatedContent-выход не виден.
                    scope.launch {
                        animate(dragPx, size.width.toFloat(), animationSpec = tween(160)) { v, _ -> dragPx = v }
                        onBack()
                    }
                } else {
                    scope.launch {
                        animate(dragPx, 0f, animationSpec = tween(200)) { v, _ -> dragPx = v }
                    }
                }
                startedAtEdge = false
            },
            onDragCancel = {
                scope.launch {
                    animate(dragPx, 0f, animationSpec = tween(200)) { v, _ -> dragPx = v }
                }
                startedAtEdge = false
            }
        )
    }.graphicsLayer {
        translationX = dragPx.coerceIn(0f, size.width.toFloat())
    }
}
