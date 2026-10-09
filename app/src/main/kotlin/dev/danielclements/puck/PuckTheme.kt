package dev.danielclements.puck

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val DarkColors = darkColorScheme(
    primaryContainer = Color(0xFF2C2C2E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    primary = Color(0xFF6EA8FF),
    onPrimary = Color(0xFF00203F),
    surface = Color(0xFF16161B),
    onSurface = Color(0xFFE6E6EA),
    background = Color(0xFF0E0E12),
    onBackground = Color(0xFFE6E6EA),
    surfaceVariant = Color(0xFF23232B),
    onSurfaceVariant = Color(0xFFB9B9C4),
    error = Color(0xFFFF6B6B),
)

val LightColors = lightColorScheme(
    // Apple-native button look: charcoal keys with white foreground on the
    // pale remote body.
    primaryContainer = Color(0xFF1C1C1E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    primary = Color(0xFF0A5AC8),
    onPrimary = Color.White,
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1C),
    background = Color(0xFFF2F2F6),
    onBackground = Color(0xFF1A1A1C),
    surfaceVariant = Color(0xFFE4E4EB),
    onSurfaceVariant = Color(0xFF54545E),
    error = Color(0xFFB3261E),
)

/**
 * Shared theme for every screen in the app, including the widget
 * configuration activity — not just MainActivity's.
 */
@Composable
fun PuckTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
