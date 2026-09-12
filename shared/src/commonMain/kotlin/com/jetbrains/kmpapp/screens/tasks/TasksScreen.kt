package com.jetbrains.kmpapp.screens.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.AssessmentType
import com.jetbrains.kmpapp.data.model.DefaultSubjectColors
import com.jetbrains.kmpapp.data.model.StudyTask
import com.jetbrains.kmpapp.data.model.Subject
import com.jetbrains.kmpapp.data.model.SubjectImportance
import com.jetbrains.kmpapp.data.model.TaskCategory
import com.jetbrains.kmpapp.data.model.TaskPriority
import com.jetbrains.kmpapp.data.model.TaskStatus
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.components.swipeToDismissBack
import kotlinx.coroutines.launch

// internal: используется и в SubjectDetailScreen.kt, и в TaskEditSheets.kt
internal fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF00E5FF)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0x00000000FF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        defaultColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()

    var showCreateSubjectSheet by remember { mutableStateOf(false) }
    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var selectedSubjectForTask by remember { mutableStateOf<String?>(null) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var taskToEdit by remember { mutableStateOf<StudyTask?>(null) }
    var taskToDelete by remember { mutableStateOf<StudyTask?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    AnimatedContent(
        targetState = selectedSubjectId,
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
    ) { activeSubjectId ->
        if (activeSubjectId == null) {
            TasksMainContent(
                subjects = subjects,
                tasks = tasks,
                activeCount = activeCount,
                completedCount = completedCount,
                overallProgress = overallProgress,
                onSelectSubject = { selectedSubjectId = it },
                onCreateSubject = { showCreateSubjectSheet = true }
            )
        } else {
            val currentSubject = subjects.find { it.id == activeSubjectId }
            if (currentSubject != null) {
                val subjectTasks = tasks.filter { it.subjectId == currentSubject.id }
                SubjectDetailScreen(
                    subject = currentSubject,
                    tasks = subjectTasks,
                    onBack = { selectedSubjectId = null },
                    onAddTask = {
                        selectedSubjectForTask = currentSubject.id
                        showCreateTaskSheet = true
                    },
                    onEditSubject = { subjectToEdit = currentSubject },
                    onDeleteSubject = { subjectToDelete = currentSubject },
                    onToggleTask = { task -> viewModel.toggleTaskCompletion(task.id) },
                    onToggleSubtask = { taskId, subtaskId -> viewModel.toggleSubtask(taskId, subtaskId) },
                    onEditTask = { task -> taskToEdit = task },
                    onDeleteTask = { task -> taskToDelete = task },
                    onStatusChange = { taskId, status -> viewModel.setTaskStatus(taskId, status) }
                )
            } else {
                LaunchedEffect(Unit) {
                    selectedSubjectId = null
                }
            }
        }
    }

    // Create / Edit Subject Sheet
    if (showCreateSubjectSheet || subjectToEdit != null) {
        val editing = subjectToEdit
        SubjectEditModalSheet(
            initialSubject = editing,
            onDismiss = {
                showCreateSubjectSheet = false
                subjectToEdit = null
            },
            onSave = { name, shortCode, colorHex, importance, assessmentType, teacher, room, notes ->
                if (editing != null) {
                    viewModel.updateSubject(
                        editing.copy(
                            name = name,
                            shortCode = shortCode,
                            colorHex = colorHex,
                            importance = importance,
                            assessmentType = assessmentType,
                            teacherName = teacher,
                            roomOrLink = room,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.createSubject(name, shortCode, colorHex, importance, assessmentType, teacher, room, notes)
                }
                showCreateSubjectSheet = false
                subjectToEdit = null
            }
        )
    }

    // Create / Edit Task Sheet
    if (showCreateTaskSheet || taskToEdit != null) {
        val editing = taskToEdit
        TaskEditModalSheet(
            subjects = subjects,
            defaultSubjectId = selectedSubjectForTask ?: subjects.firstOrNull()?.id ?: "",
            taskToEdit = editing,
            onDismiss = {
                showCreateTaskSheet = false
                taskToEdit = null
            },
            onSave = { subjectId, title, desc, cat, prio, subtasks, dueDate ->
                if (editing != null) {
                    viewModel.updateTask(
                        editing.copy(
                            subjectId = subjectId,
                            title = title,
                            taskDescription = desc,
                            category = cat,
                            priority = prio,
                            dueDateIso = dueDate
                        )
                    )
                } else {
                    viewModel.createTask(subjectId, title, desc, cat, prio, subtasks, dueDate)
                }
                showCreateTaskSheet = false
                taskToEdit = null
            }
        )
    }

    // Delete Task Confirmation Dialog
    if (taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Удалить задание?") },
            text = { Text("Вы уверены, что хотите удалить «${task.title}»?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        taskToDelete = null
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Subject Confirmation Dialog
    if (subjectToDelete != null) {
        val subject = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Удалить предмет «${subject.name}»?") },
            text = { Text("Все связанные с ним задачи и подзадачи будут также безвозвратно удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedSubjectId == subject.id) {
                            selectedSubjectId = null
                        }
                        viewModel.deleteSubject(subject.id)
                        subjectToDelete = null
                    }
                ) {
                    Text("Удалить предмет", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TasksMainContent(
    subjects: List<Subject>,
    tasks: List<StudyTask>,
    activeCount: Int,
    completedCount: Int,
    overallProgress: Int,
    onSelectSubject: (String) -> Unit,
    onCreateSubject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Задачи",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Активных: $activeCount • Завершено: $completedCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Overall Progress Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$overallProgress%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { overallProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar with "ПРЕДМЕТЫ И ПРАКТИКИ" and "+ Предмет"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПРЕДМЕТЫ И ПРАКТИКИ",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )

                TextButton(
                    onClick = onCreateSubject,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Предмет", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (subjects.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Предметов пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Создайте свой первый предмет, чтобы добавлять в него лабораторные работы, практики и чеклисты сдачи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onCreateSubject,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Создать предмет", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // List of Subject Cards
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        val subjectTasks = tasks.filter { it.subjectId == subject.id }
                        SubjectCompactCard(
                            subject = subject,
                            tasks = subjectTasks,
                            onClick = { onSelectSubject(subject.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectCompactCard(
    subject: Subject,
    tasks: List<StudyTask>,
    onClick: () -> Unit
) {
    val subjectColor = parseHexColor(subject.colorHex)
    val completedCount = tasks.count { it.status.isFinished }
    val progress = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size.toFloat()
    val progressPercent = (progress * 100).toInt()

    val importanceColor = when (subject.importance) {
        SubjectImportance.CRITICAL -> Color(0xFFEF4444)
        SubjectImportance.HIGH -> Color(0xFFF97316)
        SubjectImportance.MEDIUM -> Color(0xFFEAB308)
        SubjectImportance.LOW -> Color(0xFF22C55E)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: Short Code + Full Name + Importance + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Short Code Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(subjectColor.copy(alpha = 0.18f))
                        .border(0.8.dp, subjectColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subject.shortCode,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = subjectColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = subject.assessmentType.displayName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Importance Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(importanceColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(importanceColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = subject.importance.displayName,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = importanceColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Thin Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(subjectColor)
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Задач: $completedCount из ${tasks.size}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Завершено $progressPercent%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progressPercent == 100) Color(0xFF10B981) else subjectColor
                )
            }
        }
    }
}
