package br.com.abreujp.hexreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElixirPlum,
    onPrimary = ElixirNight,
    primaryContainer = ElixirPurple,
    onPrimaryContainer = ElixirLavender,
    secondary = ElixirLavender,
    onSecondary = ElixirNight,
    background = ElixirNight,
    onBackground = ElixirLavender,
    surface = ElixirNight,
    onSurface = ElixirLavender,
    surfaceVariant = ElixirSurfaceDark,
    onSurfaceVariant = Color(0xFFCDBED7)
)

private val LightColorScheme = lightColorScheme(
    primary = ElixirPurple,
    onPrimary = Color.White,
    primaryContainer = ElixirLavender,
    onPrimaryContainer = ElixirInk,
    secondary = ElixirPurpleLight,
    onSecondary = Color.White,
    background = ElixirMist,
    onBackground = ElixirInk,
    surface = Color.White,
    onSurface = ElixirInk,
    surfaceVariant = Color(0xFFEBDDFA),
    onSurfaceVariant = ElixirSlate
)

@Composable
fun HexReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
