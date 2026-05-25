package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DentalBluePrimaryDark,
    secondary = DentalSuccessGreen,
    background = DentalBgDark,
    surface = DentalSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DentalTextMainDark,
    onSurface = DentalTextMainDark,
    error = DentalErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = DentalBluePrimary,
    secondary = DentalSuccessGreen,
    background = DentalBgLight,
    surface = DentalSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DentalTextMainLight,
    onSurface = DentalTextMainLight,
    error = DentalErrorRed
)

@Composable
fun DentalBiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
