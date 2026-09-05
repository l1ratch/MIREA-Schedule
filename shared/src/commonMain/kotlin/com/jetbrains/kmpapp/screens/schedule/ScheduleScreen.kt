package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.ScheduleSlot
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val selectedLessonForDetail by viewModel.selectedLessonForDetail.collectAsState()

    AnimatedContent(
        targetState = selectedLessonForDetail,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { detailLesson ->
        if (detailLesson != null) {
            LessonDetailScreen(
                lesson = detailLesson,
                onBack = { viewModel.selectLessonForDetail(null) }
            )
        } else {
            ScheduleMainContent(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleMainContent(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val savedTargets by viewModel.savedTargets.collectAsState()
    val selectedTarget by viewModel.selectedTarget.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val daySlots by viewModel.daySlots.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeDiff by viewModel.activeDiff.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDiffSheet by remember { mutableStateOf(false) }
    val diffSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    var totalDrag by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            Column {
                ScheduleTopBar(
                    selectedTarget = selectedTarget,
                    savedTargets = savedTargets,
                    isLoading = isLoading,
                    activeDiff = activeDiff,
                    onSelectTarget = { viewModel.selectTarget(it) },
                    onDiffClick = { showDiffSheet = true },
                    onAddClick = { showAddSheet = true }
                )

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTarget == null) {
                // No schedule selected yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Расписание не выбрано",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Добавьте группу, преподавателя или аудиторию, чтобы просматривать расписание занятий",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddSheet = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Добавить расписание", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Week calendar strip with navigation bar
                WeekCalendarStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )

                // Swipable schedule content area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedDate) {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag < -90f) {
                                        viewModel.nextDay()
                                    } else if (totalDrag > 90f) {
                                        viewModel.previousDay()
                                    }
                                    totalDrag = 0f
                                },
                                onDragCancel = { totalDrag = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AnimatedContent(
                        targetState = selectedDate,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { _ ->
                        if (daySlots.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 90.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🎉",
                                        fontSize = 48.sp
                                    )
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
                                            text = errorMessage ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        IconButton(onClick = { viewModel.refresh() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Повторить")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
                            ) {
                                daySlots.forEachIndexed { index, slot ->
                                    if (index > 0) {
                                        val prevSlot = daySlots[index - 1]
                                        val breakMin = com.jetbrains.kmpapp.data.model.calculateBreakMinutes(prevSlot.endTime, slot.startTime)
                                        if (breakMin > 0) {
                                            item(key = "break_${prevSlot.bellNumber}_${slot.bellNumber}") {
                                                LessonBreakIndicator(breakMinutes = breakMin)
                                            }
                                        }
                                    }
                                    val slotKey = when (slot) {
                                        is ScheduleSlot.Active -> "active_${slot.bellNumber}_${slot.lessons.firstOrNull()?.id}"
                                        is ScheduleSlot.Empty -> "empty_${slot.bellNumber}"
                                    }
                                    item(key = slotKey) {
                                        ScheduleSlotCard(
                                            slot = slot,
                                            onLessonClick = { lesson ->
                                                viewModel.selectLessonForDetail(lesson)
                                            }
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }
    }

    if (showAddSheet) {
        AddScheduleBottomSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showAddSheet = false
                }
            },
            onSearch = { viewModel.search(it) },
            onSelectTarget = { target ->
                viewModel.addAndSelectTarget(target)
            }
        )
    }

    if (showDiffSheet && activeDiff != null) {
        ScheduleDiffBottomSheet(
            diff = activeDiff!!,
            sheetState = diffSheetState,
            onDismiss = {
                scope.launch { diffSheetState.hide() }.invokeOnCompletion {
                    showDiffSheet = false
                }
            },
            onAccept = {
                viewModel.dismissDiff()
                scope.launch { diffSheetState.hide() }.invokeOnCompletion {
                    showDiffSheet = false
                }
            }
        )
    }
}
