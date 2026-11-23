package com.example.data.system

import android.os.Build
import com.example.domain.model.DailyScheduleState
import com.example.domain.model.LauncherSettings
import com.example.domain.model.PrayerCalculationMethod
import com.example.domain.model.PrayerSlotId
import com.example.domain.model.PrayerTimeSlot
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimesCalculator {

  fun buildScheduleState(
    settings: LauncherSettings,
    nowMillis: Long = System.currentTimeMillis(),
    temperatureLabel: String = "26°",
  ): DailyScheduleState {
    val isArabic = settings.forceRtl || settings.appLanguage.name == "AR"
    val locale = if (isArabic) Locale("ar") else Locale.getDefault()

    val clockPattern = if (settings.use24HourClock) "HH:mm" else "h:mm"
    val formattedClock = SimpleDateFormat(clockPattern, Locale.US).format(Date(nowMillis))
    val amPm = if (settings.use24HourClock) {
      ""
    } else {
      SimpleDateFormat("a", locale).format(Date(nowMillis))
    }

    val gregorianPattern = if (isArabic) "EEEE، d MMMM" else "EEE, MMM d"
    val gregorianDate = SimpleDateFormat(gregorianPattern, locale).format(Date(nowMillis))
    val hijriDate = if (settings.showHijriDate) formatHijriDate(locale) else ""

    val slots = calculateDailySlots(
      nowMillis = nowMillis,
      latitude = settings.latitude,
      longitude = settings.longitude,
      method = settings.prayerCalculationMethod,
      use24Hour = settings.use24HourClock,
    )

    return DailyScheduleState(
      formattedClock = formattedClock,
      amPmLabel = amPm,
      gregorianDate = gregorianDate,
      hijriDate = hijriDate,
      cityName = settings.cityName,
      temperatureLabel = temperatureLabel,
      slots = slots,
    )
  }

  fun calculateDailySlots(
    nowMillis: Long,
    latitude: Double,
    longitude: Double,
    method: PrayerCalculationMethod,
    use24Hour: Boolean,
  ): List<PrayerTimeSlot> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val tzOffsetHours = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / 3_600_000.0

    val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
    val decl = sunDeclination(jd)
    val eqTime = equationOfTime(jd)

    val dhuhr = 12.0 + tzOffsetHours - longitude / 15.0 - eqTime
    val sunrise = dhuhr - sunHourAngle(latitude, decl, 0.833) / 15.0
    val sunset = dhuhr + sunHourAngle(latitude, decl, 0.833) / 15.0
    val fajr = dhuhr - sunHourAngle(latitude, decl, method.fajrAngle) / 15.0
    val isha = dhuhr + sunHourAngle(latitude, decl, method.ishaAngle) / 15.0
    val asr = dhuhr + asrHourAngle(latitude, decl, 1.0) / 15.0
    val midnight = sunset + ((fajr + 24.0) - sunset) / 2.0

    val baseMidnightCal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val dayStartMillis = baseMidnightCal.timeInMillis

    data class RawSlot(val id: PrayerSlotId, val en: String, val ar: String, val hours: Double)

    val rawSlots = listOf(
      RawSlot(PrayerSlotId.FAJR, "Fajr", "الفجر", fajr),
      RawSlot(PrayerSlotId.SUNRISE, "Sunrise", "الشروق", sunrise),
      RawSlot(PrayerSlotId.DHUHR, "Dhuhr", "الظهر", dhuhr),
      RawSlot(PrayerSlotId.ASR, "Asr", "العصر", asr),
      RawSlot(PrayerSlotId.MAGHRIB, "Maghrib", "المغرب", sunset),
      RawSlot(PrayerSlotId.ISHA, "Isha", "العشاء", isha),
      RawSlot(PrayerSlotId.MIDNIGHT, "Midnight", "القيام", midnight),
    )

    val epochs = rawSlots.map { slot ->
      val clampedHours = slot.hours.coerceIn(0.0, 29.0)
      dayStartMillis + (clampedHours * 3_600_000.0).toLong()
    }

    var currentIndex = epochs.indexOfLast { it <= nowMillis }
    if (currentIndex == -1) currentIndex = epochs.lastIndex
    val nextIndex = (currentIndex + 1) % epochs.size

    val timePattern = if (use24Hour) "HH:mm" else "h:mm"
    val formatter = SimpleDateFormat(timePattern, Locale.US)

    return rawSlots.mapIndexed { index, raw ->
      val epoch = epochs[index]
      PrayerTimeSlot(
        id = raw.id,
        nameEn = raw.en,
        nameAr = raw.ar,
        formattedTime = formatter.format(Date(epoch)),
        epochMillis = epoch,
        isCurrent = index == currentIndex,
        isNext = index == nextIndex,
      )
    }
  }

  private fun formatHijriDate(locale: Locale): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return runCatching {
        val hijrah = HijrahDate.from(LocalDate.now())
        DateTimeFormatter.ofPattern("d MMMM yyyy", locale).format(hijrah)
      }.getOrDefault("")
    }
    return ""
  }

  private fun julianDate(year: Int, month: Int, day: Int): Double {
    var y = year
    var m = month
    if (m <= 2) {
      y -= 1
      m += 12
    }
    val a = floor(y / 100.0)
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
  }

  private fun sunDeclination(jd: Double): Double {
    val d = jd - 2451545.0
    val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
    val q = (280.459 + 0.98564736 * d) % 360.0
    val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
    val e = Math.toRadians(23.439 - 0.00000036 * d)
    return Math.toDegrees(asin(sin(e) * sin(l)))
  }

  private fun equationOfTime(jd: Double): Double {
    val d = jd - 2451545.0
    val g = Math.toRadians((357.529 + 0.98560028 * d) % 360.0)
    val q = (280.459 + 0.98564736 * d) % 360.0
    val l = Math.toRadians((q + 1.915 * sin(g) + 0.020 * sin(2 * g)) % 360.0)
    val e = Math.toRadians(23.439 - 0.00000036 * d)
    val ra = Math.toDegrees(atan2(cos(e) * sin(l), cos(l))) / 15.0
    val eot = q / 15.0 - fixHour(ra)
    return eot
  }

  private fun sunHourAngle(lat: Double, decl: Double, angle: Double): Double {
    val latRad = Math.toRadians(lat)
    val declRad = Math.toRadians(decl)
    val num = -sin(Math.toRadians(angle)) - sin(latRad) * sin(declRad)
    val den = cos(latRad) * cos(declRad)
    val cosVal = (num / den).coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cosVal))
  }

  private fun asrHourAngle(lat: Double, decl: Double, factor: Double): Double {
    val latRad = Math.toRadians(lat)
    val declRad = Math.toRadians(decl)
    val diff = kotlin.math.abs(latRad - declRad)
    val angle = -Math.toDegrees(atan2(1.0, factor + tan(diff)))
    return sunHourAngle(lat, decl, angle)
  }

  private fun fixHour(a: Double): Double {
    var v = a % 24.0
    if (v < 0) v += 24.0
    return v
  }
}
