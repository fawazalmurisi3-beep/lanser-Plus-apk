package com.example.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.system.InstalledAppsRepository
import com.example.domain.model.AppCategory
import com.example.domain.model.InstalledApp
import com.example.domain.model.LauncherSettings
import com.example.ui.components.LauncherAppIconCell
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.home.AppMenuTarget
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily
import kotlinx.coroutines.launch

@Composable
fun AppDrawerOverlay(
  installedApps: List<InstalledApp>,
  settings: LauncherSettings,
  isVaultUnlocked: Boolean,
  appsRepository: InstalledAppsRepository,
  onLaunchApp: (InstalledApp) -> Unit,
  onLongPressApp: (AppMenuTarget) -> Unit,
  onSetVaultUnlocked: (Boolean) -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val context = LocalContext.current
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()

  var query by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
  var showVaultOnly by remember { mutableStateOf(false) }
  var showWorkProfileOnly by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }

  val hasWorkApps = remember(installedApps) { installedApps.any { it.isWorkProfile } }
  val filteredApps = remember(
    installedApps,
    query,
    selectedCategory,
    showVaultOnly,
    showWorkProfileOnly,
    isVaultUnlocked,
  ) {
    installedApps.filter { app ->
      val vaultMatch = if (showVaultOnly) app.isHidden && isVaultUnlocked else !app.isHidden
      val workMatch = if (showWorkProfileOnly) app.isWorkProfile else true
      val catMatch = selectedCategory == AppCategory.ALL || app.category == selectedCategory
      val qMatch = query.isBlank() ||
        app.displayLabel.contains(query, ignoreCase = true) ||
        app.packageName.contains(query, ignoreCase = true)
      vaultMatch && workMatch && catMatch && qMatch
    }
  }

  val indexLetters = remember(isRtl) {
    if (isRtl) {
      listOf("أ", "ب", "ت", "ج", "ح", "د", "ر", "س", "ص", "ع", "ف", "ق", "ك", "ل", "م", "ن", "هـ", "و", "ي", "A", "G", "M", "S")
    } else {
      ('A'..'Z').map { it.toString() }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF060A16).copy(alpha = 0.84f))
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("app_drawer_overlay"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      // Top Search & Vault / Settings Header
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
              text = if (isRtl) "ابحث في جميع التطبيقات..." else "Search all apps...",
              fontFamily = fontFamily,
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 13.sp,
            )
          },
          leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = accent)
          },
          trailingIcon = {
            if (query.isNotEmpty()) {
              IconButton(onClick = { query = "" }) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White)
              }
            }
          },
          shape = RoundedCornerShape(22.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("drawer_search_input"),
        )

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(
          onClick = {
            if (showVaultOnly) {
              showVaultOnly = false
              onSetVaultUnlocked(false)
            } else {
              val activity = context as? FragmentActivity
              if (activity != null) {
                triggerBiometricUnlock(
                  activity = activity,
                  title = if (isRtl) "خزنة التطبيقات المخفية" else "Hidden Apps Vault",
                  subtitle = if (isRtl) "تحقق لفتح التطبيقات المخفية" else "Authenticate to view hidden apps",
                  onSuccess = {
                    onSetVaultUnlocked(true)
                    showVaultOnly = true
                  },
                  onFallbackPin = { showPinDialog = true },
                )
              } else {
                showPinDialog = true
              }
            }
          },
          modifier = Modifier
            .clip(CircleShape)
            .background(if (showVaultOnly) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .testTag("drawer_vault_button"),
        ) {
          Icon(
            imageVector = if (showVaultOnly) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
            contentDescription = "Hidden Apps Vault",
            tint = if (showVaultOnly) accent else Color.White,
          )
        }

        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier.testTag("drawer_settings_button"),
        ) {
          Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_app_drawer"),
        ) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Category Chips + Work Profile Toggle
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
      ) {
        if (hasWorkApps) {
          item {
            val active = showWorkProfileOnly
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (active) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
                .border(1.dp, if (active) accent else Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                .clickable { showWorkProfileOnly = !showWorkProfileOnly }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
              Text(
                text = if (isRtl) "ملف العمل" else "Work",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        items(AppCategory.entries) { category ->
          val active = selectedCategory == category
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(if (active) accent.copy(alpha = 0.28f) else glassContainerColor(settings))
              .border(1.dp, if (active) accent else Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
              .clickable { selectedCategory = category }
              .padding(horizontal = 14.dp, vertical = 7.dp),
          ) {
            Text(
              text = if (isRtl) category.titleAr else category.titleEn,
              fontFamily = fontFamily,
              color = if (active) accent else Color.White.copy(alpha = 0.85f),
              fontSize = 12.sp,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // App Grid + Side Alphabetical Index Scrubber
      Row(modifier = Modifier.weight(1f)) {
        if (filteredApps.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (showVaultOnly) {
                if (isRtl) "الخزنة فارغة حالياً" else "No hidden apps in vault"
              } else {
                if (isRtl) "لا توجد تطبيقات مطابقة" else "No matching apps"
              },
              color = Color.White.copy(alpha = 0.65f),
              fontSize = 14.sp,
            )
          }
        } else {
          LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(settings.gridColumns),
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(filteredApps, key = { it.uniqueKey }) { app ->
              LauncherAppIconCell(
                label = app.displayLabel,
                packageName = app.packageName,
                activityName = app.activityName,
                userSerialNumber = app.userSerialNumber,
                isWorkProfile = app.isWorkProfile,
                isClonedCopy = false,
                badgeCount = app.badgeCount,
                settings = settings,
                appsRepository = appsRepository,
                onClick = { onLaunchApp(app) },
                onLongClick = {
                  onLongPressApp(
                    AppMenuTarget(
                      homeItemId = null,
                      packageName = app.packageName,
                      activityName = app.activityName,
                      label = app.displayLabel,
                      userSerialNumber = app.userSerialNumber,
                      isFromDrawer = true,
                      isHidden = app.isHidden,
                    )
                  )
                },
              )
            }
          }
        }

        // A-Z / أ-ي Side Index Scrubber
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 4.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.SpaceEvenly,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          indexLetters.forEach { letter ->
            Text(
              text = letter,
              color = accent.copy(alpha = 0.9f),
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier
                .clickable {
                  val targetIdx = filteredApps.indexOfFirst {
                    it.displayLabel.startsWith(letter, ignoreCase = true)
                  }
                  if (targetIdx >= 0) {
                    scope.launch { gridState.animateScrollToItem(targetIdx) }
                  }
                }
                .padding(horizontal = 3.dp, vertical = 1.dp),
            )
          }
        }
      }
    }
  }

  if (showPinDialog) {
    var pinInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showPinDialog = false },
      title = { Text(if (isRtl) "أدخل رمز الخزنة (الافتراضي 0000)" else "Enter Vault PIN (Default 0000)") },
      text = {
        OutlinedTextField(
          value = pinInput,
          onValueChange = { pinInput = it },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          label = { Text(if (isRtl) "الرمز السري" else "PIN") },
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            val expected = settings.vaultPinHash.ifBlank { "0000" }
            if (pinInput == expected) {
              onSetVaultUnlocked(true)
              showVaultOnly = true
              showPinDialog = false
            }
          },
        ) {
          Text(if (isRtl) "فتح" else "Unlock")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPinDialog = false }) {
          Text(if (isRtl) "إلغاء" else "Cancel")
        }
      },
    )
  }
}

private fun triggerBiometricUnlock(
  activity: FragmentActivity,
  title: String,
  subtitle: String,
  onSuccess: () -> Unit,
  onFallbackPin: () -> Unit,
) {
  val executor = ContextCompat.getMainExecutor(activity)
  val prompt = BiometricPrompt(
    activity,
    executor,
    object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        onFallbackPin()
      }
    },
  )
  val promptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle(title)
    .setSubtitle(subtitle)
    .setNegativeButtonText("PIN")
    .build()

  runCatching { prompt.authenticate(promptInfo) }.onFailure { onFallbackPin() }
}
