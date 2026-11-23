package com.example.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.domain.model.BuiltInWidgetType
import com.example.domain.model.LauncherSettings
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.theme.parseColorHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
  providers: List<AppWidgetProviderInfo>,
  filterPackageName: String?,
  settings: LauncherSettings,
  onSelectBuiltIn: (BuiltInWidgetType, Int, Int) -> Unit,
  onSelectSystemWidget: (AppWidgetProviderInfo) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF0F172A).copy(alpha = 0.94f),
    contentColor = Color.White,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 560.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.Widgets,
            contentDescription = null,
            tint = accent,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isRtl) "التطبيقات المصغرة" else "Widgets",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_widget_picker"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close",
            tint = Color.White,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (filterPackageName == null) {
          item {
            Text(
              text = if (isRtl) "أدوات رواق المدمجة" else "Built-in Rawaq Widgets",
              fontSize = 12.sp,
              color = accent,
              fontWeight = FontWeight.SemiBold,
            )
          }

          item {
            BuiltInWidgetOptionCard(
              title = if (isRtl) "الساعة ومواقيت الصلاة (4×2)" else "Clock & Prayer Bar (4×2)",
              subtitle = if (isRtl) "ساعة رقمية، التاريخ الهجري، الطقس ومواقيت الصلاة" else "Digital clock, Hijri date, weather & prayer bar",
              icon = Icons.Rounded.AccessTime,
              settings = settings,
              onClick = { onSelectBuiltIn(BuiltInWidgetType.CLOCK_PRAYER_BAR, 4, 2) },
            )
          }

          item {
            BuiltInWidgetOptionCard(
              title = if (isRtl) "لوحة النوافذ العائمة السريعة (4×1)" else "Floating Mini-Apps Dock (4×1)",
              subtitle = if (isRtl) "متصفح، ملاحظات، حاسبة وتطبيقات سريعة" else "Browser, Notes, Calculator & Quick Apps",
              icon = Icons.Rounded.Apps,
              settings = settings,
              onClick = { onSelectBuiltIn(BuiltInWidgetType.FLOATING_TOOLS_CARD, 4, 1) },
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (isRtl) "أدوات النظام والتطبيقات الكلاسيكية" else "System & Classic App Widgets",
            fontSize = 12.sp,
            color = accent,
            fontWeight = FontWeight.SemiBold,
          )
        }

        if (providers.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(20.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (isRtl) "لا توجد أدوات مصغرة متاحة لهذا التطبيق" else "No system widgets found for this app",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
              )
            }
          }
        } else {
          items(providers, key = { it.provider.flattenToShortString() }) { info ->
            val label = remember(info) { info.loadLabel(context.packageManager) }
            val previewBmp = remember(info) {
              runCatching {
                info.loadPreviewImage(context, 0)?.toBitmap(120, 120)?.asImageBitmap()
                  ?: info.loadIcon(context, 0)?.toBitmap(96, 96)?.asImageBitmap()
              }.getOrNull()
            }
            val cols = ((info.minWidth + 70) / 74).coerceIn(1, settings.gridColumns)
            val rows = ((info.minHeight + 70) / 74).coerceIn(1, 4)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(glassContainerColor(settings))
                .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(18.dp))
                .clickable { onSelectSystemWidget(info) }
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (previewBmp != null) {
                Image(
                  bitmap = previewBmp,
                  contentDescription = label,
                  modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Widgets,
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(36.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = label,
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "${cols}×${rows} • ${info.provider.packageName}",
                  color = Color.White.copy(alpha = 0.65f),
                  fontSize = 11.sp,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BuiltInWidgetOptionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  settings: LauncherSettings,
  onClick: () -> Unit,
) {
  val accent = parseColorHex(settings.accentColorHex)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(glassContainerColor(settings))
      .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(accent.copy(alpha = 0.18f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = accent)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      Text(text = subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
    }
  }
}
