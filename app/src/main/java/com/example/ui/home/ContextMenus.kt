package com.example.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppShortcutInfo
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.LauncherSettings
import com.example.ui.components.glassBorderBrush
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily

@Composable
fun FolderContextMenuOverlay(
  folder: HomeScreenItem.Folder,
  settings: LauncherSettings,
  onToggleShrinkExpand: () -> Unit,
  onEnterInteractiveResize: () -> Unit,
  onRename: (String) -> Unit,
  onOpenFolderToAddApps: () -> Unit,
  onRemoveFolder: () -> Unit,
  onDismiss: () -> Unit,
) {
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  var showRenameDialog by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.45f))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(min = 240.dp, max = 310.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFF0F172A).copy(alpha = 0.92f))
        .border(1.dp, glassBorderBrush(settings, accent), RoundedCornerShape(24.dp))
        .clickable(enabled = false, onClick = {})
        .padding(12.dp)
        .testTag("folder_context_menu"),
    ) {
      Text(
        text = folder.title,
        fontFamily = fontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      )

      HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
      Spacer(modifier = Modifier.height(6.dp))

      val isCompact = folder.displayMode == FolderDisplayMode.COMPACT_1X1
      ContextMenuActionRow(
        icon = Icons.Rounded.ZoomOutMap,
        title = if (isCompact) {
          if (isRtl) "توسيع المجلد (Bento)" else "Expand Folder (Bento)"
        } else {
          if (isRtl) "تصغير المجلد (1×1)" else "Shrink Folder (1×1)"
        },
        accent = accent,
        onClick = {
          onToggleShrinkExpand()
          onDismiss()
        },
        modifier = Modifier.testTag("folder_action_shrink_expand"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.AspectRatio,
        title = if (isRtl) "تحجيم بالمقابض التفاعلية" else "Interactive Resize Handles",
        accent = accent,
        onClick = onEnterInteractiveResize,
        modifier = Modifier.testTag("folder_action_resize"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.DriveFileRenameOutline,
        title = if (isRtl) "إعادة التسمية" else "Rename",
        accent = accent,
        onClick = { showRenameDialog = true },
        modifier = Modifier.testTag("folder_action_rename"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.AddCircleOutline,
        title = if (isRtl) "إضافة تطبيقات" else "Add Apps",
        accent = accent,
        onClick = onOpenFolderToAddApps,
        modifier = Modifier.testTag("folder_action_add_apps"),
      )

      HorizontalDivider(
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.padding(vertical = 4.dp),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.DeleteOutline,
        title = if (isRtl) "إزالة المجلد" else "Remove Folder",
        accent = Color(0xFFF43F5E),
        textColor = Color(0xFFFDA4AF),
        onClick = onRemoveFolder,
        modifier = Modifier.testTag("folder_action_remove"),
      )
    }
  }

  if (showRenameDialog) {
    var draft by remember(folder.title) { mutableStateOf(folder.title) }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text(if (isRtl) "إعادة تسمية المجلد" else "Rename Folder") },
      text = {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it },
          singleLine = true,
          label = { Text(if (isRtl) "اسم المجلد" else "Folder Name") },
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (draft.isNotBlank()) onRename(draft)
            showRenameDialog = false
            onDismiss()
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
}

@Composable
fun AppIconContextMenuOverlay(
  target: AppMenuTarget,
  shortcuts: List<AppShortcutInfo>,
  folders: List<HomeScreenItem.Folder>,
  settings: LauncherSettings,
  onAppInfo: () -> Unit,
  onRemoveFromHome: () -> Unit,
  onUninstall: () -> Unit,
  onOpenWidgets: () -> Unit,
  onOpenFloatingWindow: () -> Unit,
  onDuplicateShortcut: (targetFolderId: Long?) -> Unit,
  onEditCustomLabel: (String?) -> Unit,
  onToggleHidden: () -> Unit,
  onLaunchShortcut: (AppShortcutInfo) -> Unit,
  onPinShortcut: (AppShortcutInfo) -> Unit,
  onDismiss: () -> Unit,
) {
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  var showRenameDialog by remember { mutableStateOf(false) }
  var showDuplicateTargetDialog by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.45f))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(min = 260.dp, max = 330.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
        .border(1.dp, glassBorderBrush(settings, accent), RoundedCornerShape(24.dp))
        .clickable(enabled = false, onClick = {})
        .padding(12.dp)
        .testTag("app_context_menu"),
    ) {
      Text(
        text = target.label,
        fontFamily = fontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      )

      HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
      Spacer(modifier = Modifier.height(6.dp))

      if (shortcuts.isNotEmpty()) {
        Text(
          text = if (isRtl) "الاختصارات السريعة" else "App Shortcuts",
          fontSize = 11.sp,
          color = accent,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
        shortcuts.take(3).forEach { shortcut ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onLaunchShortcut(shortcut) }
              .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.OpenInNew,
              contentDescription = null,
              tint = accent,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = shortcut.shortLabel,
              color = Color.White,
              fontSize = 13.sp,
              modifier = Modifier.weight(1f),
            )
            Icon(
              imageVector = Icons.Rounded.PushPin,
              contentDescription = "Pin shortcut",
              tint = Color.White.copy(alpha = 0.65f),
              modifier = Modifier
                .size(20.dp)
                .clickable { onPinShortcut(shortcut) },
            )
          }
        }
        HorizontalDivider(
          color = Color.White.copy(alpha = 0.12f),
          modifier = Modifier.padding(vertical = 4.dp),
        )
      }

      ContextMenuActionRow(
        icon = Icons.Rounded.PictureInPictureAlt,
        title = if (isRtl) "فتح في نافذة عائمة" else "Open in Floating Window",
        accent = accent,
        onClick = onOpenFloatingWindow,
        modifier = Modifier.testTag("app_action_floating"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.ContentCopy,
        title = if (isRtl) "نسخ الاختصار" else "Duplicate Shortcut",
        accent = accent,
        onClick = {
          if (folders.isEmpty()) onDuplicateShortcut(null) else showDuplicateTargetDialog = true
        },
        modifier = Modifier.testTag("app_action_duplicate"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.Widgets,
        title = if (isRtl) "التطبيقات المصغرة الكلاسيكية" else "Classic Widgets",
        accent = accent,
        onClick = onOpenWidgets,
        modifier = Modifier.testTag("app_action_widgets"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.DriveFileRenameOutline,
        title = if (isRtl) "تعديل الاسم" else "Edit Label",
        accent = accent,
        onClick = { showRenameDialog = true },
        modifier = Modifier.testTag("app_action_rename"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.VisibilityOff,
        title = if (target.isHidden) {
          if (isRtl) "إظهار من الخزنة" else "Unhide from Vault"
        } else {
          if (isRtl) "إخفاء في الخزنة" else "Hide in Vault"
        },
        accent = accent,
        onClick = onToggleHidden,
        modifier = Modifier.testTag("app_action_hide"),
      )

      ContextMenuActionRow(
        icon = Icons.Rounded.Info,
        title = if (isRtl) "معلومات التطبيق" else "App Info",
        accent = accent,
        onClick = onAppInfo,
        modifier = Modifier.testTag("app_action_info"),
      )

      if (target.homeItemId != null) {
        ContextMenuActionRow(
          icon = Icons.Rounded.RemoveCircleOutline,
          title = if (isRtl) "إزالة من الشاشة" else "Remove from Home",
          accent = Color(0xFFF59E0B),
          onClick = onRemoveFromHome,
          modifier = Modifier.testTag("app_action_remove_home"),
        )
      }

      ContextMenuActionRow(
        icon = Icons.Rounded.DeleteOutline,
        title = if (isRtl) "إزالة التثبيت" else "Uninstall",
        accent = Color(0xFFF43F5E),
        textColor = Color(0xFFFDA4AF),
        onClick = onUninstall,
        modifier = Modifier.testTag("app_action_uninstall"),
      )
    }
  }

  if (showRenameDialog) {
    var draft by remember(target.label) { mutableStateOf(target.label) }
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      title = { Text(if (isRtl) "تعديل اسم التطبيق" else "Edit App Label") },
      text = {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it },
          singleLine = true,
          label = { Text(if (isRtl) "الاسم المعروض" else "Display Label") },
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onEditCustomLabel(draft)
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

  if (showDuplicateTargetDialog) {
    AlertDialog(
      onDismissRequest = { showDuplicateTargetDialog = false },
      title = { Text(if (isRtl) "نسخ الاختصار إلى" else "Duplicate Shortcut To") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                showDuplicateTargetDialog = false
                onDuplicateShortcut(null)
              }
              .padding(12.dp),
          ) {
            Text(
              text = if (isRtl) "الشاشة الرئيسية (نسخة جديدة)" else "Home Screen (New Copy)",
              fontWeight = FontWeight.SemiBold,
            )
          }
          folders.forEach { folder ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  showDuplicateTargetDialog = false
                  onDuplicateShortcut(folder.id)
                }
                .padding(12.dp),
            ) {
              Text(text = if (isRtl) "مجلد: ${folder.title}" else "Folder: ${folder.title}")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDuplicateTargetDialog = false }) {
          Text(if (isRtl) "إغلاق" else "Close")
        }
      },
    )
  }
}

@Composable
private fun ContextMenuActionRow(
  icon: ImageVector,
  title: String,
  accent: Color,
  textColor: Color = Color.White,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = accent,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      color = textColor,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
    )
  }
}
