package com.jetbrains.kmpapp.screens.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

private val TimeColumnWidth = 62.dp
private val TargetColumnWidth = 178.dp
private val CellHeight = 64.dp

@Composable
fun CompareScheduleScreen(
    viewModel: CompareScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val savedTargets by viewModel.savedTargets.collectAsState()
    val selectedTargetIds by viewModel.selectedTargetIds.collectAsState()
    val selectedTargets by viewModel.selectedTargets.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    val weekStart by viewModel.selectedWeekStart.collectAsState()
    val cachedLessons by viewModel.cachedLessons.collectAsState()

    val currentWeekStart = DateUtils.getWeekDates(DateUtils.today()).first()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Сравнение",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Два и более расписания рядом",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { viewModel.refreshAll() },
                    enabled = selectedTargets.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить выбранные расписания",
                        tint = if (selectedTargets.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            if (savedTargets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Нет расписаний для сравнения",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Добавьте две или более группы, преподавателей или аудитории во вкладке «Расписание», и они появятся здесь для выбора.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Column
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
            ) {
                savedTargets.forEach { target ->
                    val isSelected = target.id in selectedTargetIds
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleTarget(target.id) },
                        label = {
                            Text(
                                text = target.targetTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            if (selectedTargets.size < 2) {
                Text(
                    text = "Выберите два или более расписания для сравнения (выбрано ${selectedTargets.size}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
                return@Column
            }

            WeekNavBar(
                weekStart = weekStart,
                isCurrentWeek = weekStart == currentWeekStart,
                onPrevious = { viewModel.previousWeek() },
                onNext = { viewModel.nextWeek() },
                onToday = { viewModel.goToCurrentWeek() }
            )

            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = if (comparison.matchesCount > 0) {
                            "Занятий: ${comparison.matchesCount} · Различий: ${comparison.differencesCount}"
                        } else {
                            "На этой неделе занятий нет"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• — различие",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CompareGrid(
                targets = selectedTargets,
                comparison = comparison,
                cachedLessons = cachedLessons
            )
        }
    }
}

@Composable
private fun WeekNavBar(
    weekStart: kotlinx.datetime.LocalDate,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val weekEnd = weekStart.plus(DatePeriod(days = 6))
    val weekInfo = DateUtils.getWeekInfo(weekStart)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Предыдущая неделя"
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Неделя №${weekInfo.weekNumber} · ${DateUtils.formatDayOfWeekShort(weekStart.dayOfWeek)}, " +
                    "${weekStart.dayOfMonth} ${DateUtils.formatMonthRu(weekStart.month)} — " +
                    "${DateUtils.formatDayOfWeekShort(weekEnd.dayOfWeek)}, ${weekEnd.dayOfMonth} ${DateUtils.formatMonthRu(weekEnd.month)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (weekInfo.isEven) "чётная неделя" else "нечётная неделя",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Следующая неделя"
            )
        }
        if (!isCurrentWeek) {
            Button(
                onClick = onToday,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сегодня", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CompareGrid(
    targets: List<ScheduleTarget>,
    comparison: ScheduleComparison,
    cachedLessons: Map<Int, List<Lesson>>
) {
    val horizontalScrollState = rememberScrollState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "column_headers") {
            Row {
                TimeHeaderCell()
                Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    targets.forEach { target ->
                        TargetHeaderCell(
                            target = target,
                            hasData = !(cachedLessons[target.id] ?: emptyList()).isNullOrEmpty()
                        )
                    }
                }
            }
        }

        comparison.days.forEach { day ->
            item(key = "day_${day.date}") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${DateUtils.formatDayOfWeekShort(day.date.dayOfWeek)}, " +
                                "${day.date.dayOfMonth} ${DateUtils.formatMonthRu(day.date.month)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    day.rows.forEach { row ->
                        CompareRowView(
                            row = row,
                            scrollState = horizontalScrollState
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeHeaderCell() {
    Box(
        modifier = Modifier
            .width(TimeColumnWidth)
            .height(48.dp)
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Пара",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TargetHeaderCell(
    target: ScheduleTarget,
    hasData: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .width(TargetColumnWidth)
            .height(48.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = target.targetTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hasData) target.type.displayName else "нет данных",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompareRowView(
    row: CompareRow,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Row {
        Box(
            modifier = Modifier
                .width(TimeColumnWidth)
                .height(CellHeight)
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${row.bellNumber} пара",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = row.startTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            row.cells.forEach { cell ->
                CompareCellView(cell = cell)
            }
        }
    }
}

@Composable
private fun CompareCellView(cell: CompareCell) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (cell.isDifferent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .width(TargetColumnWidth)
            .height(CellHeight)
            .padding(end = 6.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            if (cell.isDifferent && cell.isEmpty) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
                DifferenceDot(modifier = Modifier.align(Alignment.TopEnd))
            } else if (cell.isEmpty) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth()
                ) {
                    cell.lessons.forEach { lesson ->
                        LessonMiniCard(lesson = lesson, isDifferent = cell.isDifferent)
                        if (lesson != cell.lessons.last()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                if (cell.isDifferent) {
                    DifferenceDot(modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
private fun DifferenceDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 6.dp)
            .size(9.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    )
}

@Composable
private fun LessonMiniCard(
    lesson: Lesson,
    isDifferent: Boolean
) {
    val contentColor: Color = if (isDifferent) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    val mutedColor: Color = if (isDifferent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = lesson.subject,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${lesson.lessonType.shortName} ${lesson.startTime}–${lesson.endTime}",
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor
        )
        if (lesson.classrooms.isNotEmpty() || lesson.teachers.isNotEmpty()) {
            val extras = buildList {
                if (lesson.classrooms.isNotEmpty()) add("ауд. ${lesson.classrooms.joinToString(", ")}")
                if (lesson.teachers.isNotEmpty()) add(lesson.teachers.joinToString(", "))
            }.joinToString(" · ")
            Text(
                text = extras,
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}