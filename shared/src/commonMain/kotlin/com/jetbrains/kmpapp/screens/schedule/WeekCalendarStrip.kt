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

private const val BASE_PAGE = 1000

@Composable
fun WeekCalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
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

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val pageMonday = baseMonday.plus(DatePeriod(days = (page - BASE_PAGE) * 7))
            val currentSelectedMonday = DateUtils.getWeekDates(selectedDate).first()
            if (pageMonday != currentSelectedMonday) {
                val newSelectedDate = if (pageMonday == baseMonday) {
                    today
                } else {
                    val dayOffset = selectedDate.dayOfWeek.ordinal
                    pageMonday.plus(DatePeriod(days = dayOffset))
                }
                onDateSelected(newSelectedDate)
            }
        }
    }


    // Active displayed week Monday
    val currentWeekMonday = baseMonday.plus(DatePeriod(days = (pagerState.currentPage - BASE_PAGE) * 7))
    val weekInfo = DateUtils.getWeekInfo(currentWeekMonday)
    val monthTitle = DateUtils.formatMonthTitle(currentWeekMonday.month)
    val year = currentWeekMonday.year

    Column(modifier = modifier.fillMaxWidth()) {
        // Month, Year, Week bar with navigation arrows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
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

            Text(
                text = "$monthTitle $year • ${weekInfo.weekNumber} неделя",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
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
                            .clickable { onDateSelected(date) },
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = date.day.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
