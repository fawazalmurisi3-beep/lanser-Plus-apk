package com.example.data.backup

import com.example.data.local.entity.AppCustomizationEntity
import com.example.data.local.entity.FloatingNoteEntity
import com.example.data.local.entity.HomeItemEntity
import com.example.domain.model.AppLanguage
import com.example.domain.model.GestureAction
import com.example.domain.model.IconShapeStyle
import com.example.domain.model.LauncherFontFamily
import com.example.domain.model.LauncherSettings
import com.example.domain.model.PageTransitionStyle
import com.example.domain.model.PrayerCalculationMethod
import com.example.domain.model.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class LauncherBackupArchive(
  val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
  val exportedAtEpochMs: Long = System.currentTimeMillis(),
  val settings: SerializableLauncherSettings,
  val homeItems: List<SerializableHomeItem>,
  val appCustomizations: List<SerializableAppCustomization>,
  val floatingNotes: List<SerializableFloatingNote> = emptyList(),
) {
  companion object {
    const val CURRENT_SCHEMA_VERSION = 1
  }
}

@Serializable
data class SerializableLauncherSettings(
  val gridColumns: Int = 4,
  val gridRows: Int = 6,
  val dockIconCount: Int = 5,
  val iconSizeDp: Int = 56,
  val showIconLabels: Boolean = true,
  val iconShape: String = IconShapeStyle.SQUIRCLE.name,
  val labelFontFamily: String = LauncherFontFamily.CAIRO.name,
  val labelFontWeight: Int = 500,
  val labelFontSizeSp: Float = 12f,
  val labelColorHex: String = "#FFF8FAFC",
  val labelShadowEnabled: Boolean = true,
  val themeMode: String = ThemeMode.LIQUID_GLASS.name,
  val accentColorHex: String = "#FF38BDF8",
  val wallpaperBlurRadius: Float = 20f,
  val glassOpacity: Float = 0.24f,
  val wallpaperDimAmount: Float = 0.18f,
  val useSystemWallpaper: Boolean = true,
  val pageTransitionStyle: String = PageTransitionStyle.PARALLAX.name,
  val appLanguage: String = AppLanguage.AR.name,
  val forceRtl: Boolean = true,
  val showTopClockWidget: Boolean = true,
  val use24HourClock: Boolean = false,
  val showHijriDate: Boolean = true,
  val showWeatherBadge: Boolean = true,
  val showPrayerBar: Boolean = true,
  val prayerCalculationMethod: String = PrayerCalculationMethod.UMM_AL_QURA.name,
  val latitude: Double = 24.7136,
  val longitude: Double = 46.6753,
  val cityName: String = "الرياض",
  val prayerAccentColorHex: String = "#FFF59E0B",
  val doubleTapGesture: String = GestureAction.OPEN_SEARCH.name,
  val swipeDownGesture: String = GestureAction.EXPAND_NOTIFICATIONS.name,
  val swipeUpGesture: String = GestureAction.OPEN_APP_DRAWER.name,
  val vaultEnabled: Boolean = false,
)

@Serializable
data class SerializableHomeItem(
  val id: Long,
  val itemType: String,
  val page: Int,
  val cellX: Int,
  val cellY: Int,
  val spanX: Int,
  val spanY: Int,
  val isDocked: Boolean,
  val dockOrder: Int,
  val folderId: Long?,
  val folderOrder: Int,
  val folderTitle: String,
  val folderDisplayMode: String,
  val accentColorHex: String?,
  val packageName: String,
  val activityName: String,
  val customLabel: String?,
  val userSerialNumber: Long,
  val copyInstanceId: String,
  val shortcutId: String?,
  val appWidgetId: Int,
  val widgetProviderPackage: String,
  val widgetProviderClass: String,
  val builtInWidgetType: String,
  val createdAt: Long,
)

@Serializable
data class SerializableAppCustomization(
  val componentKey: String,
  val packageName: String,
  val activityName: String,
  val userSerialNumber: Long,
  val customLabel: String?,
  val customCategory: String?,
  val isHidden: Boolean,
  val isPinnedInDrawer: Boolean,
  val customIconShape: String?,
  val launchCount: Int,
  val lastLaunchedAt: Long,
)

@Serializable
data class SerializableFloatingNote(
  val id: Long,
  val title: String,
  val content: String,
  val updatedAt: Long,
)

fun LauncherSettings.toSerializable(): SerializableLauncherSettings =
  SerializableLauncherSettings(
    gridColumns = gridColumns,
    gridRows = gridRows,
    dockIconCount = dockIconCount,
    iconSizeDp = iconSizeDp,
    showIconLabels = showIconLabels,
    iconShape = iconShape.name,
    labelFontFamily = labelFontFamily.name,
    labelFontWeight = labelFontWeight,
    labelFontSizeSp = labelFontSizeSp,
    labelColorHex = labelColorHex,
    labelShadowEnabled = labelShadowEnabled,
    themeMode = themeMode.name,
    accentColorHex = accentColorHex,
    wallpaperBlurRadius = wallpaperBlurRadius,
    glassOpacity = glassOpacity,
    wallpaperDimAmount = wallpaperDimAmount,
    useSystemWallpaper = useSystemWallpaper,
    pageTransitionStyle = pageTransitionStyle.name,
    appLanguage = appLanguage.name,
    forceRtl = forceRtl,
    showTopClockWidget = showTopClockWidget,
    use24HourClock = use24HourClock,
    showHijriDate = showHijriDate,
    showWeatherBadge = showWeatherBadge,
    showPrayerBar = showPrayerBar,
    prayerCalculationMethod = prayerCalculationMethod.name,
    latitude = latitude,
    longitude = longitude,
    cityName = cityName,
    prayerAccentColorHex = prayerAccentColorHex,
    doubleTapGesture = doubleTapGesture.name,
    swipeDownGesture = swipeDownGesture.name,
    swipeUpGesture = swipeUpGesture.name,
    vaultEnabled = vaultEnabled,
  )

fun SerializableLauncherSettings.toDomain(existingPinHash: String): LauncherSettings {
  val d = LauncherSettings()
  return LauncherSettings(
    gridColumns = gridColumns.coerceIn(4, 6),
    gridRows = gridRows.coerceIn(5, 7),
    dockIconCount = dockIconCount.coerceIn(4, 7),
    iconSizeDp = iconSizeDp.coerceIn(44, 68),
    showIconLabels = showIconLabels,
    iconShape = enumOrDefault(iconShape, d.iconShape),
    labelFontFamily = enumOrDefault(labelFontFamily, d.labelFontFamily),
    labelFontWeight = labelFontWeight.coerceIn(300, 700),
    labelFontSizeSp = labelFontSizeSp.coerceIn(9f, 18f),
    labelColorHex = labelColorHex,
    labelShadowEnabled = labelShadowEnabled,
    themeMode = enumOrDefault(themeMode, d.themeMode),
    accentColorHex = accentColorHex,
    wallpaperBlurRadius = wallpaperBlurRadius.coerceIn(0f, 40f),
    glassOpacity = glassOpacity.coerceIn(0.08f, 0.75f),
    wallpaperDimAmount = wallpaperDimAmount.coerceIn(0f, 0.7f),
    useSystemWallpaper = useSystemWallpaper,
    pageTransitionStyle = enumOrDefault(pageTransitionStyle, d.pageTransitionStyle),
    appLanguage = enumOrDefault(appLanguage, d.appLanguage),
    forceRtl = forceRtl,
    showTopClockWidget = showTopClockWidget,
    use24HourClock = use24HourClock,
    showHijriDate = showHijriDate,
    showWeatherBadge = showWeatherBadge,
    showPrayerBar = showPrayerBar,
    prayerCalculationMethod = enumOrDefault(prayerCalculationMethod, d.prayerCalculationMethod),
    latitude = latitude.coerceIn(-90.0, 90.0),
    longitude = longitude.coerceIn(-180.0, 180.0),
    cityName = cityName,
    prayerAccentColorHex = prayerAccentColorHex,
    doubleTapGesture = enumOrDefault(doubleTapGesture, d.doubleTapGesture),
    swipeDownGesture = enumOrDefault(swipeDownGesture, d.swipeDownGesture),
    swipeUpGesture = enumOrDefault(swipeUpGesture, d.swipeUpGesture),
    vaultEnabled = vaultEnabled && existingPinHash.isNotBlank(),
    vaultPinHash = existingPinHash,
  )
}

fun HomeItemEntity.toSerializable(): SerializableHomeItem =
  SerializableHomeItem(
    id = id,
    itemType = itemType,
    page = page,
    cellX = cellX,
    cellY = cellY,
    spanX = spanX,
    spanY = spanY,
    isDocked = isDocked,
    dockOrder = dockOrder,
    folderId = folderId,
    folderOrder = folderOrder,
    folderTitle = folderTitle,
    folderDisplayMode = folderDisplayMode,
    accentColorHex = accentColorHex,
    packageName = packageName,
    activityName = activityName,
    customLabel = customLabel,
    userSerialNumber = userSerialNumber,
    copyInstanceId = copyInstanceId,
    shortcutId = shortcutId,
    appWidgetId = appWidgetId,
    widgetProviderPackage = widgetProviderPackage,
    widgetProviderClass = widgetProviderClass,
    builtInWidgetType = builtInWidgetType,
    createdAt = createdAt,
  )

fun SerializableHomeItem.toEntity(maxCols: Int, maxRows: Int): HomeItemEntity =
  HomeItemEntity(
    id = id,
    itemType = itemType,
    page = page.coerceAtLeast(0),
    cellX = cellX.coerceIn(0, (maxCols - 1).coerceAtLeast(0)),
    cellY = cellY.coerceIn(0, (maxRows - 1).coerceAtLeast(0)),
    spanX = spanX.coerceIn(1, maxCols),
    spanY = spanY.coerceIn(1, maxRows),
    isDocked = isDocked,
    dockOrder = dockOrder,
    folderId = folderId,
    folderOrder = folderOrder,
    folderTitle = folderTitle,
    folderDisplayMode = folderDisplayMode,
    accentColorHex = accentColorHex,
    packageName = packageName,
    activityName = activityName,
    customLabel = customLabel,
    userSerialNumber = userSerialNumber,
    copyInstanceId = copyInstanceId,
    shortcutId = shortcutId,
    appWidgetId = appWidgetId,
    widgetProviderPackage = widgetProviderPackage,
    widgetProviderClass = widgetProviderClass,
    builtInWidgetType = builtInWidgetType,
    createdAt = createdAt,
  )

fun AppCustomizationEntity.toSerializable(): SerializableAppCustomization =
  SerializableAppCustomization(
    componentKey = componentKey,
    packageName = packageName,
    activityName = activityName,
    userSerialNumber = userSerialNumber,
    customLabel = customLabel,
    customCategory = customCategory,
    isHidden = isHidden,
    isPinnedInDrawer = isPinnedInDrawer,
    customIconShape = customIconShape,
    launchCount = launchCount,
    lastLaunchedAt = lastLaunchedAt,
  )

fun SerializableAppCustomization.toEntity(): AppCustomizationEntity =
  AppCustomizationEntity(
    componentKey = componentKey,
    packageName = packageName,
    activityName = activityName,
    userSerialNumber = userSerialNumber,
    customLabel = customLabel,
    customCategory = customCategory,
    isHidden = isHidden,
    isPinnedInDrawer = isPinnedInDrawer,
    customIconShape = customIconShape,
    launchCount = launchCount,
    lastLaunchedAt = lastLaunchedAt,
  )

fun FloatingNoteEntity.toSerializable(): SerializableFloatingNote =
  SerializableFloatingNote(id = id, title = title, content = content, updatedAt = updatedAt)

fun SerializableFloatingNote.toEntity(): FloatingNoteEntity =
  FloatingNoteEntity(id = id, title = title, content = content, updatedAt = updatedAt)

private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, default: T): T =
  enumValues<T>().firstOrNull { it.name == raw } ?: default
