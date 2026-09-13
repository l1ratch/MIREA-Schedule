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
 * дней в расписании), а при пересечении порога назад срабатывает сразу,
 * в моменте — страница-родитель въезжает под пальцем, а не «в конце».
 * До отпускания пальца текущий экран продолжает ехать за ним и улетает.
 * Один модификатор — все подстраницы приложения.
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
    var committed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    pointerInput(enabled) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startedAtEdge = !requireEdge || (offset.x <= edgeWidthPx)
                dragPx = 0f
                committed = false
            },
            onHorizontalDrag = { _, dragAmount ->
                if (startedAtEdge && !committed) {
                    dragPx = (dragPx + dragAmount).coerceAtLeast(0f)
                    // Коммит в моменте: AnimatedContent-переход стартует, пока
                    // палец ещё на экране, — родитель виден сразу, «мёртвой зоны» нет.
                    if (dragPx >= thresholdPx) {
                        committed = true
                        onBack()
                    }
                }
            },
            onDragEnd = {
                if (!committed) {
                    scope.launch {
                        animate(dragPx, 0f, animationSpec = tween(200)) { v, _ -> dragPx = v }
                    }
                }
                startedAtEdge = false
            },
            onDragCancel = {
                if (!committed) {
                    scope.launch {
                        animate(dragPx, 0f, animationSpec = tween(200)) { v, _ -> dragPx = v }
                    }
                }
                startedAtEdge = false
            }
        )
    }.graphicsLayer {
        translationX = dragPx.coerceIn(0f, size.width.toFloat())
    }
}
