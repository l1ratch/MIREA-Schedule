package com.jetbrains.kmpapp.screens.rooms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.model.FreeRoomBellSlot
import com.jetbrains.kmpapp.data.model.FreeRoomItem
import com.jetbrains.kmpapp.screens.components.SyncStatusBadge

private val DEFAULT_BELL_SLOTS = listOf(
    FreeRoomBellSlot(1, "09:00", "10:30"),
    FreeRoomBellSlot(2, "10:40", "12:10"),
    FreeRoomBellSlot(3, "12:40", "14:10"),
    FreeRoomBellSlot(4, "14:20", "15:50"),
    FreeRoomBellSlot(5, "16:20", "17:50"),
    FreeRoomBellSlot(6, "18:00", "19:30"),
    FreeRoomBellSlot(7, "19:40", "21:10")
)

@Composable
fun FreeRoomsScreen(
    viewModel: FreeRoomsViewModel,
    modifier: Modifier = Modifier
) {
    val freeRoomsData by viewModel.freeRoomsData.collectAsState()
    val selectedCampus by viewModel.selectedCampus.collectAsState()
    val selectedFloor by viewModel.selectedFloor.collectAsState()
    val selectedBell by viewModel.selectedBell.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val filteredRooms by viewModel.filteredRooms.collectAsState()
    val availableCampuses by viewModel.availableCampuses.collectAsState()
    val availableFloors by viewModel.availableFloors.collectAsState()
    val selectedRoomForDetail by viewModel.selectedRoomForDetail.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val bellSlots = if (freeRoomsData.bellSlots.isNotEmpty()) freeRoomsData.bellSlots else DEFAULT_BELL_SLOTS
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Свободные аудитории",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Дата: ${viewModel.currentDateIso} • Пара $selectedBell",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.loadFreeRooms(forceRefresh = true) },
                        enabled = !isRefreshing && !isLoading
                    ) {
                        if (isRefreshing || isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Номер аудитории (например: 349, А-1)", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            var showCampusDialog by remember { mutableStateOf(false) }
            var showBellDialog by remember { mutableStateOf(false) }
            var showFloorDialog by remember { mutableStateOf(false) }

            // Compact Filter Row matching schedule target selector style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Campus Selector
                FilterDropdownButton(
                    title = "Корпус",
                    value = selectedCampus,
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        focusManager.clearFocus()
                        showCampusDialog = true
                    }
                )

                // Bell / Time Selector
                val currentSlot = bellSlots.firstOrNull { it.bell == selectedBell }
                FilterDropdownButton(
                    title = "Пара",
                    value = "$selectedBell пара (${currentSlot?.start ?: ""})",
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1.3f),
                    onClick = {
                        focusManager.clearFocus()
                        showBellDialog = true
                    }
                )

                // Floor Selector
                FilterDropdownButton(
                    title = "Этаж",
                    value = selectedFloor?.let { "$it этаж" } ?: "Все",
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(0.9f),
                    onClick = {
                        focusManager.clearFocus()
                        showFloorDialog = true
                    }
                )
            }

            if (showCampusDialog) {
                AlertDialog(
                    onDismissRequest = { showCampusDialog = false },
                    title = { Text("Выберите корпус", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            availableCampuses.forEach { campus ->
                                val isSelected = selectedCampus.equals(campus, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectCampus(campus)
                                            showCampusDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = campus,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCampusDialog = false }) { Text("Закрыть") }
                    }
                )
            }

            if (showBellDialog) {
                AlertDialog(
                    onDismissRequest = { showBellDialog = false },
                    title = { Text("Выберите пару", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            bellSlots.forEach { slot ->
                                val isSelected = selectedBell == slot.bell
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectBell(slot.bell)
                                            showBellDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${slot.bell} пара (${slot.start} - ${slot.end})",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBellDialog = false }) { Text("Закрыть") }
                    }
                )
            }

            if (showFloorDialog) {
                AlertDialog(
                    onDismissRequest = { showFloorDialog = false },
                    title = { Text("Выберите этаж", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            val isAllSelected = selectedFloor == null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectFloor(null)
                                        showFloorDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Все этажи",
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isAllSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            availableFloors.forEach { floor ->
                                val isSelected = selectedFloor == floor
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.selectFloor(floor)
                                            showFloorDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$floor этаж",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showFloorDialog = false }) { Text("Закрыть") }
                    }
                )
            }

            // Results count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Свободно: ${filteredRooms.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Grid of Rooms
            if (isLoading && filteredRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredRooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Свободные аудитории не найдены",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Попробуйте выбрать другую пару, кампус или сбросить фильтр",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 105.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { focusManager.clearFocus() }
                        }
                ) {
                    items(filteredRooms, key = { it.id }) { room ->
                        FreeRoomGridCard(
                            room = room,
                            dateIso = viewModel.currentDateIso,
                            currentBell = selectedBell,
                            bellSlots = bellSlots,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.selectRoomForDetail(room)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (selectedRoomForDetail != null) {
        val room = selectedRoomForDetail!!
        RoomDetailDialog(
            room = room,
            dateIso = viewModel.currentDateIso,
            bellSlots = bellSlots,
            onDismiss = { viewModel.selectRoomForDetail(null) }
        )
    }

    SyncStatusBadge(
        status = syncStatus,
        onDismiss = { viewModel.dismissStatusBadge() }
    )
}
}

@Composable
private fun FreeRoomGridCard(
    room: FreeRoomItem,
    dateIso: String,
    currentBell: Int,
    bellSlots: List<FreeRoomBellSlot>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = if (room.floor != null) "${room.floor} эт" else room.campus,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val untilText = room.getFreeUntilDescription(dateIso, currentBell, bellSlots)
            Text(
                text = untilText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoomDetailDialog(
    room: FreeRoomItem,
    dateIso: String,
    bellSlots: List<FreeRoomBellSlot>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Аудитория ${room.fullTitle}", fontWeight = FontWeight.Bold)
                if (room.floor != null) {
                    Text(
                        text = "Кампус ${room.campus} • Этаж ${room.floor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Расписание занятости на сегодня:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                bellSlots.forEach { slot ->
                    val isFree = room.isFreeAt(dateIso, slot.bell)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${slot.bell} пара (${slot.start} – ${slot.end})",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFree) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isFree) "Свободна" else "Занята",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFree) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun FilterDropdownButton(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

