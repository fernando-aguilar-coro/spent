package com.app.spent.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
private val DarkColorScheme = darkColorScheme(
primary = SageGreenMedium,
onPrimary = Color.White,
primaryContainer = SageGreenDark,
onPrimaryContainer = SageGreenLight,
secondary = AccentSlate,
onSecondary = Color.White,
secondaryContainer = CharcoalSurfaceVariant,
onSecondaryContainer = CharcoalTextPrimary,
tertiary = AccentAmber,
onTertiary = Color.Black,
background = CharcoalBackgroundDark,
onBackground = CharcoalTextPrimary,
surface = CharcoalSurfaceDark,
onSurface = CharcoalTextPrimary,
surfaceVariant = CharcoalSurfaceVariant,
onSurfaceVariant = CharcoalTextSecondary,
outline = CharcoalBorder
)

private val LightColorScheme = lightColorScheme(
primary = SageGreenPrimary,
onPrimary = Color.White,
primaryContainer = SageGreenLight,
onPrimaryContainer = SageGreenDark,
secondary = AccentSlate,
onSecondary = Color.White,
secondaryContainer = SoftGraySurfaceVariant,
onSecondaryContainer = SoftGrayTextPrimary,
tertiary = AccentAmber,
onTertiary = Color.White,
background = SoftGrayBackgroundLight,
onBackground = SoftGrayTextPrimary,
surface = SoftGraySurfaceLight,
onSurface = SoftGrayTextPrimary,
surfaceVariant = SoftGraySurfaceVariant,
onSurfaceVariant = SoftGrayTextSecondary,
outline = SoftGrayBorder
)

@Composable
fun SpentTheme(
darkTheme: Boolean = isSystemInDarkTheme(),
dynamicColor: Boolean = false,
content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(
  colorScheme = colorScheme,
  typography = Typography,
  content = content
  )
}
