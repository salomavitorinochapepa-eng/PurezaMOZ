package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    secondary = LightBlueAccent,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = OffWhite,
    onSurface = OffWhite,
    error = SoftAlert
)

private val LightColorScheme = lightColorScheme(
    primary = LightBlueAccent,
    onPrimary = Color.White,
    secondary = EmeraldPrimary,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = SoftAlert
)

@Composable
fun VencerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
