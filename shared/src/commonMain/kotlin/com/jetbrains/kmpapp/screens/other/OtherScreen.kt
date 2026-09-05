package com.jetbrains.kmpapp.screens.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OtherScreen(
    viewModel: OtherViewModel,
    tasksViewModel: com.jetbrains.kmpapp.screens.tasks.TasksViewModel = org.koin.compose.viewmodel.koinViewModel(),
    modifier: Modifier = Modifier
) {
    val activeSubScreen by viewModel.activeSubScreen.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()
    val uriHandler = LocalUriHandler.current

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            if (targetState.depth >= initialState.depth) {
                // Moving forward: new screen enters from right
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                // Moving back: previous screen enters from left
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { subScreen ->
        when (subScreen) {
            OtherSubScreen.ROOT -> {
                OtherMainContent(
                    viewModel = viewModel,
                    onNavigate = { viewModel.navigateToSubScreen(it) }
                )
            }
            OtherSubScreen.MANAGE_SCHEDULES -> {
                ManageSchedulesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.resetToRoot() }
                )
            }
            OtherSubScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.resetToRoot() },
                    onOpenDataAndCache = { viewModel.navigateToSubScreen(OtherSubScreen.DATA_AND_CACHE) },
                    onOpenDockSettings = { viewModel.navigateToSubScreen(OtherSubScreen.DOCK_SETTINGS) },
                    onOpenTaskSettings = { viewModel.navigateToSubScreen(OtherSubScreen.TASK_SETTINGS) }
                )
            }
            OtherSubScreen.DATA_AND_CACHE -> {
                DataAndCacheScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToSubScreen(OtherSubScreen.SETTINGS) }
                )
            }
            OtherSubScreen.DOCK_SETTINGS -> {
                DockSettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToSubScreen(OtherSubScreen.SETTINGS) }
                )
            }
            OtherSubScreen.TASK_SETTINGS -> {
                TaskSettingsScreen(
                    tasksViewModel = tasksViewModel,
                    onBack = { viewModel.navigateToSubScreen(OtherSubScreen.SETTINGS) }
                )
            }
            OtherSubScreen.ABOUT -> {
                AboutScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.resetToRoot() },
                    onOpenDebugMenu = { viewModel.navigateToSubScreen(OtherSubScreen.DEBUG_SETTINGS) }
                )
            }
            OtherSubScreen.DEBUG_SETTINGS -> {
                DebugSettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToSubScreen(OtherSubScreen.ABOUT) }
                )
            }
        }
    }

    if (updateResult != null && updateResult!!.hasUpdate) {
        val update = updateResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = { Text("Доступно обновление ${update.latestVersion}") },
            text = {
                Column {
                    Text("Текущая версия: ${update.currentVersion}")
                    if (!update.changelog.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = update.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        uriHandler.openUri(update.downloadUrl)
                        viewModel.dismissUpdateDialog()
                    }
                ) {
                    Text("Скачать")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("Позже")
                }
            }
        )
    }
}

@Composable
private fun OtherMainContent(
    viewModel: OtherViewModel,
    onNavigate: (OtherSubScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedTargets by viewModel.savedTargets.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateStatusMessage by viewModel.updateStatusMessage.collectAsState()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Другое",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. My schedules card
            OtherNavCard(
                title = "Мои расписания",
                subtitle = if (savedTargets.isEmpty()) "Нет сохранённых расписаний"
                else "Сохранено: ${savedTargets.size}",
                icon = Icons.AutoMirrored.Filled.EventNote,
                onClick = { onNavigate(OtherSubScreen.MANAGE_SCHEDULES) }
            )

            // 2. Settings card
            OtherNavCard(
                title = "Настройки",
                subtitle = "Оформление, тема, навигация",
                icon = Icons.Default.Tune,
                onClick = { onNavigate(OtherSubScreen.SETTINGS) }
            )

            // 3. App Update Block (placed directly on main screen before "О программе")
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Обновление приложения",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Текущая: ${com.jetbrains.kmpapp.data.model.AppVersion.DISPLAY_VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isCheckingUpdate) "Проверка..." else "Проверить обновления",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (updateStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = updateStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 4. About card
            OtherNavCard(
                title = "О программе",
                subtitle = "Версия, разработчик, участники",
                icon = Icons.Default.Info,
                onClick = { onNavigate(OtherSubScreen.ABOUT) }
            )
        }
    }
}

@Composable
private fun OtherNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
