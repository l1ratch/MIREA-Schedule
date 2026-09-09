package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun ExperimentalSettingsScreen(viewModel: OtherViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlatformBackHandler(onBack = onBack)
    val agreed by viewModel.cheatsAgreed.collectAsState()
    val blocked by viewModel.cheatsBlocked.collectAsState()
    val matrix by viewModel.isMatrixTheme.collectAsState()
    var disclaimer by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                Spacer(Modifier.width(4.dp))
                Text("Экспериментальные параметры", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        modifier = modifier.fillMaxSize().swipeToDismissBack(requireEdge = true, onBack = onBack)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Читы", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("активирует читы", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = agreed == true && !blocked,
                            enabled = !blocked,
                            onCheckedChange = { if (agreed == null) disclaimer = true else result = "Договор уже зафиксирован." }
                        )
                    }
                    if (agreed == true && !blocked) {
                        Text("Хочешь в матрицу?", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                        Switch(checked = matrix, onCheckedChange = viewModel::setMatrixTheme)
                    }
                }
            }
        }
    }

    if (disclaimer) AlertDialog(
        onDismissRequest = { disclaimer = false },
        title = { Text("Дисклеймер") },
        text = { Text("Обещаешь не использовать читы? 😏") },
        confirmButton = { TextButton(onClick = { viewModel.setCheatsAgreed(true); disclaimer = false }) { Text("Да, обещаю") } },
        dismissButton = { TextButton(onClick = { viewModel.setCheatsAgreed(false); disclaimer = false }) { Text("Нет") } }
    )
    if (result != null) AlertDialog(onDismissRequest = { result = null }, text = { Text(result ?: "") }, confirmButton = { TextButton(onClick = { result = null }) { Text("Готово") } })
}
