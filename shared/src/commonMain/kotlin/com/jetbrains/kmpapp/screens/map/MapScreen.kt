package com.jetbrains.kmpapp.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import com.jetbrains.kmpapp.data.storage.PlatformStorage
import org.koin.compose.koinInject

@Composable
fun MapScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val controller = remember { CampusMapController() }
    val uriHandler = LocalUriHandler.current
    val platformStorage: PlatformStorage = koinInject()

    var showDisclaimerDialog by remember {
        mutableStateOf(platformStorage.getString("mirea_map_disclaimer_seen") != "true")
    }

    var selectedCampus by remember { mutableStateOf(CAMPUSES.first()) }
    var selectedFloor by remember { mutableStateOf(selectedCampus.defaultFloor) }
    var svgContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var campusDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<RoomSearchResult>>(emptyList()) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var highlightedRoom by remember { mutableStateOf<String?>(null) }

    // Load SVG whenever campus or floor changes
    LaunchedEffect(selectedCampus, selectedFloor) {
        isLoading = true
        val svg = MapRepository.loadFloorSvg(selectedCampus.id, selectedFloor)
        svgContent = svg
        isLoading = false
    }

    // Search effect
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            searchResults = MapRepository.searchRooms(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Campus Map WebView Canvas
            if (svgContent != null) {
                val html = remember(svgContent, highlightedRoom) {
                    MapHtmlGenerator.generateHtml(svgContent ?: "", highlightedRoom)
                }
                CampusMapView(
                    htmlContent = html,
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Loading Indicator Overlay
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color(0xCC161B22),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // 3. Top Header Bar: Campus Selector & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Campus Selector Bar
                Surface(
                    color = Color(0xEE161B22),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { campusDropdownExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCampus.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = selectedCampus.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8B949E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Выбрать кампус",
                            tint = Color(0xFF8B949E)
                        )

                        // Campus Dropdown Menu
                        DropdownMenu(
                            expanded = campusDropdownExpanded,
                            onDismissRequest = { campusDropdownExpanded = false }
                        ) {
                            CAMPUSES.forEach { campus ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = campus.name,
                                                fontWeight = if (campus.id == selectedCampus.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (campus.id == selectedCampus.id) MaterialTheme.colorScheme.primary else Color.Unspecified
                                            )
                                            Text(
                                                text = campus.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCampus = campus
                                        selectedFloor = campus.defaultFloor
                                        campusDropdownExpanded = false
                                        highlightedRoom = null
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                Surface(
                    color = Color(0xEE161B22),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Поиск аудитории (например, Г-302, 127)", color = Color(0xFF8B949E), fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить", tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Search Results Dropdown List
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xF21C2128),
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(searchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val targetCampus = CAMPUSES.find { it.id == result.campusId } ?: selectedCampus
                                            selectedCampus = targetCampus
                                            selectedFloor = result.floor
                                            highlightedRoom = result.name
                                            searchQuery = ""
                                            searchResults = emptyList()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = result.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Этаж ${result.floor}",
                                            color = Color(0xFF58A6FF),
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .background(Color(0x33388BFD), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = result.campusName,
                                        color = Color(0xFF8B949E),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Floating Floor Selector (Right Side - Vertical Pill)
            Surface(
                color = Color(0xEE161B22),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedCampus.floors.forEach { floor ->
                        val isSelected = floor == selectedFloor
                        val displayText = if (floor == -1) "-1" else "$floor"
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable {
                                    selectedFloor = floor
                                    highlightedRoom = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayText,
                                color = if (isSelected) Color.White else Color(0xFFC9D1D9),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 5. Floating Zoom Controls (Bottom-Right)
            Surface(
                color = Color(0xEE161B22),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 84.dp)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { controller.zoomIn() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Приблизить", tint = Color(0xFFC9D1D9))
                    }
                    IconButton(
                        onClick = { controller.zoomOut() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Отдалить", tint = Color(0xFFC9D1D9))
                    }
                    IconButton(
                        onClick = { controller.resetView() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Центрировать", tint = Color(0xFFC9D1D9), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 6. First-Time Disclaimer Dialog
            if (showDisclaimerDialog) {
                AlertDialog(
                    onDismissRequest = {
                        platformStorage.saveString("mirea_map_disclaimer_seen", "true")
                        showDisclaimerDialog = false
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Схемы корпусов",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Планы этажей и расположение аудиторий могут быть неточными из-за перепланировок и ремонтов в корпусах.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                            Text(
                                text = "Для точной информации сверяйтесь со схемами эвакуации и указателями внутри здания или официальным сервисом «Карта РТУ МИРЭА»:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        uriHandler.openUri("https://pulse.mirea.ru/services/maps")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "pulse.mirea.ru/services/maps\n(доступен через ЛКС)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                platformStorage.saveString("mirea_map_disclaimer_seen", "true")
                                showDisclaimerDialog = false
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Понятно", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}
