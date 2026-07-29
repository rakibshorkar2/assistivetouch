package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkSleekPrimary,
    onPrimary = DarkSleekOnPrimary,
    primaryContainer = DarkSleekPrimaryContainer,
    onPrimaryContainer = DarkSleekOnPrimaryContainer,
    secondary = SleekSecondary,
    background = DarkSleekBackground,
    surface = DarkSleekSurface,
    surfaceContainer = DarkSleekSurfaceContainer
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    background = SleekBackground,
    surface = SleekSurface,
    surfaceContainer = SleekSurfaceContainer,
    surfaceContainerHigh = SleekSurfaceContainerHigh,
    surfaceContainerLow = SleekSurfaceContainerLow,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant,
    onSurface = SleekOnSurface,
    onSurfaceVariant = SleekOnSurfaceVariant
)

@Composable
fun VolumeAssistantTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    VolumeAssistantTheme(
        themeMode = if (darkTheme) "DARK" else "LIGHT",
        dynamicColor = dynamicColor,
        content = content
    )
}
