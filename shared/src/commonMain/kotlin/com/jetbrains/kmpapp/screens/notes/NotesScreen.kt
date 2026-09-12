package com.jetbrains.kmpapp.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.NotePage
import com.jetbrains.kmpapp.data.model.NoteSection
import kotlinx.coroutines.delay

private val LIGHT_PALETTE = listOf(
    0xFF1E5BB0, // синий
    0xFFB3261E, // красный
    0xFFB25000, // оранжевый
    0xFF2E7D32, // зелёный
    0xFF00695C, // бирюзовый
    0xFF6D4FA1, // фиолетовый
    0xFFB0348A, // розовый
    0xFF5B6572  // серый
)

private val DARK_PALETTE = listOf(
    0xFFA8C7FA, // синий
    0xFFFFB4A9, // красный
    0xFFFFBF9E, // оранжевый
    0xFF8CE99A, // зелёный
    0xFF7FE5D0, // бирюзовый
    0xFFCDB9F0, // фиолетовый
    0xFFF0A7D4, // розовый
    0xFFB7C1CC  // серый
)

@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val pages by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val askBeforeNoteDelete by viewModel.askBeforeNoteDelete.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .imePadding()
            .padding(bottom = 88.dp)
    ) {
        PageHeader(
            currentPage = currentPage,
            pages = pages,
            askBeforeNoteDelete = askBeforeNoteDelete,
            onRename = viewModel::renamePage,
            onAddPage = viewModel::addPage,
            onDeletePage = viewModel::deletePage,
            onSelectPage = viewModel::selectPage
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp
        )

        val current = currentPage
        if (current == null) {
            EmptyNotesState(onCreatePage = viewModel::addPage)
            return@Column
        }

        val palette = if (isSystemInDarkTheme()) DARK_PALETTE else LIGHT_PALETTE
        var activeSection by remember(current.id) { mutableIntStateOf(0) }
        if (activeSection >= current.sections.size) {
            activeSection = (current.sections.size - 1).coerceAtLeast(0)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { color ->
                val isSelected = current.sections.getOrNull(activeSection)?.color == color
                PaletteSwatch(
                    color = Color(color),
                    selected = isSelected,
                    onClick = {
                        if (current.sections.isNotEmpty()) {
                            viewModel.setSectionColor(current.id, activeSection, color)
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.size(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = current.sections,
                key = { index, _ -> "$current.id-s$index" }
            ) { index, section ->
                SectionCard(
                    section = section,
                    isActive = index == activeSection,
                    askBeforeNoteDelete = askBeforeNoteDelete,
                    onActivate = { activeSection = index },
                    onTextChange = { text -> viewModel.setSectionText(current.id, index, text) },
                    onRemove = { viewModel.removeSection(current.id, index) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { viewModel.addSection(current.id) },
                label = { Text("Добавить поле") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            Text(
                text = "Сохраняется автоматически",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PageHeader(
    currentPage: NotePage?,
    pages: List<NotePage>,
    askBeforeNoteDelete: Boolean,
    onRename: (String, String) -> Unit,
    onAddPage: () -> Unit,
    onDeletePage: (String) -> Unit,
    onSelectPage: (String) -> Unit
) {
    var renameTarget by remember { mutableStateOf<NotePage?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Электронные конспекты",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        if (currentPage != null) {
            IconButton(
                onClick = {
                    renameTarget = currentPage
                    draftTitle = currentPage.title
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Переименовать страницу"
                )
            }
            IconButton(
                onClick = {
                    if (askBeforeNoteDelete) confirmDelete = true
                    else onDeletePage(currentPage.id)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить страницу",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    LazyRow(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(pages, key = { it.id }) { page ->
            FilterChip(
                selected = page.id == currentPage?.id,
                onClick = { onSelectPage(page.id) },
                label = { Text(page.title.ifBlank { "Без названия" }) }
            )
        }
        item {
            AssistChip(
                onClick = onAddPage,
                label = { Text("+") }
            )
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Переименовать страницу") },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                    label = { Text("Название страницы") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(renameTarget!!.id, draftTitle.trim())
                        renameTarget = null
                    }
                ) {
                    Text("Переименовать")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (confirmDelete && currentPage != null) {
        val pageForDialog = currentPage
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить страницу?") },
            text = {
                Text(
                    "Страница «${pageForDialog.title.ifBlank { "Без названия" }}» " +
                        "и все её поля будут удалены безвозвратно."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeletePage(pageForDialog.id)
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ColumnScope.EmptyNotesState(onCreatePage: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "Нет страниц",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Создайте первую страницу конспекта",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(16.dp))
            AssistChip(
                onClick = onCreatePage,
                label = { Text("Создать страницу") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun PaletteSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SectionCard(
    section: NoteSection,
    isActive: Boolean,
    askBeforeNoteDelete: Boolean,
    onActivate: () -> Unit,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val accent = Color(section.color)
    var localText by remember { mutableStateOf(section.text) }
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(section.text) {
        if (section.text != localText) localText = section.text
    }
    LaunchedEffect(localText) {
        delay(350)
        if (localText != section.text) {
            onTextChange(localText)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(1.5.dp, accent, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onActivate() },
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        if (askBeforeNoteDelete) confirmRemove = true
                        else onRemove()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Удалить поле",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            TextField(
                value = localText,
                onValueChange = { localText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Введите текст…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = accent),
                minLines = 2,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Удалить поле?") },
            text = { Text("Текст поля будет удалён безвозвратно.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemove()
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}