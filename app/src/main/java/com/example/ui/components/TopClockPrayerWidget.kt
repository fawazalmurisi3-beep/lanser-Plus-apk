package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailyScheduleState
import com.example.domain.model.LauncherSettings
import com.example.domain.model.ThemeMode
import com.example.ui.theme.parseColorHex
import com.example.ui.theme.toComposeFontFamily

@Composable
fun TopClockPrayerWidget(
  schedule: DailyScheduleState,
  settings: LauncherSettings,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isRtl = settings.forceRtl
  val isLight = settings.themeMode == ThemeMode.LIGHT_AIR
  val primaryTextColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
  val secondaryTextColor = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
  val prayerAccent = parseColorHex(settings.prayerAccentColorHex, Color(0xFFF59E0B))
  val fontFamily = settings.labelFontFamily.toComposeFontFamily()
  val shape = RoundedCornerShape(26.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(glassContainerColor(settings, boostAlpha = 0.06f))
      .border(1.dp, glassBorderBrush(settings), shape)
      .clickable(onClick = onOpenSettings)
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag("top_clock_prayer_widget"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = schedule.formattedClock,
          fontFamily = fontFamily,
          fontSize = 42.sp,
          fontWeight = FontWeight.Bold,
          color = primaryTextColor,
          lineHeight = 44.sp,
        )
        if (schedule.amPmLabel.isNotBlank()) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = schedule.amPmLabel,
            fontFamily = fontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = prayerAccent,
            modifier = Modifier.padding(bottom = 6.dp),
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        if (settings.showWeatherBadge) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(Color.White.copy(alpha = if (isLight) 0.55f else 0.12f))
              .padding(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WbSunny,
              contentDescription = null,
              tint = prayerAccent,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "${schedule.temperatureLabel} • ${schedule.cityName}",
              fontFamily = fontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = primaryTextColor,
              maxLines = 1,
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
          text = schedule.gregorianDate,
          fontFamily = fontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = secondaryTextColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (settings.showHijriDate && schedule.hijriDate.isNotBlank()) {
          Text(
            text = schedule.hijriDate,
            fontFamily = fontFamily,
            fontSize = 11.sp,
            color = prayerAccent.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    if (settings.showPrayerBar && schedule.slots.isNotEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(Color.Black.copy(alpha = if (isLight) 0.06f else 0.22f))
          .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        schedule.slots.forEach { slot ->
          val isHighlighted = slot.isNext
          val bg = when {
            isHighlighted -> prayerAccent.copy(alpha = 0.28f)
            slot.isCurrent -> Color.White.copy(alpha = 0.08f)
            else -> Color.Transparent
          }
          val borderColor = if (isHighlighted) prayerAccent.copy(alpha = 0.85f) else Color.Transparent

          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(bg)
              .border(1.dp, borderColor, RoundedCornerShape(12.dp))
              .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = if (isRtl) slot.nameAr else slot.nameEn,
              fontFamily = fontFamily,
              fontSize = 9.5.sp,
              fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
              color = if (isHighlighted) prayerAccent else secondaryTextColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = slot.formattedTime,
              fontFamily = fontFamily,
              fontSize = 10.sp,
              fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
              color = if (isHighlighted) primaryTextColor else primaryTextColor.copy(alpha = 0.85f),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}
