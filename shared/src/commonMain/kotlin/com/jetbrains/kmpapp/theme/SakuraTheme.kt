package com.jetbrains.kmpapp.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SakuraLightColors = lightColorScheme(
    primary = Color(0xFFE87A90),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDE6),
    onPrimaryContainer = Color(0xFF5B112B),
    secondary = Color(0xFFB57C8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDECEF),
    onSecondaryContainer = Color(0xFF3E1A23),
    background = Color(0xFFFFF7F9),
    onBackground = Color(0xFF281C1E),
    surface = Color(0xFFFFF7F9),
    onSurface = Color(0xFF281C1E),
    surfaceContainer = Color(0xFFFDF0F3),
    surfaceContainerHigh = Color(0xFFF9E4E8)
)

val CyberpunkLightColors = lightColorScheme(
    primary = Color(0xFFB000B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFC6F8),
    onPrimaryContainer = Color(0xFF3B003D),
    secondary = Color(0xFF007F86),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9CF8FF),
    onSecondaryContainer = Color(0xFF002022),
    background = Color(0xFFFFF7FF),
    onBackground = Color(0xFF211A22),
    surface = Color(0xFFFFF7FF),
    onSurface = Color(0xFF211A22),
    surfaceContainer = Color(0xFFF8EAF8),
    surfaceContainerHigh = Color(0xFFF0D6F0)
)

val CyberpunkDarkColors = darkColorScheme(
    primary = Color(0xFFFF5CE1),
    onPrimary = Color(0xFF4A004B),
    primaryContainer = Color(0xFF780078),
    onPrimaryContainer = Color(0xFFFFD7F5),
    secondary = Color(0xFF00F0FF),
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF00565C),
    onSecondaryContainer = Color(0xFF9CF8FF),
    background = Color(0xFF090B18),
    onBackground = Color(0xFFEFE5F5),
    surface = Color(0xFF090B18),
    onSurface = Color(0xFFEFE5F5),
    surfaceContainer = Color(0xFF17142B),
    surfaceContainerHigh = Color(0xFF241D3D)
)

val SakuraDarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C3), onPrimary = Color(0xFF5B112B),
    primaryContainer = Color(0xFF7E2442), onPrimaryContainer = Color(0xFFFFDDE6),
    secondary = Color(0xFFE3BDC7), onSecondary = Color(0xFF422830),
    secondaryContainer = Color(0xFF5B3D46), onSecondaryContainer = Color(0xFFFFDDE6),
    background = Color(0xFF1B1114), onBackground = Color(0xFFF0DFE2),
    surface = Color(0xFF1B1114), onSurface = Color(0xFFF0DFE2),
    surfaceContainer = Color(0xFF281C20), surfaceContainerHigh = Color(0xFF33242A)
)

val MatrixLightColors = lightColorScheme(
    primary = Color(0xFF006E2E), onPrimary = Color.White,
    primaryContainer = Color(0xFF9FF2AC), onPrimaryContainer = Color(0xFF003914),
    secondary = Color(0xFF276F57), onSecondary = Color.White,
    secondaryContainer = Color(0xFFABF2D7), onSecondaryContainer = Color(0xFF002117),
    background = Color(0xFFF7FCF3), onBackground = Color(0xFF161E18),
    surface = Color(0xFFF7FCF3), onSurface = Color(0xFF161E18),
    surfaceContainer = Color(0xFFEBF4E7), surfaceContainerHigh = Color(0xFFE2EDDF)
)

val MatrixDarkColors = darkColorScheme(
    primary = Color(0xFF00E676), onPrimary = Color(0xFF003A00),
    primaryContainer = Color(0xFF00521D), onPrimaryContainer = Color(0xFF8DFFAC),
    secondary = Color(0xFF2EB88F), onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF00513F), onSecondaryContainer = Color(0xFFA8F5DE),
    background = Color(0xFF030505), onBackground = Color(0xFFD8F5E0),
    surface = Color(0xFF030505), onSurface = Color(0xFFD8F5E0),
    surfaceContainer = Color(0xFF0E1612), surfaceContainerHigh = Color(0xFF18241D)
)

val MatrixSakuraLightColors = MatrixLightColors.copy(
    secondary = Color(0xFFAD4A6E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDE6), onSecondaryContainer = Color(0xFF3E1323)
)
val MatrixSakuraDarkColors = MatrixDarkColors.copy(
    secondary = Color(0xFFFFA9BE), onSecondary = Color(0xFF4A1020),
    secondaryContainer = Color(0xFF7E2442), onSecondaryContainer = Color(0xFFFFDDE6)
)
