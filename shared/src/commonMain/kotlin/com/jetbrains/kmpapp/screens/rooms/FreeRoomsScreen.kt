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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.FreeRoomBellSlot
import com.jetbrains.kmpapp.data.model.FreeRoomItem
import com.jetbrains.kmpapp.screens.components.SyncStatusBadge
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

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
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

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
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Выбрать дату",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedBell != null) {
                                        "${selectedDate.day} ${DateUtils.formatMonthRu(selectedDate.month)} • Пара $selectedBell"
                                    } else {
                                        "${selectedDate.day} ${DateUtils.formatMonthRu(selectedDate.month)} • Все пары"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
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
                .clickable { focusManager.clearFocus() }
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

            // Sunday Friendly Banner
            val isSunday = selectedDate.dayOfWeek == kotlinx.datetime.DayOfWeek.SUNDAY
            if (isSunday) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "☀️",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Сегодня воскресенье — выходной!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Учебные пары сегодня не проводятся, поэтому свободные аудитории вам вряд ли понадобятся. Отдыхайте и набирайтесь сил! ☕",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

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
                    value = selectedBell?.let { "$it пара (${currentSlot?.start ?: ""})" } ?: "Все пары",
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
                CampusPickerDialog(
                    campuses = availableCampuses,
                    selectedCampus = selectedCampus,
                    onSelect = {
                        viewModel.selectCampus(it)
                        showCampusDialog = false
                    },
                    onDismiss = { showCampusDialog = false }
                )
            }

            if (showBellDialog) {
                BellPickerDialog(
                    bellSlots = bellSlots,
                    selectedBell = selectedBell,
                    onSelect = {
                        viewModel.selectBell(it)
                        showBellDialog = false
                    },
                    onDismiss = { showBellDialog = false }
                )
            }

            if (showFloorDialog) {
                FloorPickerDialog(
                    floors = availableFloors,
                    selectedFloor = selectedFloor,
                    onSelect = {
                        viewModel.selectFloor(it)
                        showFloorDialog = false
                    },
                    onDismiss = { showFloorDialog = false }
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
                    text = if (selectedBell != null) "Свободно на $selectedBell пару: ${filteredRooms.size}" else "Всего аудиторий: ${filteredRooms.size}",
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
                            dateIso = selectedDate.toString(),
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
            dateIso = selectedDate.toString(),
            bellSlots = bellSlots,
            onDismiss = { viewModel.selectRoomForDetail(null) }
        )
    }

    if (showDatePicker) {
        FreeRoomsDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false }
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
    currentBell: Int?,
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

            val untilText = if (currentBell != null) {
                room.getFreeUntilDescription(dateIso, currentBell, bellSlots)
            } else {
                room.getFreeSummaryDescription(dateIso, bellSlots.size.coerceAtLeast(7))
            }
            Text(
                text = untilText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (untilText.startsWith("Занята")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
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

