package com.jetbrains.kmpapp.screens.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isFixed: Boolean = false
) {
    SCHEDULE("Расписание", Icons.Filled.DateRange, Icons.Outlined.DateRange, isFixed = true),
    FREE_ROOMS("Аудитории", Icons.Filled.MeetingRoom, Icons.Outlined.MeetingRoom, isFixed = false),
    TASKS("Задачи", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, isFixed = false),
    MAP("Карта", Icons.Filled.Map, Icons.Filled.Map, isFixed = false),
    OTHER("Другое", Icons.Filled.Settings, Icons.Outlined.Settings, isFixed = true)
}

@Composable
fun FloatingDock(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onTabReselected: ((AppTab) -> Unit)? = null,
    tabs: List<AppTab> = AppTab.entries,
    modifier: Modifier = Modifier
) {
    val displayedTabs = remember(tabs) { tabs.take(5) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Elevated comfortably above bottom: iOS home indicator (34dp) sits at ~20dp offset; Android button bar sits safely above buttons
    val bottomOffset = when {
        navBottom in 30.dp..38.dp -> (navBottom - 14.dp).coerceAtLeast(8.dp)
        navBottom > 38.dp -> navBottom + 4.dp
        navBottom > 0.dp -> navBottom + 4.dp
        else -> 12.dp
    }

    // Responsive horizontal padding based on tab count
    val itemHorizontalPadding = when {
        tabs.size <= 2 -> 26.dp
        tabs.size == 3 -> 18.dp
        else -> 12.dp
    }
    val itemVerticalPadding = if (tabs.size >= 4) 10.dp else 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomOffset),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.shadow(12.dp, shape = RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                displayedTabs.forEach { tab ->
                    val isSelected = tab == currentTab
                    val backgroundColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    val contentColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(backgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isSelected) {
                                    onTabReselected?.invoke(tab)
                                } else {
                                    onTabSelected(tab)
                                }
                            }
                            .padding(horizontal = itemHorizontalPadding, vertical = itemVerticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(scale)
                        )
                    }
                }
            }
        }
    }
}
