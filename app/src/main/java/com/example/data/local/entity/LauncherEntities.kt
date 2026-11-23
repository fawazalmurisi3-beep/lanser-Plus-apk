package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AppCategory
import com.example.domain.model.BuiltInWidgetType
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.HomeItemType
import com.example.domain.model.IconShapeStyle

@Entity(
  tableName = "home_items",
  indices = [
    Index(value = ["page", "cellX", "cellY"]),
    Index(value = ["folderId"]),
    Index(value = ["isDocked", "dockOrder"]),
  ],
)
data class HomeItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val itemType: String = HomeItemType.APP_SHORTCUT.name,
  val page: Int = 0,
  val cellX: Int = 0,
  val cellY: Int = 0,
  val spanX: Int = 1,
  val spanY: Int = 1,
  val isDocked: Boolean = false,
  val dockOrder: Int = -1,
  val folderId: Long? = null,
  val folderOrder: Int = 0,
  val folderTitle: String = "",
  val folderDisplayMode: String = FolderDisplayMode.COMPACT_1X1.name,
  val accentColorHex: String? = null,
  val packageName: String = "",
  val activityName: String = "",
  val customLabel: String? = null,
  val userSerialNumber: Long = 0L,
  val copyInstanceId: String = "",
  val shortcutId: String? = null,
  val appWidgetId: Int = -1,
  val widgetProviderPackage: String = "",
  val widgetProviderClass: String = "",
  val builtInWidgetType: String = BuiltInWidgetType.CLOCK_PRAYER_BAR.name,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "app_customizations")
data class AppCustomizationEntity(
  @PrimaryKey val componentKey: String,
  val packageName: String,
  val activityName: String,
  val userSerialNumber: Long = 0L,
  val customLabel: String? = null,
  val customCategory: String? = null,
  val isHidden: Boolean = false,
  val isPinnedInDrawer: Boolean = false,
  val customIconShape: String? = null,
  val launchCount: Int = 0,
  val lastLaunchedAt: Long = 0L,
) {
  fun resolvedCategory(): AppCategory? =
    customCategory?.let { raw -> AppCategory.entries.firstOrNull { it.name == raw } }

  fun resolvedIconShape(): IconShapeStyle? =
    customIconShape?.let { raw -> IconShapeStyle.entries.firstOrNull { it.name == raw } }
}

@Entity(tableName = "floating_notes")
data class FloatingNoteEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val content: String,
  val updatedAt: Long = System.currentTimeMillis(),
)
