package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.datastore.LauncherPreferencesDataSource
import com.example.data.local.LauncherDatabase
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed interface BackupResult {
  data class ExportSuccess(val itemCount: Int) : BackupResult
  data class ImportSuccess(val restoredItems: Int, val restoredApps: Int) : BackupResult
  data class Failure(val reason: String) : BackupResult
}

class LauncherBackupManager(
  private val context: Context,
  private val database: LauncherDatabase,
  private val preferencesDataSource: LauncherPreferencesDataSource,
) {
  private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  suspend fun createBackupSnapshot(): LauncherBackupArchive = withContext(Dispatchers.IO) {
    val settings = preferencesDataSource.getCurrentSettings()
    val homeItems = database.homeItemDao().getAllHomeItemsSnapshot()
    val customizations = database.appCustomizationDao().getAllCustomizationsSnapshot()
    val notes = database.floatingNoteDao().getAllNotesSnapshot()

    LauncherBackupArchive(
      schemaVersion = LauncherBackupArchive.CURRENT_SCHEMA_VERSION,
      exportedAtEpochMs = System.currentTimeMillis(),
      settings = settings.toSerializable(),
      homeItems = homeItems.map { it.toSerializable() },
      appCustomizations = customizations.map { it.toSerializable() },
      floatingNotes = notes.map { it.toSerializable() },
    )
  }

  suspend fun exportAsJson(targetUri: Uri): BackupResult = withContext(Dispatchers.IO) {
    runCatching {
      val archive = createBackupSnapshot()
      val payload = json.encodeToString(LauncherBackupArchive.serializer(), archive)
      context.contentResolver.openOutputStream(targetUri)?.use { output ->
        output.write(payload.toByteArray(StandardCharsets.UTF_8))
      } ?: error("Unable to open output stream for backup file.")
      BackupResult.ExportSuccess(archive.homeItems.size)
    }.getOrElse { BackupResult.Failure(it.message ?: "JSON export failed") }
  }

  suspend fun exportAsZip(targetUri: Uri): BackupResult = withContext(Dispatchers.IO) {
    runCatching {
      val archive = createBackupSnapshot()
      val payload = json.encodeToString(LauncherBackupArchive.serializer(), archive)
      context.contentResolver.openOutputStream(targetUri)?.use { output ->
        ZipOutputStream(output).use { zip ->
          zip.putNextEntry(ZipEntry(ARCHIVE_JSON_ENTRY))
          zip.write(payload.toByteArray(StandardCharsets.UTF_8))
          zip.closeEntry()
        }
      } ?: error("Unable to open output stream for ZIP backup.")
      BackupResult.ExportSuccess(archive.homeItems.size)
    }.getOrElse { BackupResult.Failure(it.message ?: "ZIP export failed") }
  }

  suspend fun importFromUri(sourceUri: Uri): BackupResult = withContext(Dispatchers.IO) {
    runCatching {
      val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        input.readBytes()
      } ?: error("Unable to read selected backup file.")

      val jsonString = if (isZipHeader(rawBytes)) {
        extractJsonFromZip(rawBytes)
      } else {
        rawBytes.toString(StandardCharsets.UTF_8)
      }

      val archive = json.decodeFromString(LauncherBackupArchive.serializer(), jsonString)
      require(archive.schemaVersion in 1..LauncherBackupArchive.CURRENT_SCHEMA_VERSION) {
        "Unsupported backup schema version: ${archive.schemaVersion}"
      }

      val existingPinHash = preferencesDataSource.getCurrentSettings().vaultPinHash
      val restoredSettings = archive.settings.toDomain(existingPinHash = existingPinHash)

      val entities = archive.homeItems.map {
        it.toEntity(maxCols = restoredSettings.gridColumns, maxRows = restoredSettings.gridRows)
      }
      val customizations = archive.appCustomizations.map { it.toEntity() }
      val notes = archive.floatingNotes.map { it.toEntity() }

      database.homeItemDao().replaceAllItems(entities)
      database.appCustomizationDao().replaceAllCustomizations(customizations)
      database.floatingNoteDao().replaceAllNotes(notes)
      preferencesDataSource.overwriteSettings(restoredSettings)

      BackupResult.ImportSuccess(
        restoredItems = entities.size,
        restoredApps = customizations.size,
      )
    }.getOrElse { BackupResult.Failure(it.message ?: "Import failed") }
  }

  private fun isZipHeader(bytes: ByteArray): Boolean =
    bytes.size >= 4 &&
      bytes[0] == 0x50.toByte() &&
      bytes[1] == 0x4B.toByte() &&
      bytes[2] == 0x03.toByte() &&
      bytes[3] == 0x04.toByte()

  private fun extractJsonFromZip(bytes: ByteArray): String {
    ZipInputStream(BufferedInputStream(bytes.inputStream())).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && (entry.name == ARCHIVE_JSON_ENTRY || entry.name.endsWith(".json"))) {
          val buffer = ByteArrayOutputStream()
          zip.copyTo(buffer)
          return buffer.toString(StandardCharsets.UTF_8.name())
        }
        entry = zip.nextEntry
      }
    }
    error("Backup ZIP archive does not contain $ARCHIVE_JSON_ENTRY")
  }

  companion object {
    private const val ARCHIVE_JSON_ENTRY = "rawaq_launcher_backup.json"
  }
}
