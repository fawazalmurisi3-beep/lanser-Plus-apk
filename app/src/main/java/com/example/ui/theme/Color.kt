package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val MidnightSlate = Color(0xFF070B19)
val DeepNavy = Color(0xFF0F172A)
val FrostedInk = Color(0xFF1E293B)
val GlassCyan = Color(0xFF38BDF8)
val WarmAmber = Color(0xFFF59E0B)
val EmeraldMint = Color(0xFF10B981)
val CoralRose = Color(0xFFF43F5E)
val SoftCloud = Color(0xFFF8FAFC)
val MutedSilver = Color(0xFF94A3B8)

fun parseColorHex(hex: String, fallback: Color = GlassCyan): Color {
  return runCatching {
    val clean = hex.trim().removePrefix("#")
    val value = clean.toLong(16)
    when (clean.length) {
      6 -> Color(value or 0xFF000000L)
      8 -> Color(value)
      else -> fallback
    }
  }.getOrDefault(fallback)
}
