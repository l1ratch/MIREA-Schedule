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
import com.jetbrains.kmpapp.theme.ThemeOverlay
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
    val themeOverlay by viewModel.themeOverlay.collectAsState()

    var showDisclaimer by remember { mutableStateOf(false) }
    var showReward by remember { mutableStateOf(false) }
    var showRefusal by remember { mutableStateOf(false) }
    var showDisappointment by remember { mutableStateOf(false) }
    var showNoSecondChance by remember { mutableStateOf(false) }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
        ExperimentalSettingsContent(
            modifier = Modifier.padding(innerPadding),
            showMatrix = cheatsAgreed == true && !cheatsBlocked,
            isMatrixTheme = themeOverlay == ThemeOverlay.MATRIX,
            onCheatsClick = { turnOn ->
                if (turnOn) {
                    when (cheatsAgreed) {
                        null -> showDisclaimer = true
                        true -> showDisappointment = true
                        false -> showNoSecondChance = true
                    }
                }
            },
            onMatrixChanged = viewModel::setMatrixTheme
        )
    }

    DisclaimerDialog(
        visible = showDisclaimer,
        onDismiss = { showDisclaimer = false },
        onAgree = {
            viewModel.setCheatsAgreed(true)
            showDisclaimer = false
            showReward = true
        },
        onRefuse = {
            viewModel.setCheatsAgreed(false)
            showDisclaimer = false
            showRefusal = true
        }
    )
    RewardDialog(visible = showReward, onDismiss = { showReward = false })
    RefusalDialog(visible = showRefusal, onDismiss = { showRefusal = false })
    DisappointmentDialog(
        visible = showDisappointment,
        onDismiss = { showDisappointment = false },
        onConfirm = {
            viewModel.setCheatsBlocked(true)
            viewModel.setMatrixTheme(false)
            showDisappointment = false
        }
    )
    NoSecondChanceDialog(
        visible = showNoSecondChance,
        onDismiss = { showNoSecondChance = false },
        onConfirm = {
            viewModel.setCheatsBlocked(true)
            showNoSecondChance = false
        }
    )
}

@Composable
private fun ExperimentalSettingsContent(
    showMatrix: Boolean,
    isMatrixTheme: Boolean,
    onCheatsClick: (Boolean) -> Unit,
    onMatrixChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
                CheatsRow(onCheckedChange = onCheatsClick)
                if (showMatrix) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    MatrixThemeRow(
                        checked = isMatrixTheme,
                        onCheckedChange = onMatrixChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun CheatsRow(onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Читы", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "активирует читы",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = false, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MatrixThemeRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Хочешь в матрицу?", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Перенесем в матрицу",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DisclaimerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAgree: () -> Unit,
    onRefuse: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Дисклеймер", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("так, кто у нас тут пытается включить читы? 😏", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("разработчики категорически против использования таких функций. Обещаешь ли ты больше никогда не использовать эту функцию?")
            }
        },
        confirmButton = { Button(onClick = onAgree) { Text("да, обещаю") } },
        dismissButton = { TextButton(onClick = onRefuse) { Text("нет, включить читы") } }
    )
}

@Composable
private fun RewardDialog(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Награда 🎁", fontWeight = FontWeight.Bold) },
        text = { Text("Хм... Хорошо, мы следим за тобой, держи авансом награду.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun RefusalDialog(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text("Очень жаль... Мы врятли сможем договориться.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun DisappointmentDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Мы разочарованы в тебе", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Мы думали, что у нас договор...")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Печально, больше не обращайся к нам по этому вопросу 😢")
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Готово") } }
    )
}

@Composable
private fun NoSecondChanceDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ты сделал свой выбор", fontWeight = FontWeight.Bold) },
        text = { Text("к сожалению, мы не можем дать тебе второй шанс 😔") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Готово") } }
    )
}
