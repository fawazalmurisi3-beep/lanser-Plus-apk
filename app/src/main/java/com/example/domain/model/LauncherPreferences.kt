package com.example.domain.model

enum class ThemeMode {
  LIQUID_GLASS,
  DARK_FROSTED,
  LIGHT_AIR,
  MATERIAL_YOU,
}

enum class PageTransitionStyle {
  CUBE,
  STACK,
  DEPTH,
  FADE,
  ACCORDION,
  PARALLAX,
}

enum class IconShapeStyle {
  SQUIRCLE,
  CIRCLE,
  ROUNDED_SQUARE,
  TEARDROP,
  SYSTEM,
}

enum class LauncherFontFamily {
  CAIRO,
  TAJAWAL,
  PLUS_JAKARTA,
  JETBRAINS_MONO,
}

enum class GestureAction {
  NONE,
  EXPAND_NOTIFICATIONS,
  OPEN_APP_DRAWER,
  OPEN_SEARCH,
  LOCK_SCREEN,
  OPEN_SETTINGS,
  TOGGLE_FLOATING_DOCK,
}

enum class AppLanguage {
  SYSTEM,
  AR,
  EN,
}

enum class PrayerCalculationMethod(val fajrAngle: Double, val ishaAngle: Double) {
  UMM_AL_QURA(18.5, 19.0),
  MWL(18.0, 17.0),
  EGYPT(19.5, 17.5),
  ISNA(15.0, 15.0),
  KARACHI(18.0, 18.0),
}

data class LauncherSettings(
  // Grid & Dock
  val gridColumns: Int = 4,
  val gridRows: Int = 6,
  val dockIconCount: Int = 5,
  // Icons & Typography
  val iconSizeDp: Int = 56,
  val showIconLabels: Boolean = true,
  val iconShape: IconShapeStyle = IconShapeStyle.SQUIRCLE,
  val labelFontFamily: LauncherFontFamily = LauncherFontFamily.CAIRO,
  val labelFontWeight: Int = 500,
  val labelFontSizeSp: Float = 12f,
  val labelColorHex: String = "#FFF8FAFC",
  val labelShadowEnabled: Boolean = true,
  // Visual Aesthetic & Blur
  val themeMode: ThemeMode = ThemeMode.LIQUID_GLASS,
  val accentColorHex: String = "#FF38BDF8",
  val wallpaperBlurRadius: Float = 20f,
  val glassOpacity: Float = 0.24f,
  val wallpaperDimAmount: Float = 0.18f,
  val useSystemWallpaper: Boolean = true,
  val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.PARALLAX,
  // Locale & RTL
  val appLanguage: AppLanguage = AppLanguage.AR,
  val forceRtl: Boolean = true,
  // Top Clock & Prayer Widget
  val showTopClockWidget: Boolean = true,
  val use24HourClock: Boolean = false,
  val showHijriDate: Boolean = true,
  val showWeatherBadge: Boolean = true,
  val showPrayerBar: Boolean = true,
  val prayerCalculationMethod: PrayerCalculationMethod = PrayerCalculationMethod.UMM_AL_QURA,
  val latitude: Double = 24.7136,
  val longitude: Double = 46.6753,
  val cityName: String = "الرياض",
  val prayerAccentColorHex: String = "#FFF59E0B",
  // Gestures & Vault Security
  val doubleTapGesture: GestureAction = GestureAction.OPEN_SEARCH,
  val swipeDownGesture: GestureAction = GestureAction.EXPAND_NOTIFICATIONS,
  val swipeUpGesture: GestureAction = GestureAction.OPEN_APP_DRAWER,
  val vaultEnabled: Boolean = false,
  val vaultPinHash: String = "",
)
