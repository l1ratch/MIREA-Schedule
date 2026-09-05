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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.DateUtils
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

    LaunchedEffect(selectedMonday) {
        val currentMonday = baseMonday.plus(DatePeriod(days = (pagerState.currentPage - BASE_PAGE) * 7))
        if (currentMonday != selectedMonday) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val pageMonday = baseMonday.plus(DatePeriod(days = (page - BASE_PAGE) * 7))
            val currentSelectedMonday = DateUtils.getWeekDates(selectedDate).first()
            if (pageMonday != currentSelectedMonday) {
                val dayOffset = selectedDate.dayOfWeek.ordinal
                val newSelectedDate = pageMonday.plus(DatePeriod(days = dayOffset))
                onDateSelected(newSelectedDate)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth()
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

                val bgColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainer
                )
                val textColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )

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
                                    color = MaterialTheme.colorScheme.primary,
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
                        color = if (isSelected) textColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
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
