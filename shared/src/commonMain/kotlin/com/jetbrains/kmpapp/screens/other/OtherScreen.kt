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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.jetbrains.kmpapp.screens.components.AppTab

@Composable
fun OtherScreen(
    viewModel: OtherViewModel,
    tasksViewModel: com.jetbrains.kmpapp.screens.tasks.TasksViewModel = org.koin.compose.viewmodel.koinViewModel(),
    onNavigateToTab: (AppTab) -> Unit = {},
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
                    onNavigate = { viewModel.navigateToSubScreen(it) },
                    onNavigateToTab = onNavigateToTab
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
            OtherSubScreen.RESOURCES -> {
                ResourcesScreen(
                    onBack = { viewModel.resetToRoot() }
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
}

@Composable
private fun OtherMainContent(
    viewModel: OtherViewModel,
    onNavigate: (OtherSubScreen) -> Unit,
    onNavigateToTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedTargets by viewModel.savedTargets.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()
    val dockTabs by viewModel.dockTabs.collectAsState()
    val uriHandler = LocalUriHandler.current
    val hiddenTabs = remember(dockTabs) {
        AppTab.entries.filter { it != AppTab.OTHER && it !in dockTabs.take(5) }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Другое",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { onNavigate(OtherSubScreen.ABOUT) }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "О программе",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Adaptive Concentrator Block (only visible if any tabs are hidden from the dock)
            if (hiddenTabs.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Сервисы",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (tab in hiddenTabs) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.clickable { onNavigateToTab(tab) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.selectedIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 1. My schedules card
            OtherNavCard(
                title = "Мои расписания",
                subtitle = if (savedTargets.isEmpty()) "Нет сохранённых расписаний"
                else "Сохранено: ${savedTargets.size}",
                icon = Icons.AutoMirrored.Filled.EventNote,
                onClick = { onNavigate(OtherSubScreen.MANAGE_SCHEDULES) }
            )

            // 2. University resources card
            OtherNavCard(
                title = "Ресурсы университета",
                subtitle = "Личный кабинет, СДО, Пульс и сервисы",
                icon = Icons.Default.School,
                onClick = { onNavigate(OtherSubScreen.RESOURCES) }
            )

            // 3. Settings card
            OtherNavCard(
                title = "Настройки",
                subtitle = "Оформление, тема, навигация",
                icon = Icons.Default.Tune,
                onClick = { onNavigate(OtherSubScreen.SETTINGS) }
            )

            // 4. App Version / Auto-Update Card with 3-tier colors
            val urgency = updateResult?.urgency ?: com.jetbrains.kmpapp.data.update.UpdateUrgency.UP_TO_DATE
            val hasUpdate = updateResult?.hasUpdate == true

            val cardContainerColor = when (urgency) {
                com.jetbrains.kmpapp.data.update.UpdateUrgency.CRITICAL -> Color(0xFF581C87).copy(alpha = 0.20f)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.NEW_VERSION -> Color(0xFFDC2626).copy(alpha = 0.16f)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.MINOR_BUILD -> Color(0xFFD97706).copy(alpha = 0.16f)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.UP_TO_DATE -> MaterialTheme.colorScheme.surfaceContainer
            }

            val iconBoxColor = when (urgency) {
                com.jetbrains.kmpapp.data.update.UpdateUrgency.CRITICAL -> Color(0xFF581C87).copy(alpha = 0.40f)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.NEW_VERSION -> Color(0xFFDC2626).copy(alpha = 0.30f)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.MINOR_BUILD -> Color(0xFFD97706).copy(alpha = 0.30f)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.UP_TO_DATE -> MaterialTheme.colorScheme.surfaceContainerHigh
            }

            val accentTint = when (urgency) {
                com.jetbrains.kmpapp.data.update.UpdateUrgency.CRITICAL -> Color(0xFFC084FC)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.NEW_VERSION -> Color(0xFFEF4444)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.MINOR_BUILD -> Color(0xFFF59E0B)
                com.jetbrains.kmpapp.data.update.UpdateUrgency.UP_TO_DATE -> if (isCheckingUpdate) MaterialTheme.colorScheme.primary else Color(0xFF22C55E)
            }

            val titleText = when {
                isCheckingUpdate -> "Проверка обновлений..."
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.CRITICAL -> "Критическое обновление!"
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.NEW_VERSION -> "Вышла новая версия!"
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.MINOR_BUILD -> "Доступна новая сборка"
                else -> "У вас актуальная версия"
            }

            val subtitleText = when {
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.CRITICAL ->
                    "Версия ${updateResult?.latestVersion} (сборка ${updateResult?.latestBuild}) • Важные исправления безопасности"
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.NEW_VERSION ->
                    "Версия ${updateResult?.latestVersion} (сборка ${updateResult?.latestBuild}) • Нажмите для перехода"
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.MINOR_BUILD ->
                    "Сборка ${updateResult?.latestBuild} • Доступны микро-правки"
                else -> com.jetbrains.kmpapp.data.model.AppVersion.DISPLAY_VERSION
            }

            val statusIcon = when {
                isCheckingUpdate -> Icons.Default.Refresh
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.CRITICAL -> Icons.Default.SystemUpdate
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.NEW_VERSION -> Icons.Default.SystemUpdate
                urgency == com.jetbrains.kmpapp.data.update.UpdateUrgency.MINOR_BUILD -> Icons.Default.Refresh
                else -> Icons.Default.CheckCircle
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        if (hasUpdate) {
                            val url = updateResult?.downloadUrl 
                                ?: updateResult?.releaseUrl 
                                ?: com.jetbrains.kmpapp.data.model.AppVersion.GITHUB_REPO_URL
                            uriHandler.openUri(url)
                        } else {
                            viewModel.checkForUpdates()
                        }
                    }
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconBoxColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = accentTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasUpdate) accentTint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (hasUpdate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Перейти к релизу",
                            tint = accentTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 5. GitHub Issues Feedback Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        uriHandler.openUri(com.jetbrains.kmpapp.data.model.AppVersion.GITHUB_ISSUES_URL)
                    }
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Есть проблема или идея?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Создать Issue на GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
