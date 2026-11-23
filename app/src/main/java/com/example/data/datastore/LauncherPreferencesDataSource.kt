package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.GestureAction
import com.example.domain.model.IconShapeStyle
import com.example.domain.model.LauncherFontFamily
import com.example.domain.model.LauncherSettings
import com.example.domain.model.PageTransitionStyle
import com.example.domain.model.PrayerCalculationMethod
import com.example.domain.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "rawaq_launcher_settings"
)

class LauncherPreferencesDataSource(private val context: Context) {

  private object Keys {
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val GRID_ROWS = intPreferencesKey("grid_rows")
    val DOCK_ICON_COUNT = intPreferencesKey("dock_icon_count")

    val ICON_SIZE_DP = intPreferencesKey("icon_size_dp")
    val SHOW_ICON_LABELS = booleanPreferencesKey("show_icon_labels")
    val ICON_SHAPE = stringPreferencesKey("icon_shape")
    val LABEL_FONT_FAMILY = stringPreferencesKey("label_font_family")
    val LABEL_FONT_WEIGHT = intPreferencesKey("label_font_weight")
    val LABEL_FONT_SIZE_SP = floatPreferencesKey("label_font_size_sp")
    val LABEL_COLOR_HEX = stringPreferencesKey("label_color_hex")
    val LABEL_SHADOW_ENABLED = booleanPreferencesKey("label_shadow_enabled")

    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
    val WALLPAPER_BLUR_RADIUS = floatPreferencesKey("wallpaper_blur_radius")
    val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
    val WALLPAPER_DIM_AMOUNT = floatPreferencesKey("wallpaper_dim_amount")
    val USE_SYSTEM_WALLPAPER = booleanPreferencesKey("use_system_wallpaper")
    val PAGE_TRANSITION_STYLE = stringPreferencesKey("page_transition_style")

    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val FORCE_RTL = booleanPreferencesKey("force_rtl")

    val SHOW_TOP_CLOCK_WIDGET = booleanPreferencesKey("show_top_clock_widget")
    val USE_24_HOUR_CLOCK = booleanPreferencesKey("use_24_hour_clock")
    val SHOW_HIJRI_DATE = booleanPreferencesKey("show_hijri_date")
    val SHOW_WEATHER_BADGE = booleanPreferencesKey("show_weather_badge")
    val SHOW_PRAYER_BAR = booleanPreferencesKey("show_prayer_bar")
    val PRAYER_METHOD = stringPreferencesKey("prayer_method")
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
    val CITY_NAME = stringPreferencesKey("city_name")
    val PRAYER_ACCENT_COLOR_HEX = stringPreferencesKey("prayer_accent_color_hex")

    val DOUBLE_TAP_GESTURE = stringPreferencesKey("double_tap_gesture")
    val SWIPE_DOWN_GESTURE = stringPreferencesKey("swipe_down_gesture")
    val SWIPE_UP_GESTURE = stringPreferencesKey("swipe_up_gesture")
    val VAULT_ENABLED = booleanPreferencesKey("vault_enabled")
    val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
  }

  val settingsFlow: Flow<LauncherSettings> = context.launcherDataStore.data
    .catch { exception ->
      if (exception is IOException) {
        emit(emptyPreferences())
      } else {
        throw exception
      }
    }
    .map(::mapPreferencesToSettings)

  suspend fun getCurrentSettings(): LauncherSettings = settingsFlow.first()

  suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
    context.launcherDataStore.edit { prefs ->
      val current = mapPreferencesToSettings(prefs)
      val updated = transform(current)
      writeSettingsToPreferences(prefs, updated)
    }
  }

  suspend fun overwriteSettings(settings: LauncherSettings) {
    context.launcherDataStore.edit { prefs ->
      writeSettingsToPreferences(prefs, settings)
    }
  }

  suspend fun resetToDefaults() {
    overwriteSettings(LauncherSettings())
  }

  private fun mapPreferencesToSettings(prefs: Preferences): LauncherSettings {
    val defaults = LauncherSettings()
    return LauncherSettings(
      gridColumns = (prefs[Keys.GRID_COLUMNS] ?: defaults.gridColumns).coerceIn(4, 6),
      gridRows = (prefs[Keys.GRID_ROWS] ?: defaults.gridRows).coerceIn(5, 7),
      dockIconCount = (prefs[Keys.DOCK_ICON_COUNT] ?: defaults.dockIconCount).coerceIn(4, 7),
      iconSizeDp = (prefs[Keys.ICON_SIZE_DP] ?: defaults.iconSizeDp).coerceIn(44, 68),
      showIconLabels = prefs[Keys.SHOW_ICON_LABELS] ?: defaults.showIconLabels,
      iconShape = enumOrDefault(prefs[Keys.ICON_SHAPE], defaults.iconShape),
      labelFontFamily = enumOrDefault(prefs[Keys.LABEL_FONT_FAMILY], defaults.labelFontFamily),
      labelFontWeight = (prefs[Keys.LABEL_FONT_WEIGHT] ?: defaults.labelFontWeight).coerceIn(300, 700),
      labelFontSizeSp = (prefs[Keys.LABEL_FONT_SIZE_SP] ?: defaults.labelFontSizeSp).coerceIn(9f, 18f),
      labelColorHex = prefs[Keys.LABEL_COLOR_HEX] ?: defaults.labelColorHex,
      labelShadowEnabled = prefs[Keys.LABEL_SHADOW_ENABLED] ?: defaults.labelShadowEnabled,
      themeMode = enumOrDefault(prefs[Keys.THEME_MODE], defaults.themeMode),
      accentColorHex = prefs[Keys.ACCENT_COLOR_HEX] ?: defaults.accentColorHex,
      wallpaperBlurRadius = (prefs[Keys.WALLPAPER_BLUR_RADIUS] ?: defaults.wallpaperBlurRadius).coerceIn(0f, 40f),
      glassOpacity = (prefs[Keys.GLASS_OPACITY] ?: defaults.glassOpacity).coerceIn(0.08f, 0.75f),
      wallpaperDimAmount = (prefs[Keys.WALLPAPER_DIM_AMOUNT] ?: defaults.wallpaperDimAmount).coerceIn(0f, 0.7f),
      useSystemWallpaper = prefs[Keys.USE_SYSTEM_WALLPAPER] ?: defaults.useSystemWallpaper,
      pageTransitionStyle = enumOrDefault(prefs[Keys.PAGE_TRANSITION_STYLE], defaults.pageTransitionStyle),
      appLanguage = enumOrDefault(prefs[Keys.APP_LANGUAGE], defaults.appLanguage),
      forceRtl = prefs[Keys.FORCE_RTL] ?: defaults.forceRtl,
      showTopClockWidget = prefs[Keys.SHOW_TOP_CLOCK_WIDGET] ?: defaults.showTopClockWidget,
      use24HourClock = prefs[Keys.USE_24_HOUR_CLOCK] ?: defaults.use24HourClock,
      showHijriDate = prefs[Keys.SHOW_HIJRI_DATE] ?: defaults.showHijriDate,
      showWeatherBadge = prefs[Keys.SHOW_WEATHER_BADGE] ?: defaults.showWeatherBadge,
      showPrayerBar = prefs[Keys.SHOW_PRAYER_BAR] ?: defaults.showPrayerBar,
      prayerCalculationMethod = enumOrDefault(prefs[Keys.PRAYER_METHOD], defaults.prayerCalculationMethod),
      latitude = prefs[Keys.LATITUDE] ?: defaults.latitude,
      longitude = prefs[Keys.LONGITUDE] ?: defaults.longitude,
      cityName = prefs[Keys.CITY_NAME] ?: defaults.cityName,
      prayerAccentColorHex = prefs[Keys.PRAYER_ACCENT_COLOR_HEX] ?: defaults.prayerAccentColorHex,
      doubleTapGesture = enumOrDefault(prefs[Keys.DOUBLE_TAP_GESTURE], defaults.doubleTapGesture),
      swipeDownGesture = enumOrDefault(prefs[Keys.SWIPE_DOWN_GESTURE], defaults.swipeDownGesture),
      swipeUpGesture = enumOrDefault(prefs[Keys.SWIPE_UP_GESTURE], defaults.swipeUpGesture),
      vaultEnabled = prefs[Keys.VAULT_ENABLED] ?: defaults.vaultEnabled,
      vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: defaults.vaultPinHash,
    )
  }

  private fun writeSettingsToPreferences(
    prefs: androidx.datastore.preferences.core.MutablePreferences,
    settings: LauncherSettings,
  ) {
    prefs[Keys.GRID_COLUMNS] = settings.gridColumns.coerceIn(4, 6)
    prefs[Keys.GRID_ROWS] = settings.gridRows.coerceIn(5, 7)
    prefs[Keys.DOCK_ICON_COUNT] = settings.dockIconCount.coerceIn(4, 7)

    prefs[Keys.ICON_SIZE_DP] = settings.iconSizeDp.coerceIn(44, 68)
    prefs[Keys.SHOW_ICON_LABELS] = settings.showIconLabels
    prefs[Keys.ICON_SHAPE] = settings.iconShape.name
    prefs[Keys.LABEL_FONT_FAMILY] = settings.labelFontFamily.name
    prefs[Keys.LABEL_FONT_WEIGHT] = settings.labelFontWeight.coerceIn(300, 700)
    prefs[Keys.LABEL_FONT_SIZE_SP] = settings.labelFontSizeSp.coerceIn(9f, 18f)
    prefs[Keys.LABEL_COLOR_HEX] = settings.labelColorHex
    prefs[Keys.LABEL_SHADOW_ENABLED] = settings.labelShadowEnabled

    prefs[Keys.THEME_MODE] = settings.themeMode.name
    prefs[Keys.ACCENT_COLOR_HEX] = settings.accentColorHex
    prefs[Keys.WALLPAPER_BLUR_RADIUS] = settings.wallpaperBlurRadius.coerceIn(0f, 40f)
    prefs[Keys.GLASS_OPACITY] = settings.glassOpacity.coerceIn(0.08f, 0.75f)
    prefs[Keys.WALLPAPER_DIM_AMOUNT] = settings.wallpaperDimAmount.coerceIn(0f, 0.7f)
    prefs[Keys.USE_SYSTEM_WALLPAPER] = settings.useSystemWallpaper
    prefs[Keys.PAGE_TRANSITION_STYLE] = settings.pageTransitionStyle.name

    prefs[Keys.APP_LANGUAGE] = settings.appLanguage.name
    prefs[Keys.FORCE_RTL] = settings.forceRtl

    prefs[Keys.SHOW_TOP_CLOCK_WIDGET] = settings.showTopClockWidget
    prefs[Keys.USE_24_HOUR_CLOCK] = settings.use24HourClock
    prefs[Keys.SHOW_HIJRI_DATE] = settings.showHijriDate
    prefs[Keys.SHOW_WEATHER_BADGE] = settings.showWeatherBadge
    prefs[Keys.SHOW_PRAYER_BAR] = settings.showPrayerBar
    prefs[Keys.PRAYER_METHOD] = settings.prayerCalculationMethod.name
    prefs[Keys.LATITUDE] = settings.latitude
    prefs[Keys.LONGITUDE] = settings.longitude
    prefs[Keys.CITY_NAME] = settings.cityName
    prefs[Keys.PRAYER_ACCENT_COLOR_HEX] = settings.prayerAccentColorHex

    prefs[Keys.DOUBLE_TAP_GESTURE] = settings.doubleTapGesture.name
    prefs[Keys.SWIPE_DOWN_GESTURE] = settings.swipeDownGesture.name
    prefs[Keys.SWIPE_UP_GESTURE] = settings.swipeUpGesture.name
    prefs[Keys.VAULT_ENABLED] = settings.vaultEnabled
    prefs[Keys.VAULT_PIN_HASH] = settings.vaultPinHash
  }

  private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
    if (raw == null) return default
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
  }
}
