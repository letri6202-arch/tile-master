package com.tilemaster.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A mahjong table at night: dark felt, ivory tiles, gold accents. */
object TableColors {
    val Felt = Color(0xFF0C2318)
    val FeltRaised = Color(0xFF133024)
    val FeltHigh = Color(0xFF1A3D2E)
    val FeltHighest = Color(0xFF224A38)
    val Gold = Color(0xFFE8B84A)
    val Jade = Color(0xFF7FD1A8)
    val Coral = Color(0xFFFF8A73)
    val Ivory = Color(0xFFFBF6EA)
}

// The app is always dark: it is used at the table, mostly over a camera preview.
private val TableScheme = darkColorScheme(
    primary = TableColors.Gold,
    onPrimary = Color(0xFF2B1D00),
    primaryContainer = Color(0xFF5A4300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = TableColors.Jade,
    onSecondary = Color(0xFF00391F),
    secondaryContainer = TableColors.FeltHighest,
    onSecondaryContainer = Color(0xFFCDEBDA),
    tertiary = TableColors.Coral,
    background = TableColors.Felt,
    onBackground = Color(0xFFE6EFE9),
    surface = TableColors.Felt,
    onSurface = Color(0xFFE6EFE9),
    surfaceVariant = TableColors.FeltHigh,
    onSurfaceVariant = Color(0xFFB4C8BC),
    surfaceContainerLowest = Color(0xFF071810),
    surfaceContainerLow = Color(0xFF0F2A1D),
    surfaceContainer = TableColors.FeltRaised,
    surfaceContainerHigh = TableColors.FeltHigh,
    surfaceContainerHighest = TableColors.FeltHighest,
    outline = Color(0xFF4F6E5D),
    outlineVariant = Color(0xFF2C4A3B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF4A1A15),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val TableShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TileMasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TableScheme, shapes = TableShapes, content = content)
}
