package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),               // Light Emerald
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = VibrantSecondary,
    onSecondary = Color.White,
    tertiary = VibrantTertiary,
    onTertiary = Color.Black,
    background = DarkSlateBackground,
    onBackground = TextLightSlate,
    surface = DarkSlateCard,
    onSurface = TextLightSlate,
    error = RedCancel
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantPrimary,
    onPrimary = Color.White,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondary = VibrantSecondary,
    onSecondary = Color.White,
    tertiary = VibrantTertiary,
    onTertiary = Color.White,
    background = VibrantBackground,
    onBackground = VibrantTextDark,
    surface = VibrantCardWhite,
    onSurface = VibrantTextDark,
    error = RedCancel
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force our beautiful organic branding colors!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
