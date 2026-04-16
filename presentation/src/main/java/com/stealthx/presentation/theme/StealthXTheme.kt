package com.stealthx.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bg = Color(0xFF0A0A0F)
private val Surface = Color(0xFF0D1B2A)
private val Surface2 = Color(0xFF1B2A3B)
private val Border = Color(0xFF1E3A52)
private val Cyan = Color(0xFF00D4FF)
private val Green = Color(0xFF00FF88)
private val Gold = Color(0xFFFFB800)
private val TextColor = Color(0xFFE8EDF2)

private val SecureChatColors = darkColorScheme(
    primary = Green,
    secondary = Cyan,
    tertiary = Gold,
    background = Bg,
    surface = Surface,
    surfaceVariant = Surface2,
    outline = Border,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextColor,
    onSurface = TextColor
)

@Composable
fun StealthXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SecureChatColors,
        content = content
    )
}
