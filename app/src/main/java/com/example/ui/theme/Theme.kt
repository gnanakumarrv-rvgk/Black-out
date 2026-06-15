package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getDarkColorScheme(primaryColor: Color): ColorScheme {
    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color(0xFF141519),
        primaryContainer = primaryColor.copy(alpha = 0.2f),
        onPrimaryContainer = primaryColor,
        secondary = primaryColor,
        onSecondary = Color(0xFF141519),
        background = Color(0xFF0C0C0E), // Ultra-premium obsidian background
        onBackground = Color(0xFFECECEC),
        surface = Color(0xFF16161A), // Sleek layered card background
        onSurface = Color(0xFFECECEC),
        surfaceVariant = Color(0xFF232329),
        onSurfaceVariant = Color(0xFFCCCCCC),
        outline = primaryColor.copy(alpha = 0.35f)
    )
}

fun getLightColorScheme(primaryColor: Color): ColorScheme {
    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.15f),
        onPrimaryContainer = primaryColor,
        secondary = primaryColor,
        onSecondary = Color.White,
        background = Color(0xFFF7F8FA), // Light premium clean canvas
        onBackground = Color(0xFF141519),
        surface = Color.White,
        onSurface = Color(0xFF141519),
        surfaceVariant = Color(0xFFEFEFEF),
        onSurfaceVariant = Color(0xFF444444),
        outline = primaryColor.copy(alpha = 0.35f)
    )
}

@Composable
fun MyApplicationTheme(
    themeMode: Int = 0, // 0 = Dark, 1 = Light, 2 = System Default
    customColor: Color = ElegantPrimary,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        0 -> true
        1 -> false
        else -> isSystemInDarkTheme()
    }

    val scheme = if (isDark) {
        getDarkColorScheme(customColor)
    } else {
        getLightColorScheme(customColor)
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}

