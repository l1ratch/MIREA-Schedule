package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.ScheduleSlot
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.math.abs

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
            val slide = tween<IntOffset>(280, easing = FastOutSlowInEasing)
            val fade = tween<Float>(280, easing = FastOutSlowInEasing)
            if (targetState != null) {
                (slideInHorizontally(slide) { width -> width } + fadeIn(fade)).togetherWith(
                    slideOutHorizontally(slide) { width -> -width } + fadeOut(fade)
                )
            } else {
                (slideInHorizontally(slide) { width -> -width } + fadeIn(fade)).togetherWith(
                    slideOutHorizontally(slide) { width -> width } + fadeOut(fade)
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
    // Значение НЕ читаем здесь: тик раз в 30 секунд не должен пересобирать
    // всё дерево расписания. State уходит вниз и читается только в карточках
    // «сегодня» (см. LessonCard).
    val currentMinutesState = viewModel.currentMinutes.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDiffSheet by remember { mutableStateOf(false) }
    val diffSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()
    val isToday = selectedDate == com.jetbrains.kmpapp.data.model.DateUtils.today()
    val basePage = 1000
    val today = com.jetbrains.kmpapp.data.model.DateUtils.today()
    val selectedPage = basePage + today.daysUntil(selectedDate)
    val pagerState = rememberPagerState(initialPage = selectedPage, pageCount = { 2001 })

    LaunchedEffect(selectedDate) {
        val targetPage = basePage + today.daysUntil(selectedDate)
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            // Дальние дни — мгновенно (иначе долгий «полёт» через страницы),
            // соседний день — обычная анимация.
            if (abs(targetPage - pagerState.currentPage) > 1) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState) {
        // currentPage, а не settledPage: дата обновляется в момент переворота
        // страницы (пока палец ещё на экране), лента календаря реагирует сразу.
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val date = today.plus(DatePeriod(days = page - basePage))
            if (date != selectedDate) viewModel.selectDate(date)
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
                        val pageDate = today.plus(DatePeriod(days = page - basePage))
                        val pageSlots = viewModel.slotsForDate(pageDate, currentLessons, showEmptyLessons)
                        key(pageDate) {
                            DaySchedulePage(
                                date = pageDate,
                                slots = pageSlots,
                                listState = rememberLazyListState(),
                                errorMessage = errorMessage,
                                currentMinutesState = currentMinutesState,
                                showLessonProgress = showLessonProgress,
                                showAbbreviatedNames = showAbbreviatedNames,
                                scheduleTargetType = selectedTarget?.type ?: com.jetbrains.kmpapp.data.model.ScheduleTargetType.GROUP,
                                autoScrollToCurrentLesson = autoScrollToCurrentLesson,
                                canAutoScroll = { viewModel.canAutoScroll(it, selectedTarget?.id) },
                                markAutoScrolled = { viewModel.markAutoScrolled(it, selectedTarget?.id) },
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
