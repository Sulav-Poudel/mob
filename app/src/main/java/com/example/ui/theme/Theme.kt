package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CollegiateNavy,
    onPrimary = CreamCard,
    primaryContainer = NavyContainerLight,
    onPrimaryContainer = NavyOnContainerLight,
    secondary = AcademicGold,
    onSecondary = CreamCard,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = GoldOnContainerLight,
    tertiary = BotanicalSage,
    onTertiary = CreamCard,
    tertiaryContainer = SageContainerLight,
    onTertiaryContainer = SageOnContainerLight,
    error = TerracottaCrimson,
    onError = CreamCard,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = CrimsonOnContainerLight,
    background = WarmPaper,
    onBackground = DeepInk,
    surface = CreamCard,
    onSurface = DeepInk,
    surfaceVariant = MutedParchment,
    onSurfaceVariant = MutedSlate,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = LightNavy,
    onPrimary = LightNavyOn,
    primaryContainer = DarkNavyContainer,
    onPrimaryContainer = DarkNavyOnContainer,
    secondary = LightGold,
    onSecondary = LightGoldOn,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = DarkGoldOnContainer,
    tertiary = LightSage,
    onTertiary = LightSageOn,
    tertiaryContainer = DarkSageContainer,
    onTertiaryContainer = DarkSageOnContainer,
    error = LightCrimson,
    onError = LightCrimsonOn,
    errorContainer = DarkCrimsonContainer,
    onErrorContainer = DarkCrimsonOnContainer,
    background = DarkSlateBg,
    onBackground = WarmIvoryInk,
    surface = DarkCard,
    onSurface = WarmIvoryInk,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = CoolAsh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val EditorialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object CollegeTrackerTheme {
    val academicColors: AcademicColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAcademicColors.current
}

@Composable
fun CollegeTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val academicColors = if (darkTheme) DarkAcademicColors else LightAcademicColors

    CompositionLocalProvider(LocalAcademicColors provides academicColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = EditorialShapes,
            content = content
        )
    }
}
