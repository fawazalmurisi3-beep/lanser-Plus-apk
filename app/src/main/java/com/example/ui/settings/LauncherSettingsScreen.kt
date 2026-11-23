package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppLanguage
import com.example.domain.model.GestureAction
import com.example.domain.model.IconShapeStyle
import com.example.domain.model.LauncherFontFamily
import com.example.domain.model.LauncherSettings
import com.example.domain.model.PageTransitionStyle
import com.example.domain.model.PrayerCalculationMethod
import com.example.domain.model.ThemeMode
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily

private data class CityPreset(val nameAr: String, val nameEn: String, val lat: Double, val lon: Double)

private val CITY_PRESETS = listOf(
  CityPreset("الرياض", "Riyadh", 24.7136, 46.6753),
  CityPreset("مكة المكرمة", "Makkah", 21.3891, 39.8579),
  CityPreset("جدة", "Jeddah", 21.4858, 39.1925),
  CityPreset("القاهرة", "Cairo", 30.0444, 31.2357),
  CityPreset("دبي", "Dubai", 25.2048, 55.2708),
  CityPreset("الدوحة", "Doha", 25.2854, 51.5310),
  CityPreset("عمّان", "Amman", 31.9454, 35.9284),
  CityPreset("لندن", "London", 51.5072, -0.1276),
)

@Composable
fun LauncherSettingsScreen(
  settings: LauncherSettings,
  isNotificationListenerConnected: Boolean,
  onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
  onExportJson: () -> Unit,
  onExportZip: () -> Unit,
  onImportBackup: () -> Unit,
  onResetDefaults: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val isRtl = settings.forceRtl
  val accent = parseColorHex(settings.accentColorHex)
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF070B19).copy(alpha = 0.94f))
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("launcher_settings_screen"),
  ) {
    // Top Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("settings_back_button"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (isRtl) "تخصيص مشغل رواق" else "Rawaq Launcher Settings",
        fontFamily = fontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Theme & Glassmorphism
      SettingsCard(title = if (isRtl) "المظهر والزجاج والضبابية" else "Theme & Glassmorphism", settings = settings) {
        TextLabel(if (isRtl) "نمط السمة" else "Theme Mode")
        ChipRow(
          options = listOf(
            ThemeMode.LIQUID_GLASS to (if (isRtl) "زجاج سائل" else "Liquid Glass"),
            ThemeMode.DARK_FROSTED to (if (isRtl) "داكن ضبابي" else "Dark Frosted"),
            ThemeMode.LIGHT_AIR to (if (isRtl) "فاتح هوائي" else "Light Air"),
            ThemeMode.MATERIAL_YOU to "Material You",
          ),
          selected = settings.themeMode,
          accent = accent,
          onSelect = { mode -> onUpdateSettings { it.copy(themeMode = mode) } },
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextLabel(if (isRtl) "لون التمييز" else "Accent Color")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          listOf("#FF38BDF8", "#FFF59E0B", "#FF10B981", "#FFF43F5E", "#FF8B5CF6", "#FFEC4899").forEach { hex ->
            val color = parseColorHex(hex)
            val selected = settings.accentColorHex.equals(hex, ignoreCase = true)
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                .clickable { onUpdateSettings { it.copy(accentColorHex = hex) } },
            )
          }
        }

        SliderRow(
          label = if (isRtl) "ضبابية الخلفية" else "Wallpaper Blur",
          value = settings.wallpaperBlurRadius,
          range = 0f..40f,
          valueText = "${settings.wallpaperBlurRadius.toInt()}dp",
          onValueChange = { v -> onUpdateSettings { it.copy(wallpaperBlurRadius = v) } },
        )

        SliderRow(
          label = if (isRtl) "شفافية الزجاج" else "Glass Opacity",
          value = settings.glassOpacity,
          range = 0.08f..0.7f,
          valueText = "${(settings.glassOpacity * 100).toInt()}%",
          onValueChange = { v -> onUpdateSettings { it.copy(glassOpacity = v) } },
        )

        SliderRow(
          label = if (isRtl) "تعتيم الخلفية" else "Wallpaper Dim",
          value = settings.wallpaperDimAmount,
          range = 0f..0.65f,
          valueText = "${(settings.wallpaperDimAmount * 100).toInt()}%",
          onValueChange = { v -> onUpdateSettings { it.copy(wallpaperDimAmount = v) } },
        )

        SwitchRow(
          label = if (isRtl) "استخدام خلفية النظام الحية" else "Use System Live Wallpaper",
          checked = settings.useSystemWallpaper,
          onCheckedChange = { c -> onUpdateSettings { it.copy(useSystemWallpaper = c) } },
        )

        Spacer(modifier = Modifier.height(6.dp))
        TextLabel(if (isRtl) "حركة انتقال الصفحات" else "Page Transition")
        ChipRow(
          options = PageTransitionStyle.entries.map { it to it.name.lowercase().replaceFirstChar(Char::uppercase) },
          selected = settings.pageTransitionStyle,
          accent = accent,
          onSelect = { style -> onUpdateSettings { it.copy(pageTransitionStyle = style) } },
        )
      }

      // 2. Grid, Icons & Typography
      SettingsCard(title = if (isRtl) "الشبكة والأيقونات والخطوط" else "Grid, Icons & Typography", settings = settings) {
        TextLabel(if (isRtl) "تخطيط الشبكة (أعمدة × صفوف)" else "Home Grid Size")
        ChipRow(
          options = listOf(
            (4 to 5) to "4×5",
            (4 to 6) to "4×6",
            (5 to 6) to "5×6",
            (5 to 7) to "5×7",
          ),
          selected = settings.gridColumns to settings.gridRows,
          accent = accent,
          onSelect = { (c, r) -> onUpdateSettings { it.copy(gridColumns = c, gridRows = r) } },
        )

        SliderRow(
          label = if (isRtl) "عدد أيقونات الشريط السفلي" else "Dock Icon Count",
          value = settings.dockIconCount.toFloat(),
          range = 4f..7f,
          valueText = settings.dockIconCount.toString(),
          onValueChange = { v -> onUpdateSettings { it.copy(dockIconCount = v.toInt()) } },
        )

        SliderRow(
          label = if (isRtl) "حجم الأيقونات" else "Icon Size",
          value = settings.iconSizeDp.toFloat(),
          range = 44f..68f,
          valueText = "${settings.iconSizeDp}dp",
          onValueChange = { v -> onUpdateSettings { it.copy(iconSizeDp = v.toInt()) } },
        )

        TextLabel(if (isRtl) "شكل الأيقونات" else "Icon Shape")
        ChipRow(
          options = listOf(
            IconShapeStyle.SQUIRCLE to (if (isRtl) "مربع مرن" else "Squircle"),
            IconShapeStyle.CIRCLE to (if (isRtl) "دائري" else "Circle"),
            IconShapeStyle.ROUNDED_SQUARE to (if (isRtl) "مستدير" else "Rounded"),
            IconShapeStyle.TEARDROP to (if (isRtl) "قطرة" else "Teardrop"),
          ),
          selected = settings.iconShape,
          accent = accent,
          onSelect = { s -> onUpdateSettings { it.copy(iconShape = s) } },
        )

        SwitchRow(
          label = if (isRtl) "إظهار أسماء التطبيقات" else "Show Icon Labels",
          checked = settings.showIconLabels,
          onCheckedChange = { c -> onUpdateSettings { it.copy(showIconLabels = c) } },
        )

        TextLabel(if (isRtl) "نوع الخط" else "Font Family")
        ChipRow(
          options = listOf(
            LauncherFontFamily.CAIRO to "Cairo",
            LauncherFontFamily.TAJAWAL to "Tajawal",
            LauncherFontFamily.PLUS_JAKARTA to "Jakarta",
            LauncherFontFamily.JETBRAINS_MONO to "Mono",
          ),
          selected = settings.labelFontFamily,
          accent = accent,
          onSelect = { f -> onUpdateSettings { it.copy(labelFontFamily = f) } },
        )

        SliderRow(
          label = if (isRtl) "حجم خط الأسماء" else "Label Font Size",
          value = settings.labelFontSizeSp,
          range = 10f..16f,
          valueText = "${settings.labelFontSizeSp.toInt()}sp",
          onValueChange = { v -> onUpdateSettings { it.copy(labelFontSizeSp = v) } },
        )

        SliderRow(
          label = if (isRtl) "سماكة الخط" else "Font Weight",
          value = settings.labelFontWeight.toFloat(),
          range = 300f..700f,
          valueText = settings.labelFontWeight.toString(),
          onValueChange = { v -> onUpdateSettings { it.copy(labelFontWeight = (v.toInt() / 100) * 100) } },
        )

        SwitchRow(
          label = if (isRtl) "ظل للنصوص" else "Label Text Shadow",
          checked = settings.labelShadowEnabled,
          onCheckedChange = { c -> onUpdateSettings { it.copy(labelShadowEnabled = c) } },
        )
      }

      // 3. Top Clock & Prayer Times
      SettingsCard(title = if (isRtl) "الساعة ومواقيت الصلاة" else "Clock & Prayer Schedule", settings = settings) {
        SwitchRow(
          label = if (isRtl) "إظهار أداة الساعة العليا" else "Show Top Clock Widget",
          checked = settings.showTopClockWidget,
          onCheckedChange = { c -> onUpdateSettings { it.copy(showTopClockWidget = c) } },
        )
        SwitchRow(
          label = if (isRtl) "نظام 24 ساعة" else "24-Hour Clock",
          checked = settings.use24HourClock,
          onCheckedChange = { c -> onUpdateSettings { it.copy(use24HourClock = c) } },
        )
        SwitchRow(
          label = if (isRtl) "التاريخ الهجري" else "Show Hijri Date",
          checked = settings.showHijriDate,
          onCheckedChange = { c -> onUpdateSettings { it.copy(showHijriDate = c) } },
        )
        SwitchRow(
          label = if (isRtl) "شريط مواقيت الصلاة" else "Show Prayer Times Bar",
          checked = settings.showPrayerBar,
          onCheckedChange = { c -> onUpdateSettings { it.copy(showPrayerBar = c) } },
        )

        TextLabel(if (isRtl) "المدينة" else "City")
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CITY_PRESETS.forEach { city ->
            val cityLabel = if (isRtl) city.nameAr else city.nameEn
            val selected = settings.cityName == city.nameAr || settings.cityName == city.nameEn
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                .border(1.dp, if (selected) accent else Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                .clickable {
                  onUpdateSettings {
                    it.copy(cityName = cityLabel, latitude = city.lat, longitude = city.lon)
                  }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Text(text = cityLabel, color = Color.White, fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        TextLabel(if (isRtl) "طريقة الحساب" else "Calculation Method")
        ChipRow(
          options = PrayerCalculationMethod.entries.map { it to it.name.replace("_", " ") },
          selected = settings.prayerCalculationMethod,
          accent = accent,
          onSelect = { m -> onUpdateSettings { it.copy(prayerCalculationMethod = m) } },
        )
      }

      // 4. RTL, Gestures, Vault & System Integration
      SettingsCard(title = if (isRtl) "اللغة والإيماءات وأذونات النظام" else "RTL, Gestures & System Roles", settings = settings) {
        SwitchRow(
          label = if (isRtl) "تفعيل الاتجاه من اليمين لليسار (RTL)" else "Force Arabic RTL Layout",
          checked = settings.forceRtl,
          onCheckedChange = { c ->
            onUpdateSettings {
              it.copy(forceRtl = c, appLanguage = if (c) AppLanguage.AR else AppLanguage.EN)
            }
          },
        )

        var pinDraft by remember(settings.vaultPinHash) { mutableStateOf(settings.vaultPinHash) }
        OutlinedTextField(
          value = pinDraft,
          onValueChange = {
            pinDraft = it.take(8)
            onUpdateSettings { s -> s.copy(vaultPinHash = pinDraft, vaultEnabled = pinDraft.isNotBlank()) }
          },
          singleLine = true,
          label = { Text(if (isRtl) "رمز PIN لخزنة التطبيقات (الافتراضي 0000)" else "Vault PIN (Default 0000)", fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
        )

        TextLabel(if (isRtl) "النقر المزدوج" else "Double Tap Gesture")
        ChipRow(
          options = listOf(
            GestureAction.OPEN_SEARCH to (if (isRtl) "بحث" else "Search"),
            GestureAction.OPEN_APP_DRAWER to (if (isRtl) "الدرج" else "Drawer"),
            GestureAction.TOGGLE_FLOATING_DOCK to (if (isRtl) "عائم" else "Floating"),
            GestureAction.NONE to (if (isRtl) "لا شيء" else "None"),
          ),
          selected = settings.doubleTapGesture,
          accent = accent,
          onSelect = { g -> onUpdateSettings { it.copy(doubleTapGesture = g) } },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SystemPermissionRow(
          icon = Icons.Rounded.Home,
          title = if (isRtl) "تعيين رواق كمشغل الشاشة الافتراضي" else "Set Rawaq as Default Launcher",
          accent = accent,
          onClick = {
            runCatching {
              context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
          },
        )
        SystemPermissionRow(
          icon = Icons.Rounded.NotificationsActive,
          title = if (isNotificationListenerConnected) {
            if (isRtl) "إذن شارات الإشعارات (مفعّل)" else "Notification Badges (Active)"
          } else {
            if (isRtl) "تفعيل قراءة شارات الإشعارات" else "Enable Notification Badges"
          },
          accent = accent,
          onClick = {
            runCatching {
              context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              )
            }
          },
        )
        SystemPermissionRow(
          icon = Icons.Rounded.PictureInPictureAlt,
          title = if (isRtl) "إذن النوافذ العائمة فوق التطبيقات" else "Floating Overlay Permission",
          accent = accent,
          onClick = {
            runCatching {
              context.startActivity(
                Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              )
            }
          },
        )
      }

      // 5. Backup & Restore
      SettingsCard(title = if (isRtl) "النسخ الاحتياطي والاستعادة" else "Backup & Restore", settings = settings) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onExportJson,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            modifier = Modifier
              .weight(1f)
              .testTag("export_json_button"),
          ) {
            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isRtl) "تصدير JSON" else "Export JSON", fontSize = 12.sp)
          }
          Button(
            onClick = onExportZip,
            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.8f)),
            modifier = Modifier
              .weight(1f)
              .testTag("export_zip_button"),
          ) {
            Icon(Icons.Rounded.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isRtl) "تصدير ZIP" else "Export ZIP", fontSize = 12.sp)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onImportBackup,
            modifier = Modifier
              .weight(1f)
              .testTag("import_backup_button"),
          ) {
            Icon(Icons.Rounded.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isRtl) "استيراد نسخة" else "Import Backup", color = Color.White, fontSize = 12.sp)
          }
          OutlinedButton(
            onClick = onResetDefaults,
            modifier = Modifier
              .weight(1f)
              .testTag("reset_defaults_button"),
          ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (isRtl) "استعادة الافتراضي" else "Reset", color = Color(0xFFFDA4AF), fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun SettingsCard(
  title: String,
  settings: LauncherSettings,
  content: @Composable ColumnScope.() -> Unit,
) {
  val accent = parseColorHex(settings.accentColorHex)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(glassContainerColor(settings, boostAlpha = 0.14f))
      .border(1.dp, glassBorderBrush(settings), RoundedCornerShape(24.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = title,
      color = accent,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
    )
    content()
  }
}

@Composable
private fun TextLabel(text: String) {
  Text(text = text, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun SliderRow(
  label: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  valueText: String,
  onValueChange: (Float) -> Unit,
) {
  Column {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
      Text(text = valueText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Slider(value = value, onValueChange = onValueChange, valueRange = range)
  }
}

@Composable
private fun SwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun <T> ChipRow(
  options: List<Pair<T, String>>,
  selected: T,
  accent: Color,
  onSelect: (T) -> Unit,
) {
  Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    options.forEach { (item, title) ->
      val active = item == selected
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(if (active) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
          .border(1.dp, if (active) accent else Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
          .clickable { onSelect(item) }
          .padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Text(
          text = title,
          color = if (active) accent else Color.White,
          fontSize = 12.sp,
          fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
      }
    }
  }
}

@Composable
private fun SystemPermissionRow(
  icon: ImageVector,
  title: String,
  accent: Color,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color.White.copy(alpha = 0.06f))
      .clickable(onClick = onClick)
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(10.dp))
    Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}
