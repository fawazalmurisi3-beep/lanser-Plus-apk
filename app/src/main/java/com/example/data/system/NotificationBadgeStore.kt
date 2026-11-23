package com.example.data.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationBadgeStore {
  private val _badgeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
  val badgeCounts: StateFlow<Map<String, Int>> = _badgeCounts.asStateFlow()

  private val _isListenerConnected = MutableStateFlow(false)
  val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

  fun setConnected(connected: Boolean) {
    _isListenerConnected.value = connected
    if (!connected) {
      _badgeCounts.value = emptyMap()
    }
  }

  fun updateCounts(counts: Map<String, Int>) {
    _badgeCounts.value = counts
  }

  fun countForPackage(packageName: String): Int = _badgeCounts.value[packageName] ?: 0
}
