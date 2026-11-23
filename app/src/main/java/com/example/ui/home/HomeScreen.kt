package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.BuiltInWidgetType
import com.example.domain.model.FloatingMiniAppType
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.PageTransitionStyle
import com.example.domain.model.ThemeMode
import com.example.ui.components.LauncherAppIconCell
import com.example.ui.components.TopClockPrayerWidget
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.widgets.LauncherAppWidgetManager
import com.example.ui.widgets.SystemAppWidgetCard
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
  state: LauncherUiState,
  appsRepository: InstalledAppsRepository,
  widgetHostManager: LauncherAppWidgetManager,
  onLaunchApp: (packageName: String, activityName: String, userSerial: Long) -> Unit,
  onLaunchDeepShortcut: (packageName: String, shortcutId: String, userSerial: Long) -> Unit,
  onLongPressApp: (HomeScreenItem.AppShortcut) -> Unit,
  onOpenFolder: (Long) -> Unit,
  onLongPressFolder: (Long) -> Unit,
  onResizeItemSpan: (itemId: Long, spanX: Int, spanY: Int) -> Unit,
  onExitResizeMode: () -> Unit,
  onMoveItem: (itemId: Long, page: Int, x: Int, y: Int, docked: Boolean, dockOrder: Int, folderId: Long?) -> Unit,
  onMergeIntoFolder: (firstId: Long, secondId: Long) -> Unit,
  onCreateSmartFolder: (title: String, mode: FolderDisplayMode, page: Int) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenDrawer: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenWidgetPicker: () -> Unit,
  onOpenFloatingMiniApp: (FloatingMiniAppType) -> Unit,
  onSwipeDownGesture: () -> Unit,
  onDoubleTapGesture: () -> Unit,
  onLongPressWidget: (HomeScreenItem) -> Unit,
) {
  val settings = state.settings
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  val pagerState = rememberPagerState(pageCount = { state.pageCount })
  val scope = rememberCoroutineScope()

  var isArrangeMode by remember { mutableStateOf(false) }
  var showHomeMenuBar by remember { mutableStateOf(false) }
  var showCreateFolderDialog by remember { mutableStateOf(false) }

  // Drag & Drop state across grid & pages
  var draggingItem by remember { mutableStateOf<HomeScreenItem?>(null) }
  var dragOffset by remember { mutableStateOf(Offset.Zero) }
  var gridBoundsWidthPx by remember { mutableFloatStateOf(1f) }
  var gridBoundsHeightPx by remember { mutableFloatStateOf(1f) }
  var edgeHoverSide by remember { mutableStateOf(0) } // -1 prev, +1 next, 0 none

  LaunchedEffect(edgeHoverSide, draggingItem) {
    if (draggingItem != null && edgeHoverSide != 0) {
      delay(550L)
      val target = (pagerState.currentPage + edgeHoverSide).coerceIn(0, state.pageCount - 1)
      if (target != pagerState.currentPage) {
        pagerState.animateScrollToPage(target)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .pointerInput(settings.doubleTapGesture, settings.swipeUpGesture, settings.swipeDownGesture) {
        detectTapGestures(
          onDoubleTap = { onDoubleTapGesture() },
          onLongPress = { showHomeMenuBar = !showHomeMenuBar },
        )
      }
      .pointerInput(Unit) {
        var verticalAccum = 0f
        detectVerticalDragGestures(
          onDragStart = { verticalAccum = 0f },
          onVerticalDrag = { _, dragAmount ->
            verticalAccum += dragAmount
          },
          onDragEnd = {
            if (verticalAccum < -90f) {
              onOpenDrawer()
            } else if (verticalAccum > 95f) {
              onSwipeDownGesture()
            }
          },
        )
      },
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp),
    ) {
      // Optional Arrange / Home Edit Top Pill
      AnimatedVisibility(visible = isArrangeMode || showHomeMenuBar) {
        HomeQuickToolsBar(
          isRtl = isRtl,
          isArrangeMode = isArrangeMode,
          accent = accent,
          settings = settings,
          onToggleArrange = { isArrangeMode = !isArrangeMode },
          onCreateFolder = { showCreateFolderDialog = true },
          onOpenWidgets = onOpenWidgetPicker,
          onOpenFloating = { onOpenFloatingMiniApp(FloatingMiniAppType.QUICK_APPS) },
          onOpenSettings = onOpenSettings,
          onClose = {
            isArrangeMode = false
            showHomeMenuBar = false
          },
        )
      }

      // Main Multi-Page Grid Workspace
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        val density = LocalDensity.current
        val pageWidthDp = maxWidth
        val pageHeightDp = maxHeight
        val cols = settings.gridColumns
        val rows = settings.gridRows
        val cellWidthDp = pageWidthDp / cols
        val cellHeightDp = pageHeightDp / rows
        val cellWidthPx = with(density) { cellWidthDp.toPx() }
        val cellHeightPx = with(density) { cellHeightDp.toPx() }
        gridBoundsWidthPx = with(density) { pageWidthDp.toPx() }
        gridBoundsHeightPx = with(density) { pageHeightDp.toPx() }

        HorizontalPager(
          state = pagerState,
          userScrollEnabled = draggingItem == null,
          modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
          val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
          val pageItems = remember(state.homeItems, pageIndex) {
            state.homeItems.filter { it.position.page == pageIndex }
          }

          Box(
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                applyPageTransition(
                  style = settings.pageTransitionStyle,
                  pageOffset = pageOffset,
                  isRtl = isRtl,
                )
              },
          ) {
            pageItems.forEach { item ->
              val isBeingDragged = draggingItem?.id == item.id
              val posX = cellWidthDp * item.position.cellX
              val posY = cellHeightDp * item.position.cellY
              val itemW = cellWidthDp * item.position.spanX.coerceAtMost(cols - item.position.cellX)
              val itemH = cellHeightDp * item.position.spanY.coerceAtMost(rows - item.position.cellY)

              val dragMod = if (isArrangeMode) {
                Modifier.pointerInput(item.id, pagerState.currentPage) {
                  detectDragGestures(
                    onDragStart = {
                      draggingItem = item
                      dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                      change.consume()
                      dragOffset += dragAmount
                      val currentScreenX = (item.position.cellX * cellWidthPx) + dragOffset.x + (cellWidthPx / 2f)
                      edgeHoverSide = when {
                        currentScreenX < 52f -> if (isRtl) 1 else -1
                        currentScreenX > gridBoundsWidthPx - 52f -> if (isRtl) -1 else 1
                        else -> 0
                      }
                    },
                    onDragCancel = {
                      draggingItem = null
                      dragOffset = Offset.Zero
                      edgeHoverSide = 0
                    },
                    onDragEnd = {
                      val currentItem = draggingItem
                      if (currentItem != null) {
                        val rawDropX = (currentItem.position.cellX * cellWidthPx) + dragOffset.x
                        val rawDropY = (currentItem.position.cellY * cellHeightPx) + dragOffset.y
                        val targetX = (rawDropX / cellWidthPx).roundToInt().coerceIn(0, cols - 1)
                        val targetY = (rawDropY / cellHeightPx).roundToInt()

                        if (targetY >= rows) {
                          // Dropped onto Dock area!
                          val dockSlot = ((rawDropX / gridBoundsWidthPx) * settings.dockIconCount)
                            .toInt()
                            .coerceIn(0, settings.dockIconCount - 1)
                          onMoveItem(currentItem.id, 0, 0, 0, true, dockSlot, null)
                        } else {
                          val clampedY = targetY.coerceIn(0, rows - 1)
                          val targetFolder = pageItems.firstOrNull { candidate ->
                            candidate is HomeScreenItem.Folder &&
                              candidate.id != currentItem.id &&
                              targetX in candidate.position.cellX until (candidate.position.cellX + candidate.position.spanX) &&
                              clampedY in candidate.position.cellY until (candidate.position.cellY + candidate.position.spanY)
                          }
                          val targetApp = pageItems.firstOrNull { candidate ->
                            candidate is HomeScreenItem.AppShortcut &&
                              candidate.id != currentItem.id &&
                              candidate.position.cellX == targetX &&
                              candidate.position.cellY == clampedY
                          }

                          when {
                            targetFolder != null && currentItem is HomeScreenItem.AppShortcut -> {
                              onMoveItem(currentItem.id, pagerState.currentPage, targetX, clampedY, false, -1, targetFolder.id)
                            }
                            targetApp != null && currentItem is HomeScreenItem.AppShortcut -> {
                              onMergeIntoFolder(targetApp.id, currentItem.id)
                            }
                            else -> {
                              onMoveItem(currentItem.id, pagerState.currentPage, targetX, clampedY, false, -1, null)
                            }
                          }
                        }
                      }
                      draggingItem = null
                      dragOffset = Offset.Zero
                      edgeHoverSide = 0
                    },
                  )
                }
              } else {
                Modifier
              }

              Box(
                modifier = Modifier
                  .offset(x = posX, y = posY)
                  .offset {
                    if (isBeingDragged) IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                    else IntOffset.Zero
                  }
                  .size(width = itemW, height = itemH)
                  .then(dragMod)
                  .graphicsLayer {
                    if (isBeingDragged) {
                      scaleX = 1.07f
                      scaleY = 1.07f
                      alpha = 0.88f
                    }
                  },
              ) {
                when (item) {
                  is HomeScreenItem.AppShortcut -> {
                    LauncherAppIconCell(
                      label = item.displayLabel,
                      packageName = item.packageName,
                      activityName = item.activityName,
                      userSerialNumber = item.userSerialNumber,
                      isWorkProfile = item.isWorkProfile,
                      isClonedCopy = item.copyInstanceId.isNotEmpty(),
                      badgeCount = item.badgeCount,
                      settings = settings,
                      appsRepository = appsRepository,
                      onClick = { onLaunchApp(item.packageName, item.activityName, item.userSerialNumber) },
                      onLongClick = { onLongPressApp(item) },
                      modifier = Modifier.fillMaxSize(),
                    )
                  }

                  is HomeScreenItem.DeepShortcut -> {
                    LauncherAppIconCell(
                      label = item.label,
                      packageName = item.packageName,
                      activityName = "",
                      userSerialNumber = item.userSerialNumber,
                      isWorkProfile = false,
                      isClonedCopy = false,
                      badgeCount = 0,
                      settings = settings,
                      appsRepository = appsRepository,
                      onClick = { onLaunchDeepShortcut(item.packageName, item.shortcutId, item.userSerialNumber) },
                      onLongClick = { onLongPressWidget(item) },
                      modifier = Modifier.fillMaxSize(),
                    )
                  }

                  is HomeScreenItem.Folder -> {
                    SmartFolderCard(
                      folder = item,
                      settings = settings,
                      appsRepository = appsRepository,
                      isHighlighted = state.selectedFolderMenuId == item.id,
                      isResizing = state.resizingItemId == item.id,
                      onOpenFolder = { onOpenFolder(item.id) },
                      onLongPressFolder = { onLongPressFolder(item.id) },
                      onLaunchChild = { child ->
                        onLaunchApp(child.packageName, child.activityName, child.userSerialNumber)
                      },
                      onResizeSpan = { sx, sy -> onResizeItemSpan(item.id, sx, sy) },
                      onExitResize = onExitResizeMode,
                    )
                  }

                  is HomeScreenItem.BuiltInWidget -> {
                    when (item.widgetType) {
                      BuiltInWidgetType.CLOCK_PRAYER_BAR -> {
                        if (settings.showTopClockWidget) {
                          Box(
                            modifier = Modifier
                              .fillMaxSize()
                              .padding(4.dp)
                              .combinedClickable(
                                onClick = onOpenSettings,
                                onLongClick = { onLongPressWidget(item) },
                              ),
                          ) {
                            TopClockPrayerWidget(
                              schedule = state.schedule,
                              settings = settings,
                              onOpenSettings = onOpenSettings,
                              modifier = Modifier.fillMaxSize(),
                            )
                          }
                        }
                      }
                      BuiltInWidgetType.FLOATING_TOOLS_CARD -> {
                        FloatingToolsBuiltInWidget(
                          isRtl = isRtl,
                          settings = settings,
                          onOpenMiniApp = onOpenFloatingMiniApp,
                          modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        )
                      }
                      BuiltInWidgetType.QUICK_WEATHER_DATE -> {
                        TopClockPrayerWidget(
                          schedule = state.schedule,
                          settings = settings.copy(showPrayerBar = false),
                          onOpenSettings = onOpenSettings,
                          modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        )
                      }
                    }
                  }

                  is HomeScreenItem.SystemAppWidget -> {
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .combinedClickable(
                          onClick = {},
                          onLongClick = { onLongPressWidget(item) },
                        ),
                    ) {
                      SystemAppWidgetCard(
                        appWidgetId = item.appWidgetId,
                        widgetHostManager = widgetHostManager,
                        settings = settings,
                      )
                      if (state.resizingItemId == item.id) {
                        InteractiveResizeOverlay(
                          currentSpanX = item.position.spanX,
                          currentSpanY = item.position.spanY,
                          maxCols = cols,
                          maxRows = rows,
                          accent = accent,
                          onResizeSpan = { sx, sy -> onResizeItemSpan(item.id, sx, sy) },
                          onDone = onExitResizeMode,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Page Indicator Dots
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        repeat(state.pageCount) { idx ->
          val active = pagerState.currentPage == idx
          Box(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .size(width = if (active) 20.dp else 7.dp, height = 7.dp)
              .clip(CircleShape)
              .background(if (active) accent else Color.White.copy(alpha = 0.35f))
              .clickable {
                scope.launch { pagerState.animateScrollToPage(idx) }
              },
          )
        }
      }

      // Floating Glassmorphic Search Pill ("بحث")
      val isLight = settings.themeMode == ThemeMode.LIGHT_AIR
      val pillTextColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(glassContainerColor(settings, boostAlpha = 0.08f))
          .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(24.dp))
          .clickable(onClick = onOpenSearch)
          .padding(horizontal = 14.dp, vertical = 8.dp)
          .testTag("bottom_search_pill"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = "Search",
          tint = accent,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRtl) "بحث في التطبيقات والاختصارات..." else "Search apps & shortcuts...",
          fontFamily = fontFamily,
          fontSize = 13.sp,
          color = pillTextColor.copy(alpha = 0.78f),
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = { onOpenFloatingMiniApp(FloatingMiniAppType.QUICK_APPS) },
          modifier = Modifier
            .size(32.dp)
            .testTag("quick_floating_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PictureInPictureAlt,
            contentDescription = "Floating Windows",
            tint = pillTextColor.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onOpenDrawer,
          modifier = Modifier
            .size(32.dp)
            .testTag("quick_drawer_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = "App Drawer",
            tint = pillTextColor.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier
            .size(32.dp)
            .testTag("quick_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Settings",
            tint = pillTextColor.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Glassmorphic Bottom Dock (4-7 icons)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(28.dp))
          .background(glassContainerColor(settings, boostAlpha = 0.10f))
          .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(28.dp))
          .padding(horizontal = 8.dp, vertical = 8.dp)
          .testTag("bottom_dock"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val dockList = state.dockedItems.take(settings.dockIconCount)
        if (dockList.isEmpty()) {
          Text(
            text = if (isRtl) "اسحب التطبيقات إلى شريط التثبيت" else "Drag apps to Dock",
            color = pillTextColor.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 12.dp),
          )
        } else {
          dockList.forEach { item ->
            Box(modifier = Modifier.weight(1f)) {
              when (item) {
                is HomeScreenItem.AppShortcut -> {
                  LauncherAppIconCell(
                    label = item.displayLabel,
                    packageName = item.packageName,
                    activityName = item.activityName,
                    userSerialNumber = item.userSerialNumber,
                    isWorkProfile = item.isWorkProfile,
                    isClonedCopy = item.copyInstanceId.isNotEmpty(),
                    badgeCount = item.badgeCount,
                    settings = settings,
                    appsRepository = appsRepository,
                    showLabelOverride = false,
                    onClick = { onLaunchApp(item.packageName, item.activityName, item.userSerialNumber) },
                    onLongClick = { onLongPressApp(item) },
                  )
                }
                is HomeScreenItem.Folder -> {
                  SmartFolderCard(
                    folder = item,
                    settings = settings,
                    appsRepository = appsRepository,
                    isHighlighted = false,
                    isResizing = false,
                    onOpenFolder = { onOpenFolder(item.id) },
                    onLongPressFolder = { onLongPressFolder(item.id) },
                    onLaunchChild = { child ->
                      onLaunchApp(child.packageName, child.activityName, child.userSerialNumber)
                    },
                    onResizeSpan = { _, _ -> },
                    onExitResize = {},
                  )
                }
                else -> Unit
              }
            }
          }
        }
      }
    }
  }

  if (showCreateFolderDialog) {
    var folderName by remember { mutableStateOf(if (isRtl) "مجلد ذكي" else "Smart Folder") }
    var selectedMode by remember { mutableStateOf(FolderDisplayMode.BENTO_2X2) }
    AlertDialog(
      onDismissRequest = { showCreateFolderDialog = false },
      title = { Text(if (isRtl) "إنشاء مجلد ذكي" else "Create Smart Folder") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = folderName,
            onValueChange = { folderName = it },
            singleLine = true,
            label = { Text(if (isRtl) "اسم المجلد" else "Folder Name") },
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              FolderDisplayMode.COMPACT_1X1 to "1×1",
              FolderDisplayMode.WIDE_2X1 to "2×1",
              FolderDisplayMode.BENTO_2X2 to "2×2",
              FolderDisplayMode.BANNER_4X2 to "4×2",
            ).forEach { (mode, label) ->
              val active = selectedMode == mode
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (active) accent.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f))
                  .border(1.dp, if (active) accent else Color.Transparent, RoundedCornerShape(10.dp))
                  .clickable { selectedMode = mode }
                  .padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (folderName.isNotBlank()) {
              onCreateSmartFolder(folderName, selectedMode, pagerState.currentPage)
            }
            showCreateFolderDialog = false
          },
        ) {
          Text(if (isRtl) "إنشاء" else "Create")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCreateFolderDialog = false }) {
          Text(if (isRtl) "إلغاء" else "Cancel")
        }
      },
    )
  }
}

@Composable
private fun HomeQuickToolsBar(
  isRtl: Boolean,
  isArrangeMode: Boolean,
  accent: Color,
  settings: com.example.domain.model.LauncherSettings,
  onToggleArrange: () -> Unit,
  onCreateFolder: () -> Unit,
  onOpenWidgets: () -> Unit,
  onOpenFloating: () -> Unit,
  onOpenSettings: () -> Unit,
  onClose: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(glassContainerColor(settings, boostAlpha = 0.16f))
      .border(1.dp, glassBorderBrush(settings, accent), RoundedCornerShape(20.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    QuickBarChip(
      icon = Icons.Rounded.OpenWith,
      label = if (isArrangeMode) (if (isRtl) "إنهاء الترتيب" else "Done") else (if (isRtl) "ترتيب" else "Arrange"),
      highlighted = isArrangeMode,
      accent = accent,
      onClick = onToggleArrange,
    )
    QuickBarChip(
      icon = Icons.Rounded.CreateNewFolder,
      label = if (isRtl) "مجلد" else "Folder",
      highlighted = false,
      accent = accent,
      onClick = onCreateFolder,
    )
    QuickBarChip(
      icon = Icons.Rounded.Widgets,
      label = if (isRtl) "أدوات" else "Widgets",
      highlighted = false,
      accent = accent,
      onClick = onOpenWidgets,
    )
    QuickBarChip(
      icon = Icons.Rounded.PictureInPictureAlt,
      label = if (isRtl) "عائم" else "Float",
      highlighted = false,
      accent = accent,
      onClick = onOpenFloating,
    )
    IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
      Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp))
    }
    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
      Icon(Icons.Rounded.Check, contentDescription = "Close", tint = accent, modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
private fun QuickBarChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  highlighted: Boolean,
  accent: Color,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (highlighted) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(15.dp))
    Spacer(modifier = Modifier.width(4.dp))
    Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
private fun FloatingToolsBuiltInWidget(
  isRtl: Boolean,
  settings: com.example.domain.model.LauncherSettings,
  onOpenMiniApp: (FloatingMiniAppType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = parseColorHex(settings.accentColorHex)
  val shape = RoundedCornerShape(22.dp)
  Row(
    modifier = modifier
      .clip(shape)
      .background(glassContainerColor(settings, boostAlpha = 0.08f))
      .border(1.dp, glassBorderBrush(settings, accent), shape)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MiniToolChip(Icons.Rounded.Language, if (isRtl) "متصفح" else "Browser", accent) {
      onOpenMiniApp(FloatingMiniAppType.BROWSER)
    }
    MiniToolChip(Icons.Rounded.NoteAlt, if (isRtl) "ملاحظات" else "Notes", accent) {
      onOpenMiniApp(FloatingMiniAppType.NOTES)
    }
    MiniToolChip(Icons.Rounded.Calculate, if (isRtl) "حاسبة" else "Calc", accent) {
      onOpenMiniApp(FloatingMiniAppType.CALCULATOR)
    }
    MiniToolChip(Icons.Rounded.Apps, if (isRtl) "سريع" else "Quick", accent) {
      onOpenMiniApp(FloatingMiniAppType.QUICK_APPS)
    }
  }
}

@Composable
private fun MiniToolChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  accent: Color,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
    Text(text = label, color = Color.White, fontSize = 10.sp)
  }
}

private fun androidx.compose.ui.graphics.GraphicsLayerScope.applyPageTransition(
  style: PageTransitionStyle,
  pageOffset: Float,
  isRtl: Boolean,
) {
  val dir = if (isRtl) -1f else 1f
  val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
  when (style) {
    PageTransitionStyle.PARALLAX -> {
      translationX = pageOffset * size.width * 0.14f * dir
      alpha = 1f - absOffset * 0.25f
    }
    PageTransitionStyle.CUBE -> {
      cameraDistance = 16f * density
      transformOrigin = if (pageOffset < 0f) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
      rotationY = -45f * pageOffset * dir
      alpha = 1f - absOffset * 0.2f
    }
    PageTransitionStyle.STACK -> {
      if (pageOffset < 0f) {
        translationX = -pageOffset * size.width * dir
        val scale = 1f - absOffset * 0.18f
        scaleX = scale
        scaleY = scale
        alpha = 1f - absOffset * 0.35f
      }
    }
    PageTransitionStyle.DEPTH -> {
      val scale = 1f - absOffset * 0.22f
      scaleX = scale
      scaleY = scale
      alpha = 1f - absOffset * 0.55f
    }
    PageTransitionStyle.FADE -> {
      alpha = 1f - absOffset
    }
    PageTransitionStyle.ACCORDION -> {
      transformOrigin = if (pageOffset < 0f) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
      scaleX = (1f - absOffset).coerceAtLeast(0.15f)
      alpha = 1f - absOffset * 0.2f
    }
  }
}
