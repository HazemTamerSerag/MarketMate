package com.example.marketmate.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MarketMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Checks system theme
    content: @Composable () -> Unit
) {
    val colors = darkColorScheme( // Always use dark colors
        primary = Color(0xFF6200EE), // Purple
        background = Color.Black,   // Black background
        surface = Color.Black,      // Black surface
        onPrimary = Color.White,    // White text/icons
        onBackground = Color.White, // White text/icons
        onSurface = Color.White     // White text/icons
    )

    MaterialTheme(
        colorScheme = colors, // Apply theme
        typography = Typography,
        content = content
    )
}