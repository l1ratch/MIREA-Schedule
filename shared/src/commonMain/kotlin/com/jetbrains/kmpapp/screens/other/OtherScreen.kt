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
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
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
                    onOpenTaskSettings = { viewModel.navigateToSubScreen(OtherSubScreen.TASK_SETTINGS) },
                    onOpenIconPicker = { viewModel.navigateToSubScreen(OtherSubScreen.ICON_PICKER) }
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
            OtherSubScreen.ICON_PICKER -> {
                IconPickerScreen(
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
                    onBack = { viewModel.navigateToSubScreen(OtherSubScreen.ABOUT) },
                    onOpenExperimentalSettings = { viewModel.navigateToSubScreen(OtherSubScreen.EXPERIMENTAL_SETTINGS) }
                )
            }
            OtherSubScreen.EXPERIMENTAL_SETTINGS -> {
                ExperimentalSettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateToSubScreen(OtherSubScreen.DEBUG_SETTINGS) }
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
                HiddenTabsCard(
                    hiddenTabs = hiddenTabs,
                    onNavigateToTab = onNavigateToTab
                )
            }
            // 1. University resources card
            OtherNavCard(
                title = "Ресурсы университета",
                subtitle = "Личный кабинет, СДО, Пульс и сервисы",
                icon = Icons.Default.School,
                onClick = { onNavigate(OtherSubScreen.RESOURCES) }
            )

            // 2. My schedules card
            OtherNavCard(
                title = "Мои расписания",
                subtitle = if (savedTargets.isEmpty()) "Нет сохранённых расписаний"
                else "Сохранено: ${savedTargets.size}",
                icon = Icons.AutoMirrored.Filled.EventNote,
                onClick = { onNavigate(OtherSubScreen.MANAGE_SCHEDULES) }
            )

            // 3. Settings card
            OtherNavCard(
                title = "Настройки",
                subtitle = "Оформление, тема, навигация",
                icon = Icons.Default.Tune,
                onClick = { onNavigate(OtherSubScreen.SETTINGS) }
            )

            // 4. App Version / Auto-Update Card with 3-tier colors
            UpdateStatusCard(
                updateResult = updateResult,
                isCheckingUpdate = isCheckingUpdate,
                onCheckForUpdates = { viewModel.checkForUpdates() }
            )

            // 5. GitHub Issues Feedback Card
            GitHubIssuesCard()
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
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
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
