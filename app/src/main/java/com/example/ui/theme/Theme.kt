package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoDarkSurface,
    onPrimaryContainer = MonoWhite,
    secondary = MonoDarkTextSecondary,
    onSecondary = MonoBlack,
    secondaryContainer = MonoDarkSurface,
    onSecondaryContainer = MonoDarkTextSecondary,
    background = MonoDarkBg,
    onBackground = MonoDarkTextPrimary,
    surface = MonoDarkSurface,
    onSurface = MonoDarkTextPrimary,
    surfaceVariant = MonoDarkSurface,
    onSurfaceVariant = MonoDarkTextSecondary,
    outline = MonoDarkBorder,
    error = MonoDarkTextSecondary,
    onError = MonoBlack
)

private val LightColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoLightSurface,
    onPrimaryContainer = MonoBlack,
    secondary = MonoLightTextSecondary,
    onSecondary = MonoWhite,
    secondaryContainer = MonoLightSurface,
    onSecondaryContainer = MonoLightTextSecondary,
    background = MonoLightBg,
    onBackground = MonoLightTextPrimary,
    surface = MonoLightSurface,
    onSurface = MonoLightTextPrimary,
    surfaceVariant = MonoLightSurface,
    onSurfaceVariant = MonoLightTextSecondary,
    outline = MonoLightBorder,
    error = MonoLightTextSecondary,
    onError = MonoWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
