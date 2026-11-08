package com.example.blocdenotas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PremiumDarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkMenuBackground,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    primary = DarkTextPrimary,
    onPrimary = DarkBackground
)

private val PremiumLightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightMenuBackground,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    primary = LightTextPrimary,
    onPrimary = LightBackground
)

@Composable
fun BlocDeNotasTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PremiumDarkColorScheme else PremiumLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
