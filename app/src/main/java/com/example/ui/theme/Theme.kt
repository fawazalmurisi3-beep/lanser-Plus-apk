package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.domain.model.LauncherSettings
import com.example.domain.model.ThemeMode

@Composable
fun RawaqLauncherTheme(
  settings: LauncherSettings = LauncherSettings(),
  content: @Composable () -> Unit,
) {
  val accent = parseColorHex(settings.accentColorHex, GlassCyan)
  val prayerAccent = parseColorHex(settings.prayerAccentColorHex, WarmAmber)
  val systemDark = isSystemInDarkTheme()

  val colorScheme = when (settings.themeMode) {
    ThemeMode.LIQUID_GLASS -> darkColorScheme(
      primary = accent,
      onPrimary = MidnightSlate,
      secondary = prayerAccent,
      onSecondary = MidnightSlate,
      tertiary = EmeraldMint,
      background = MidnightSlate,
      onBackground = SoftCloud,
      surface = DeepNavy,
      onSurface = SoftCloud,
      surfaceVariant = FrostedInk,
      onSurfaceVariant = MutedSilver,
      outline = Color.White.copy(alpha = 0.24f),
    )
    ThemeMode.DARK_FROSTED -> darkColorScheme(
      primary = accent,
      onPrimary = Color.Black,
      secondary = prayerAccent,
      background = Color(0xFF05070E),
      onBackground = SoftCloud,
      surface = Color(0xFF111827),
      onSurface = SoftCloud,
      surfaceVariant = Color(0xFF1F2937),
      onSurfaceVariant = Color(0xFFCBD5E1),
      outline = Color.White.copy(alpha = 0.16f),
    )
    ThemeMode.LIGHT_AIR -> lightColorScheme(
      primary = accent,
      onPrimary = Color.White,
      secondary = prayerAccent,
      background = Color(0xFFF1F5F9),
      onBackground = DeepNavy,
      surface = Color.White,
      onSurface = DeepNavy,
      surfaceVariant = Color(0xFFE2E8F0),
      onSurfaceVariant = Color(0xFF475569),
      outline = DeepNavy.copy(alpha = 0.14f),
    )
    ThemeMode.MATERIAL_YOU -> {
      val context = LocalContext.current
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      } else {
        if (systemDark) {
          darkColorScheme(primary = accent, secondary = prayerAccent)
        } else {
          lightColorScheme(primary = accent, secondary = prayerAccent)
        }
      }
    }
  }

  val layoutDirection = if (settings.forceRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = buildLauncherTypography(fontFamily),
      content = content,
    )
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  RawaqLauncherTheme(
    settings = LauncherSettings(
      themeMode = if (dynamicColor) ThemeMode.MATERIAL_YOU else if (darkTheme) ThemeMode.DARK_FROSTED else ThemeMode.LIGHT_AIR,
    ),
    content = content,
  )
}
