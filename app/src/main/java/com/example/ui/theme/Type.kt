package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.LauncherFontFamily

val CairoFontFamily = FontFamily(Font(R.font.cairo, FontWeight.Normal))
val TajawalFontFamily = FontFamily(Font(R.font.tajawal, FontWeight.Normal))
val PlusJakartaFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

fun LauncherFontFamily.toComposeFontFamily(): FontFamily = when (this) {
  LauncherFontFamily.CAIRO -> CairoFontFamily
  LauncherFontFamily.TAJAWAL -> TajawalFontFamily
  LauncherFontFamily.PLUS_JAKARTA -> PlusJakartaFontFamily
  LauncherFontFamily.JETBRAINS_MONO -> JetBrainsMonoFontFamily
}

fun Int.toComposeFontWeight(): FontWeight = when {
  this <= 350 -> FontWeight.Light
  this <= 450 -> FontWeight.Normal
  this <= 550 -> FontWeight.Medium
  this <= 650 -> FontWeight.SemiBold
  else -> FontWeight.Bold
}

fun buildLauncherTypography(fontFamily: FontFamily): Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 54.sp,
    lineHeight = 60.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = fontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
  ),
)
