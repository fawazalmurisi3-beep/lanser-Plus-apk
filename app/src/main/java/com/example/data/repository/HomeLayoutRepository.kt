package com.example.data.repository

import com.example.data.datastore.LauncherPreferencesDataSource
import com.example.data.local.dao.FloatingNoteDao
import com.example.data.local.dao.HomeItemDao
import com.example.data.local.entity.FloatingNoteEntity
import com.example.data.local.entity.HomeItemEntity
import com.example.data.system.InstalledAppsRepository
import com.example.data.system.NotificationBadgeStore
import com.example.domain.model.BuiltInWidgetType
import com.example.domain.model.FloatingNote
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.GridPosition
import com.example.domain.model.HomeItemType
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.InstalledApp
import com.example.domain.model.LauncherSettings
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HomeLayoutRepository(
  private val homeItemDao: HomeItemDao,
  private val floatingNoteDao: FloatingNoteDao,
  private val installedAppsRepository: InstalledAppsRepository,
  private val preferencesDataSource: LauncherPreferencesDataSource,
) {

  val homeItemsFlow: Flow<List<HomeScreenItem>> = combine(
    homeItemDao.observeAllHomeItems(),
    installedAppsRepository.installedAppsFlow,
    NotificationBadgeStore.badgeCounts,
  ) { entities, installedApps, badges ->
    mapEntitiesToDomain(entities, installedApps, badges)
  }

  val floatingNotesFlow: Flow<List<FloatingNote>> = floatingNoteDao.observeAllNotes().map { list ->
    list.map { FloatingNote(id = it.id, title = it.title, content = it.content, updatedAt = it.updatedAt) }
  }

  suspend fun ensureInitialHomeLayoutIfEmpty() {
    if (homeItemDao.getItemCount() > 0) return

    val settings = preferencesDataSource.getCurrentSettings()
    val apps = installedAppsRepository.getInstalledAppsSnapshot()
    val cols = settings.gridColumns
    val isRtl = settings.forceRtl

    val seedItems = mutableListOf<HomeItemEntity>()

    // 1. Built-in Top Clock & Prayer Bar widget spanning top 2 rows on Page 0
    seedItems += HomeItemEntity(
      itemType = HomeItemType.BUILTIN_WIDGET.name,
      page = 0,
      cellX = 0,
      cellY = 0,
      spanX = cols,
      spanY = 2,
      builtInWidgetType = BuiltInWidgetType.CLOCK_PRAYER_BAR.name,
    )

    // Pick best real installed apps for dock & categorized folders
    fun pickApps(vararg keywords: String, exclude: Set<String> = emptySet()): List<InstalledApp> {
      val matched = apps.filter { app ->
        app.uniqueKey !in exclude && keywords.any { kw ->
          app.packageName.contains(kw, ignoreCase = true) || app.label.contains(kw, ignoreCase = true)
        }
      }
      return matched
    }

    val usedKeys = mutableSetOf<String>()

    // Dock apps (Phone, Messages, Browser, Camera, Settings)
    val preferredDock = listOf("dialer", "phone", "messaging", "mms", "chrome", "browser", "camera", "settings")
    val dockList = mutableListOf<InstalledApp>()
    for (kw in preferredDock) {
      if (dockList.size >= settings.dockIconCount.coerceAtMost(5)) break
      val found = apps.firstOrNull {
        it.uniqueKey !in usedKeys &&
          (it.packageName.contains(kw, true) || it.label.contains(kw, true))
      }
      if (found != null) {
        dockList += found
        usedKeys += found.uniqueKey
      }
    }
    for (app in apps) {
      if (dockList.size >= settings.dockIconCount.coerceAtMost(5)) break
      if (app.uniqueKey !in usedKeys) {
        dockList += app
        usedKeys += app.uniqueKey
      }
    }
    dockList.forEachIndexed { idx, app ->
      seedItems += HomeItemEntity(
        itemType = HomeItemType.APP_SHORTCUT.name,
        isDocked = true,
        dockOrder = idx,
        packageName = app.packageName,
        activityName = app.activityName,
        userSerialNumber = app.userSerialNumber,
      )
    }

    // Helper to create a Bento Smart Folder with matched + fallback apps
    suspend fun seedFolder(
      title: String,
      mode: FolderDisplayMode,
      page: Int,
      cellX: Int,
      cellY: Int,
      accentHex: String,
      keywords: Array<String>,
      maxChildren: Int = 6,
    ) {
      val folderId = homeItemDao.insertItem(
        HomeItemEntity(
          itemType = HomeItemType.FOLDER.name,
          page = page,
          cellX = cellX,
          cellY = cellY,
          spanX = mode.defaultSpanX.coerceAtMost(cols),
          spanY = mode.defaultSpanY,
          folderTitle = title,
          folderDisplayMode = mode.name,
          accentColorHex = accentHex,
        )
      )
      val matched = pickApps(*keywords, exclude = usedKeys).take(maxChildren).toMutableList()
      if (matched.size < 4) {
        for (app in apps) {
          if (matched.size >= 4) break
          if (app.uniqueKey !in usedKeys && matched.none { it.uniqueKey == app.uniqueKey }) {
            matched += app
          }
        }
      }
      matched.forEachIndexed { idx, app ->
        usedKeys += app.uniqueKey
        seedItems += HomeItemEntity(
          itemType = HomeItemType.APP_SHORTCUT.name,
          folderId = folderId,
          folderOrder = idx,
          packageName = app.packageName,
          activityName = app.activityName,
          userSerialNumber = app.userSerialNumber,
        )
      }
    }

    // 2. Signature Bento Folders on Page 0 (rows 2..5)
    seedFolder(
      title = if (isRtl) "التواصل الاجتماعي والترفيه" else "Social & Media",
      mode = FolderDisplayMode.BENTO_2X2,
      page = 0,
      cellX = 0,
      cellY = 2,
      accentHex = "#FF38BDF8",
      keywords = arrayOf("whatsapp", "telegram", "youtube", "instagram", "facebook", "music", "video", "contacts"),
    )
    seedFolder(
      title = "Google",
      mode = FolderDisplayMode.BENTO_2X2,
      page = 0,
      cellX = 2,
      cellY = 2,
      accentHex = "#FF10B981",
      keywords = arrayOf("google", "gmail", "maps", "drive", "photos", "youtube", "chrome", "play"),
    )
    seedFolder(
      title = if (isRtl) "الأدوات" else "Tools",
      mode = FolderDisplayMode.WIDE_2X1,
      page = 0,
      cellX = 0,
      cellY = 4,
      accentHex = "#FFF59E0B",
      keywords = arrayOf("clock", "calc", "calendar", "files", "documents", "settings", "recorder"),
    )
    seedFolder(
      title = if (isRtl) "التسوق" else "Shopping",
      mode = FolderDisplayMode.WIDE_2X1,
      page = 0,
      cellX = 2,
      cellY = 4,
      accentHex = "#FFF43F5E",
      keywords = arrayOf("amazon", "noon", "shop", "store", "vending", "wallet", "pay"),
    )

    // Remaining apps on row 5 of Page 0 & Page 1
    val remaining = apps.filter { it.uniqueKey !in usedKeys }.ifEmpty { apps }
    var idx = 0
    for (x in 0 until cols) {
      if (idx >= remaining.size) break
      val app = remaining[idx++]
      seedItems += HomeItemEntity(
        itemType = HomeItemType.APP_SHORTCUT.name,
        page = 0,
        cellX = x,
        cellY = 5,
        packageName = app.packageName,
        activityName = app.activityName,
        userSerialNumber = app.userSerialNumber,
      )
    }

    var page1Idx = 0
    for (y in 0 until settings.gridRows) {
      for (x in 0 until cols) {
        if (idx + page1Idx >= remaining.size || page1Idx >= cols * 3) break
        val app = remaining[idx + page1Idx++]
        seedItems += HomeItemEntity(
          itemType = HomeItemType.APP_SHORTCUT.name,
          page = 1,
          cellX = x,
          cellY = y,
          packageName = app.packageName,
          activityName = app.activityName,
          userSerialNumber = app.userSerialNumber,
        )
      }
    }

    homeItemDao.insertAll(seedItems)
  }

  suspend fun addAppToHomeScreen(
    app: InstalledApp,
    targetPage: Int = 0,
    cellX: Int? = null,
    cellY: Int? = null,
    folderId: Long? = null,
    isDuplicateCopy: Boolean = false,
  ): Long {
    val settings = preferencesDataSource.getCurrentSettings()
    if (folderId != null) {
      val children = homeItemDao.getFolderChildren(folderId)
      val entity = HomeItemEntity(
        itemType = HomeItemType.APP_SHORTCUT.name,
        folderId = folderId,
        folderOrder = children.size,
        packageName = app.packageName,
        activityName = app.activityName,
        customLabel = app.customLabel,
        userSerialNumber = app.userSerialNumber,
        copyInstanceId = if (isDuplicateCopy) UUID.randomUUID().toString() else "",
      )
      return homeItemDao.insertItem(entity)
    }

    val (resolvedPage, resolvedX, resolvedY) = if (cellX != null && cellY != null) {
      Triple(targetPage, cellX, cellY)
    } else {
      findNextAvailableCell(targetPage, settings)
    }

    val entity = HomeItemEntity(
      itemType = HomeItemType.APP_SHORTCUT.name,
      page = resolvedPage,
      cellX = resolvedX,
      cellY = resolvedY,
      packageName = app.packageName,
      activityName = app.activityName,
      customLabel = app.customLabel,
      userSerialNumber = app.userSerialNumber,
      copyInstanceId = if (isDuplicateCopy) UUID.randomUUID().toString() else "",
    )
    return homeItemDao.insertItem(entity)
  }

  suspend fun cloneHomeShortcut(sourceItemId: Long, targetPage: Int? = null, targetFolderId: Long? = null): Long? {
    val source = homeItemDao.getHomeItemById(sourceItemId) ?: return null
    val settings = preferencesDataSource.getCurrentSettings()
    return if (targetFolderId != null) {
      val children = homeItemDao.getFolderChildren(targetFolderId)
      homeItemDao.insertItem(
        source.copy(
          id = 0L,
          isDocked = false,
          dockOrder = -1,
          folderId = targetFolderId,
          folderOrder = children.size,
          copyInstanceId = UUID.randomUUID().toString(),
          createdAt = System.currentTimeMillis(),
        )
      )
    } else {
      val (page, x, y) = findNextAvailableCell(targetPage ?: source.page, settings)
      homeItemDao.insertItem(
        source.copy(
          id = 0L,
          page = page,
          cellX = x,
          cellY = y,
          isDocked = false,
          dockOrder = -1,
          folderId = null,
          folderOrder = 0,
          copyInstanceId = UUID.randomUUID().toString(),
          createdAt = System.currentTimeMillis(),
        )
      )
    }
  }

  suspend fun addDeepShortcutToHome(
    packageName: String,
    shortcutId: String,
    label: String,
    userSerialNumber: Long,
    targetPage: Int = 0,
  ): Long {
    val settings = preferencesDataSource.getCurrentSettings()
    val (page, x, y) = findNextAvailableCell(targetPage, settings)
    return homeItemDao.insertItem(
      HomeItemEntity(
        itemType = HomeItemType.DEEP_SHORTCUT.name,
        page = page,
        cellX = x,
        cellY = y,
        packageName = packageName,
        shortcutId = shortcutId,
        customLabel = label,
        userSerialNumber = userSerialNumber,
      )
    )
  }

  suspend fun addSystemAppWidget(
    appWidgetId: Int,
    providerPackage: String,
    providerClass: String,
    spanX: Int,
    spanY: Int,
    targetPage: Int = 0,
  ): Long {
    val settings = preferencesDataSource.getCurrentSettings()
    val clampedSpanX = spanX.coerceIn(1, settings.gridColumns)
    val clampedSpanY = spanY.coerceIn(1, settings.gridRows)
    val (page, x, y) = findNextAvailableSpan(targetPage, clampedSpanX, clampedSpanY, settings)
    return homeItemDao.insertItem(
      HomeItemEntity(
        itemType = HomeItemType.APP_WIDGET.name,
        page = page,
        cellX = x,
        cellY = y,
        spanX = clampedSpanX,
        spanY = clampedSpanY,
        appWidgetId = appWidgetId,
        widgetProviderPackage = providerPackage,
        widgetProviderClass = providerClass,
      )
    )
  }

  suspend fun addBuiltInWidget(
    widgetType: BuiltInWidgetType,
    spanX: Int,
    spanY: Int,
    targetPage: Int = 0,
  ): Long {
    val settings = preferencesDataSource.getCurrentSettings()
    val clampedSpanX = spanX.coerceIn(1, settings.gridColumns)
    val clampedSpanY = spanY.coerceIn(1, settings.gridRows)
    val (page, x, y) = findNextAvailableSpan(targetPage, clampedSpanX, clampedSpanY, settings)
    return homeItemDao.insertItem(
      HomeItemEntity(
        itemType = HomeItemType.BUILTIN_WIDGET.name,
        page = page,
        cellX = x,
        cellY = y,
        spanX = clampedSpanX,
        spanY = clampedSpanY,
        builtInWidgetType = widgetType.name,
      )
    )
  }

  suspend fun createFolderFromItems(
    firstItemId: Long,
    secondItemId: Long,
    title: String,
    mode: FolderDisplayMode = FolderDisplayMode.COMPACT_1X1,
  ): Long? {
    val first = homeItemDao.getHomeItemById(firstItemId) ?: return null
    val second = homeItemDao.getHomeItemById(secondItemId) ?: return null

    val folderId = homeItemDao.insertItem(
      HomeItemEntity(
        itemType = HomeItemType.FOLDER.name,
        page = first.page,
        cellX = first.cellX,
        cellY = first.cellY,
        spanX = mode.defaultSpanX,
        spanY = mode.defaultSpanY,
        isDocked = first.isDocked,
        dockOrder = first.dockOrder,
        folderTitle = title,
        folderDisplayMode = mode.name,
      )
    )

    homeItemDao.updateItemPlacement(
      id = first.id,
      page = 0,
      cellX = 0,
      cellY = 0,
      isDocked = false,
      dockOrder = -1,
      folderId = folderId,
      folderOrder = 0,
    )
    homeItemDao.updateItemPlacement(
      id = second.id,
      page = 0,
      cellX = 0,
      cellY = 0,
      isDocked = false,
      dockOrder = -1,
      folderId = folderId,
      folderOrder = 1,
    )
    return folderId
  }

  suspend fun createSmartFolder(
    title: String,
    mode: FolderDisplayMode = FolderDisplayMode.BENTO_2X2,
    targetPage: Int = 0,
  ): Long {
    val settings = preferencesDataSource.getCurrentSettings()
    val spanX = mode.defaultSpanX.coerceAtMost(settings.gridColumns)
    val spanY = mode.defaultSpanY.coerceAtMost(settings.gridRows)
    val (page, x, y) = findNextAvailableSpan(targetPage, spanX, spanY, settings)
    return homeItemDao.insertItem(
      HomeItemEntity(
        itemType = HomeItemType.FOLDER.name,
        page = page,
        cellX = x,
        cellY = y,
        spanX = spanX,
        spanY = spanY,
        folderTitle = title,
        folderDisplayMode = mode.name,
      )
    )
  }

  suspend fun moveItem(
    itemId: Long,
    targetPage: Int,
    targetX: Int,
    targetY: Int,
    isDocked: Boolean = false,
    dockOrder: Int = -1,
    targetFolderId: Long? = null,
  ) {
    val item = homeItemDao.getHomeItemById(itemId) ?: return
    val settings = preferencesDataSource.getCurrentSettings()

    if (targetFolderId != null) {
      val children = homeItemDao.getFolderChildren(targetFolderId)
      homeItemDao.updateItemPlacement(
        id = itemId,
        page = 0,
        cellX = 0,
        cellY = 0,
        isDocked = false,
        dockOrder = -1,
        folderId = targetFolderId,
        folderOrder = children.size,
      )
      return
    }

    if (isDocked) {
      val existingDock = homeItemDao.getAllHomeItemsSnapshot().firstOrNull {
        it.isDocked && it.dockOrder == dockOrder && it.id != itemId
      }
      if (existingDock != null) {
        homeItemDao.updateItemPlacement(
          id = existingDock.id,
          page = item.page,
          cellX = item.cellX,
          cellY = item.cellY,
          isDocked = item.isDocked,
          dockOrder = item.dockOrder,
          folderId = item.folderId,
          folderOrder = item.folderOrder,
        )
      }
      homeItemDao.updateItemPlacement(
        id = itemId,
        page = 0,
        cellX = 0,
        cellY = 0,
        isDocked = true,
        dockOrder = dockOrder,
        folderId = null,
        folderOrder = 0,
      )
      return
    }

    val clampedX = targetX.coerceIn(0, (settings.gridColumns - item.spanX).coerceAtLeast(0))
    val clampedY = targetY.coerceIn(0, (settings.gridRows - item.spanY).coerceAtLeast(0))

    val occupant = homeItemDao.getAllHomeItemsSnapshot().firstOrNull {
      !it.isDocked &&
        it.folderId == null &&
        it.id != itemId &&
        it.page == targetPage &&
        clampedX in it.cellX until (it.cellX + it.spanX) &&
        clampedY in it.cellY until (it.cellY + it.spanY)
    }

    if (occupant != null && item.spanX == 1 && item.spanY == 1 && occupant.spanX == 1 && occupant.spanY == 1) {
      // Swap 1x1 items cleanly
      homeItemDao.updateItemPlacement(
        id = occupant.id,
        page = item.page,
        cellX = item.cellX,
        cellY = item.cellY,
        isDocked = item.isDocked,
        dockOrder = item.dockOrder,
        folderId = item.folderId,
        folderOrder = item.folderOrder,
      )
    }

    homeItemDao.updateItemPlacement(
      id = itemId,
      page = targetPage.coerceAtLeast(0),
      cellX = clampedX,
      cellY = clampedY,
      isDocked = false,
      dockOrder = -1,
      folderId = null,
      folderOrder = 0,
    )
  }

  suspend fun resizeFolder(folderId: Long, mode: FolderDisplayMode) {
    val settings = preferencesDataSource.getCurrentSettings()
    val folder = homeItemDao.getHomeItemById(folderId) ?: return
    val newSpanX = mode.defaultSpanX.coerceIn(1, settings.gridColumns)
    val newSpanY = mode.defaultSpanY.coerceIn(1, settings.gridRows)
    val newX = folder.cellX.coerceIn(0, (settings.gridColumns - newSpanX).coerceAtLeast(0))
    val newY = folder.cellY.coerceIn(0, (settings.gridRows - newSpanY).coerceAtLeast(0))
    homeItemDao.updateItem(
      folder.copy(
        folderDisplayMode = mode.name,
        spanX = newSpanX,
        spanY = newSpanY,
        cellX = newX,
        cellY = newY,
      )
    )
  }

  suspend fun resizeItemCustomSpan(itemId: Long, spanX: Int, spanY: Int) {
    val settings = preferencesDataSource.getCurrentSettings()
    val item = homeItemDao.getHomeItemById(itemId) ?: return
    val clampedX = spanX.coerceIn(1, settings.gridColumns)
    val clampedY = spanY.coerceIn(1, settings.gridRows)
    val adjustedX = item.cellX.coerceIn(0, (settings.gridColumns - clampedX).coerceAtLeast(0))
    val adjustedY = item.cellY.coerceIn(0, (settings.gridRows - clampedY).coerceAtLeast(0))

    val resolvedMode = if (item.itemType == HomeItemType.FOLDER.name) {
      FolderDisplayMode.entries.firstOrNull {
        it.defaultSpanX == clampedX && it.defaultSpanY == clampedY
      }?.name ?: when {
        clampedX == 1 && clampedY == 1 -> FolderDisplayMode.COMPACT_1X1.name
        clampedX >= 3 -> FolderDisplayMode.BANNER_4X2.name
        clampedX >= 2 && clampedY >= 3 -> FolderDisplayMode.BENTO_2X3.name
        clampedX >= 2 && clampedY >= 2 -> FolderDisplayMode.BENTO_2X2.name
        clampedX >= 2 -> FolderDisplayMode.WIDE_2X1.name
        else -> FolderDisplayMode.TALL_1X2.name
      }
    } else {
      item.folderDisplayMode
    }

    homeItemDao.updateItem(
      item.copy(
        spanX = clampedX,
        spanY = clampedY,
        cellX = adjustedX,
        cellY = adjustedY,
        folderDisplayMode = resolvedMode,
      )
    )
  }

  suspend fun renameFolder(folderId: Long, newTitle: String) {
    homeItemDao.renameFolder(folderId, newTitle.trim())
  }

  suspend fun removeFolder(folderId: Long) {
    homeItemDao.deleteFolderAndChildren(folderId)
  }

  suspend fun removeItem(itemId: Long) {
    val item = homeItemDao.getHomeItemById(itemId) ?: return
    if (item.itemType == HomeItemType.FOLDER.name) {
      homeItemDao.deleteFolderAndChildren(itemId)
    } else {
      homeItemDao.deleteItemById(itemId)
    }
  }

  suspend fun saveFloatingNote(note: FloatingNote): Long {
    return floatingNoteDao.upsertNote(
      FloatingNoteEntity(
        id = note.id,
        title = note.title,
        content = note.content,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun deleteFloatingNote(id: Long) {
    floatingNoteDao.deleteNoteById(id)
  }

  private suspend fun findNextAvailableCell(page: Int, settings: LauncherSettings): Triple<Int, Int, Int> =
    findNextAvailableSpan(page, 1, 1, settings)

  private suspend fun findNextAvailableSpan(
    startPage: Int,
    spanX: Int,
    spanY: Int,
    settings: LauncherSettings,
  ): Triple<Int, Int, Int> {
    val allItems = homeItemDao.getAllHomeItemsSnapshot().filter { !it.isDocked && it.folderId == null }
    var page = startPage.coerceAtLeast(0)
    while (page < 20) {
      val pageItems = allItems.filter { it.page == page }
      for (y in 0..(settings.gridRows - spanY)) {
        for (x in 0..(settings.gridColumns - spanX)) {
          val collides = pageItems.any { existing ->
            rectsOverlap(
              x, y, spanX, spanY,
              existing.cellX, existing.cellY, existing.spanX, existing.spanY,
            )
          }
          if (!collides) return Triple(page, x, y)
        }
      }
      page++
    }
    return Triple(startPage, 0, 0)
  }

  private fun rectsOverlap(
    x1: Int, y1: Int, w1: Int, h1: Int,
    x2: Int, y2: Int, w2: Int, h2: Int,
  ): Boolean {
    return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
  }

  private fun mapEntitiesToDomain(
    entities: List<HomeItemEntity>,
    installedApps: List<InstalledApp>,
    badges: Map<String, Int>,
  ): List<HomeScreenItem> {
    val appByKey = installedApps.associateBy { it.uniqueKey }
    val childrenByFolder = entities
      .filter { it.folderId != null }
      .groupBy { it.folderId!! }

    return entities
      .filter { it.folderId == null }
      .mapNotNull { entity ->
        val type = HomeItemType.entries.firstOrNull { it.name == entity.itemType }
          ?: HomeItemType.APP_SHORTCUT
        val pos = GridPosition(
          page = entity.page,
          cellX = entity.cellX,
          cellY = entity.cellY,
          spanX = entity.spanX,
          spanY = entity.spanY,
        )

        when (type) {
          HomeItemType.APP_SHORTCUT -> {
            val key = "${entity.packageName}/${entity.activityName}#${entity.userSerialNumber}"
            val matchedApp = appByKey[key]
            HomeScreenItem.AppShortcut(
              id = entity.id,
              position = pos,
              isDocked = entity.isDocked,
              dockOrder = entity.dockOrder,
              packageName = entity.packageName,
              activityName = entity.activityName,
              label = matchedApp?.label ?: entity.packageName.substringAfterLast('.'),
              customLabel = entity.customLabel ?: matchedApp?.customLabel,
              userSerialNumber = entity.userSerialNumber,
              isWorkProfile = matchedApp?.isWorkProfile ?: (entity.userSerialNumber != 0L),
              copyInstanceId = entity.copyInstanceId,
              badgeCount = badges[entity.packageName] ?: 0,
            )
          }

          HomeItemType.DEEP_SHORTCUT -> HomeScreenItem.DeepShortcut(
            id = entity.id,
            position = pos,
            isDocked = entity.isDocked,
            dockOrder = entity.dockOrder,
            packageName = entity.packageName,
            shortcutId = entity.shortcutId.orEmpty(),
            label = entity.customLabel ?: entity.shortcutId.orEmpty(),
            userSerialNumber = entity.userSerialNumber,
          )

          HomeItemType.FOLDER -> {
            val mode = FolderDisplayMode.entries.firstOrNull { it.name == entity.folderDisplayMode }
              ?: FolderDisplayMode.COMPACT_1X1
            val children = childrenByFolder[entity.id].orEmpty()
              .sortedBy { it.folderOrder }
              .map { child ->
                val key = "${child.packageName}/${child.activityName}#${child.userSerialNumber}"
                val matched = appByKey[key]
                HomeScreenItem.AppShortcut(
                  id = child.id,
                  position = GridPosition(0, 0, 0),
                  packageName = child.packageName,
                  activityName = child.activityName,
                  label = matched?.label ?: child.packageName.substringAfterLast('.'),
                  customLabel = child.customLabel ?: matched?.customLabel,
                  userSerialNumber = child.userSerialNumber,
                  isWorkProfile = matched?.isWorkProfile ?: (child.userSerialNumber != 0L),
                  copyInstanceId = child.copyInstanceId,
                  badgeCount = badges[child.packageName] ?: 0,
                )
              }
            HomeScreenItem.Folder(
              id = entity.id,
              position = pos,
              isDocked = entity.isDocked,
              dockOrder = entity.dockOrder,
              title = entity.folderTitle.ifBlank { "Folder" },
              displayMode = mode,
              accentColorHex = entity.accentColorHex,
              children = children,
            )
          }

          HomeItemType.APP_WIDGET -> HomeScreenItem.SystemAppWidget(
            id = entity.id,
            position = pos,
            appWidgetId = entity.appWidgetId,
            providerPackage = entity.widgetProviderPackage,
            providerClass = entity.widgetProviderClass,
          )

          HomeItemType.BUILTIN_WIDGET -> {
            val widgetType = BuiltInWidgetType.entries.firstOrNull {
              it.name == entity.builtInWidgetType
            } ?: BuiltInWidgetType.CLOCK_PRAYER_BAR
            HomeScreenItem.BuiltInWidget(
              id = entity.id,
              position = pos,
              widgetType = widgetType,
            )
          }
        }
      }
  }
}
