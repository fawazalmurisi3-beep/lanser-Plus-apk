package com.example

import android.app.Application
import com.example.data.backup.LauncherBackupManager
import com.example.data.datastore.LauncherPreferencesDataSource
import com.example.data.local.LauncherDatabase
import com.example.data.repository.HomeLayoutRepository
import com.example.data.system.InstalledAppsRepository

class AppContainer(application: Application) {
  val database: LauncherDatabase by lazy { LauncherDatabase.getInstance(application) }
  val preferencesDataSource: LauncherPreferencesDataSource by lazy {
    LauncherPreferencesDataSource(application)
  }
  val installedAppsRepository: InstalledAppsRepository by lazy {
    InstalledAppsRepository(
      context = application,
      appCustomizationDao = database.appCustomizationDao(),
    )
  }
  val homeLayoutRepository: HomeLayoutRepository by lazy {
    HomeLayoutRepository(
      homeItemDao = database.homeItemDao(),
      floatingNoteDao = database.floatingNoteDao(),
      installedAppsRepository = installedAppsRepository,
      preferencesDataSource = preferencesDataSource,
    )
  }
  val backupManager: LauncherBackupManager by lazy {
    LauncherBackupManager(
      context = application,
      database = database,
      preferencesDataSource = preferencesDataSource,
    )
  }
}

class RawaqLauncherApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
