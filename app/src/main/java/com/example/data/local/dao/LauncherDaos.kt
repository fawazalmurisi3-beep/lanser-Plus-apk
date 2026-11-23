package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AppCustomizationEntity
import com.example.data.local.entity.FloatingNoteEntity
import com.example.data.local.entity.HomeItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeItemDao {
  @Query("SELECT * FROM home_items ORDER BY page ASC, cellY ASC, cellX ASC, folderOrder ASC")
  fun observeAllHomeItems(): Flow<List<HomeItemEntity>>

  @Query("SELECT * FROM home_items ORDER BY page ASC, cellY ASC, cellX ASC, folderOrder ASC")
  suspend fun getAllHomeItemsSnapshot(): List<HomeItemEntity>

  @Query("SELECT * FROM home_items WHERE id = :id LIMIT 1")
  suspend fun getHomeItemById(id: Long): HomeItemEntity?

  @Query("SELECT * FROM home_items WHERE folderId = :folderId ORDER BY folderOrder ASC")
  suspend fun getFolderChildren(folderId: Long): List<HomeItemEntity>

  @Query("SELECT COUNT(*) FROM home_items")
  suspend fun getItemCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertItem(item: HomeItemEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<HomeItemEntity>): List<Long>

  @Update
  suspend fun updateItem(item: HomeItemEntity)

  @Update
  suspend fun updateItems(items: List<HomeItemEntity>)

  @Query(
    """
    UPDATE home_items
    SET page = :page, cellX = :cellX, cellY = :cellY, isDocked = :isDocked, dockOrder = :dockOrder, folderId = :folderId, folderOrder = :folderOrder
    WHERE id = :id
    """
  )
  suspend fun updateItemPlacement(
    id: Long,
    page: Int,
    cellX: Int,
    cellY: Int,
    isDocked: Boolean,
    dockOrder: Int,
    folderId: Long?,
    folderOrder: Int,
  )

  @Query(
    """
    UPDATE home_items
    SET folderDisplayMode = :mode, spanX = :spanX, spanY = :spanY
    WHERE id = :folderId
    """
  )
  suspend fun updateFolderSize(
    folderId: Long,
    mode: String,
    spanX: Int,
    spanY: Int,
  )

  @Query("UPDATE home_items SET folderTitle = :title WHERE id = :folderId")
  suspend fun renameFolder(folderId: Long, title: String)

  @Query("UPDATE home_items SET spanX = :spanX, spanY = :spanY WHERE id = :id")
  suspend fun updateItemSpan(id: Long, spanX: Int, spanY: Int)

  @Query("DELETE FROM home_items WHERE id = :id")
  suspend fun deleteItemById(id: Long)

  @Query("DELETE FROM home_items WHERE folderId = :folderId")
  suspend fun deleteChildrenByFolderId(folderId: Long)

  @Query("DELETE FROM home_items WHERE packageName = :packageName AND userSerialNumber = :userSerialNumber")
  suspend fun deleteByPackage(packageName: String, userSerialNumber: Long)

  @Query("DELETE FROM home_items")
  suspend fun clearAll()

  @Transaction
  suspend fun replaceAllItems(items: List<HomeItemEntity>) {
    clearAll()
    insertAll(items)
  }

  @Transaction
  suspend fun deleteFolderAndChildren(folderId: Long) {
    deleteChildrenByFolderId(folderId)
    deleteItemById(folderId)
  }
}

@Dao
interface AppCustomizationDao {
  @Query("SELECT * FROM app_customizations")
  fun observeAllCustomizations(): Flow<List<AppCustomizationEntity>>

  @Query("SELECT * FROM app_customizations")
  suspend fun getAllCustomizationsSnapshot(): List<AppCustomizationEntity>

  @Query("SELECT * FROM app_customizations WHERE componentKey = :componentKey LIMIT 1")
  suspend fun getCustomization(componentKey: String): AppCustomizationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(customization: AppCustomizationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(customizations: List<AppCustomizationEntity>)

  @Query("UPDATE app_customizations SET isHidden = :isHidden WHERE componentKey = :componentKey")
  suspend fun setHidden(componentKey: String, isHidden: Boolean)

  @Query("UPDATE app_customizations SET customLabel = :customLabel WHERE componentKey = :componentKey")
  suspend fun updateCustomLabel(componentKey: String, customLabel: String?)

  @Query("UPDATE app_customizations SET customCategory = :category WHERE componentKey = :componentKey")
  suspend fun updateCustomCategory(componentKey: String, category: String?)

  @Query("UPDATE app_customizations SET customIconShape = :shape WHERE componentKey = :componentKey")
  suspend fun updateCustomIconShape(componentKey: String, shape: String?)

  @Query("UPDATE app_customizations SET launchCount = launchCount + 1, lastLaunchedAt = :timestamp WHERE componentKey = :componentKey")
  suspend fun recordLaunch(componentKey: String, timestamp: Long)

  @Query("DELETE FROM app_customizations")
  suspend fun clearAll()

  @Transaction
  suspend fun replaceAllCustomizations(items: List<AppCustomizationEntity>) {
    clearAll()
    upsertAll(items)
  }
}

@Dao
interface FloatingNoteDao {
  @Query("SELECT * FROM floating_notes ORDER BY updatedAt DESC")
  fun observeAllNotes(): Flow<List<FloatingNoteEntity>>

  @Query("SELECT * FROM floating_notes ORDER BY updatedAt DESC")
  suspend fun getAllNotesSnapshot(): List<FloatingNoteEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNote(note: FloatingNoteEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(notes: List<FloatingNoteEntity>)

  @Query("DELETE FROM floating_notes WHERE id = :id")
  suspend fun deleteNoteById(id: Long)

  @Query("DELETE FROM floating_notes")
  suspend fun clearAll()

  @Transaction
  suspend fun replaceAllNotes(notes: List<FloatingNoteEntity>) {
    clearAll()
    insertAll(notes)
  }
}
