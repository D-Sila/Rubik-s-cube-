package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CenterCubeColorScheme = darkColorScheme(
    primary = CyanReticle,
    onPrimary = ObsidianBg,
    primaryContainer = CyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = IndigoAccent,
    onSecondary = ObsidianBg,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSuccess,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = CarbonElevated,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = CoralError,
    onError = ObsidianBg
)

private val CenterCubeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun CenterCubeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CenterCubeColorScheme,
        typography = CenterCubeTypography,
        shapes = CenterCubeShapes,
        content = content
    )
}
