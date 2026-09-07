package com.jetbrains.kmpapp.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.LessonType
import com.jetbrains.kmpapp.data.model.ScheduleSlot

@Composable
fun ScheduleSlotCard(
    slot: ScheduleSlot,
    onLessonClick: (Lesson) -> Unit,
    isToday: Boolean = false,
    currentMinutes: Int = com.jetbrains.kmpapp.data.model.DateUtils.currentTimeMinutes(),
    showLessonProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    when (slot) {
        is ScheduleSlot.Active -> {
            if (slot.lessons.size == 1) {
                LessonCard(
                    lesson = slot.lessons.first(),
                    onClick = { onLessonClick(slot.lessons.first()) },
                    isToday = isToday,
                    currentMinutes = currentMinutes,
                    showLessonProgress = showLessonProgress,
                    modifier = modifier
                )
            } else {
                MultiLessonCard(
                    bellNumber = slot.bellNumber,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    lessons = slot.lessons,
                    onLessonClick = onLessonClick,
                    isToday = isToday,
                    currentMinutes = currentMinutes,
                    showLessonProgress = showLessonProgress,
                    modifier = modifier
                )
            }
        }
        is ScheduleSlot.Empty -> {
            EmptyLessonCard(
                bellNumber = slot.bellNumber,
                startTime = slot.startTime,
                endTime = slot.endTime,
                modifier = modifier
            )
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    onClick: () -> Unit,
    isToday: Boolean = false,
    currentMinutes: Int = com.jetbrains.kmpapp.data.model.DateUtils.currentTimeMinutes(),
    showLessonProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    val (typeBg, typeTextColor) = getTypeBadgeColors(lesson.lessonType)

    val progress = if (isToday && showLessonProgress) {
        com.jetbrains.kmpapp.data.model.DateUtils.getLessonProgress(lesson.startTime, lesson.endTime, currentMinutes)
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Pair number, time, type badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${lesson.bellNumber} пара",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${lesson.startTime} — ${lesson.endTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(typeBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = lesson.lessonType.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subject name
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Details: Teacher and Classroom
                if (lesson.teachers.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Преподаватель",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lesson.teachers.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (lesson.classrooms.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Аудитория",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lesson.classrooms.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (progress != null) {
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = progress,
                    animationSpec = androidx.compose.animation.core.tween(500)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = if (animatedProgress >= 0.98f) 20.dp else 0.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun MultiLessonCard(
    bellNumber: Int,
    startTime: String,
    endTime: String,
    lessons: List<Lesson>,
    onLessonClick: (Lesson) -> Unit,
    isToday: Boolean = false,
    currentMinutes: Int = com.jetbrains.kmpapp.data.model.DateUtils.currentTimeMinutes(),
    showLessonProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { lessons.size })

    val progress = if (isToday && showLessonProgress) {
        com.jetbrains.kmpapp.data.model.DateUtils.getLessonProgress(startTime, endTime, currentMinutes)
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // Common slot header with indicator for multiple lessons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$bellNumber пара",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "$startTime — $endTime",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val (headerTypeBg, headerTypeText) = getTypeBadgeColors(lessons[pagerState.currentPage].lessonType)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(headerTypeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lessons[pagerState.currentPage].lessonType.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerTypeText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Swipable pager for the subgroup lessons
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(end = 40.dp),
                    pageSpacing = 12.dp,
                    beyondViewportPageCount = 1
                ) { page ->
                    val lesson = lessons[page]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLessonClick(lesson) }
                    ) {
                        Text(
                            text = lesson.subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (lesson.teachers.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Преподаватель",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lesson.teachers.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (lesson.classrooms.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Аудитория",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lesson.classrooms.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (lesson.groups.isNotEmpty()) {
                            Text(
                                text = lesson.groups.joinToString(", "),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            if (progress != null) {
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = progress,
                    animationSpec = androidx.compose.animation.core.tween(500)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = if (animatedProgress >= 0.98f) 20.dp else 0.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLessonCard(
    bellNumber: Int,
    startTime: String,
    endTime: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$bellNumber пара • $startTime — $endTime",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
            Text(
                text = "Нет пары",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LessonBreakIndicator(
    breakMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (breakMinutes <= 0) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "••• перемена $breakMinutes мин •••",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 0.2.sp
        )
    }
}


@Composable
internal fun getTypeBadgeColors(lessonType: LessonType): Pair<Color, Color> {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (lessonType) {
        LessonType.LECTURE ->
            if (isDark) Color(0xFF0C4A6E) to Color(0xFFBAE6FD) else Color(0xFFBAE6FD) to Color(0xFF0369A1)
        LessonType.PRACTICE ->
            if (isDark) Color(0xFF14532D) to Color(0xFFBBF7D0) else Color(0xFFBBF7D0) to Color(0xFF15803D)
        LessonType.LAB ->
            if (isDark) Color(0xFF7C2D12) to Color(0xFFFED7AA) else Color(0xFFFED7AA) to Color(0xFFC2410C)
        LessonType.OTHER ->
            if (isDark) Color(0xFF581C87) to Color(0xFFE9D5FF) else Color(0xFFE9D5FF) to Color(0xFF7E22CE)
    }
}
