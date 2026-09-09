package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Surface
import com.jetbrains.kmpapp.data.model.RefreshStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.ScheduleSlot
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

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
    val currentLessons by viewModel.currentLessons.collectAsState()
    val showEmptyLessons by viewModel.showEmptyLessons.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeDiff by viewModel.activeDiff.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()
    val dayLessonSummaries by viewModel.dayLessonSummaries.collectAsState()
    val showLessonProgress by viewModel.showLessonProgress.collectAsState()
    val autoScrollToCurrentLesson by viewModel.autoScrollToCurrentLesson.collectAsState()
    val showAbbreviatedNames by viewModel.showAbbreviatedNames.collectAsState()
    val currentMinutes by viewModel.currentMinutes.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDiffSheet by remember { mutableStateOf(false) }
    val diffSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()
    val isToday = selectedDate == com.jetbrains.kmpapp.data.model.DateUtils.today()
    val basePage = 1000
    var pagerBaseDate by remember { mutableStateOf(selectedDate) }
    val pagerState = rememberPagerState(initialPage = basePage, pageCount = { 2001 })

    LaunchedEffect(selectedDate) {
        if (pagerState.currentPage == basePage) {
            pagerBaseDate = selectedDate
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != basePage) {
                val targetDate = pagerBaseDate.plus(DatePeriod(days = page - basePage))
                viewModel.selectDate(targetDate)
                pagerState.scrollToPage(basePage)
                pagerBaseDate = targetDate
            }
        }
    }

    // Magnetic auto-scroll to current ongoing lesson or break
    val daySlots = viewModel.slotsForDate(selectedDate)
    val listState = rememberLazyListState()
    LaunchedEffect(selectedDate, selectedTarget?.id, daySlots, autoScrollToCurrentLesson) {
        if (isToday && autoScrollToCurrentLesson && viewModel.canAutoScroll(selectedDate, selectedTarget?.id) && daySlots.isNotEmpty()) {
            val nowMin = com.jetbrains.kmpapp.data.model.DateUtils.currentTimeMinutes()

            // Build an indexed list representing the actual items displayed in LazyColumn
            data class SlotListItem(
                val index: Int,
                val isBreak: Boolean,
                val slot: ScheduleSlot?,
                val startMin: Int,
                val endMin: Int,
                val isNextSlotActive: Boolean
            )

            val items = mutableListOf<SlotListItem>()
            var currentIndex = 0

            for (i in daySlots.indices) {
                if (i > 0) {
                    val prevSlot = daySlots[i - 1]
                    val currSlot = daySlots[i]
                    val breakMin = com.jetbrains.kmpapp.data.model.calculateBreakMinutes(prevSlot.endTime, currSlot.startTime)
                    if (breakMin > 0) {
                        val breakStart = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(prevSlot.endTime) ?: 0
                        val breakEnd = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(currSlot.startTime) ?: 0
                        items.add(
                            SlotListItem(
                                index = currentIndex++,
                                isBreak = true,
                                slot = null,
                                startMin = breakStart,
                                endMin = breakEnd,
                                isNextSlotActive = currSlot is ScheduleSlot.Active
                            )
                        )
                    }
                }

                val slot = daySlots[i]
                val slotStart = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(slot.startTime) ?: 0
                val slotEnd = com.jetbrains.kmpapp.data.model.DateUtils.parseTimeToMinutes(slot.endTime) ?: 0
                items.add(
                    SlotListItem(
                        index = currentIndex++,
                        isBreak = false,
                        slot = slot,
                        startMin = slotStart,
                        endMin = slotEnd,
                        isNextSlotActive = false
                    )
                )
            }

            // 1. Ongoing active lesson (highest priority)
            val ongoingActive = items.firstOrNull {
                !it.isBreak && it.slot is ScheduleSlot.Active && nowMin in it.startMin until it.endMin
            }

            // 2. Ongoing break before an active lesson
            val ongoingBreak = items.firstOrNull {
                it.isBreak && it.isNextSlotActive && nowMin in it.startMin until it.endMin
            }

            // 3. Next upcoming active lesson today
            val upcomingActive = items.firstOrNull {
                !it.isBreak && it.slot is ScheduleSlot.Active && nowMin < it.startMin
            }

            // 4. Fallback if day has only empty slots
            val fallback = items.firstOrNull {
                !it.isBreak && (nowMin in it.startMin until it.endMin || nowMin < it.startMin)
            }

            val targetItem = ongoingActive
                ?: ongoingBreak
                ?: upcomingActive?.let { active ->
                    val prevItem = if (active.index > 0) items[active.index - 1] else null
                    if (prevItem != null && prevItem.isBreak && nowMin >= prevItem.startMin) prevItem else active
                }
                ?: fallback

            if (targetItem != null && targetItem.index > 0) {
                var scrolled = false

                // On cold start the list may need more than one frame to measure.
                repeat(4) {
                    if (!scrolled) {
                        val layoutReady = withTimeoutOrNull(800) {
                            snapshotFlow { listState.layoutInfo.totalItemsCount }
                                .filter { it > targetItem.index }
                                .first()
                            true
                        } == true

                        if (layoutReady) {
                            try {
                                kotlinx.coroutines.delay(100)
                                listState.animateScrollToItem(targetItem.index)
                                scrolled = true
                            } catch (_: Throwable) {}
                        } else {
                            kotlinx.coroutines.delay(100)
                        }
                    }
                }

                if (scrolled) {
                    viewModel.markAutoScrolled(selectedDate, selectedTarget?.id)
                }
            } else if (targetItem != null && targetItem.index == 0) {
                viewModel.markAutoScrolled(selectedDate, selectedTarget?.id)
            }
        }
    }


    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ScheduleTopBar(
                selectedTarget = selectedTarget,
                savedTargets = savedTargets,
                isLoading = isLoading,
                activeDiff = activeDiff,
                onSelectTarget = { viewModel.selectTarget(it) },
                onDiffClick = { showDiffSheet = true },
                onAddClick = { showAddSheet = true }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                    lessonSummaries = dayLessonSummaries,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageDate = selectedDate.plus(DatePeriod(days = page - basePage))
                        val pageSlots = viewModel.slotsForDate(pageDate, currentLessons, showEmptyLessons)
                        key(pageDate) {
                            DaySchedulePage(
                                date = pageDate,
                                slots = pageSlots,
                                listState = rememberLazyListState(),
                                errorMessage = errorMessage,
                                currentMinutes = currentMinutes,
                                showLessonProgress = showLessonProgress,
                                showAbbreviatedNames = showAbbreviatedNames,
                                scheduleTargetType = selectedTarget?.type ?: com.jetbrains.kmpapp.data.model.ScheduleTargetType.GROUP,
                                onRetry = { viewModel.refresh() },
                                onLessonClick = { viewModel.selectLessonForDetail(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Floating update badge overlay (appears OVER the calendar and cards without shifting anything)
        androidx.compose.animation.AnimatedVisibility(
            visible = refreshStatus != null,
            enter = slideInVertically { -it } + androidx.compose.animation.fadeIn(),
            exit = slideOutVertically { -it } + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
                val status = refreshStatus
                if (status != null) {
                    val isSuccess = status is RefreshStatus.Success
                    val bgColor = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    val textColor = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    val text = when (status) {
                        is RefreshStatus.Success -> status.message
                        is RefreshStatus.Error -> "Ошибка (${status.code.code}): ${status.code.shortTitle}"
                    }
                    val icon = if (isSuccess) Icons.Default.Check else Icons.Default.Warning

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = bgColor,
                        shadowElevation = 8.dp,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { viewModel.dismissStatusBadge() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = text,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
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
