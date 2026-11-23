package com.example.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.LauncherSettings
import com.example.ui.components.glassBorderBrush
import com.example.ui.components.glassContainerColor

class LauncherAppWidgetManager(private val context: Context) {
  val appWidgetManager: AppWidgetManager? = runCatching {
    AppWidgetManager.getInstance(context)
  }.getOrNull()

  val appWidgetHost: AppWidgetHost = AppWidgetHost(context.applicationContext, HOST_ID)

  fun startListening() {
    runCatching { appWidgetHost.startListening() }
  }

  fun stopListening() {
    runCatching { appWidgetHost.stopListening() }
  }

  fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

  fun deleteWidgetId(appWidgetId: Int) {
    if (appWidgetId > 0) {
      runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }
  }

  fun tryBindWidget(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean {
    val manager = appWidgetManager ?: return false
    return runCatching {
      manager.bindAppWidgetIdIfAllowed(appWidgetId, provider.profile, provider.provider, Bundle())
    }.getOrDefault(false)
  }

  fun createBindIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent {
    return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
    }
  }

  fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? {
    return runCatching { appWidgetManager?.getAppWidgetInfo(appWidgetId) }.getOrNull()
  }

  fun createHostView(appWidgetId: Int): AppWidgetHostView? {
    val info = getProviderInfo(appWidgetId) ?: return null
    return runCatching {
      appWidgetHost.createView(context, appWidgetId, info).apply {
        setAppWidget(appWidgetId, info)
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      }
    }.getOrNull()
  }

  companion object {
    const val HOST_ID = 2048
  }
}

@Composable
fun SystemAppWidgetCard(
  appWidgetId: Int,
  widgetHostManager: LauncherAppWidgetManager,
  settings: LauncherSettings,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val hostView = remember(appWidgetId, context) {
    widgetHostManager.createHostView(appWidgetId)
  }
  val shape = RoundedCornerShape(24.dp)

  Box(
    modifier = modifier
      .fillMaxSize()
      .clip(shape)
      .background(glassContainerColor(settings))
      .border(1.dp, glassBorderBrush(settings), shape),
    contentAlignment = Alignment.Center,
  ) {
    if (hostView != null) {
      AndroidView(
        factory = { hostView },
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Text(
        text = if (settings.forceRtl) "أداة النظام غير متوفرة" else "Widget Unavailable",
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 12.sp,
      )
    }
  }
}
