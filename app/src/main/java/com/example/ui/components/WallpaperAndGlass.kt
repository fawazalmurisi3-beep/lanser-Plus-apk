package com.example.ui.components

import android.app.WallpaperManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.IconShapeStyle
import com.example.domain.model.LauncherSettings
import com.example.domain.model.ThemeMode
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily
import com.example.ui.theme.toComposeFontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WallpaperBackdrop(
  settings: LauncherSettings,
  extraBlurDp: Dp = 0.dp,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val systemWallpaper by produceState<Drawable?>(initialValue = null, settings.useSystemWallpaper) {
    value = if (settings.useSystemWallpaper) {
      withContext(Dispatchers.IO) {
        runCatching { WallpaperManager.getInstance(context).peekDrawable() }.getOrNull()
      }
    } else {
      null
    }
  }

  val totalBlur = (settings.wallpaperBlurRadius * 0.45f).dp + extraBlurDp
  val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && totalBlur > 0.dp) {
    Modifier.blur(totalBlur)
  } else {
    Modifier
  }

  Box(modifier = modifier.fillMaxSize()) {
    val wallBitmap = remember(systemWallpaper) {
      runCatching { systemWallpaper?.toBitmap(width = 720, height = 1280)?.asImageBitmap() }.getOrNull()
    }

    if (wallBitmap != null) {
      Image(
        bitmap = wallBitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .then(blurModifier),
      )
    } else if (!settings.useSystemWallpaper || totalBlur > 2.dp || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      Image(
        painter = painterResource(id = R.drawable.img_default_wallpaper_1795447987207),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize()
          .then(blurModifier),
      )
    } else {
      // Built-in rich aurora wallpaper as subtle canvas behind system windowShowWallpaper
      Image(
        painter = painterResource(id = R.drawable.img_default_wallpaper_1795447987207),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.92f,
        modifier = Modifier
          .fillMaxSize()
          .then(blurModifier),
      )
    }

    // Adaptive dim & Android 10-11 frosted scrim fallback
    val extraFallbackAlpha = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && totalBlur > 0.dp) {
      (totalBlur.value / 45f).coerceIn(0f, 0.42f)
    } else {
      0f
    }
    val scrimColor = if (settings.themeMode == ThemeMode.LIGHT_AIR) {
      Color.White.copy(alpha = (settings.wallpaperDimAmount * 0.6f + extraFallbackAlpha).coerceIn(0f, 0.72f))
    } else {
      Color(0xFF050814).copy(alpha = (settings.wallpaperDimAmount + extraFallbackAlpha).coerceIn(0f, 0.78f))
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(scrimColor),
    )
  }
}

@Composable
fun glassContainerColor(
  settings: LauncherSettings,
  boostAlpha: Float = 0f,
): Color {
  val baseAlpha = (settings.glassOpacity + boostAlpha).coerceIn(0.08f, 0.88f)
  return when (settings.themeMode) {
    ThemeMode.LIQUID_GLASS -> Color(0xFF10192D).copy(alpha = baseAlpha)
    ThemeMode.DARK_FROSTED -> Color(0xFF0B0F1A).copy(alpha = (baseAlpha + 0.14f).coerceAtMost(0.9f))
    ThemeMode.LIGHT_AIR -> Color.White.copy(alpha = (baseAlpha + 0.34f).coerceAtMost(0.9f))
    ThemeMode.MATERIAL_YOU -> MaterialTheme.colorScheme.surface.copy(alpha = (baseAlpha + 0.12f).coerceAtMost(0.88f))
  }
}

@Composable
fun glassBorderBrush(
  settings: LauncherSettings,
  accentOverride: Color? = null,
): Brush {
  val isLight = settings.themeMode == ThemeMode.LIGHT_AIR
  val topColor = accentOverride?.copy(alpha = 0.55f)
    ?: if (isLight) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.32f)
  val bottomColor = if (isLight) Color(0xFF94A3B8).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
  return Brush.linearGradient(colors = listOf(topColor, bottomColor))
}

fun IconShapeStyle.toComposeShape(): Shape = when (this) {
  IconShapeStyle.SQUIRCLE -> RoundedCornerShape(percent = 28)
  IconShapeStyle.CIRCLE -> CircleShape
  IconShapeStyle.ROUNDED_SQUARE -> RoundedCornerShape(percent = 20)
  IconShapeStyle.TEARDROP -> RoundedCornerShape(
    topStartPercent = 50,
    topEndPercent = 50,
    bottomEndPercent = 18,
    bottomStartPercent = 50,
  )
  IconShapeStyle.SYSTEM -> RoundedCornerShape(percent = 24)
}

@Composable
fun AppDrawableIcon(
  packageName: String,
  activityName: String,
  userSerialNumber: Long,
  fallbackLabel: String,
  appsRepository: InstalledAppsRepository,
  iconShape: IconShapeStyle,
  iconSize: Dp,
  modifier: Modifier = Modifier,
) {
  val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
    initialValue = null,
    packageName,
    activityName,
    userSerialNumber,
  ) {
    value = withContext(Dispatchers.IO) {
      runCatching {
        appsRepository.getAppIcon(packageName, activityName, userSerialNumber)
          ?.toBitmap(width = 160, height = 160)
          ?.asImageBitmap()
      }.getOrNull()
    }
  }

  val shape = iconShape.toComposeShape()
  Box(
    modifier = modifier
      .size(iconSize)
      .clip(shape),
    contentAlignment = Alignment.Center,
  ) {
    val bmp = bitmap
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = fallbackLabel,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      val hashColor = remember(packageName) {
        val palette = listOf(
          Color(0xFF0EA5E9),
          Color(0xFF10B981),
          Color(0xFFF59E0B),
          Color(0xFFF43F5E),
          Color(0xFF8B5CF6),
          Color(0xFF14B8A6),
        )
        palette[kotlin.math.abs(packageName.hashCode()) % palette.size]
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.linearGradient(
              listOf(hashColor, hashColor.copy(alpha = 0.72f))
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = fallbackLabel.take(1).uppercase(),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = (iconSize.value * 0.42f).sp,
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherAppIconCell(
  label: String,
  packageName: String,
  activityName: String,
  userSerialNumber: Long,
  isWorkProfile: Boolean,
  isClonedCopy: Boolean,
  badgeCount: Int,
  settings: LauncherSettings,
  appsRepository: InstalledAppsRepository,
  iconSizeOverride: Dp? = null,
  showLabelOverride: Boolean? = null,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val iconSize = iconSizeOverride ?: settings.iconSizeDp.dp
  val showLabel = showLabelOverride ?: settings.showIconLabels
  val labelColor = parseColorHex(
    settings.labelColorHex,
    fallback = if (settings.themeMode == ThemeMode.LIGHT_AIR) Color(0xFF0F172A) else Color(0xFFF8FAFC),
  )
  val accentColor = parseColorHex(settings.accentColorHex)

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      )
      .padding(horizontal = 2.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      AppDrawableIcon(
        packageName = packageName,
        activityName = activityName,
        userSerialNumber = userSerialNumber,
        fallbackLabel = label,
        appsRepository = appsRepository,
        iconShape = settings.iconShape,
        iconSize = iconSize,
      )

      if (badgeCount > 0) {
        Box(
          modifier = Modifier
            .offset(x = 4.dp, y = (-4).dp)
            .clip(CircleShape)
            .background(Color(0xFFEF4444))
            .border(1.5.dp, Color.White, CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (isWorkProfile || isClonedCopy) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 2.dp, y = 2.dp)
            .size(18.dp)
            .clip(CircleShape)
            .background(accentColor)
            .border(1.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isWorkProfile) Icons.Rounded.Work else Icons.Rounded.ContentCopy,
            contentDescription = if (isWorkProfile) "Work Profile" else "Copied Shortcut",
            tint = Color.White,
            modifier = Modifier.size(10.dp),
          )
        }
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        style = TextStyle(
          fontFamily = settings.labelFontFamily.toComposeFontFamily(),
          fontWeight = settings.labelFontWeight.toComposeFontWeight(),
          fontSize = settings.labelFontSizeSp.sp,
          color = labelColor,
          shadow = if (settings.labelShadowEnabled) {
            Shadow(
              color = Color.Black.copy(alpha = 0.65f),
              offset = Offset(0f, 1.5f),
              blurRadius = 4f,
            )
          } else {
            null
          },
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
