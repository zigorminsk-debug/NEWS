package com.techpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TechPulseColors = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF00252B),
    primaryContainer = Color(0xFF00323C),
    onPrimaryContainer = AccentCyan,
    secondary = AccentGreen,
    onSecondary = Color(0xFF04290F),
    tertiary = AccentCyan,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor,
    outlineVariant = OutlineColor,
    error = DangerRed,
    onError = Color(0xFF33000A)
)

@Composable
fun TechPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TechPulseColors,
        content = content
    )
}
