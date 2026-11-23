package com.example.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.FolderDisplayMode
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.LauncherSettings
import com.example.domain.model.ThemeMode
import com.example.ui.components.AppDrawableIcon
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.theme.toComposeFontWeight

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmartFolderCard(
  folder: HomeScreenItem.Folder,
  settings: LauncherSettings,
  appsRepository: InstalledAppsRepository,
  isHighlighted: Boolean,
  isResizing: Boolean,
  onOpenFolder: () -> Unit,
  onLongPressFolder: () -> Unit,
  onLaunchChild: (HomeScreenItem.AppShortcut) -> Unit,
  onResizeSpan: (spanX: Int, spanY: Int) -> Unit,
  onExitResize: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = parseColorHex(folder.accentColorHex ?: settings.accentColorHex)
  val isLight = settings.themeMode == ThemeMode.LIGHT_AIR
  val textColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  val isCompact = folder.displayMode == FolderDisplayMode.COMPACT_1X1 &&
    folder.position.spanX == 1 && folder.position.spanY == 1

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("folder_card_${folder.id}"),
  ) {
    if (isCompact) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(18.dp))
          .combinedClickable(
            onClick = onOpenFolder,
            onLongClick = onLongPressFolder,
          )
          .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Box(contentAlignment = Alignment.TopEnd) {
          Box(
            modifier = Modifier
              .size(settings.iconSizeDp.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(glassContainerColor(settings, boostAlpha = 0.08f))
              .border(
                width = if (isHighlighted || isResizing) 2.dp else 1.dp,
                brush = if (isHighlighted || isResizing) {
                  androidx.compose.ui.graphics.SolidColor(accent)
                } else {
                  glassBorderBrush(settings, accent)
                },
                shape = RoundedCornerShape(16.dp),
              )
              .padding(6.dp),
          ) {
            val previewApps = folder.children.take(4)
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              for (row in 0..1) {
                Row(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  for (col in 0..1) {
                    val child = previewApps.getOrNull(row * 2 + col)
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                      contentAlignment = Alignment.Center,
                    ) {
                      if (child != null) {
                        AppDrawableIcon(
                          packageName = child.packageName,
                          activityName = child.activityName,
                          userSerialNumber = child.userSerialNumber,
                          fallbackLabel = child.displayLabel,
                          appsRepository = appsRepository,
                          iconShape = settings.iconShape,
                          iconSize = ((settings.iconSizeDp - 16) / 2).coerceAtLeast(16).dp,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          if (folder.totalBadgeCount > 0) {
            Box(
              modifier = Modifier
                .offset(x = 4.dp, y = (-4).dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .border(1.5.dp, Color.White, CircleShape)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
              Text(
                text = folder.totalBadgeCount.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        if (settings.showIconLabels) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = folder.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
              fontFamily = fontFamily,
              fontWeight = settings.labelFontWeight.toComposeFontWeight(),
              fontSize = settings.labelFontSizeSp.sp,
              color = textColor,
              shadow = if (settings.labelShadowEnabled) {
                Shadow(Color.Black.copy(alpha = 0.65f), Offset(0f, 1.5f), 4f)
              } else {
                null
              },
            ),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    } else {
      // Enlarged Bento Smart Folder Card
      val cardShape = RoundedCornerShape(24.dp)
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(4.dp)
          .clip(cardShape)
          .background(glassContainerColor(settings, boostAlpha = 0.08f))
          .border(
            width = if (isHighlighted || isResizing) 2.2.dp else 1.dp,
            brush = if (isHighlighted || isResizing) {
              androidx.compose.ui.graphics.SolidColor(accent)
            } else {
              glassBorderBrush(settings, accent)
            },
            shape = cardShape,
          )
          .combinedClickable(
            onClick = onOpenFolder,
            onLongClick = onLongPressFolder,
          )
          .padding(horizontal = 10.dp, vertical = 8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = folder.title,
            fontFamily = fontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (folder.totalBadgeCount > 0) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
              Text(
                text = folder.totalBadgeCount.toString(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val cols = when {
          folder.position.spanX >= 4 -> 5
          folder.position.spanX >= 3 -> 4
          folder.position.spanX == 2 -> 3
          else -> 2
        }
        val rows = when {
          folder.position.spanY >= 3 -> 3
          folder.position.spanY == 2 -> 2
          else -> 1
        }
        val maxDirectSlots = (cols * rows)
        val hasOverflow = folder.children.size > maxDirectSlots
        val visibleCount = if (hasOverflow) (maxDirectSlots - 1).coerceAtLeast(1) else folder.children.size
        val miniIconSize = if (rows == 1) 34.dp else 38.dp

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.SpaceEvenly,
        ) {
          for (r in 0 until rows) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              for (c in 0 until cols) {
                val index = r * cols + c
                when {
                  index < visibleCount -> {
                    val child = folder.children[index]
                    Box(
                      modifier = Modifier
                        .size(miniIconSize + 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                          onClick = { onLaunchChild(child) },
                          onLongClick = onLongPressFolder,
                        ),
                      contentAlignment = Alignment.Center,
                    ) {
                      AppDrawableIcon(
                        packageName = child.packageName,
                        activityName = child.activityName,
                        userSerialNumber = child.userSerialNumber,
                        fallbackLabel = child.displayLabel,
                        appsRepository = appsRepository,
                        iconShape = settings.iconShape,
                        iconSize = miniIconSize,
                      )
                    }
                  }
                  hasOverflow && index == maxDirectSlots - 1 -> {
                    Box(
                      modifier = Modifier
                        .size(miniIconSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.24f))
                        .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenFolder),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = "+${folder.children.size - visibleCount}",
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                  else -> {
                    Spacer(modifier = Modifier.size(miniIconSize + 4.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    if (isResizing) {
      InteractiveResizeOverlay(
        currentSpanX = folder.position.spanX,
        currentSpanY = folder.position.spanY,
        maxCols = settings.gridColumns,
        maxRows = settings.gridRows,
        accent = accent,
        onResizeSpan = onResizeSpan,
        onDone = onExitResize,
      )
    }
  }
}

@Composable
fun InteractiveResizeOverlay(
  currentSpanX: Int,
  currentSpanY: Int,
  maxCols: Int,
  maxRows: Int,
  accent: Color,
  onResizeSpan: (spanX: Int, spanY: Int) -> Unit,
  onDone: () -> Unit,
) {
  var dragAccumX by remember(currentSpanX) { mutableFloatStateOf(0f) }
  var dragAccumY by remember(currentSpanY) { mutableFloatStateOf(0f) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(2.dp)
      .border(2.dp, accent, RoundedCornerShape(24.dp))
      .background(accent.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
  ) {
    // Live grid span indicator & quick Done pill
    Row(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .offset(y = (-12).dp)
        .clip(CircleShape)
        .background(Color(0xFF0F172A))
        .border(1.dp, accent, CircleShape)
        .clickable(onClick = onDone)
        .padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${currentSpanX}×${currentSpanY}",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = "Done resizing",
        tint = accent,
        modifier = Modifier.size(14.dp),
      )
    }

    // Draggable bottom-end resize handle
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .offset(x = 6.dp, y = 6.dp)
        .size(32.dp)
        .clip(CircleShape)
        .background(accent)
        .border(2.dp, Color.White, CircleShape)
        .pointerInput(currentSpanX, currentSpanY, maxCols, maxRows) {
          detectDragGestures(
            onDrag = { change, dragAmount ->
              change.consume()
              dragAccumX += dragAmount.x
              dragAccumY += dragAmount.y
              val stepPx = 110f
              var nextX = currentSpanX
              var nextY = currentSpanY
              if (dragAccumX > stepPx) {
                nextX = (currentSpanX + 1).coerceAtMost(maxCols)
                dragAccumX = 0f
              } else if (dragAccumX < -stepPx) {
                nextX = (currentSpanX - 1).coerceAtLeast(1)
                dragAccumX = 0f
              }
              if (dragAccumY > stepPx) {
                nextY = (currentSpanY + 1).coerceAtMost(maxRows)
                dragAccumY = 0f
              } else if (dragAccumY < -stepPx) {
                nextY = (currentSpanY - 1).coerceAtLeast(1)
                dragAccumY = 0f
              }
              if (nextX != currentSpanX || nextY != currentSpanY) {
                onResizeSpan(nextX, nextY)
              }
            },
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.OpenInFull,
        contentDescription = "Drag to resize",
        tint = Color.White,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}
