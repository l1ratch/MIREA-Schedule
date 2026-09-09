package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleSlot
import com.jetbrains.kmpapp.data.model.ScheduleTargetType
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

@Composable
internal fun DaySchedulePage(
    date: LocalDate,
    slots: List<ScheduleSlot>,
    listState: LazyListState,
    errorMessage: String?,
    currentMinutes: Int,
    showLessonProgress: Boolean,
    showAbbreviatedNames: Boolean,
    scheduleTargetType: ScheduleTargetType,
    autoScrollToCurrentLesson: Boolean,
    canAutoScroll: (LocalDate) -> Boolean,
    markAutoScrolled: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(date, slots, autoScrollToCurrentLesson) {
        if (date != com.jetbrains.kmpapp.data.model.DateUtils.today() ||
            !autoScrollToCurrentLesson || !canAutoScroll(date) || slots.isEmpty()
        ) return@LaunchedEffect

        data class DisplayItem(
            val index: Int,
            val isBreak: Boolean,
            val slot: ScheduleSlot?,
            val start: Int,
            val end: Int,
            val nextIsActive: Boolean
        )

        val displayItems = mutableListOf<DisplayItem>()
        slots.forEachIndexed { index, slot ->
            if (index > 0) {
                val previous = slots[index - 1]
                val breakMinutes = com.jetbrains.kmpapp.data.model.calculateBreakMinutes(previous.endTime, slot.startTime)
                if (breakMinutes > 0) {
                    displayItems += DisplayItem(
                        index = displayItems.size,
                        isBreak = true,
                        slot = null,
                        start = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(previous.endTime) ?: 0,
                        end = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(slot.startTime) ?: 0,
                        nextIsActive = slot is ScheduleSlot.Active
                    )
                }
            }
            displayItems += DisplayItem(
                index = displayItems.size,
                isBreak = false,
                slot = slot,
                start = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(slot.startTime) ?: 0,
                end = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(slot.endTime) ?: 0,
                nextIsActive = false
            )
        }

        val now = com.jetbrains.kmpapp.data.model.DateUtils.currentTimeMinutes()
        val active = displayItems.firstOrNull {
            !it.isBreak && it.slot is ScheduleSlot.Active && now in it.start until it.end
        }
        val ongoingBreak = displayItems.firstOrNull {
            it.isBreak && it.nextIsActive && now in it.start until it.end
        }
        val upcoming = displayItems.firstOrNull {
            !it.isBreak && it.slot is ScheduleSlot.Active && now < it.start
        }
        val fallback = displayItems.firstOrNull {
            !it.isBreak && (now in it.start until it.end || now < it.start)
        }
        val target = active ?: ongoingBreak ?: upcoming?.let {
            val previous = if (it.index > 0) displayItems[it.index - 1] else null
            if (previous?.isBreak == true && now >= previous.start) previous else it
        } ?: fallback

        if (target == null || target.index == 0) {
            if (target != null) markAutoScrolled(date)
            return@LaunchedEffect
        }

        var scrolled = false
        repeat(4) {
            if (!scrolled) {
                val layoutReady = withTimeoutOrNull(800) {
                    snapshotFlow { listState.layoutInfo.totalItemsCount }
                        .filter { it > target.index }
                        .first()
                    true
                } == true
                if (layoutReady) {
                    kotlinx.coroutines.delay(100)
                    listState.animateScrollToItem(target.index)
                    scrolled = true
                } else {
                    kotlinx.coroutines.delay(100)
                }
            }
        }
        if (scrolled) markAutoScrolled(date)
    }

    if (slots.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("На этот день пар нет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Отличный повод отдохнуть!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
                    IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, contentDescription = "Повторить") }
                }
            }
        }
        return
    }

    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)) {
        slots.forEachIndexed { index, slot ->
            if (index > 0) {
                val previous = slots[index - 1]
                val breakMinutes = com.jetbrains.kmpapp.data.model.calculateBreakMinutes(previous.endTime, slot.startTime)
                if (breakMinutes > 0) {
                    item(key = "break_${previous.bellNumber}_${slot.bellNumber}") { LessonBreakIndicator(breakMinutes) }
                }
            }
            val slotKey = when (slot) {
                is ScheduleSlot.Active -> "active_${slot.bellNumber}_${slot.lessons.firstOrNull()?.id}"
                is ScheduleSlot.Empty -> "empty_${slot.bellNumber}"
            }
            item(key = slotKey) {
                ScheduleSlotCard(
                    slot = slot,
                    onLessonClick = onLessonClick,
                    isToday = date == com.jetbrains.kmpapp.data.model.DateUtils.today(),
                    currentMinutes = currentMinutes,
                    showLessonProgress = showLessonProgress,
                    showAbbreviatedNames = showAbbreviatedNames,
                    scheduleTargetType = scheduleTargetType
                )
            }
        }
    }
}
