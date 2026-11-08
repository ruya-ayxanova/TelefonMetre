package com.smartmeter3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = DarkGreen,
    onPrimary = Color.White,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = DarkGreen,
    background = Cream,
    onBackground = InkDark,
    surface = CreamSurface,
    onSurface = InkDark
)

@Composable
fun SmartMeterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
