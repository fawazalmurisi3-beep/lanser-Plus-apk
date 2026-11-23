package com.example.domain.model

import android.content.ComponentName
import android.os.UserHandle

enum class HomeItemType {
  APP_SHORTCUT,
  FOLDER,
  APP_WIDGET,
  BUILTIN_WIDGET,
  DEEP_SHORTCUT,
}

enum class BuiltInWidgetType {
  CLOCK_PRAYER_BAR,
  QUICK_WEATHER_DATE,
  FLOATING_TOOLS_CARD,
}

enum class FolderDisplayMode(val defaultSpanX: Int, val defaultSpanY: Int) {
  COMPACT_1X1(1, 1),
  WIDE_2X1(2, 1),
  TALL_1X2(1, 2),
  BENTO_2X2(2, 2),
  BENTO_2X3(2, 3),
  BANNER_4X2(4, 2),
}

enum class AppCategory(val titleEn: String, val titleAr: String) {
  ALL("All", "الكل"),
  COMMUNICATION("Communication", "التواصل"),
  TOOLS("Tools", "الأدوات"),
  SHOPPING("Shopping", "التسوق"),
  GAMES("Games", "الألعاب"),
  ESSENTIALS("Essentials", "الأساسيات"),
  MEDIA("Media", "الوسائط"),
  PRODUCTIVITY("Productivity", "الإنتاجية"),
}

data class GridPosition(
  val page: Int,
  val cellX: Int,
  val cellY: Int,
  val spanX: Int = 1,
  val spanY: Int = 1,
)

data class InstalledApp(
  val packageName: String,
  val activityName: String,
  val label: String,
  val customLabel: String? = null,
  val category: AppCategory = AppCategory.TOOLS,
  val userSerialNumber: Long = 0L,
  val userHandle: UserHandle? = null,
  val isWorkProfile: Boolean = false,
  val isSystemApp: Boolean = false,
  val isHidden: Boolean = false,
  val isPinnedInDrawer: Boolean = false,
  val customIconShape: IconShapeStyle? = null,
  val badgeCount: Int = 0,
  val launchCount: Int = 0,
  val lastUpdatedTime: Long = 0L,
) {
  val displayLabel: String
    get() = customLabel?.takeIf { it.isNotBlank() } ?: label

  val componentName: ComponentName
    get() = ComponentName(packageName, activityName)

  val uniqueKey: String
    get() = "$packageName/$activityName#$userSerialNumber"
}

data class AppShortcutInfo(
  val id: String,
  val packageName: String,
  val shortLabel: String,
  val longLabel: String,
  val userSerialNumber: Long,
  val isDynamic: Boolean,
  val rank: Int,
)

sealed interface HomeScreenItem {
  val id: Long
  val type: HomeItemType
  val position: GridPosition
  val isDocked: Boolean
  val dockOrder: Int

  data class AppShortcut(
    override val id: Long,
    override val position: GridPosition,
    override val isDocked: Boolean = false,
    override val dockOrder: Int = -1,
    val packageName: String,
    val activityName: String,
    val label: String,
    val customLabel: String? = null,
    val userSerialNumber: Long = 0L,
    val isWorkProfile: Boolean = false,
    val copyInstanceId: String = "",
    val badgeCount: Int = 0,
  ) : HomeScreenItem {
    override val type: HomeItemType = HomeItemType.APP_SHORTCUT
    val displayLabel: String
      get() = customLabel?.takeIf { it.isNotBlank() } ?: label
  }

  data class DeepShortcut(
    override val id: Long,
    override val position: GridPosition,
    override val isDocked: Boolean = false,
    override val dockOrder: Int = -1,
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val userSerialNumber: Long = 0L,
  ) : HomeScreenItem {
    override val type: HomeItemType = HomeItemType.DEEP_SHORTCUT
  }

  data class Folder(
    override val id: Long,
    override val position: GridPosition,
    override val isDocked: Boolean = false,
    override val dockOrder: Int = -1,
    val title: String,
    val displayMode: FolderDisplayMode = FolderDisplayMode.COMPACT_1X1,
    val accentColorHex: String? = null,
    val children: List<AppShortcut> = emptyList(),
  ) : HomeScreenItem {
    override val type: HomeItemType = HomeItemType.FOLDER
    val totalBadgeCount: Int
      get() = children.sumOf { it.badgeCount }
  }

  data class SystemAppWidget(
    override val id: Long,
    override val position: GridPosition,
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
  ) : HomeScreenItem {
    override val type: HomeItemType = HomeItemType.APP_WIDGET
    override val isDocked: Boolean = false
    override val dockOrder: Int = -1
  }

  data class BuiltInWidget(
    override val id: Long,
    override val position: GridPosition,
    val widgetType: BuiltInWidgetType,
  ) : HomeScreenItem {
    override val type: HomeItemType = HomeItemType.BUILTIN_WIDGET
    override val isDocked: Boolean = false
    override val dockOrder: Int = -1
  }
}

enum class PrayerSlotId {
  FAJR,
  SUNRISE,
  DHUHR,
  ASR,
  MAGHRIB,
  ISHA,
  MIDNIGHT,
}

data class PrayerTimeSlot(
  val id: PrayerSlotId,
  val nameEn: String,
  val nameAr: String,
  val formattedTime: String,
  val epochMillis: Long,
  val isCurrent: Boolean,
  val isNext: Boolean,
)

data class DailyScheduleState(
  val formattedClock: String,
  val amPmLabel: String,
  val gregorianDate: String,
  val hijriDate: String,
  val cityName: String,
  val temperatureLabel: String,
  val slots: List<PrayerTimeSlot>,
)

enum class FloatingMiniAppType {
  BROWSER,
  NOTES,
  CALCULATOR,
  QUICK_APPS,
}

data class FloatingNote(
  val id: Long = 0L,
  val title: String,
  val content: String,
  val updatedAt: Long = System.currentTimeMillis(),
)
