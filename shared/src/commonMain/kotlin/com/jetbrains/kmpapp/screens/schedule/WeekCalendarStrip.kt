package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.DateUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
fun WeekCalendarStrip(
    selectedDate: LocalDate,
    datesWithLessons: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDates = DateUtils.getWeekDates(selectedDate)
    val today = DateUtils.today()
    val weekInfo = DateUtils.getWeekInfo(selectedDate)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Week header info + prev/next week buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${selectedDate.day} ${DateUtils.formatMonthRu(selectedDate.month)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${weekInfo.weekNumber}-я неделя • ${if (weekInfo.isEven) "Чётная" else "Нечётная"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onDateSelected(selectedDate.minus(DatePeriod(days = 7))) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Предыдущая неделя"
                    )
                }

                if (selectedDate != today) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onDateSelected(today) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Сегодня",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                IconButton(
                    onClick = { onDateSelected(selectedDate.plus(DatePeriod(days = 7))) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующая неделя"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 7 days row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDates.forEach { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val hasLessons = date in datesWithLessons

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
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .then(
                            if (isToday && !isSelected) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            } else Modifier
                        )
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DateUtils.formatDayOfWeekShort(date.dayOfWeek),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) textColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date.day.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasLessons) {
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}
