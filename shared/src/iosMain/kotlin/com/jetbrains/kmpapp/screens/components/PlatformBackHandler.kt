package com.jetbrains.kmpapp.screens.components

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Handled natively or through gestures on iOS
}
