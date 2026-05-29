package com.janhorak.shutterdeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.janhorak.shutterdeck.core.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = SpectrumCyan,
    onPrimary = DeepInk,
    secondary = PolarBlue,
    onSecondary = DeepInk,
    tertiary = PolarBlue,
    background = MidnightObsidian,
    onBackground = OffWhite,
    surface = DarkVelvet,
    onSurface = OffWhite,
    surfaceVariant = ElevatedVelvet,
    onSurfaceVariant = GhostGray,
    outline = OutlineGray,
)

private val LightColorScheme = lightColorScheme(
    primary = OceanCyan,
    onPrimary = LightSurface,
    secondary = DeepBlue,
    onSecondary = LightSurface,
    tertiary = DeepBlue,
    background = LightBackground,
    onBackground = MidnightObsidian,
    surface = LightSurface,
    onSurface = MidnightObsidian,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = GhostGray,
    outline = OutlineGray,
)

private val NightColorScheme = darkColorScheme(
    primary = NightRed,
    onPrimary = PureBlack,
    secondary = DimRed,
    onSecondary = PureBlack,
    tertiary = DimRed,
    background = PureBlack,
    onBackground = NightRed,
    surface = NearBlack,
    onSurface = NightRed,
    surfaceVariant = NightSurface,
    onSurfaceVariant = MutedRed,
    outline = MutedRed,
)

@Composable
fun ShutterDeckTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.NIGHT -> NightColorScheme
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}