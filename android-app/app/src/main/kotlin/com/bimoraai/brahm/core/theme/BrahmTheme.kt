package com.bimoraai.brahm.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Light color scheme (default) ─────────────────────────────────────────────
private val BrahmLightColorScheme = lightColorScheme(
    primary            = BrahmGold,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFFFF8E7),
    onPrimaryContainer = BrahmGold,

    secondary          = BrahmSaffron,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFFFF0E8),
    onSecondaryContainer = BrahmSaffron,

    tertiary           = BrahmPurple,
    onTertiary         = Color.White,

    background         = BrahmBackground,
    onBackground       = BrahmForeground,

    surface            = BrahmCard,
    onSurface          = BrahmForeground,
    surfaceVariant     = BrahmMuted,
    onSurfaceVariant   = BrahmMutedForeground,

    outline            = BrahmBorder,
    outlineVariant     = Color(0xFFF3F4F6),

    error              = BrahmError,
    onError            = Color.White,
)

// ─── Dark color scheme (Phase 2) ──────────────────────────────────────────────
// Warm near-black background — avoids pure black eye fatigue.
// Gold is brightened for readability on dark surfaces.
private val BrahmDarkColorScheme = darkColorScheme(
    primary            = Color(0xFFD4A43A),     // brighter gold for dark bg
    onPrimary          = Color(0xFF1A0E00),
    primaryContainer   = Color(0xFF3D2800),
    onPrimaryContainer = Color(0xFFFFDF9A),

    secondary          = Color(0xFFEF8A50),     // warmer saffron on dark
    onSecondary        = Color(0xFF1A0800),
    secondaryContainer = Color(0xFF4A1800),
    onSecondaryContainer = Color(0xFFFFD5B0),

    tertiary           = Color(0xFFAA80FF),
    onTertiary         = Color(0xFF1A0040),

    background         = Color(0xFF0F0F13),     // warm near-black
    onBackground       = Color(0xFFEDE8DE),     // warm off-white text

    surface            = Color(0xFF1A1A24),     // elevated dark surface
    onSurface          = Color(0xFFEDE8DE),
    surfaceVariant     = Color(0xFF262630),
    onSurfaceVariant   = Color(0xFFB0A898),

    outline            = Color(0xFF3A3A44),
    outlineVariant     = Color(0xFF2A2A34),

    error              = Color(0xFFFF6B6B),
    onError            = Color(0xFF1A0000),
)

@Composable
fun BrahmTheme(
    darkTheme: Boolean = false,   // always light — brand design is light-only
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BrahmDarkColorScheme else BrahmLightColorScheme,
        typography  = BrahmTypography,
        content     = content,
    )
}
