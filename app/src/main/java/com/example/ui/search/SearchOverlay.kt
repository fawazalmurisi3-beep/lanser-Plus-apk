package com.example.ui.search

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.HomeScreenItem
import com.example.domain.model.InstalledApp
import com.example.domain.model.LauncherSettings
import com.example.ui.components.AppDrawableIcon
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily

@Composable
fun SearchOverlay(
  installedApps: List<InstalledApp>,
  folders: List<HomeScreenItem.Folder>,
  settings: LauncherSettings,
  appsRepository: InstalledAppsRepository,
  onLaunchApp: (InstalledApp) -> Unit,
  onLaunchFreeform: (InstalledApp) -> Unit,
  onOpenFolder: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val context = LocalContext.current
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  var query by remember { mutableStateOf("") }

  val matchedApps = remember(installedApps, query) {
    val visible = installedApps.filter { !it.isHidden }
    if (query.isBlank()) {
      visible.sortedByDescending { it.launchCount }.take(12)
    } else {
      visible.filter {
        fuzzyMatch(it.displayLabel, query) || it.packageName.contains(query, ignoreCase = true)
      }
    }
  }

  val matchedFolders = remember(folders, query) {
    if (query.isBlank()) emptyList()
    else folders.filter { it.title.contains(query, ignoreCase = true) }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF060A16).copy(alpha = 0.84f))
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("search_overlay"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          singleLine = true,
          placeholder = {
            Text(
              text = if (isRtl) "ابحث عن تطبيق، مجلد، أو الويب..." else "Search apps, folders, or web...",
              fontFamily = fontFamily,
              color = Color.White.copy(alpha = 0.6f),
            )
          },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = accent) },
          trailingIcon = {
            IconButton(onClick = onDismiss) {
              Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
          },
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("instant_search_input"),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
      ) {
        if (query.isBlank()) {
          item {
            Text(
              text = if (isRtl) "الأكثر استخداماً" else "Suggested Apps",
              color = accent,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            )
          }
          item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              items(matchedApps.take(8), key = { it.uniqueKey }) { app ->
                Column(
                  modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onLaunchApp(app) }
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
                    iconSize = 48.dp,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = app.displayLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        } else {
          if (matchedFolders.isNotEmpty()) {
            item {
              Text(
                text = if (isRtl) "المجلدات" else "Folders",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              )
            }
            items(matchedFolders, key = { it.id }) { folder ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(18.dp))
                  .background(glassContainerColor(settings))
                  .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(18.dp))
                  .clickable { onOpenFolder(folder.id) }
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Rounded.Folder, contentDescription = null, tint = accent)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = folder.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }

          items(matchedApps, key = { it.uniqueKey }) { app ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(glassContainerColor(settings))
                .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(18.dp))
                .clickable { onLaunchApp(app) }
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
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
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(text = app.displayLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = app.packageName, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, maxLines = 1)
              }
              IconButton(onClick = { onLaunchFreeform(app) }) {
                Icon(
                  imageVector = Icons.Rounded.PictureInPictureAlt,
                  contentDescription = "Open Floating",
                  tint = accent,
                )
              }
            }
          }

          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .clickable {
                  val url = "https://www.google.com/search?q=${Uri.encode(query)}"
                  runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                  }
                  onDismiss()
                }
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.Language, contentDescription = null, tint = accent)
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = if (isRtl) "البحث في الويب عن \"$query\"" else "Search Web for \"$query\"",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }
      }
    }
  }
}

private fun fuzzyMatch(text: String, query: String): Boolean {
  if (text.contains(query, ignoreCase = true)) return true
  var qi = 0
  val lowerText = text.lowercase()
  val lowerQuery = query.lowercase()
  for (ch in lowerText) {
    if (qi < lowerQuery.length && ch == lowerQuery[qi]) qi++
  }
  return qi == lowerQuery.length
}
