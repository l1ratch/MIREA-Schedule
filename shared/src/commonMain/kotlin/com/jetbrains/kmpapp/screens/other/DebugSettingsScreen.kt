package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.AppErrorCode
import com.jetbrains.kmpapp.data.model.AppVersion
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.components.swipeToDismissBack

@Composable
fun DebugSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)

    val storageStats by viewModel.storageStats.collectAsState()

    var simulateOffline by remember { mutableStateOf(false) }
    var simulateSlowNetwork by remember { mutableStateOf(false) }
    var detailedLogging by remember { mutableStateOf(false) }
    var lastTriggeredMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
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
                Column {
                    Text(
                        text = "Отладка и эксперименты",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Секретное меню разработчика",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
            // Section 1: Experimental Toggles
            DebugSectionCard(
                title = "Экспериментальные параметры",
                icon = Icons.Default.Science
            ) {
                DebugSwitchRow(
                    title = "Имитировать оффлайн",
                    subtitle = "Тестирование работы с сохраненным кэшем без сети",
                    checked = simulateOffline,
                    onCheckedChange = {
                        simulateOffline = it
                        lastTriggeredMessage = if (it) "Оффлайн-режим активирован" else "Оффлайн-режим отключен"
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                DebugSwitchRow(
                    title = "Искусственная задержка (2 сек)",
                    subtitle = "Эмуляция медленного 3G для проверки лоадеров",
                    checked = simulateSlowNetwork,
                    onCheckedChange = {
                        simulateSlowNetwork = it
                        lastTriggeredMessage = if (it) "Задержка 2 сек включена" else "Задержка отключена"
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                DebugSwitchRow(
                    title = "Расширенное логирование Ktor",
                    subtitle = "Вывод заголовков и тел сетевых запросов в консоль",
                    checked = detailedLogging,
                    onCheckedChange = {
                        detailedLogging = it
                        lastTriggeredMessage = if (it) "Логирование включено" else "Логирование отключено"
                    }
                )
            }

            // Section 2: Error Codes Sandbox
            DebugSectionCard(
                title = "Тестирование кодов ошибок",
                icon = Icons.Default.BugReport
            ) {
                Text(
                    text = "Быстрая проверка статус-кодов из ERROR_CODES.md:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val err = AppErrorCode.ERR_NO_NETWORK
                            lastTriggeredMessage = "${err.code}: ${err.description}"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("E-101 (Сеть)", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val err = AppErrorCode.ERR_SERVER_ERROR
                            lastTriggeredMessage = "${err.code}: ${err.description}"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("E-103 (5xx)", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val err = AppErrorCode.ERR_TIMEOUT
                            lastTriggeredMessage = "${err.code}: ${err.description}"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("E-102 (Таймаут)", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val err = AppErrorCode.ERR_PARSE_ERROR
                            lastTriggeredMessage = "${err.code}: ${err.description}"
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("E-105 (Парсинг)", fontSize = 12.sp)
                    }
                }

                if (lastTriggeredMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = lastTriggeredMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Section 3: Diagnostic Info
            DebugSectionCard(
                title = "Диагностика окружения",
                icon = Icons.Default.Info
            ) {
                DiagnosticItem("Версия приложения", AppVersion.DISPLAY_VERSION)
                DiagnosticItem("Репозиторий", AppVersion.GITHUB_REPO)
                DiagnosticItem("Фреймворк", "Compose Multiplatform 1.12")
                DiagnosticItem("Сетевой клиент", "Ktor 3.5.1")
                DiagnosticItem("Кэш расписаний", "${storageStats.schedulesCount} групп (${storageStats.formatBytes(storageStats.schedulesSizeBytes)})")
                DiagnosticItem("Всего занятий в базе", "${storageStats.lessonsCount}")
                DiagnosticItem("Размер хранилища", storageStats.formatBytes(storageStats.totalSizeBytes))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DebugSectionCard(
    title: String,
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
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun DebugSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
