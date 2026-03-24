package com.bimoraai.brahm.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light-only theme — exact mirror of website (no dark mode)
private val BrahmColorScheme = lightColorScheme(
    primary          = BrahmGold,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFF8E7),   // very light amber
    onPrimaryContainer = BrahmGold,

    secondary        = BrahmSaffron,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFFF0E8),
    onSecondaryContainer = BrahmSaffron,

    tertiary         = BrahmPurple,
    onTertiary       = Color.White,

    background       = BrahmBackground,
    onBackground     = BrahmForeground,

    surface          = BrahmCard,
    onSurface        = BrahmForeground,
    surfaceVariant   = BrahmMuted,
    onSurfaceVariant = BrahmMutedForeground,

    outline          = BrahmBorder,
    outlineVariant   = Color(0xFFF3F4F6),

    error            = BrahmError,
    onError          = Color.White,
)

@Composable
fun BrahmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrahmColorScheme,
        typography  = BrahmTypography,
        content     = content,
    )
}
