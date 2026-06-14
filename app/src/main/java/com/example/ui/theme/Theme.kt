package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantSecondary,
    primaryContainer = ElegantSecondary,
    onPrimaryContainer = ElegantPrimary,
    secondary = PurpleGrey80,
    onSecondary = ElegantDarkBg,
    background = ElegantDarkBg,
    onBackground = ElegantTextLight,
    surface = ElegantCardDarker,
    onSurface = ElegantTextLight,
    surfaceVariant = ElegantCardLighter,
    onSurfaceVariant = ElegantTextMuted,
    outline = ElegantTextSubtle
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Force the Elegant Dark theme for strict visual alignment
    MaterialTheme(
        colorScheme = ElegantDarkColorScheme,
        typography = Typography,
        content = content
    )
}
