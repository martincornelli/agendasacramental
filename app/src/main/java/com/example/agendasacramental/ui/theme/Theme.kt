package com.example.agendasacramental.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF126B75), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EFF0), onPrimaryContainer = Color(0xFF103E48),
    secondary = Color(0xFF536682), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EAF5), onSecondaryContainer = Color(0xFF24364F),
    tertiary = Color(0xFF8D6025), tertiaryContainer = Color(0xFFFFEACA), onTertiaryContainer = Color(0xFF503411),
    background = Color(0xFFF3F5F8), onBackground = Color(0xFF1C2D42),
    surface = Color.White, onSurface = Color(0xFF1C2D42),
    surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = Color(0xFF536378),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFEFF3F7), surfaceContainerHigh = Color(0xFFE7EDF3), surfaceContainerHighest = Color(0xFFDDE5EE),
    outline = Color(0xFF778598), outlineVariant = Color(0xFFD7DFE8),
    error = Color(0xFFB42335), errorContainer = Color(0xFFFFE4E7), onErrorContainer = Color(0xFF701323)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD4D7), onPrimary = Color(0xFF00383F),
    primaryContainer = Color(0xFF214B55), onPrimaryContainer = Color(0xFFD7F4F4),
    secondary = Color(0xFFB5C7E4), onSecondary = Color(0xFF20334D),
    secondaryContainer = Color(0xFF2D405B), onSecondaryContainer = Color(0xFFDCE6F7),
    tertiary = Color(0xFFF0C384), tertiaryContainer = Color(0xFF503D25), onTertiaryContainer = Color(0xFFFFE4BE),
    background = Color(0xFF101B29), onBackground = Color(0xFFE7EDF5),
    surface = Color(0xFF192638), onSurface = Color(0xFFE7EDF5),
    surfaceVariant = Color(0xFF2C3B4E), onSurfaceVariant = Color(0xFFB5C3D4),
    surfaceContainerLowest = Color(0xFF101B29), surfaceContainerLow = Color(0xFF172333),
    surfaceContainer = Color(0xFF1D2D41), surfaceContainerHigh = Color(0xFF26374B), surfaceContainerHighest = Color(0xFF304257),
    outline = Color(0xFF8C9CB0), outlineVariant = Color(0xFF3B4C61),
    error = Color(0xFFFFB1BB), errorContainer = Color(0xFF592331), onErrorContainer = Color(0xFFFFDCE1)
)

@Composable
fun AgendaSacramentalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
        content = content
    )
}
