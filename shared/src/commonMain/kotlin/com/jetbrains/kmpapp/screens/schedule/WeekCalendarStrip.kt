package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.DateUtils
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color

private const val BASE_PAGE = 1000

@Composable
fun WeekCalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    lessonSummaries: Map<LocalDate, DayLessonSummary> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val today = DateUtils.today()
    val baseMonday = DateUtils.getWeekDates(today).first()

    val selectedMonday = DateUtils.getWeekDates(selectedDate).first()
    val weeksOffset = (baseMonday.daysUntil(selectedMonday) / 7)
    val targetPage = BASE_PAGE + weeksOffset

    val pagerState = rememberPagerState(
        initialPage = targetPage,
        pageCount = { 2000 }
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedMonday) {
        val currentMonday = baseMonday.plus(DatePeriod(days = (pagerState.currentPage - BASE_PAGE) * 7))
        if (currentMonday != selectedMonday && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Active displayed week Monday
    val currentWeekMonday = baseMonday.plus(DatePeriod(days = (pagerState.currentPage - BASE_PAGE) * 7))
    val currentWeekEnd = currentWeekMonday.plus(DatePeriod(days = 7))
    val todayInCurrentWeek = today >= currentWeekMonday && today < currentWeekEnd
    val weekInfo = DateUtils.getWeekInfo(currentWeekMonday)
    val monthTitle = DateUtils.formatMonthTitle(currentWeekMonday.month)
    val year = currentWeekMonday.year

    Column(modifier = modifier.fillMaxWidth()) {
        // Month, Year, Week bar with navigation arrows & Сегодня button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущая неделя",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$monthTitle $year • ${weekInfo.weekNumber} неделя",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!todayInCurrentWeek) {
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(BASE_PAGE)
                            }
                            onDateSelected(today)
                        }
                    ) {
                        Text(
                            text = "Сегодня",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage < 1999) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующая неделя",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Week Days Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageMonday = baseMonday.plus(DatePeriod(days = (page - BASE_PAGE) * 7))
            val weekDates = (0..6).map { pageMonday.plus(DatePeriod(days = it)) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == today

                    // Selected has full priority. If selected today, uses primary.
                    // If today but not selected, uses secondary/tertiary container highlight.
                    val targetBgColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    }

                    val targetTextColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    val bgColor by animateColorAsState(targetBgColor)
                    val textColor by animateColorAsState(targetTextColor)

                    val summary = lessonSummaries[date]

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .then(
                                if (isToday && !isSelected) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else Modifier
                            )
                            .clickable { onDateSelected(date) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = DateUtils.formatDayOfWeekShort(date.dayOfWeek),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) textColor.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = date.day.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(3.dp))

                        // Colored indicator dots for lecture, practice, lab
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(5.dp)
                        ) {
                            if (summary != null && (summary.hasLecture || summary.hasPractice || summary.hasLab)) {
                                if (summary.hasLecture) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color(0xFF3B82F6))
                                    )
                                }
                                if (summary.hasPractice) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFFF59E0B))
                                    )
                                }
                                if (summary.hasLab) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF10B981))
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(4.5.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
