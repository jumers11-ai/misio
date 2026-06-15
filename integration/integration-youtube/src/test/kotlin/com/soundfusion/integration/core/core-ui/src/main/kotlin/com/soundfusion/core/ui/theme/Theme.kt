package com.soundfusion.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Purple80 = Color(0xFFBB9AF7)
private val PurpleGrey80 = Color(0xFF9D8FCC)
private val Cyan80 = Color(0xFF06D6A0)

private val Purple40 = Color(0xFF6D3BFF)
private val PurpleGrey40 = Color(0xFF4D0FD4)
private val Cyan40 = Color(0xFF059E78)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Cyan80,
    background = Color(0xFF0D0F12),
    surface = Color(0xFF0D0F12),
    surfaceVariant = Color(0xFF1A1B26),
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Cyan40,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
)

@Composable
fun SoundFusionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
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
        typography = SoundFusionTypography,
        content = content,
    )
}
