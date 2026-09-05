package com.jetbrains.kmpapp.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.FloatingActionButton
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
import kotlinx.coroutines.launch

private fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF00E5FF)): Color {
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
                            text = "Активных: $activeCount • Зачтено: $completedCount",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (subjects.isEmpty()) {
                        showCreateSubjectSheet = true
                    } else {
                        selectedSubjectForTask = subjects.first().id
                        showCreateTaskSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
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
                    onClick = { showCreateSubjectSheet = true },
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
                            onClick = { showCreateSubjectSheet = true },
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        val subjectTasks = tasks.filter { it.subjectId == subject.id }
                        SubjectCardItem(
                            subject = subject,
                            tasks = subjectTasks,
                            onAddTask = {
                                selectedSubjectForTask = subject.id
                                showCreateTaskSheet = true
                            },
                            onEditSubject = { subjectToEdit = subject },
                            onDeleteSubject = { subjectToDelete = subject },
                            onToggleTask = { task -> viewModel.toggleTaskCompletion(task.id) },
                            onToggleSubtask = { taskId, subtaskId -> viewModel.toggleSubtask(taskId, subtaskId) },
                            onEditTask = { task -> taskToEdit = task },
                            onDeleteTask = { task -> taskToDelete = task },
                            onStatusChange = { taskId, status -> viewModel.setTaskStatus(taskId, status) }
                        )
                    }
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
private fun SubjectCardItem(
    subject: Subject,
    tasks: List<StudyTask>,
    onAddTask: () -> Unit,
    onEditSubject: () -> Unit,
    onDeleteSubject: () -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onToggleSubtask: (String, String) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onDeleteTask: (StudyTask) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit
) {
    val subjectColor = parseHexColor(subject.colorHex)
    val completedCount = tasks.count { it.status.isFinished }
    val progress = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size.toFloat()

    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Short Code + Name + Importance + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Short Code Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(subjectColor.copy(alpha = 0.18f))
                        .border(0.8.dp, subjectColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subject.shortCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = subjectColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Subject Name & Assessment Type
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
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Importance Indicator
                val importanceColor = when (subject.importance) {
                    SubjectImportance.CRITICAL -> Color(0xFFEF4444)
                    SubjectImportance.HIGH -> Color(0xFFF97316)
                    SubjectImportance.MEDIUM -> Color(0xFFEAB308)
                    SubjectImportance.LOW -> Color(0xFF22C55E)
                }
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

                // Menu button
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Опции", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditSubject()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить предмет", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteSubject()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thin Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = subjectColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Сдано: $completedCount из ${tasks.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subject.teacherName.isNotBlank()) {
                    Text(
                        text = subject.teacherName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tasks List
            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(8.dp))

                tasks.forEach { task ->
                    TaskRowItem(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onToggleSubtask = { subId -> onToggleSubtask(task.id, subId) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onStatusChange = { status -> onStatusChange(task.id, status) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Add Task Button
            TextButton(
                onClick = onAddTask,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить задачу", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TaskRowItem(
    task: StudyTask,
    onToggle: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isCompleted = task.status.isFinished

    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF10B981)
        TaskStatus.IN_PROGRESS -> Color(0xFF3B82F6)
        TaskStatus.SUBMITTED -> Color(0xFFF59E0B)
        TaskStatus.NEEDS_FIX -> Color(0xFFEF4444)
        TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkmark toggle
                IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Выполнить",
                        tint = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Title + Category
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.category.displayName,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (task.subtasks.isNotEmpty()) {
                            val doneCount = task.subtasks.count { it.isCompleted }
                            Text(
                                text = "• Чеклист $doneCount/${task.subtasks.size}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Status chip / toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.clickable {
                        val next = when (task.status) {
                            TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
                            TaskStatus.IN_PROGRESS -> TaskStatus.SUBMITTED
                            TaskStatus.SUBMITTED -> TaskStatus.COMPLETED
                            TaskStatus.COMPLETED -> TaskStatus.PENDING
                            TaskStatus.NEEDS_FIX -> TaskStatus.IN_PROGRESS
                        }
                        onStatusChange(next)
                    }
                ) {
                    Text(
                        text = task.status.displayName,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                if (task.subtasks.isNotEmpty() || task.taskDescription.isNotBlank()) {
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Раскрыть",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded content: Description and Subtasks
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp, start = 34.dp)) {
                    if (task.taskDescription.isNotBlank()) {
                        Text(
                            text = task.taskDescription,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    task.subtasks.forEach { subtask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(subtask.id) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { onToggleSubtask(subtask.id) },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subtask.title,
                                fontSize = 12.sp,
                                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEdit) {
                            Text("Изменить", fontSize = 11.5.sp)
                        }
                        TextButton(onClick = onDelete) {
                            Text("Удалить", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectEditModalSheet(
    initialSubject: Subject?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        shortCode: String,
        colorHex: String,
        importance: SubjectImportance,
        assessmentType: AssessmentType,
        teacher: String,
        room: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var shortCode by remember { mutableStateOf(initialSubject?.shortCode ?: "") }
    var selectedColor by remember { mutableStateOf(initialSubject?.colorHex ?: DefaultSubjectColors.first()) }
    var importance by remember { mutableStateOf(initialSubject?.importance ?: SubjectImportance.MEDIUM) }
    var assessmentType by remember { mutableStateOf(initialSubject?.assessmentType ?: AssessmentType.EXAM) }
    var teacher by remember { mutableStateOf(initialSubject?.teacherName ?: "") }
    var room by remember { mutableStateOf(initialSubject?.roomOrLink ?: "") }

    ModalBottomSheet(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialSubject == null) "Новый предмет" else "Редактировать предмет",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    focusManager.clearFocus()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (shortCode.isBlank() || shortCode.length <= 4) {
                        val words = it.trim().split(" ").filter { w -> w.isNotBlank() }
                        shortCode = when {
                            words.size >= 2 -> words.map { w -> w.take(1) }.joinToString("").uppercase()
                            words.isNotEmpty() -> words.first().take(3).uppercase()
                            else -> ""
                        }
                    }
                },
                label = { Text("Название предмета *") },
                placeholder = { Text("Например: Математический анализ") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Short code
            OutlinedTextField(
                value = shortCode,
                onValueChange = { if (it.length <= 5) shortCode = it.uppercase() },
                label = { Text("Короткий код (2-4 буквы) *") },
                placeholder = { Text("МА") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Color Palette Selector
            Column {
                Text("Цвет предмета", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DefaultSubjectColors.forEach { hex ->
                        val color = parseHexColor(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Assessment Type
            Column {
                Text("Форма итогового контроля", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssessmentType.entries.forEach { at ->
                        FilterChip(
                            selected = assessmentType == at,
                            onClick = { assessmentType = at },
                            label = { Text(at.displayName, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Importance
            Column {
                Text("Важность предмета", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubjectImportance.entries.forEach { imp ->
                        FilterChip(
                            selected = importance == imp,
                            onClick = { importance = imp },
                            label = {
                                Text(
                                    text = imp.displayName,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Teacher
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text("Преподаватель (опционально)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Room / link
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Аудитория или ссылка на СДО") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        focusManager.clearFocus()
                        onSave(name, shortCode, selectedColor, importance, assessmentType, teacher, room, "")
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditModalSheet(
    subjects: List<Subject>,
    defaultSubjectId: String,
    taskToEdit: StudyTask?,
    onDismiss: () -> Unit,
    onSave: (
        subjectId: String,
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        subtasks: List<String>,
        dueDate: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var selectedSubjectId by remember {
        mutableStateOf(taskToEdit?.subjectId ?: defaultSubjectId.ifEmpty { subjects.firstOrNull()?.id ?: "" })
    }
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.taskDescription ?: "") }
    var category by remember { mutableStateOf(taskToEdit?.category ?: TaskCategory.LAB) }
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: TaskPriority.MEDIUM) }
    val subtasks = remember {
        mutableStateListOf<String>().apply {
            if (taskToEdit != null && taskToEdit.subtasks.isNotEmpty()) {
                addAll(taskToEdit.subtasks.map { it.title })
            } else {
                addAll(listOf("Изучить задание", "Выполнить работу", "Сдать преподавателю"))
            }
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (taskToEdit == null) "Новое задание" else "Редактировать задание",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    focusManager.clearFocus()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Subject Selector Chips
            Column {
                Text("Предмет", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.forEach { subj ->
                        val isSelected = subj.id == selectedSubjectId
                        val color = parseHexColor(subj.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubjectId = subj.id },
                            label = { Text(subj.name, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название задачи *") },
                placeholder = { Text("Например: Лабораторная №2") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание или требования") },
                placeholder = { Text("Вариант, файлы, дедлайн...") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            Column {
                Text("Категория", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Priority
            Column {
                Text("Приоритет", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = {
                                Text(
                                    text = prio.displayName,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Subtasks checklist builder
            Column {
                Text("Чеклист шагов сдачи", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                subtasks.forEachIndexed { index, subtaskTitle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(subtaskTitle, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { subtasks.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Добавить шаг (напр. Написать отчет)", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks.add(newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Добавить", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && selectedSubjectId.isNotBlank()) {
                        focusManager.clearFocus()
                        onSave(selectedSubjectId, title, description, category, priority, subtasks, null)
                    }
                },
                enabled = title.isNotBlank() && selectedSubjectId.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Сохранить задачу", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
