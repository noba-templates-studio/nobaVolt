package com.noba.volt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Claro = lightColorScheme(
    primary = Negro,
    onPrimary = Blanco,
    primaryContainer = Gris100,
    onPrimaryContainer = Negro,
    secondary = Verde,
    onSecondary = Blanco,
    secondaryContainer = VerdeFondo,
    onSecondaryContainer = VerdeOscuro,
    tertiary = Azul,
    onTertiary = Blanco,
    tertiaryContainer = AzulFondo,
    onTertiaryContainer = AzulOscuro,
    background = Blanco,
    onBackground = Negro,
    surface = Blanco,
    onSurface = Negro,
    surfaceVariant = Gris100,
    onSurfaceVariant = Gris500,
    surfaceTint = Blanco,
    inverseSurface = Negro,
    inverseOnSurface = Blanco,
    outline = Gris400,
    outlineVariant = Gris200,
    error = Rojo,
    onError = Blanco,
    surfaceBright = Blanco,
    surfaceDim = Gris100,
    surfaceContainerLowest = Blanco,
    surfaceContainerLow = Blanco,
    surfaceContainer = Gris50,
    surfaceContainerHigh = Gris100,
    surfaceContainerHighest = Gris100
)

private val Oscuro = darkColorScheme(
    primary = Gris50,
    onPrimary = Negro,
    primaryContainer = Gris900,
    onPrimaryContainer = Gris50,
    secondary = VerdeClaro,
    onSecondary = Negro,
    secondaryContainer = VerdeOscuro,
    onSecondaryContainer = VerdeFondo,
    tertiary = AzulClaro,
    onTertiary = Negro,
    tertiaryContainer = AzulOscuro,
    onTertiaryContainer = AzulFondo,
    background = Negro,
    onBackground = Gris50,
    surface = Negro,
    onSurface = Gris50,
    surfaceVariant = Gris900,
    onSurfaceVariant = Gris400,
    surfaceTint = Negro,
    inverseSurface = Gris50,
    inverseOnSurface = Negro,
    outline = Gris500,
    outlineVariant = Gris800,
    error = Color(0xFFF87171),
    onError = Negro,
    surfaceBright = Gris900,
    surfaceDim = Negro,
    surfaceContainerLowest = Negro,
    surfaceContainerLow = Negro,
    surfaceContainer = Gris900,
    surfaceContainerHigh = Gris900,
    surfaceContainerHighest = Gris800
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NobaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Oscuro else Claro,
        typography = tipografiaNoba(),
        shapes = Formas,
        content = content
    )
}
