package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AppCustomizationDao
import com.example.data.local.dao.FloatingNoteDao
import com.example.data.local.dao.HomeItemDao
import com.example.data.local.entity.AppCustomizationEntity
import com.example.data.local.entity.FloatingNoteEntity
import com.example.data.local.entity.HomeItemEntity

@Database(
  entities = [
    HomeItemEntity::class,
    AppCustomizationEntity::class,
    FloatingNoteEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
  abstract fun homeItemDao(): HomeItemDao
  abstract fun appCustomizationDao(): AppCustomizationDao
  abstract fun floatingNoteDao(): FloatingNoteDao

  companion object {
    @Volatile
    private var INSTANCE: LauncherDatabase? = null

    fun getInstance(context: Context): LauncherDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          LauncherDatabase::class.java,
          "rawaq_launcher.db",
        )
          .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
