package com.kakuma.resourcelink.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kakuma.resourcelink.core.datastore.ThemeMode

private val LightColors = lightColorScheme(
    primary = KrColors.Primary,
    onPrimary = Color.White,
    primaryContainer = KrColors.PrimaryContainer,
    onPrimaryContainer = KrColors.Primary,
    secondary = KrColors.Accent,
    onSecondary = Color.White,
    tertiary = KrColors.Gold,
    tertiaryContainer = KrColors.GoldContainer,
    onTertiaryContainer = KrColors.OnGoldContainer,
    background = KrColors.Background,
    onBackground = KrColors.Text,
    surface = KrColors.Surface,
    onSurface = KrColors.Text,
    surfaceVariant = KrColors.Background,
    onSurfaceVariant = KrColors.Muted,
    outline = KrColors.Border,
    outlineVariant = KrColors.Border,
    error = KrColors.Error,
)

private val DarkColors = darkColorScheme(
    primary = KrColors.DarkPrimary,
    onPrimary = KrColors.DarkBackground,
    primaryContainer = KrColors.DarkSurfaceVariant,
    onPrimaryContainer = KrColors.DarkPrimary,
    secondary = KrColors.Accent,
    tertiary = KrColors.Gold,
    tertiaryContainer = Color(0xFF3D2C0B),
    onTertiaryContainer = KrColors.Gold,
    background = KrColors.DarkBackground,
    onBackground = KrColors.DarkText,
    surface = KrColors.DarkSurface,
    onSurface = KrColors.DarkText,
    surfaceVariant = KrColors.DarkSurfaceVariant,
    onSurfaceVariant = KrColors.DarkMuted,
    outline = KrColors.DarkBorder,
    outlineVariant = KrColors.DarkBorder,
    error = Color(0xFFFF8A80),
)

private val KrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Spacing scale from the design tokens. */
@Immutable
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val gutter = 20.dp
}

@Immutable
data class KrExtendedColors(val heroStart: Color, val heroEnd: Color, val success: Color)

val LocalKrColors = staticCompositionLocalOf {
    KrExtendedColors(KrColors.Primary, KrColors.Accent, KrColors.Accent)
}

@Composable
fun KakumaTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalKrColors provides KrExtendedColors(KrColors.Primary, KrColors.Accent, KrColors.Accent),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = KrTypography,
            shapes = KrShapes,
            content = content,
        )
    }
}
