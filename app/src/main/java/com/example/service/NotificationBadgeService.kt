package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.system.NotificationBadgeStore

class NotificationBadgeService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    NotificationBadgeStore.setConnected(true)
    refreshCounts()
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    NotificationBadgeStore.setConnected(false)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    refreshCounts()
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
    refreshCounts()
  }

  private fun refreshCounts() {
    runCatching {
      val active = activeNotifications ?: emptyArray()
      val counts = mutableMapOf<String, Int>()
      for (sbn in active) {
        if (sbn.isOngoing) continue
        val pkg = sbn.packageName ?: continue
        if (pkg == packageName) continue
        val number = sbn.notification.number.takeIf { it > 0 } ?: 1
        counts[pkg] = (counts[pkg] ?: 0) + number
      }
      NotificationBadgeStore.updateCounts(counts)
    }
  }
}
