package com.jetbrains.kmpapp

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.screens.components.AppTab
import com.jetbrains.kmpapp.screens.components.FloatingDock
import com.jetbrains.kmpapp.screens.map.MapScreen
import com.jetbrains.kmpapp.screens.other.OtherScreen
import com.jetbrains.kmpapp.screens.other.OtherViewModel
import com.jetbrains.kmpapp.screens.rooms.FreeRoomsScreen
import com.jetbrains.kmpapp.screens.rooms.FreeRoomsViewModel
import com.jetbrains.kmpapp.screens.schedule.ScheduleScreen
import com.jetbrains.kmpapp.screens.schedule.ScheduleViewModel
import com.jetbrains.kmpapp.screens.tasks.TasksScreen
import com.jetbrains.kmpapp.screens.tasks.TasksViewModel
import com.jetbrains.kmpapp.theme.SakuraDarkColors
import com.jetbrains.kmpapp.theme.SakuraLightColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.update.UpdateUrgency
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E5BB0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF555F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF121C2B),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFFF0F3F9),
    surfaceContainerHigh = Color(0xFFE8EDF5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD9E3F8),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF282A30)
)

@Composable
fun App() {
    val repository: ScheduleRepository = koinInject()
    val themeMode by repository.themeMode.collectAsState()
    val isSakuraTheme by repository.isSakuraTheme.collectAsState()
    val dockTabs by repository.dockTabs.collectAsState()

    val scheduleViewModel: ScheduleViewModel = koinViewModel()
    val otherViewModel: OtherViewModel = koinViewModel()
    val freeRoomsViewModel: FreeRoomsViewModel = koinViewModel()
    val tasksViewModel: TasksViewModel = koinViewModel()

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colors = if (isSakuraTheme) {
        if (isDark) SakuraDarkColors else SakuraLightColors
    } else {
        if (isDark) DarkColors else LightColors
    }

    MaterialTheme(colorScheme = colors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var currentTab by remember { mutableStateOf(AppTab.SCHEDULE) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                try {
                    if (!repository.isLowPowerMode.value) {
                        otherViewModel.checkForUpdates()
                    }
                } catch (_: Throwable) {}
            }

            val updateResult by otherViewModel.updateResult.collectAsState()
            val uriHandler = LocalUriHandler.current
            var dismissedUpdateKey by rememberSaveable { mutableStateOf<String?>(null) }

            val activeUpdate = updateResult
            if (activeUpdate != null && activeUpdate.hasUpdate) {
                val updateKey = "${activeUpdate.latestVersion}_${activeUpdate.latestBuild}_${activeUpdate.urgency}"
                val isCritical = activeUpdate.urgency == UpdateUrgency.CRITICAL
                val isNewVersion = activeUpdate.urgency == UpdateUrgency.NEW_VERSION

                if ((isCritical || isNewVersion) && dismissedUpdateKey != updateKey) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!isCritical) {
                                dismissedUpdateKey = updateKey
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = if (isCritical) Color(0xFFC084FC) else MaterialTheme.colorScheme.primary
                            )
                        },
                        title = {
                            Text(
                                text = if (isCritical) "Критическое обновление!" else "Доступна новая версия",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            androidx.compose.foundation.layout.Column {
                                Text(
                                    text = if (isCritical) {
                                        "Обнаружено критическое обновление безопасности/стабильности (сборка ${activeUpdate.latestBuild}). Рекомендуется установить его сейчас."
                                    } else {
                                        "Вышла версия ${activeUpdate.latestVersion} (сборка ${activeUpdate.latestBuild})."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!activeUpdate.changelog.isNullOrBlank()) {
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = activeUpdate.changelog,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    uriHandler.openUri(activeUpdate.downloadUrl)
                                    dismissedUpdateKey = updateKey
                                },
                                colors = if (isCritical) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF881337),
                                        contentColor = Color.White
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text("Обновить сейчас")
                            }
                        },
                        dismissButton = {
                            if (!isCritical) {
                                TextButton(onClick = { dismissedUpdateKey = updateKey }) {
                                    Text("Позже")
                                }
                            } else {
                                TextButton(onClick = { dismissedUpdateKey = updateKey }) {
                                    Text("Игнорировать", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = currentTab) { tab ->
                    when (tab) {
                        AppTab.SCHEDULE -> {
                            ScheduleScreen(viewModel = scheduleViewModel)
                        }
                        AppTab.FREE_ROOMS -> {
                            FreeRoomsScreen(viewModel = freeRoomsViewModel)
                        }
                        AppTab.TASKS -> {
                            TasksScreen(viewModel = tasksViewModel)
                        }
                        AppTab.MAP -> {
                            MapScreen()
                        }
                        AppTab.OTHER -> {
                            OtherScreen(
                                viewModel = otherViewModel,
                                onNavigateToTab = { currentTab = it }
                            )
                        }
                    }
                }

                FloatingDock(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onTabReselected = { tab ->
                        when (tab) {
                            AppTab.SCHEDULE -> {
                                scheduleViewModel.selectLessonForDetail(null)
                            }
                            AppTab.FREE_ROOMS -> {
                                freeRoomsViewModel.selectRoomForDetail(null)
                            }
                            AppTab.TASKS -> {}
                            AppTab.MAP -> {}
                            AppTab.OTHER -> {
                                otherViewModel.resetToRoot()
                            }
                        }
                    },
                    tabs = dockTabs,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
