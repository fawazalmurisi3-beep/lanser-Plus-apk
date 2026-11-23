package com.example.ui.floating

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.FloatingMiniAppType
import com.example.domain.model.FloatingNote
import com.example.domain.model.InstalledApp
import com.example.domain.model.LauncherSettings
import com.example.ui.components.AppDrawableIcon
import com.example.ui.components.glassBorderBrush
import com.example.ui.theme.parseColorHex
import kotlin.math.roundToInt

@Composable
fun FloatingWindowWorkspaceOverlay(
  activeType: FloatingMiniAppType,
  isMinimized: Boolean,
  installedApps: List<InstalledApp>,
  notes: List<FloatingNote>,
  settings: LauncherSettings,
  appsRepository: InstalledAppsRepository,
  onSelectType: (FloatingMiniAppType) -> Unit,
  onToggleMinimize: () -> Unit,
  onClose: () -> Unit,
  onSaveNote: (FloatingNote) -> Unit,
  onDeleteNote: (Long) -> Unit,
  onLaunchAppFreeform: (InstalledApp) -> Unit,
) {
  val context = LocalContext.current
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)

  var offsetX by remember { mutableFloatStateOf(24f) }
  var offsetY by remember { mutableFloatStateOf(140f) }
  var widthDp by remember { mutableFloatStateOf(330f) }
  var heightDp by remember { mutableFloatStateOf(440f) }
  var isMaximized by remember { mutableStateOf(false) }

  if (isMinimized) {
    Box(
      modifier = Modifier
        .fillMaxSize(),
    ) {
      Box(
        modifier = Modifier
          .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
          .size(58.dp)
          .clip(CircleShape)
          .background(Color(0xFF0F172A).copy(alpha = 0.92f))
          .border(2.dp, accent, CircleShape)
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              offsetX += dragAmount.x
              offsetY += dragAmount.y
            }
          }
          .clickable(onClick = onToggleMinimize)
          .testTag("floating_edge_bubble"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.PictureInPictureAlt,
          contentDescription = "Restore Floating Window",
          tint = accent,
          modifier = Modifier.size(26.dp),
        )
      }
    }
    return
  }

  val windowModifier = if (isMaximized) {
    Modifier
      .fillMaxSize()
      .padding(12.dp)
  } else {
    Modifier
      .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
      .size(width = widthDp.dp, height = heightDp.dp)
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = windowModifier
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFF0F172A).copy(alpha = 0.96f))
        .border(1.5.dp, glassBorderBrush(settings, accent), RoundedCornerShape(24.dp))
        .testTag("floating_window_container"),
    ) {
      // Draggable Top Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White.copy(alpha = 0.08f))
          .pointerInput(isMaximized) {
            if (!isMaximized) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y
              }
            }
          }
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Mini-App Switcher Tabs
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FloatingMiniAppType.entries.forEach { tab ->
            val selected = tab == activeType
            val icon = when (tab) {
              FloatingMiniAppType.BROWSER -> Icons.Rounded.Language
              FloatingMiniAppType.NOTES -> Icons.Rounded.NoteAlt
              FloatingMiniAppType.CALCULATOR -> Icons.Rounded.Calculate
              FloatingMiniAppType.QUICK_APPS -> Icons.Rounded.Apps
            }
            IconButton(
              onClick = { onSelectType(tab) },
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) accent.copy(alpha = 0.25f) else Color.Transparent),
            ) {
              Icon(icon, contentDescription = tab.name, tint = if (selected) accent else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
          }
        }

        IconButton(onClick = onToggleMinimize, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Rounded.Minimize, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { isMaximized = !isMaximized }, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = if (isMaximized) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = "Maximize",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
        }
        IconButton(
          onClick = onClose,
          modifier = Modifier
            .size(32.dp)
            .testTag("close_floating_window"),
        ) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
        }
      }

      // System Overlay Permission Prompt Banner if missing
      if (!Settings.canDrawOverlays(context)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.16f))
            .clickable {
              runCatching {
                val intent = Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
              }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isRtl) "انقر لتفعيل إذن الظهور فوق التطبيقات الأخرى" else "Tap to grant System Overlay permission",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      // Active Mini-App Body
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        when (activeType) {
          FloatingMiniAppType.BROWSER -> FloatingBrowserContent(isRtl = isRtl, accent = accent)
          FloatingMiniAppType.NOTES -> FloatingNotesContent(
            isRtl = isRtl,
            accent = accent,
            notes = notes,
            onSaveNote = onSaveNote,
            onDeleteNote = onDeleteNote,
          )
          FloatingMiniAppType.CALCULATOR -> FloatingCalculatorContent(accent = accent)
          FloatingMiniAppType.QUICK_APPS -> FloatingQuickAppsContent(
            isRtl = isRtl,
            apps = installedApps.filter { !it.isHidden },
            settings = settings,
            appsRepository = appsRepository,
            onLaunchFreeform = onLaunchAppFreeform,
          )
        }

        // Corner Resize Handle
        if (!isMaximized) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .size(28.dp)
              .clip(RoundedCornerShape(topStart = 12.dp))
              .background(accent.copy(alpha = 0.35f))
              .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                  change.consume()
                  widthDp = (widthDp + dragAmount.x / density).coerceIn(250f, 420f)
                  heightDp = (heightDp + dragAmount.y / density).coerceIn(280f, 700f)
                }
              },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.OpenInFull,
              contentDescription = "Resize",
              tint = Color.White,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FloatingBrowserContent(isRtl: Boolean, accent: Color) {
  var urlInput by remember { mutableStateOf("https://www.google.com") }
  var loadedUrl by remember { mutableStateOf("https://www.google.com") }

  Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = urlInput,
        onValueChange = { urlInput = it },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = accent,
          unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
        ),
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Button(
        onClick = {
          loadedUrl = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
        },
      ) {
        Text(if (isRtl) "فتح" else "Go", fontSize = 12.sp)
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          settings.javaScriptEnabled = true
          webViewClient = WebViewClient()
          loadUrl(loadedUrl)
        }
      },
      update = { webView ->
        if (webView.url != loadedUrl) {
          webView.loadUrl(loadedUrl)
        }
      },
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp)),
    )
  }
}

@Composable
private fun FloatingNotesContent(
  isRtl: Boolean,
  accent: Color,
  notes: List<FloatingNote>,
  onSaveNote: (FloatingNote) -> Unit,
  onDeleteNote: (Long) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var body by remember { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
    OutlinedTextField(
      value = title,
      onValueChange = { title = it },
      singleLine = true,
      placeholder = { Text(if (isRtl) "عنوان الملاحظة" else "Note title", color = Color.White.copy(alpha = 0.5f)) },
      colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = body,
        onValueChange = { body = it },
        placeholder = { Text(if (isRtl) "اكتب ملاحظة سريعة..." else "Write a quick note...", color = Color.White.copy(alpha = 0.5f)) },
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      IconButton(
        onClick = {
          if (title.isNotBlank() || body.isNotBlank()) {
            onSaveNote(FloatingNote(title = title.ifBlank { if (isRtl) "ملاحظة" else "Note" }, content = body))
            title = ""
            body = ""
          }
        },
        modifier = Modifier.clip(CircleShape).background(accent),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = "Save note", tint = Color.Black)
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(notes, key = { it.id }) { note ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = note.title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (note.content.isNotBlank()) {
              Text(text = note.content, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
          }
          IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun FloatingCalculatorContent(accent: Color) {
  var display by remember { mutableStateOf("0") }
  var pendingOp by remember { mutableStateOf<String?>(null) }
  var firstOperand by remember { mutableStateOf<Double?>(null) }
  var clearNext by remember { mutableStateOf(false) }

  fun pressDigit(d: String) {
    if (clearNext || display == "0") {
      display = d
      clearNext = false
    } else {
      display += d
    }
  }

  fun evaluate(op: String, a: Double, b: Double): Double = when (op) {
    "+" -> a + b
    "-" -> a - b
    "×" -> a * b
    "÷" -> if (b != 0.0) a / b else 0.0
    else -> b
  }

  fun pressOp(op: String) {
    val current = display.toDoubleOrNull() ?: 0.0
    if (firstOperand != null && pendingOp != null && !clearNext) {
      val result = evaluate(pendingOp!!, firstOperand!!, current)
      firstOperand = result
      display = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
    } else {
      firstOperand = current
    }
    pendingOp = op
    clearNext = true
  }

  val buttons = listOf(
    listOf("C", "⌫", "÷", "×"),
    listOf("7", "8", "9", "-"),
    listOf("4", "5", "6", "+"),
    listOf("1", "2", "3", "="),
    listOf("0", ".", "", ""),
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(12.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color.Black.copy(alpha = 0.35f))
        .padding(16.dp),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Text(
        text = display,
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      buttons.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          row.forEach { key ->
            if (key.isEmpty()) {
              Spacer(modifier = Modifier.weight(1f))
            } else {
              val isOp = key in listOf("+", "-", "×", "÷", "=")
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (isOp) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f))
                  .clickable {
                    when (key) {
                      "C" -> {
                        display = "0"
                        firstOperand = null
                        pendingOp = null
                      }
                      "⌫" -> display = if (display.length > 1) display.dropLast(1) else "0"
                      "+", "-", "×", "÷" -> pressOp(key)
                      "=" -> {
                        val a = firstOperand
                        val op = pendingOp
                        val b = display.toDoubleOrNull() ?: 0.0
                        if (a != null && op != null) {
                          val res = evaluate(op, a, b)
                          display = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                          firstOperand = null
                          pendingOp = null
                          clearNext = true
                        }
                      }
                      "." -> if (!display.contains(".")) display += "."
                      else -> pressDigit(key)
                    }
                  },
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = key,
                  color = if (isOp) accent else Color.White,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
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
private fun FloatingQuickAppsContent(
  isRtl: Boolean,
  apps: List<InstalledApp>,
  settings: LauncherSettings,
  appsRepository: InstalledAppsRepository,
  onLaunchFreeform: (InstalledApp) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
    Text(
      text = if (isRtl) "اضغط لفتح التطبيق في نافذة حرة" else "Tap to launch in Freeform Window",
      color = Color.White.copy(alpha = 0.72f),
      fontSize = 11.sp,
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      contentPadding = PaddingValues(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(apps, key = { it.uniqueKey }) { app ->
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onLaunchFreeform(app) }
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
            iconSize = 42.dp,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = app.displayLabel, color = Color.White, fontSize = 10.sp, maxLines = 1)
        }
      }
    }
  }
}
