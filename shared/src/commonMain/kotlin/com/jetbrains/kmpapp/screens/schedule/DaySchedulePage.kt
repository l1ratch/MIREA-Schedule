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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleSlot
import com.jetbrains.kmpapp.data.model.ScheduleTargetType

@Composable
internal fun DaySchedulePage(
    date: kotlinx.datetime.LocalDate,
    slots: List<ScheduleSlot>,
    listState: LazyListState,
    errorMessage: String?,
    currentMinutes: Int,
    showLessonProgress: Boolean,
    showAbbreviatedNames: Boolean,
    scheduleTargetType: ScheduleTargetType,
    onRetry: () -> Unit,
    onLessonClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slots.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "На этот день пар нет",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Отличный повод отдохнуть!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Повторить")
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
    ) {
        slots.forEachIndexed { index, slot ->
            if (index > 0) {
                val previous = slots[index - 1]
                val breakMinutes = com.jetbrains.kmpapp.data.model.calculateBreakMinutes(previous.endTime, slot.startTime)
                if (breakMinutes > 0) {
                    item(key = "break_${previous.bellNumber}_${slot.bellNumber}") {
                        LessonBreakIndicator(breakMinutes = breakMinutes)
                    }
                }
            }

            val key = when (slot) {
                is ScheduleSlot.Active -> "active_${slot.bellNumber}_${slot.lessons.firstOrNull()?.id}"
                is ScheduleSlot.Empty -> "empty_${slot.bellNumber}"
            }
            item(key = key) {
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
