package com.jetbrains.kmpapp.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.StudyTask
import com.jetbrains.kmpapp.data.model.Subtask
import com.jetbrains.kmpapp.data.model.TaskCategory
import com.jetbrains.kmpapp.data.model.TaskPriority
import com.jetbrains.kmpapp.data.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()

    var showCreateSheet by remember { mutableStateOf(false) }

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

                    // Progress Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$overallProgress%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
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
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subject Filter Chips
            if (availableSubjects.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSubject == null,
                        onClick = { viewModel.selectSubjectFilter(null) },
                        label = { Text("Все предметы", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    availableSubjects.forEach { subj ->
                        val isSelected = selectedSubject.equals(subj, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSubjectFilter(subj) },
                            label = { Text(subj, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Tasks List
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedSubject != null) "Нет задач по предмету" else "Задач пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Нажмите «+», чтобы добавить задачу или практическую работу",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                            onStatusChange = { newStatus -> viewModel.setTaskStatus(task.id, newStatus) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateTaskBottomSheet(
            availableSubjects = availableSubjects,
            onDismiss = { showCreateSheet = false },
            onCreateTask = { subj, title, desc, cat, prio, subs ->
                viewModel.createTask(subj, title, desc, cat, prio, subs)
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: StudyTask,
    onToggleSubtask: (String) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> Color(0xFFEF4444)
        TaskPriority.HIGH -> Color(0xFFF97316)
        TaskPriority.MEDIUM -> Color(0xFF0EA5E9)
        TaskPriority.LOW -> Color(0xFF10B981)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Subject & Category + Priority Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = task.category.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.subjectTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.priority.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            if (task.taskDescription.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.taskDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Subtasks list
            if (task.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))

                task.subtasks.forEach { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleSubtask(subtask.id) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { onToggleSubtask(subtask.id) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtask.title,
                            fontSize = 13.sp,
                            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status toggle button
                val statusText = task.status.displayName
                val statusBg = when (task.status) {
                    TaskStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.15f)
                    TaskStatus.IN_PROGRESS -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                    TaskStatus.SUBMITTED -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    TaskStatus.NEEDS_FIX -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    TaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val statusColor = when (task.status) {
                    TaskStatus.COMPLETED -> Color(0xFF10B981)
                    TaskStatus.IN_PROGRESS -> Color(0xFF3B82F6)
                    TaskStatus.SUBMITTED -> Color(0xFFF59E0B)
                    TaskStatus.NEEDS_FIX -> Color(0xFFEF4444)
                    TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    modifier = Modifier.clickable {
                        val nextStatus = when (task.status) {
                            TaskStatus.PENDING -> TaskStatus.IN_PROGRESS
                            TaskStatus.IN_PROGRESS -> TaskStatus.SUBMITTED
                            TaskStatus.SUBMITTED -> TaskStatus.COMPLETED
                            TaskStatus.COMPLETED -> TaskStatus.PENDING
                            TaskStatus.NEEDS_FIX -> TaskStatus.IN_PROGRESS
                        }
                        onStatusChange(nextStatus)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Статус: $statusText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }

                // Quick complete button
                IconButton(
                    onClick = {
                        val newStatus = if (isCompleted) TaskStatus.PENDING else TaskStatus.COMPLETED
                        onStatusChange(newStatus)
                    }
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Отметить",
                        tint = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskBottomSheet(
    availableSubjects: List<String>,
    onDismiss: () -> Unit,
    onCreateTask: (
        subject: String,
        title: String,
        desc: String,
        category: TaskCategory,
        priority: TaskPriority,
        subtasks: List<String>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var subject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.LAB) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    val subtasks = remember { mutableStateListOf("Изучить материал", "Выполнить задание", "Сдать преподавателю") }
    var newSubtaskText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Новая задача",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Subject field / chips
            Column {
                Text("Дисциплина / Предмет", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("Название предмета") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (availableSubjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSubjects.take(6).forEach { subj ->
                            FilterChip(
                                selected = subject.equals(subj, ignoreCase = true),
                                onClick = { subject = subj },
                                label = { Text(subj, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Title field
            Column {
                Text("Название задачи *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Например: Лабораторная работа №1") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Description field
            Column {
                Text("Описание или комментарий", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Подробности задания, ссылки или требования") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category selector
            Column {
                Text("Категория", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
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

            // Priority selector
            Column {
                Text("Приоритет", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
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
                Text("Чеклист подзадач", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                        placeholder = { Text("Добавить шаг чеклиста", fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(10.dp))

            // Create button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateTask(subject.ifBlank { "Общие задачи" }, title, description, category, priority, subtasks)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Создать задачу", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
