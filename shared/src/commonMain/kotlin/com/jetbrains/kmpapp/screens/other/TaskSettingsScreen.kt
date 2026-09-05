package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.TaskPriority
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.components.swipeToDismissBack
import com.jetbrains.kmpapp.screens.tasks.TasksViewModel
import kotlinx.coroutines.launch

@Composable
fun TaskSettingsScreen(
    tasksViewModel: TasksViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)

    val availableSubjects by tasksViewModel.availableSubjects.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSubject by remember { mutableStateOf(availableSubjects.firstOrNull() ?: "") }
    var labCount by remember { mutableIntStateOf(8) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Настройки задач",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .swipeToDismissBack(requireEdge = true, onBack = onBack)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Batch Generator
            TaskSettingsCard(
                title = "Генератор лабораторных работ",
                subtitle = "Автоматическое создание серии лабораторных по предмету с готовыми этапами выполнения",
                icon = Icons.Default.Science
            ) {
                Text("Дисциплина", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedSubject,
                    onValueChange = { selectedSubject = it },
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
                        availableSubjects.forEach { subj ->
                            FilterChip(
                                selected = selectedSubject.equals(subj, ignoreCase = true),
                                onClick = { selectedSubject = subj },
                                label = { Text(subj, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Количество работ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(4, 6, 8, 10, 12).forEach { count ->
                        FilterChip(
                            selected = labCount == count,
                            onClick = { labCount = count },
                            label = {
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Приоритет по умолчанию", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val subj = selectedSubject.trim().ifBlank { "Лабораторные работы" }
                        tasksViewModel.createBatchLabs(subj, labCount, selectedPriority)
                        scope.launch {
                            snackbarHostState.showSnackbar("Создано $labCount лабораторных по «$subj»")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать $labCount лабораторных работ", fontWeight = FontWeight.Bold)
                }
            }

            // Section 2: Data Management
            TaskSettingsCard(
                title = "Управление данными",
                subtitle = "Очистка базы задач и сброс прогресса",
                icon = Icons.Default.DeleteSweep
            ) {
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Очистить все задачи",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить все задачи?") },
            text = { Text("Все сохраненные задачи, подзадачи и чеклисты будут безвозвратно удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tasksViewModel.clearAllTasks()
                        showClearDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Все задачи удалены")
                        }
                    }
                ) {
                    Text("Удалить всё", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun TaskSettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
