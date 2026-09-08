package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.components.swipeToDismissBack

@Composable
fun ExperimentalSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)

    val cheatsAgreed by viewModel.cheatsAgreed.collectAsState()
    val cheatsBlocked by viewModel.cheatsBlocked.collectAsState()
    val isMatrixTheme by viewModel.isMatrixTheme.collectAsState()

    var showDisclaimer by remember { mutableStateOf(false) }
    var showReward by remember { mutableStateOf(false) }
    var showRefusal by remember { mutableStateOf(false) }
    var showDisappointment by remember { mutableStateOf(false) }
    var showNoSecondChance by remember { mutableStateOf(false) }

    val cheatsChecked = cheatsAgreed != null && !cheatsBlocked
    val matrixSwitchVisible = cheatsAgreed == true && !cheatsBlocked

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
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
                    text = "Экспериментальные параметры",
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
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Читы",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "активирует читы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = cheatsChecked,
                            enabled = !cheatsBlocked,
                            onCheckedChange = { turnOn ->
                                if (cheatsAgreed == null) {
                                    if (turnOn) showDisclaimer = true
                                } else {
                                    if (cheatsAgreed == true) {
                                        showDisappointment = true
                                    } else {
                                        showNoSecondChance = true
                                    }
                                }
                            }
                        )
                    }

                    if (matrixSwitchVisible) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Хочешь в матрицу?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Перенесем в матрицу",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isMatrixTheme,
                                onCheckedChange = { viewModel.setMatrixTheme(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            title = { Text("Дисклеймер", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "так, кто у нас тут пытается включить читы? 😏",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "разработчики категорически против использования таких функций. Обещаешь ли ты больше никогда не использовать эту функцию?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCheatsAgreed(true)
                        showDisclaimer = false
                        showReward = true
                    }
                ) {
                    Text("да, обещаю")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setCheatsAgreed(false)
                        showDisclaimer = false
                        showRefusal = true
                    }
                ) {
                    Text("нет, включить читы")
                }
            }
        )
    }

    if (showReward) {
        AlertDialog(
            onDismissRequest = { showReward = false },
            title = { Text("Награда 🎁", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Хм... Хорошо, мы следим за тобой, держи авансом награду.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showReward = false }) {
                    Text("Готово")
                }
            }
        )
    }

    if (showRefusal) {
        AlertDialog(
            onDismissRequest = { showRefusal = false },
            text = {
                Text(
                    text = "Очень жаль... Мы врятли сможем договориться.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showRefusal = false }) {
                    Text("Готово")
                }
            }
        )
    }

    if (showDisappointment) {
        AlertDialog(
            onDismissRequest = { showDisappointment = false },
            title = { Text("Мы разочарованы в тебе", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Мы думали, что у нас договор...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Печально, больше не обращайся к нам по этому вопросу 😢",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCheatsBlocked(true)
                        viewModel.setMatrixTheme(false)
                        showDisappointment = false
                    }
                ) {
                    Text("Готово")
                }
            }
        )
    }

    if (showNoSecondChance) {
        AlertDialog(
            onDismissRequest = { showNoSecondChance = false },
            title = { Text("Ты сделал свой выбор", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "к сожалению, мы не можем дать тебе второй шанс 😔",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCheatsBlocked(true)
                        showNoSecondChance = false
                    }
                ) {
                    Text("Готово")
                }
            }
        )
    }
}