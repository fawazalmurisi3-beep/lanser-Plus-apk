package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.InstalledApp
import com.example.domain.model.LauncherSettings
import com.example.ui.components.AppDrawableIcon
import com.example.ui.components.LauncherAppIconCell
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily

@Composable
fun ExpandedFolderOverlay(
  folder: HomeScreenItem.Folder,
  allInstalledApps: List<InstalledApp>,
  settings: LauncherSettings,
  appsRepository: InstalledAppsRepository,
  onLaunchApp: (HomeScreenItem.AppShortcut) -> Unit,
  onLongPressChild: (HomeScreenItem.AppShortcut) -> Unit,
  onRenameFolder: (String) -> Unit,
  onAddAppToFolder: (InstalledApp) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val isRtl = settings.forceRtl
  val accent = parseColorHex(folder.accentColorHex ?: settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  val pages = remember(folder.children) { folder.children.chunked(9).ifEmpty { listOf(emptyList()) } }
  val pagerState = rememberPagerState(pageCount = { pages.size })
  var showRenameDialog by remember { mutableStateOf(false) }
  var showAddAppsDialog by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.56f))
      .clickable(onClick = onDismiss)
      .testTag("expanded_folder_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .clip(RoundedCornerShape(32.dp))
        .background(glassContainerColor(settings, boostAlpha = 0.25f))
        .border(1.2.dp, glassBorderBrush(settings, accent), RoundedCornerShape(32.dp))
        .clickable(enabled = false, onClick = {})
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showRenameDialog = true }
            .padding(4.dp),
        ) {
          Text(
            text = folder.title,
            fontFamily = fontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Rounded.DriveFileRenameOutline,
            contentDescription = "Rename",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp),
          )
        }

        IconButton(
          onClick = { showAddAppsDialog = true },
          modifier = Modifier.testTag("folder_add_apps_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = if (isRtl) "إضافة تطبيقات" else "Add Apps",
            tint = accent,
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_expanded_folder"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close",
            tint = Color.White,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 240.dp, max = 320.dp),
      ) { pageIdx ->
        val pageApps = pages[pageIdx]
        if (pageApps.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = if (isRtl) "المجلد فارغ — اضغط + لإضافة تطبيقات" else "Empty folder — tap + to add apps",
              color = Color.White.copy(alpha = 0.7f),
              fontSize = 13.sp,
            )
          }
        } else {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            pageApps.chunked(3).forEach { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
              ) {
                rowItems.forEach { child ->
                  Box(modifier = Modifier.weight(1f)) {
                    LauncherAppIconCell(
                      label = child.displayLabel,
                      packageName = child.packageName,
                      activityName = child.activityName,
                      userSerialNumber = child.userSerialNumber,
                      isWorkProfile = child.isWorkProfile,
                      isClonedCopy = child.copyInstanceId.isNotEmpty(),
                      badgeCount = child.badgeCount,
                      settings = settings,
                      appsRepository = appsRepository,
                      onClick = { onLaunchApp(child) },
                      onLongClick = { onLongPressChild(child) },
                    )
                  }
                }
                repeat(3 - rowItems.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      if (pages.size > 1) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          repeat(pages.size) { idx ->
            val selected = pagerState.currentPage == idx
            Box(
              modifier = Modifier
                .size(if (selected) 8.dp else 6.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.White.copy(alpha = 0.35f)),
            )
          }
        }
      }
    }
  }

  if (showRenameDialog) {
    var draft by remember(folder.title) { mutableStateOf(folder.title) }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text(if (isRtl) "اسم المجلد" else "Folder Title") },
      text = {
        OutlinedTextField(value = draft, onValueChange = { draft = it }, singleLine = true)
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (draft.isNotBlank()) onRenameFolder(draft)
            showRenameDialog = false
          },
        ) {
          Text(if (isRtl) "حفظ" else "Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) {
          Text(if (isRtl) "إلغاء" else "Cancel")
        }
      },
    )
  }

  if (showAddAppsDialog) {
    AlertDialog(
      onDismissRequest = { showAddAppsDialog = false },
      title = { Text(if (isRtl) "إضافة تطبيقات إلى ${folder.title}" else "Add Apps to ${folder.title}") },
      text = {
        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier.heightIn(max = 360.dp),
          contentPadding = PaddingValues(4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(allInstalledApps, key = { it.uniqueKey }) { app ->
            Column(
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  onAddAppToFolder(app)
                  showAddAppsDialog = false
                }
                .padding(6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              AppDrawableIcon(
                packageName = app.packageName,
                activityName = app.activityName,
                userSerialNumber = app.userSerialNumber,
                fallbackLabel = app.displayLabel,
                appsRepository = appsRepository,
                iconShape = settings.iconShape,
                iconSize = 44.dp,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = app.displayLabel,
                fontSize = 11.sp,
                maxLines = 1,
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAddAppsDialog = false }) {
          Text(if (isRtl) "إغلاق" else "Done")
        }
      },
    )
  }
}
