package com.mamer.steptrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryGreenDark,
    secondary = PrimaryDarkGreenDark,
    tertiary = AccentOrangeDark,
    background = PhoneBgDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextMainDark,
    onSurface = TextMainDark,
    surfaceVariant = SurfaceSoftDark,
    onSurfaceVariant = TextMutedDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryGreen,
    secondary = PrimaryDarkGreen,
    tertiary = AccentOrange,
    background = PhoneBgLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMainLight,
    onSurface = TextMainLight,
    surfaceVariant = SurfaceSoftLight,
    onSurfaceVariant = TextMutedLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color disabled to ensure consistent custom branding as requested
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme, content = content)
  }
}
