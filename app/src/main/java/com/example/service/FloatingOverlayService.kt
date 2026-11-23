package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.FloatingMiniAppType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FloatingOverlayRuntimeState(
  val isOverlayActive: Boolean = false,
  val isMinimizedToBubble: Boolean = false,
  val activeMiniApp: FloatingMiniAppType = FloatingMiniAppType.QUICK_APPS,
)

class FloatingOverlayService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP_OVERLAY -> {
        _runtimeState.value = _runtimeState.value.copy(isOverlayActive = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_TOGGLE_MINIMIZE -> {
        _runtimeState.value = _runtimeState.value.copy(
          isMinimizedToBubble = !_runtimeState.value.isMinimizedToBubble
        )
      }
      ACTION_OPEN_MINI_APP -> {
        val typeName = intent.getStringExtra(EXTRA_MINI_APP_TYPE)
        val type = FloatingMiniAppType.entries.firstOrNull { it.name == typeName }
          ?: FloatingMiniAppType.QUICK_APPS
        startForegroundCompat()
        _runtimeState.value = FloatingOverlayRuntimeState(
          isOverlayActive = true,
          isMinimizedToBubble = false,
          activeMiniApp = type,
        )
      }
      else -> {
        startForegroundCompat()
        _runtimeState.value = _runtimeState.value.copy(isOverlayActive = true)
      }
    }
    return START_STICKY
  }

  private fun startForegroundCompat() {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.floating_service_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      )
      manager.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(): Notification {
    val openLauncherIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val stopIntent = PendingIntent.getService(
      this,
      1,
      Intent(this, FloatingOverlayService::class.java).setAction(ACTION_STOP_OVERLAY),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(getString(R.string.floating_service_notification_title))
      .setContentText(getString(R.string.floating_service_notification_text))
      .setContentIntent(openLauncherIntent)
      .addAction(0, "Close", stopIntent)
      .setOngoing(true)
      .build()
  }

  companion object {
    const val CHANNEL_ID = "rawaq_floating_overlay"
    const val NOTIFICATION_ID = 4102
    const val ACTION_OPEN_MINI_APP = "com.example.action.OPEN_MINI_APP"
    const val ACTION_TOGGLE_MINIMIZE = "com.example.action.TOGGLE_MINIMIZE"
    const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
    const val EXTRA_MINI_APP_TYPE = "extra_mini_app_type"

    private val _runtimeState = MutableStateFlow(FloatingOverlayRuntimeState())
    val runtimeState: StateFlow<FloatingOverlayRuntimeState> = _runtimeState.asStateFlow()
  }
}
