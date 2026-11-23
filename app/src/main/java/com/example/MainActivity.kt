package com.example

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.HomeScreenItem
import com.example.ui.components.WallpaperBackdrop
import com.example.ui.components.glassBorderBrush
import com.example.ui.drawer.AppDrawerOverlay
import com.example.ui.floating.FloatingWindowWorkspaceOverlay
import com.example.ui.home.AppIconContextMenuOverlay
import com.example.ui.home.AppMenuTarget
import com.example.ui.home.ExpandedFolderOverlay
import com.example.ui.home.FolderContextMenuOverlay
import com.example.ui.home.HomeViewModel
import com.example.ui.home.LauncherHomeScreen
import com.example.ui.search.SearchOverlay
import com.example.ui.settings.LauncherSettingsScreen
import com.example.ui.theme.RawaqLauncherTheme
import com.example.ui.theme.parseColorHex
import com.example.ui.widgets.LauncherAppWidgetManager
import com.example.ui.widgets.WidgetPickerSheet

class MainActivity : AppCompatActivity() {

  private lateinit var widgetHostManager: LauncherAppWidgetManager

  private val viewModel: HomeViewModel by viewModels {
    val app = application as RawaqLauncherApp
    HomeViewModel.Factory(
      context = applicationContext,
      appsRepository = app.container.installedAppsRepository,
      homeLayoutRepository = app.container.homeLayoutRepository,
      preferencesDataSource = app.container.preferencesDataSource,
      backupManager = app.container.backupManager,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    widgetHostManager = LauncherAppWidgetManager(this)

    setContent {
      RawaqLauncherRoot(
        viewModel = viewModel,
        widgetHostManager = widgetHostManager,
      )
    }
  }

  override fun onStart() {
    super.onStart()
    widgetHostManager.startListening()
  }

  override fun onStop() {
    super.onStop()
    widgetHostManager.stopListening()
  }
}

@Composable
fun RawaqLauncherRoot(
  viewModel: HomeViewModel,
  widgetHostManager: LauncherAppWidgetManager,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val settings = state.settings
  val snackbarHostState = remember { SnackbarHostState() }

  var pendingWidgetInfo by remember { mutableStateOf<AppWidgetProviderInfo?>(null) }
  var pendingWidgetId by remember { mutableStateOf(-1) }
  var selectedWidgetMenu by remember { mutableStateOf<HomeScreenItem?>(null) }

  val widgetConfigureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    val info = pendingWidgetInfo
    if (result.resultCode == AppCompatActivity.RESULT_OK && info != null && pendingWidgetId > 0) {
      viewModel.addSystemWidget(pendingWidgetId, info)
    } else if (pendingWidgetId > 0) {
      widgetHostManager.deleteWidgetId(pendingWidgetId)
    }
    pendingWidgetInfo = null
    pendingWidgetId = -1
  }

  val widgetBindLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    val info = pendingWidgetInfo
    if (result.resultCode == AppCompatActivity.RESULT_OK && info != null && pendingWidgetId > 0) {
      if (info.configure != null) {
        val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
          component = info.configure
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        }
        widgetConfigureLauncher.launch(cfgIntent)
      } else {
        viewModel.addSystemWidget(pendingWidgetId, info)
        pendingWidgetInfo = null
        pendingWidgetId = -1
      }
    } else {
      if (pendingWidgetId > 0) widgetHostManager.deleteWidgetId(pendingWidgetId)
      pendingWidgetInfo = null
      pendingWidgetId = -1
    }
  }

  val exportJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    uri?.let { viewModel.exportBackupJson(it) }
  }

  val exportZipLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/zip"),
  ) { uri ->
    uri?.let { viewModel.exportBackupZip(it) }
  }

  val importBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    uri?.let { viewModel.importBackup(it) }
  }

  LaunchedEffect(state.statusMessage) {
    val msg = state.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  RawaqLauncherTheme(settings = settings) {
    val anyOverlayOpen = state.isAppDrawerOpen ||
      state.isSearchOpen ||
      state.isSettingsOpen ||
      state.expandedFolderId != null

    Box(modifier = Modifier.fillMaxSize()) {
      WallpaperBackdrop(
        settings = settings,
        extraBlurDp = if (anyOverlayOpen) 16.dp else 0.dp,
      )

      LauncherHomeScreen(
        state = state,
        appsRepository = viewModel.appsRepository,
        widgetHostManager = widgetHostManager,
        onLaunchApp = { pkg, act, serial -> viewModel.launchApp(pkg, act, serial) },
        onLaunchDeepShortcut = { pkg, scId, serial -> viewModel.launchDeepShortcut(pkg, scId, serial) },
        onLongPressApp = { shortcut ->
          viewModel.openAppContextMenu(
            AppMenuTarget(
              homeItemId = shortcut.id,
              packageName = shortcut.packageName,
              activityName = shortcut.activityName,
              label = shortcut.displayLabel,
              userSerialNumber = shortcut.userSerialNumber,
              isFromDrawer = false,
            )
          )
        },
        onOpenFolder = { folderId -> viewModel.openFolderExpanded(folderId) },
        onLongPressFolder = { folderId -> viewModel.openFolderContextMenu(folderId) },
        onResizeItemSpan = { itemId, sx, sy -> viewModel.resizeItemSpan(itemId, sx, sy) },
        onExitResizeMode = { viewModel.exitResizeMode() },
        onMoveItem = { id, page, x, y, docked, dockOrder, folderId ->
          viewModel.moveHomeItem(id, page, x, y, docked, dockOrder, folderId)
        },
        onMergeIntoFolder = { firstId, secondId -> viewModel.createFolderFromItems(firstId, secondId) },
        onCreateSmartFolder = { title, mode, page -> viewModel.createSmartFolder(title, mode, page) },
        onOpenSearch = { viewModel.setSearchOpen(true) },
        onOpenDrawer = { viewModel.setAppDrawerOpen(true) },
        onOpenSettings = { viewModel.setSettingsOpen(true) },
        onOpenWidgetPicker = { viewModel.openWidgetPicker(null) },
        onOpenFloatingMiniApp = { type -> viewModel.openFloatingMiniApp(type) },
        onSwipeDownGesture = { viewModel.handleGesture(settings.swipeDownGesture) },
        onDoubleTapGesture = { viewModel.handleGesture(settings.doubleTapGesture) },
        onLongPressWidget = { item -> selectedWidgetMenu = item },
      )

      // Expanded Folder Modal
      val expandedFolder = remember(state.expandedFolderId, state.homeItems, state.dockedItems) {
        (state.homeItems + state.dockedItems)
          .filterIsInstance<HomeScreenItem.Folder>()
          .firstOrNull { it.id == state.expandedFolderId }
      }
      AnimatedVisibility(
        visible = expandedFolder != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        if (expandedFolder != null) {
          ExpandedFolderOverlay(
            folder = expandedFolder,
            allInstalledApps = state.installedApps,
            settings = settings,
            appsRepository = viewModel.appsRepository,
            onLaunchApp = { child ->
              viewModel.closeExpandedFolder()
              viewModel.launchApp(child.packageName, child.activityName, child.userSerialNumber)
            },
            onLongPressChild = { child ->
              viewModel.openAppContextMenu(
                AppMenuTarget(
                  homeItemId = child.id,
                  packageName = child.packageName,
                  activityName = child.activityName,
                  label = child.displayLabel,
                  userSerialNumber = child.userSerialNumber,
                  isFromDrawer = false,
                )
              )
            },
            onRenameFolder = { newTitle -> viewModel.renameFolder(expandedFolder.id, newTitle) },
            onAddAppToFolder = { app ->
              viewModel.addAppToHome(app = app, folderId = expandedFolder.id, isDuplicateCopy = true)
            },
            onDismiss = { viewModel.closeExpandedFolder() },
          )
        }
      }

      // Swipe-up App Drawer Overlay
      AnimatedVisibility(
        visible = state.isAppDrawerOpen,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
      ) {
        AppDrawerOverlay(
          installedApps = state.installedApps,
          settings = settings,
          isVaultUnlocked = state.isVaultUnlocked,
          appsRepository = viewModel.appsRepository,
          onLaunchApp = { app ->
            viewModel.setAppDrawerOpen(false)
            viewModel.launchApp(app.packageName, app.activityName, app.userSerialNumber)
          },
          onLongPressApp = { target -> viewModel.openAppContextMenu(target) },
          onSetVaultUnlocked = { unlocked -> viewModel.unlockVault(unlocked) },
          onOpenSettings = { viewModel.setSettingsOpen(true) },
          onDismiss = { viewModel.setAppDrawerOpen(false) },
        )
      }

      // Instant Fuzzy Search Overlay
      AnimatedVisibility(
        visible = state.isSearchOpen,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        val folders = remember(state.homeItems) {
          state.homeItems.filterIsInstance<HomeScreenItem.Folder>()
        }
        SearchOverlay(
          installedApps = state.installedApps,
          folders = folders,
          settings = settings,
          appsRepository = viewModel.appsRepository,
          onLaunchApp = { app ->
            viewModel.setSearchOpen(false)
            viewModel.launchApp(app.packageName, app.activityName, app.userSerialNumber)
          },
          onLaunchFreeform = { app ->
            viewModel.setSearchOpen(false)
            viewModel.launchApp(app.packageName, app.activityName, app.userSerialNumber, freeform = true)
          },
          onOpenFolder = { folderId ->
            viewModel.setSearchOpen(false)
            viewModel.openFolderExpanded(folderId)
          },
          onDismiss = { viewModel.setSearchOpen(false) },
        )
      }

      // Customization Settings Screen
      AnimatedVisibility(
        visible = state.isSettingsOpen,
        enter = slideInVertically { it / 4 } + fadeIn(),
        exit = slideOutVertically { it / 4 } + fadeOut(),
      ) {
        LauncherSettingsScreen(
          settings = settings,
          isNotificationListenerConnected = state.isNotificationListenerConnected,
          onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
          onExportJson = { exportJsonLauncher.launch("rawaq_launcher_backup.json") },
          onExportZip = { exportZipLauncher.launch("rawaq_launcher_backup.zip") },
          onImportBackup = { importBackupLauncher.launch(arrayOf("application/json", "application/zip", "*/*")) },
          onResetDefaults = { viewModel.resetSettingsToDefaults() },
          onBack = { viewModel.setSettingsOpen(false) },
        )
      }

      // Floating Mini-Apps Workspace Overlay
      val activeFloating = state.activeFloatingMiniApp
      if (activeFloating != null) {
        FloatingWindowWorkspaceOverlay(
          activeType = activeFloating,
          isMinimized = state.isFloatingMinimized,
          installedApps = state.installedApps,
          notes = state.floatingNotes,
          settings = settings,
          appsRepository = viewModel.appsRepository,
          onSelectType = { type -> viewModel.openFloatingMiniApp(type) },
          onToggleMinimize = { viewModel.toggleFloatingMinimize() },
          onClose = { viewModel.closeFloatingMiniApp() },
          onSaveNote = { note -> viewModel.saveNote(note) },
          onDeleteNote = { id -> viewModel.deleteNote(id) },
          onLaunchAppFreeform = { app ->
            viewModel.launchApp(app.packageName, app.activityName, app.userSerialNumber, freeform = true)
          },
        )
      }

      // Folder Long-Press Context Menu
      val folderMenuTarget = remember(state.selectedFolderMenuId, state.homeItems, state.dockedItems) {
        (state.homeItems + state.dockedItems)
          .filterIsInstance<HomeScreenItem.Folder>()
          .firstOrNull { it.id == state.selectedFolderMenuId }
      }
      if (folderMenuTarget != null) {
        FolderContextMenuOverlay(
          folder = folderMenuTarget,
          settings = settings,
          onToggleShrinkExpand = {
            val nextMode = if (folderMenuTarget.displayMode == FolderDisplayMode.COMPACT_1X1) {
              FolderDisplayMode.BENTO_2X2
            } else {
              FolderDisplayMode.COMPACT_1X1
            }
            viewModel.resizeFolder(folderMenuTarget.id, nextMode)
          },
          onEnterInteractiveResize = { viewModel.enterResizeMode(folderMenuTarget.id) },
          onRename = { newTitle -> viewModel.renameFolder(folderMenuTarget.id, newTitle) },
          onOpenFolderToAddApps = { viewModel.openFolderExpanded(folderMenuTarget.id) },
          onRemoveFolder = { viewModel.removeFolder(folderMenuTarget.id) },
          onDismiss = { viewModel.dismissMenus() },
        )
      }

      // App Icon Long-Press Context Menu
      val appMenuTarget = state.selectedAppMenu
      if (appMenuTarget != null) {
        val folders = remember(state.homeItems) {
          state.homeItems.filterIsInstance<HomeScreenItem.Folder>()
        }
        AppIconContextMenuOverlay(
          target = appMenuTarget,
          shortcuts = state.dynamicShortcuts,
          folders = folders,
          settings = settings,
          onAppInfo = { viewModel.openAppInfo(appMenuTarget.packageName, appMenuTarget.userSerialNumber) },
          onRemoveFromHome = {
            appMenuTarget.homeItemId?.let { viewModel.removeHomeItem(it) } ?: viewModel.dismissMenus()
          },
          onUninstall = { viewModel.uninstallApp(appMenuTarget.packageName) },
          onOpenWidgets = { viewModel.openWidgetPicker(appMenuTarget.packageName) },
          onOpenFloatingWindow = {
            viewModel.launchApp(
              packageName = appMenuTarget.packageName,
              activityName = appMenuTarget.activityName,
              userSerialNumber = appMenuTarget.userSerialNumber,
              freeform = true,
            )
          },
          onDuplicateShortcut = { targetFolderId ->
            if (appMenuTarget.homeItemId != null) {
              viewModel.cloneHomeShortcut(appMenuTarget.homeItemId, targetFolderId = targetFolderId)
            } else {
              val matched = state.installedApps.firstOrNull {
                it.packageName == appMenuTarget.packageName &&
                  it.activityName == appMenuTarget.activityName &&
                  it.userSerialNumber == appMenuTarget.userSerialNumber
              }
              if (matched != null) {
                viewModel.addAppToHome(matched, folderId = targetFolderId, isDuplicateCopy = true)
              }
              viewModel.dismissMenus()
            }
          },
          onEditCustomLabel = { newLabel ->
            viewModel.updateAppCustomLabel(
              packageName = appMenuTarget.packageName,
              activityName = appMenuTarget.activityName,
              userSerialNumber = appMenuTarget.userSerialNumber,
              newLabel = newLabel,
            )
          },
          onToggleHidden = {
            val matched = state.installedApps.firstOrNull {
              it.packageName == appMenuTarget.packageName &&
                it.activityName == appMenuTarget.activityName &&
                it.userSerialNumber == appMenuTarget.userSerialNumber
            }
            if (matched != null) {
              viewModel.setAppHidden(matched, !appMenuTarget.isHidden)
            } else {
              viewModel.dismissMenus()
            }
          },
          onLaunchShortcut = { sc ->
            viewModel.launchDeepShortcut(sc.packageName, sc.id, sc.userSerialNumber)
          },
          onPinShortcut = { sc -> viewModel.pinDeepShortcutToHome(sc) },
          onDismiss = { viewModel.dismissMenus() },
        )
      }

      // Widget Long-Press Quick Menu
      val widgetMenu = selectedWidgetMenu
      if (widgetMenu != null) {
        val isRtl = settings.forceRtl
        val accent = parseColorHex(settings.accentColorHex)
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { selectedWidgetMenu = null },
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier
              .widthIn(min = 220.dp, max = 280.dp)
              .clip(RoundedCornerShape(22.dp))
              .background(Color(0xFF0F172A).copy(alpha = 0.94f))
              .border(1.dp, glassBorderBrush(settings, accent), RoundedCornerShape(22.dp))
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = if (isRtl) "خيارات الأداة" else "Widget Options",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            if (widgetMenu is HomeScreenItem.SystemAppWidget) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    viewModel.enterResizeMode(widgetMenu.id)
                    selectedWidgetMenu = null
                  }
                  .padding(10.dp),
              ) {
                Text(text = if (isRtl) "تغيير الحجم" else "Resize Widget", color = accent, fontSize = 13.sp)
              }
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  if (widgetMenu is HomeScreenItem.SystemAppWidget) {
                    widgetHostManager.deleteWidgetId(widgetMenu.appWidgetId)
                  }
                  viewModel.removeHomeItem(widgetMenu.id)
                  selectedWidgetMenu = null
                }
                .padding(10.dp),
            ) {
              Text(text = if (isRtl) "إزالة من الشاشة" else "Remove from Home", color = Color(0xFFFDA4AF), fontSize = 13.sp)
            }
          }
        }
      }

      // Widget Picker Bottom Sheet
      if (state.isWidgetPickerOpen) {
        val providers = remember(state.widgetPickerPackageFilter) {
          viewModel.getWidgetProviders(state.widgetPickerPackageFilter)
        }
        WidgetPickerSheet(
          providers = providers,
          filterPackageName = state.widgetPickerPackageFilter,
          settings = settings,
          onSelectBuiltIn = { type, sx, sy -> viewModel.addBuiltInWidget(type, sx, sy) },
          onSelectSystemWidget = { info ->
            val widgetId = widgetHostManager.allocateWidgetId()
            if (widgetHostManager.tryBindWidget(widgetId, info)) {
              if (info.configure != null) {
                pendingWidgetInfo = info
                pendingWidgetId = widgetId
                val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                  component = info.configure
                  putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                widgetConfigureLauncher.launch(cfgIntent)
              } else {
                viewModel.addSystemWidget(widgetId, info)
              }
            } else {
              pendingWidgetInfo = info
              pendingWidgetId = widgetId
              widgetBindLauncher.launch(widgetHostManager.createBindIntent(widgetId, info))
            }
          },
          onDismiss = { viewModel.closeWidgetPicker() },
        )
      }

      // Snackbar feedback
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 86.dp),
      )
    }
  }
}
