package com.example.ui.home

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupResult
import com.example.data.backup.LauncherBackupManager
import com.example.data.datastore.LauncherPreferencesDataSource
import com.example.data.repository.HomeLayoutRepository
import com.example.data.system.InstalledAppsRepository
import com.example.data.system.NotificationBadgeStore
import com.example.data.system.PrayerTimesCalculator
import com.example.domain.model.AppCategory
import com.example.domain.model.AppShortcutInfo
import com.example.domain.model.BuiltInWidgetType
import com.example.domain.model.DailyScheduleState
import com.example.domain.model.FloatingMiniAppType
import com.example.domain.model.FloatingNote
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.GestureAction
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.InstalledApp
import com.example.domain.model.LauncherSettings
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppMenuTarget(
  val homeItemId: Long? = null,
  val packageName: String,
  val activityName: String,
  val label: String,
  val userSerialNumber: Long = 0L,
  val isFromDrawer: Boolean = false,
  val isHidden: Boolean = false,
)

data class LauncherUiState(
  val settings: LauncherSettings = LauncherSettings(),
  val homeItems: List<HomeScreenItem> = emptyList(),
  val dockedItems: List<HomeScreenItem> = emptyList(),
  val installedApps: List<InstalledApp> = emptyList(),
  val schedule: DailyScheduleState = PrayerTimesCalculator.buildScheduleState(LauncherSettings()),
  val floatingNotes: List<FloatingNote> = emptyList(),
  val pageCount: Int = 2,
  val isNotificationListenerConnected: Boolean = false,
  val isAppDrawerOpen: Boolean = false,
  val isSearchOpen: Boolean = false,
  val isSettingsOpen: Boolean = false,
  val expandedFolderId: Long? = null,
  val selectedFolderMenuId: Long? = null,
  val resizingItemId: Long? = null,
  val selectedAppMenu: AppMenuTarget? = null,
  val dynamicShortcuts: List<AppShortcutInfo> = emptyList(),
  val isWidgetPickerOpen: Boolean = false,
  val widgetPickerPackageFilter: String? = null,
  val activeFloatingMiniApp: FloatingMiniAppType? = null,
  val isFloatingMinimized: Boolean = false,
  val isVaultUnlocked: Boolean = false,
  val statusMessage: String? = null,
)

class HomeViewModel(
  private val appContext: Context,
  val appsRepository: InstalledAppsRepository,
  private val homeLayoutRepository: HomeLayoutRepository,
  private val preferencesDataSource: LauncherPreferencesDataSource,
  private val backupManager: LauncherBackupManager,
) : ViewModel() {

  private val nowTick = MutableStateFlow(System.currentTimeMillis())
  private val uiOverlayState = MutableStateFlow(OverlayUiFlags())

  private data class OverlayUiFlags(
    val isAppDrawerOpen: Boolean = false,
    val isSearchOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val expandedFolderId: Long? = null,
    val selectedFolderMenuId: Long? = null,
    val resizingItemId: Long? = null,
    val selectedAppMenu: AppMenuTarget? = null,
    val dynamicShortcuts: List<AppShortcutInfo> = emptyList(),
    val isWidgetPickerOpen: Boolean = false,
    val widgetPickerPackageFilter: String? = null,
    val activeFloatingMiniApp: FloatingMiniAppType? = null,
    val isFloatingMinimized: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val statusMessage: String? = null,
  )

  val uiState: StateFlow<LauncherUiState> = combine(
    preferencesDataSource.settingsFlow,
    homeLayoutRepository.homeItemsFlow,
    appsRepository.installedAppsFlow,
    homeLayoutRepository.floatingNotesFlow,
    combine(nowTick, NotificationBadgeStore.isListenerConnected, uiOverlayState) { t, notif, flags ->
      Triple(t, notif, flags)
    },
  ) { settings, allHomeItems, installedApps, notes, (tick, notifConnected, flags) ->
    val docked = allHomeItems.filter { it.isDocked }.sortedBy { it.dockOrder }.take(settings.dockIconCount)
    val paged = allHomeItems.filter { !it.isDocked }
    val maxPage = (paged.maxOfOrNull { it.position.page } ?: 0).coerceAtLeast(1) + 1
    val schedule = PrayerTimesCalculator.buildScheduleState(settings = settings, nowMillis = tick)

    LauncherUiState(
      settings = settings,
      homeItems = paged,
      dockedItems = docked,
      installedApps = installedApps,
      schedule = schedule,
      floatingNotes = notes,
      pageCount = maxPage.coerceAtLeast(2),
      isNotificationListenerConnected = notifConnected,
      isAppDrawerOpen = flags.isAppDrawerOpen,
      isSearchOpen = flags.isSearchOpen,
      isSettingsOpen = flags.isSettingsOpen,
      expandedFolderId = flags.expandedFolderId,
      selectedFolderMenuId = flags.selectedFolderMenuId,
      resizingItemId = flags.resizingItemId,
      selectedAppMenu = flags.selectedAppMenu,
      dynamicShortcuts = flags.dynamicShortcuts,
      isWidgetPickerOpen = flags.isWidgetPickerOpen,
      widgetPickerPackageFilter = flags.widgetPickerPackageFilter,
      activeFloatingMiniApp = flags.activeFloatingMiniApp,
      isFloatingMinimized = flags.isFloatingMinimized,
      isVaultUnlocked = flags.isVaultUnlocked,
      statusMessage = flags.statusMessage,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = LauncherUiState(),
  )

  init {
    viewModelScope.launch {
      homeLayoutRepository.ensureInitialHomeLayoutIfEmpty()
    }
    viewModelScope.launch {
      while (true) {
        delay(15_000L)
        nowTick.value = System.currentTimeMillis()
      }
    }
  }

  fun launchApp(
    packageName: String,
    activityName: String,
    userSerialNumber: Long = 0L,
    freeform: Boolean = false,
  ) {
    dismissMenus()
    appsRepository.launchApp(
      packageName = packageName,
      activityName = activityName,
      userSerialNumber = userSerialNumber,
      freeform = freeform,
    )
  }

  fun launchDeepShortcut(packageName: String, shortcutId: String, userSerialNumber: Long = 0L) {
    dismissMenus()
    appsRepository.launchDeepShortcut(packageName, shortcutId, userSerialNumber)
  }

  fun openAppContextMenu(target: AppMenuTarget) {
    uiOverlayState.value = uiOverlayState.value.copy(
      selectedAppMenu = target,
      selectedFolderMenuId = null,
      dynamicShortcuts = emptyList(),
    )
    viewModelScope.launch {
      val shortcuts = appsRepository.loadDynamicShortcuts(target.packageName, target.userSerialNumber)
      if (uiOverlayState.value.selectedAppMenu?.packageName == target.packageName) {
        uiOverlayState.value = uiOverlayState.value.copy(dynamicShortcuts = shortcuts)
      }
    }
  }

  fun openFolderContextMenu(folderId: Long) {
    uiOverlayState.value = uiOverlayState.value.copy(
      selectedFolderMenuId = folderId,
      selectedAppMenu = null,
      resizingItemId = null,
    )
  }

  fun enterResizeMode(itemId: Long) {
    uiOverlayState.value = uiOverlayState.value.copy(
      resizingItemId = itemId,
      selectedFolderMenuId = null,
      selectedAppMenu = null,
    )
  }

  fun exitResizeMode() {
    uiOverlayState.value = uiOverlayState.value.copy(resizingItemId = null)
  }

  fun openFolderExpanded(folderId: Long) {
    uiOverlayState.value = uiOverlayState.value.copy(
      expandedFolderId = folderId,
      selectedFolderMenuId = null,
      selectedAppMenu = null,
    )
  }

  fun closeExpandedFolder() {
    uiOverlayState.value = uiOverlayState.value.copy(expandedFolderId = null)
  }

  fun setAppDrawerOpen(open: Boolean) {
    uiOverlayState.value = uiOverlayState.value.copy(
      isAppDrawerOpen = open,
      isSearchOpen = false,
      selectedAppMenu = null,
      selectedFolderMenuId = null,
    )
  }

  fun setSearchOpen(open: Boolean) {
    uiOverlayState.value = uiOverlayState.value.copy(
      isSearchOpen = open,
      isAppDrawerOpen = false,
      selectedAppMenu = null,
      selectedFolderMenuId = null,
    )
  }

  fun setSettingsOpen(open: Boolean) {
    uiOverlayState.value = uiOverlayState.value.copy(
      isSettingsOpen = open,
      isAppDrawerOpen = false,
      isSearchOpen = false,
      selectedAppMenu = null,
      selectedFolderMenuId = null,
    )
  }

  fun openWidgetPicker(packageFilter: String? = null) {
    uiOverlayState.value = uiOverlayState.value.copy(
      isWidgetPickerOpen = true,
      widgetPickerPackageFilter = packageFilter,
      selectedAppMenu = null,
      selectedFolderMenuId = null,
    )
  }

  fun closeWidgetPicker() {
    uiOverlayState.value = uiOverlayState.value.copy(
      isWidgetPickerOpen = false,
      widgetPickerPackageFilter = null,
    )
  }

  fun openFloatingMiniApp(type: FloatingMiniAppType) {
    uiOverlayState.value = uiOverlayState.value.copy(
      activeFloatingMiniApp = type,
      isFloatingMinimized = false,
      selectedAppMenu = null,
      isAppDrawerOpen = false,
      isSearchOpen = false,
    )
    if (Settings.canDrawOverlays(appContext)) {
      val intent = Intent(appContext, FloatingOverlayService::class.java).apply {
        action = FloatingOverlayService.ACTION_OPEN_MINI_APP
        putExtra(FloatingOverlayService.EXTRA_MINI_APP_TYPE, type.name)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { appContext.startForegroundService(intent) }
      } else {
        runCatching { appContext.startService(intent) }
      }
    }
  }

  fun toggleFloatingMinimize() {
    uiOverlayState.value = uiOverlayState.value.copy(
      isFloatingMinimized = !uiOverlayState.value.isFloatingMinimized,
    )
  }

  fun closeFloatingMiniApp() {
    uiOverlayState.value = uiOverlayState.value.copy(
      activeFloatingMiniApp = null,
      isFloatingMinimized = false,
    )
    runCatching {
      val intent = Intent(appContext, FloatingOverlayService::class.java).apply {
        action = FloatingOverlayService.ACTION_STOP_OVERLAY
      }
      appContext.startService(intent)
    }
  }

  fun dismissMenus() {
    uiOverlayState.value = uiOverlayState.value.copy(
      selectedAppMenu = null,
      selectedFolderMenuId = null,
    )
  }

  fun unlockVault(unlocked: Boolean) {
    uiOverlayState.value = uiOverlayState.value.copy(isVaultUnlocked = unlocked)
  }

  fun showMessage(msg: String) {
    uiOverlayState.value = uiOverlayState.value.copy(statusMessage = msg)
  }

  fun clearMessage() {
    uiOverlayState.value = uiOverlayState.value.copy(statusMessage = null)
  }

  fun handleGesture(action: GestureAction) {
    when (action) {
      GestureAction.NONE -> Unit
      GestureAction.OPEN_APP_DRAWER -> setAppDrawerOpen(true)
      GestureAction.OPEN_SEARCH -> setSearchOpen(true)
      GestureAction.OPEN_SETTINGS -> setSettingsOpen(true)
      GestureAction.TOGGLE_FLOATING_DOCK -> openFloatingMiniApp(FloatingMiniAppType.QUICK_APPS)
      GestureAction.EXPAND_NOTIFICATIONS -> expandStatusBar()
      GestureAction.LOCK_SCREEN -> showMessage(
        if (uiState.value.settings.forceRtl) "استخدم زر الطاقة لقفل الشاشة" else "Press power key to lock screen"
      )
    }
  }

  @Suppress("WrongConstant")
  private fun expandStatusBar() {
    runCatching {
      val sbService = appContext.getSystemService("statusbar")
      val statusBarManager = Class.forName("android.app.StatusBarManager")
      val expand = statusBarManager.getMethod("expandNotificationsPanel")
      expand.invoke(sbService)
    }
  }

  fun moveHomeItem(
    itemId: Long,
    targetPage: Int,
    targetX: Int,
    targetY: Int,
    isDocked: Boolean = false,
    dockOrder: Int = -1,
    targetFolderId: Long? = null,
  ) {
    viewModelScope.launch {
      homeLayoutRepository.moveItem(
        itemId = itemId,
        targetPage = targetPage,
        targetX = targetX,
        targetY = targetY,
        isDocked = isDocked,
        dockOrder = dockOrder,
        targetFolderId = targetFolderId,
      )
    }
  }

  fun createFolderFromItems(firstItemId: Long, secondItemId: Long) {
    viewModelScope.launch {
      val title = if (uiState.value.settings.forceRtl) "مجلد جديد" else "New Folder"
      homeLayoutRepository.createFolderFromItems(
        firstItemId = firstItemId,
        secondItemId = secondItemId,
        title = title,
        mode = FolderDisplayMode.COMPACT_1X1,
      )
    }
  }

  fun createSmartFolder(title: String, mode: FolderDisplayMode, page: Int = 0) {
    viewModelScope.launch {
      homeLayoutRepository.createSmartFolder(title = title, mode = mode, targetPage = page)
    }
  }

  fun resizeFolder(folderId: Long, mode: FolderDisplayMode) {
    viewModelScope.launch {
      homeLayoutRepository.resizeFolder(folderId, mode)
    }
  }

  fun resizeItemSpan(itemId: Long, spanX: Int, spanY: Int) {
    viewModelScope.launch {
      homeLayoutRepository.resizeItemCustomSpan(itemId, spanX, spanY)
    }
  }

  fun renameFolder(folderId: Long, newTitle: String) {
    viewModelScope.launch {
      homeLayoutRepository.renameFolder(folderId, newTitle)
    }
  }

  fun removeFolder(folderId: Long) {
    viewModelScope.launch {
      homeLayoutRepository.removeFolder(folderId)
      uiOverlayState.value = uiOverlayState.value.copy(
        selectedFolderMenuId = null,
        expandedFolderId = null,
        resizingItemId = null,
      )
    }
  }

  fun removeHomeItem(itemId: Long) {
    viewModelScope.launch {
      homeLayoutRepository.removeItem(itemId)
      dismissMenus()
    }
  }

  fun addAppToHome(
    app: InstalledApp,
    targetPage: Int = 0,
    folderId: Long? = null,
    isDuplicateCopy: Boolean = false,
  ) {
    viewModelScope.launch {
      homeLayoutRepository.addAppToHomeScreen(
        app = app,
        targetPage = targetPage,
        folderId = folderId,
        isDuplicateCopy = isDuplicateCopy,
      )
      val isRtl = uiState.value.settings.forceRtl
      showMessage(
        if (isDuplicateCopy) {
          if (isRtl) "تم نسخ الاختصار: ${app.displayLabel}" else "Duplicated ${app.displayLabel}"
        } else {
          if (isRtl) "تمت الإضافة: ${app.displayLabel}" else "Added ${app.displayLabel}"
        }
      )
    }
  }

  fun cloneHomeShortcut(sourceItemId: Long, targetPage: Int? = null, targetFolderId: Long? = null) {
    viewModelScope.launch {
      homeLayoutRepository.cloneHomeShortcut(
        sourceItemId = sourceItemId,
        targetPage = targetPage,
        targetFolderId = targetFolderId,
      )
      dismissMenus()
      val isRtl = uiState.value.settings.forceRtl
      showMessage(if (isRtl) "تم إنشاء نسخة من الاختصار" else "Shortcut cloned")
    }
  }

  fun pinDeepShortcutToHome(shortcut: AppShortcutInfo, targetPage: Int = 0) {
    viewModelScope.launch {
      homeLayoutRepository.addDeepShortcutToHome(
        packageName = shortcut.packageName,
        shortcutId = shortcut.id,
        label = shortcut.shortLabel,
        userSerialNumber = shortcut.userSerialNumber,
        targetPage = targetPage,
      )
      dismissMenus()
    }
  }

  fun addBuiltInWidget(type: BuiltInWidgetType, spanX: Int, spanY: Int, page: Int = 0) {
    viewModelScope.launch {
      homeLayoutRepository.addBuiltInWidget(type, spanX, spanY, page)
      closeWidgetPicker()
    }
  }

  fun addSystemWidget(appWidgetId: Int, info: AppWidgetProviderInfo, page: Int = 0) {
    val cols = ((info.minWidth + 70) / 74).coerceIn(1, uiState.value.settings.gridColumns)
    val rows = ((info.minHeight + 70) / 74).coerceIn(1, 4)
    viewModelScope.launch {
      homeLayoutRepository.addSystemAppWidget(
        appWidgetId = appWidgetId,
        providerPackage = info.provider.packageName,
        providerClass = info.provider.className,
        spanX = cols,
        spanY = rows,
        targetPage = page,
      )
      closeWidgetPicker()
    }
  }

  fun updateAppCustomLabel(packageName: String, activityName: String, userSerialNumber: Long, newLabel: String?) {
    viewModelScope.launch {
      val app = uiState.value.installedApps.firstOrNull {
        it.packageName == packageName &&
          it.activityName == activityName &&
          it.userSerialNumber == userSerialNumber
      } ?: return@launch
      appsRepository.updateAppLabel(app, newLabel)
      dismissMenus()
    }
  }

  fun setAppHidden(app: InstalledApp, hidden: Boolean) {
    viewModelScope.launch {
      appsRepository.setAppHidden(app, hidden)
      dismissMenus()
    }
  }

  fun setAppCategory(app: InstalledApp, category: AppCategory?) {
    viewModelScope.launch {
      appsRepository.setAppCategory(app, category)
    }
  }

  fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
    viewModelScope.launch {
      preferencesDataSource.updateSettings(transform)
    }
  }

  fun resetSettingsToDefaults() {
    viewModelScope.launch {
      preferencesDataSource.resetToDefaults()
      showMessage(if (uiState.value.settings.forceRtl) "تمت استعادة الإعدادات الافتراضية" else "Reset to defaults")
    }
  }

  fun saveNote(note: FloatingNote) {
    viewModelScope.launch {
      homeLayoutRepository.saveFloatingNote(note)
    }
  }

  fun deleteNote(id: Long) {
    viewModelScope.launch {
      homeLayoutRepository.deleteFloatingNote(id)
    }
  }

  fun exportBackupJson(uri: Uri) {
    viewModelScope.launch {
      when (val res = backupManager.exportAsJson(uri)) {
        is BackupResult.ExportSuccess -> showMessage(
          if (uiState.value.settings.forceRtl) "تم تصدير النسخة الاحتياطية (${res.itemCount} عنصر)" else "Exported ${res.itemCount} items"
        )
        is BackupResult.Failure -> showMessage(res.reason)
        else -> Unit
      }
    }
  }

  fun exportBackupZip(uri: Uri) {
    viewModelScope.launch {
      when (val res = backupManager.exportAsZip(uri)) {
        is BackupResult.ExportSuccess -> showMessage(
          if (uiState.value.settings.forceRtl) "تم تصدير ملف ZIP بنجاح" else "ZIP backup exported"
        )
        is BackupResult.Failure -> showMessage(res.reason)
        else -> Unit
      }
    }
  }

  fun importBackup(uri: Uri) {
    viewModelScope.launch {
      when (val res = backupManager.importFromUri(uri)) {
        is BackupResult.ImportSuccess -> showMessage(
          if (uiState.value.settings.forceRtl) "تمت استعادة ${res.restoredItems} عنصر" else "Restored ${res.restoredItems} items"
        )
        is BackupResult.Failure -> showMessage(res.reason)
        else -> Unit
      }
    }
  }

  fun getWidgetProviders(packageFilter: String?): List<AppWidgetProviderInfo> {
    return appsRepository.getWidgetsForPackage(packageFilter)
  }

  fun openAppInfo(packageName: String, userSerialNumber: Long) {
    dismissMenus()
    appsRepository.openAppInfo(packageName, userSerialNumber)
  }

  fun uninstallApp(packageName: String) {
    dismissMenus()
    appsRepository.requestUninstallApp(packageName)
  }

  class Factory(
    private val context: Context,
    private val appsRepository: InstalledAppsRepository,
    private val homeLayoutRepository: HomeLayoutRepository,
    private val preferencesDataSource: LauncherPreferencesDataSource,
    private val backupManager: LauncherBackupManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return HomeViewModel(
        appContext = context.applicationContext,
        appsRepository = appsRepository,
        homeLayoutRepository = homeLayoutRepository,
        preferencesDataSource = preferencesDataSource,
        backupManager = backupManager,
      ) as T
    }
  }
}
