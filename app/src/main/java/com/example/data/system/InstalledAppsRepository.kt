package com.example.data.system

import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.LruCache
import com.example.data.local.dao.AppCustomizationDao
import com.example.data.local.entity.AppCustomizationEntity
import com.example.domain.model.AppCategory
import com.example.domain.model.AppShortcutInfo
import com.example.domain.model.InstalledApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsRepository(
  private val context: Context,
  private val appCustomizationDao: AppCustomizationDao,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
  private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
  private val packageManager = context.packageManager
  private val appWidgetManager: AppWidgetManager? = runCatching {
    AppWidgetManager.getInstance(context)
  }.getOrNull()

  private val rawInstalledApps = MutableStateFlow<List<InstalledApp>>(emptyList())
  private val iconCache = LruCache<String, Drawable>(160)

  val installedAppsFlow: Flow<List<InstalledApp>> = combine(
    rawInstalledApps,
    appCustomizationDao.observeAllCustomizations(),
    NotificationBadgeStore.badgeCounts,
  ) { apps, customizations, badges ->
    val customByKey = customizations.associateBy { it.componentKey }
    apps.map { app ->
      val custom = customByKey[app.uniqueKey]
      app.copy(
        customLabel = custom?.customLabel,
        category = custom?.resolvedCategory() ?: app.category,
        isHidden = custom?.isHidden ?: false,
        isPinnedInDrawer = custom?.isPinnedInDrawer ?: false,
        customIconShape = custom?.resolvedIconShape(),
        badgeCount = badges[app.packageName] ?: 0,
        launchCount = custom?.launchCount ?: 0,
      )
    }
  }

  private val launcherCallback = object : LauncherApps.Callback() {
    override fun onPackageRemoved(packageName: String, user: UserHandle) {
      invalidatePackageIcons(packageName)
      refreshInstalledApps()
    }

    override fun onPackageAdded(packageName: String, user: UserHandle) {
      refreshInstalledApps()
    }

    override fun onPackageChanged(packageName: String, user: UserHandle) {
      invalidatePackageIcons(packageName)
      refreshInstalledApps()
    }

    override fun onPackagesAvailable(
      packageNames: Array<out String>,
      user: UserHandle,
      replacing: Boolean,
    ) {
      packageNames.forEach(::invalidatePackageIcons)
      refreshInstalledApps()
    }

    override fun onPackagesUnavailable(
      packageNames: Array<out String>,
      user: UserHandle,
      replacing: Boolean,
    ) {
      refreshInstalledApps()
    }
  }

  init {
    runCatching { launcherApps.registerCallback(launcherCallback) }
    refreshInstalledApps()
  }

  fun refreshInstalledApps() {
    scope.launch {
      val loaded = loadInstalledAppsInternal()
      rawInstalledApps.value = loaded
    }
  }

  suspend fun getInstalledAppsSnapshot(): List<InstalledApp> = withContext(Dispatchers.IO) {
    if (rawInstalledApps.value.isEmpty()) {
      loadInstalledAppsInternal().also { rawInstalledApps.value = it }
    } else {
      rawInstalledApps.value
    }
  }

  private fun loadInstalledAppsInternal(): List<InstalledApp> {
    val myPackage = context.packageName
    val currentUser = Process.myUserHandle()
    val profiles = runCatching { launcherApps.profiles }.getOrNull().orEmpty()
      .ifEmpty { listOf(currentUser) }

    val collected = mutableListOf<InstalledApp>()
    for (profile in profiles) {
      val serial = userManager.getSerialNumberForUser(profile)
      val isWork = profile != currentUser
      val activities = runCatching { launcherApps.getActivityList(null, profile) }
        .getOrNull()
        .orEmpty()

      for (info in activities) {
        val pkg = info.applicationInfo.packageName
        if (pkg == myPackage) continue
        val activityName = info.componentName.className
        val label = info.label?.toString()?.takeIf { it.isNotBlank() } ?: pkg
        val appInfo = info.applicationInfo
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val category = resolveAppCategory(appInfo, pkg)
        val lastUpdate = runCatching {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime
          } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(pkg, 0).lastUpdateTime
          }
        }.getOrDefault(0L)

        collected += InstalledApp(
          packageName = pkg,
          activityName = activityName,
          label = label,
          category = category,
          userSerialNumber = serial,
          userHandle = profile,
          isWorkProfile = isWork,
          isSystemApp = isSystem,
          lastUpdatedTime = lastUpdate,
        )
      }
    }

    return collected.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
  }

  fun getAppIcon(packageName: String, activityName: String, userSerialNumber: Long = 0L): Drawable? {
    val cacheKey = "$packageName/$activityName#$userSerialNumber"
    iconCache.get(cacheKey)?.let { return it }

    val user = userManager.getUserForSerialNumber(userSerialNumber) ?: Process.myUserHandle()
    val component = ComponentName(packageName, activityName)
    val activityInfo = runCatching {
      launcherApps.resolveActivity(Intent(Intent.ACTION_MAIN).setComponent(component), user)
    }.getOrNull()

    val drawable = runCatching {
      activityInfo?.getBadgedIcon(0) ?: packageManager.getActivityIcon(component)
    }.getOrNull() ?: runCatching {
      packageManager.getApplicationIcon(packageName)
    }.getOrNull()

    if (drawable != null) {
      iconCache.put(cacheKey, drawable)
    }
    return drawable
  }

  suspend fun loadDynamicShortcuts(
    packageName: String,
    userSerialNumber: Long = 0L,
  ): List<AppShortcutInfo> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext emptyList()
    if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()

    val user = userManager.getUserForSerialNumber(userSerialNumber) ?: Process.myUserHandle()
    val query = LauncherApps.ShortcutQuery().apply {
      setPackage(packageName)
      setQueryFlags(
        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
          LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
          LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
      )
    }

    val shortcuts = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty()
    shortcuts
      .filter { it.isEnabled }
      .sortedBy { it.rank }
      .take(6)
      .map { info ->
        AppShortcutInfo(
          id = info.id,
          packageName = info.PackageNameCompat(),
          shortLabel = info.shortLabel?.toString().orEmpty().ifBlank { info.id },
          longLabel = info.longLabel?.toString().orEmpty(),
          userSerialNumber = userSerialNumber,
          isDynamic = info.isDynamic,
          rank = info.rank,
        )
      }
  }

  fun launchApp(
    packageName: String,
    activityName: String,
    userSerialNumber: Long = 0L,
    sourceBounds: Rect? = null,
    freeform: Boolean = false,
  ): Boolean {
    val user = userManager.getUserForSerialNumber(userSerialNumber) ?: Process.myUserHandle()
    val component = ComponentName(packageName, activityName)
    val options = buildLaunchOptions(sourceBounds = sourceBounds, freeform = freeform)

    val launched = runCatching {
      launcherApps.startMainActivity(component, user, sourceBounds, options?.toBundle())
      true
    }.getOrElse {
      runCatching {
        val intent = Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
          setComponent(component)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.startActivity(intent, options?.toBundle())
        true
      }.getOrDefault(false)
    }

    if (launched) {
      scope.launch {
        val key = "$packageName/$activityName#$userSerialNumber"
        val existing = appCustomizationDao.getCustomization(key)
        if (existing == null) {
          appCustomizationDao.upsert(
            AppCustomizationEntity(
              componentKey = key,
              packageName = packageName,
              activityName = activityName,
              userSerialNumber = userSerialNumber,
              launchCount = 1,
              lastLaunchedAt = System.currentTimeMillis(),
            )
          )
        } else {
          appCustomizationDao.recordLaunch(key, System.currentTimeMillis())
        }
      }
    }
    return launched
  }

  fun launchDeepShortcut(packageName: String, shortcutId: String, userSerialNumber: Long = 0L): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
    val user = userManager.getUserForSerialNumber(userSerialNumber) ?: Process.myUserHandle()
    return runCatching {
      launcherApps.startShortcut(packageName, shortcutId, null, null, user)
      true
    }.getOrDefault(false)
  }

  fun openAppInfo(packageName: String, userSerialNumber: Long = 0L) {
    val user = userManager.getUserForSerialNumber(userSerialNumber) ?: Process.myUserHandle()
    runCatching {
      launcherApps.startAppDetailsActivity(
        ComponentName(packageName, ""),
        user,
        null,
        null,
      )
    }.onFailure {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    }
  }

  fun requestUninstallApp(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
      data = Uri.fromParts("package", packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }

  fun getWidgetsForPackage(packageName: String? = null): List<AppWidgetProviderInfo> {
    val providers = appWidgetManager?.installedProviders.orEmpty()
    return if (packageName.isNullOrBlank()) {
      providers
    } else {
      providers.filter { it.provider.packageName == packageName }
    }
  }

  suspend fun updateAppLabel(app: InstalledApp, newLabel: String?) {
    val existing = appCustomizationDao.getCustomization(app.uniqueKey)
      ?: AppCustomizationEntity(
        componentKey = app.uniqueKey,
        packageName = app.packageName,
        activityName = app.activityName,
        userSerialNumber = app.userSerialNumber,
      )
    appCustomizationDao.upsert(existing.copy(customLabel = newLabel?.trim()?.takeIf { it.isNotEmpty() }))
  }

  suspend fun setAppHidden(app: InstalledApp, hidden: Boolean) {
    val existing = appCustomizationDao.getCustomization(app.uniqueKey)
      ?: AppCustomizationEntity(
        componentKey = app.uniqueKey,
        packageName = app.packageName,
        activityName = app.activityName,
        userSerialNumber = app.userSerialNumber,
      )
    appCustomizationDao.upsert(existing.copy(isHidden = hidden))
  }

  suspend fun setAppCategory(app: InstalledApp, category: AppCategory?) {
    val existing = appCustomizationDao.getCustomization(app.uniqueKey)
      ?: AppCustomizationEntity(
        componentKey = app.uniqueKey,
        packageName = app.packageName,
        activityName = app.activityName,
        userSerialNumber = app.userSerialNumber,
      )
    appCustomizationDao.upsert(existing.copy(customCategory = category?.name))
  }

  private fun buildLaunchOptions(sourceBounds: Rect?, freeform: Boolean): ActivityOptions? {
    val options = if (sourceBounds != null) {
      ActivityOptions.makeClipRevealAnimation(
        android.view.View(context),
        sourceBounds.left,
        sourceBounds.top,
        sourceBounds.width().coerceAtLeast(1),
        sourceBounds.height().coerceAtLeast(1),
      )
    } else {
      ActivityOptions.makeBasic()
    }

    if (freeform && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val metrics = context.resources.displayMetrics
      val width = (metrics.widthPixels * 0.78f).toInt().coerceAtLeast(420)
      val height = (metrics.heightPixels * 0.62f).toInt().coerceAtLeast(520)
      val left = (metrics.widthPixels - width) / 2
      val top = (metrics.heightPixels - height) / 2
      options.setLaunchBounds(Rect(left, top, left + width, top + height))
      // WINDOWING_MODE_FREEFORM = 5 on Android 10–17
      runCatching {
        val method = ActivityOptions::class.java.getMethod(
          "setLaunchWindowingMode",
          Int::class.javaPrimitiveType,
        )
        method.invoke(options, 5)
      }
    }
    return options
  }

  private fun resolveAppCategory(appInfo: ApplicationInfo, packageName: String): AppCategory {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      when (appInfo.category) {
        ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
        ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.COMMUNICATION
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
      }
    }
    val lower = packageName.lowercase()
    return when {
      lower.contains("whatsapp") ||
        lower.contains("telegram") ||
        lower.contains("messaging") ||
        lower.contains("dialer") ||
        lower.contains("contacts") ||
        lower.contains("discord") ||
        lower.contains("instagram") ||
        lower.contains("facebook") ||
        lower.contains("twitter") ||
        lower.contains("snapchat") -> AppCategory.COMMUNICATION
      lower.contains("amazon") ||
        lower.contains("noon") ||
        lower.contains("shop") ||
        lower.contains("store") ||
        lower.contains("aliexpress") ||
        lower.contains("ebay") -> AppCategory.SHOPPING
      lower.contains("game") || lower.contains("play.games") -> AppCategory.GAMES
      lower.contains("camera") ||
        lower.contains("settings") ||
        lower.contains("clock") ||
        lower.contains("deskclock") ||
        lower.contains("calendar") ||
        lower.contains("gallery") ||
        lower.contains("photos") ||
        lower.contains("phone") -> AppCategory.ESSENTIALS
      lower.contains("youtube") ||
        lower.contains("music") ||
        lower.contains("spotify") ||
        lower.contains("video") ||
        lower.contains("netflix") -> AppCategory.MEDIA
      lower.contains("docs") ||
        lower.contains("drive") ||
        lower.contains("notes") ||
        lower.contains("mail") ||
        lower.contains("gmail") ||
        lower.contains("office") -> AppCategory.PRODUCTIVITY
      else -> AppCategory.TOOLS
    }
  }

  private fun invalidatePackageIcons(packageName: String) {
    val snapshot = iconCache.snapshot()
    snapshot.keys.filter { it.startsWith("$packageName/") }.forEach { iconCache.remove(it) }
  }

  private fun ShortcutInfo.PackageNameCompat(): String = `package`
}
