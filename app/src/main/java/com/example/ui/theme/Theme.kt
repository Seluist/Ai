package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberBlack,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = CyberBlack,
    secondaryContainer = CyberSurfaceElevated,
    onSecondaryContainer = NeonPink,
    tertiary = CyberYellow,
    onTertiary = CyberBlack,
    background = CyberBlack,
    onBackground = CyberTextPrimary,
    surface = CyberDark,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = CyberTextSecondary,
    outline = CyberBorder,
    outlineVariant = CyberBorderCyan
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
