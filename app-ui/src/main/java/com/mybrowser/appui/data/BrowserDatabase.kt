package com.mybrowser.appui.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserSettingsStore by preferencesDataStore(name = "mybrowser_settings")

@Immutable
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
  @PrimaryKey val url: String,
  val title: String,
  val category: String = "북마크",
  val createdAtMs: Long = System.currentTimeMillis(),
)

@Immutable
@Entity(tableName = "history")
data class HistoryEntity(
  @PrimaryKey val url: String,
  val title: String,
  val visitCount: Int = 1,
  val lastVisitedMs: Long = System.currentTimeMillis(),
)

@Dao
interface BrowserDao {
  @Query("SELECT * FROM bookmarks ORDER BY createdAtMs DESC")
  fun observeBookmarks(): Flow<List<BookmarkEntity>>

  @Query("SELECT COUNT(*) FROM bookmarks")
  suspend fun countBookmarks(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBookmark(bookmark: BookmarkEntity)

  @Query("DELETE FROM bookmarks WHERE url = :url")
  suspend fun deleteBookmark(url: String)

  @Query("SELECT * FROM history ORDER BY lastVisitedMs DESC LIMIT 200")
  fun observeHistory(): Flow<List<HistoryEntity>>

  @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
  suspend fun findHistoryByUrl(url: String): HistoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHistory(entry: HistoryEntity)

  @Query("DELETE FROM history")
  suspend fun clearHistory()

  @Query("DELETE FROM history WHERE url = :url")
  suspend fun deleteHistoryItem(url: String)
}

@Database(
  entities = [BookmarkEntity::class, HistoryEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class BrowserDatabase : RoomDatabase() {
  abstract fun browserDao(): BrowserDao

  companion object {
    @Volatile private var instance: BrowserDatabase? = null

    fun getInstance(context: Context): BrowserDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                BrowserDatabase::class.java,
                "mybrowser_local.db",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}

@Immutable
data class BrowserPreferences(
  val toolbarConfigJson: String? = null,
  val settingsStoreJson: String? = null,
  val designConfigJson: String? = null,
  val sitePermissionsJson: String? = null,
  val tabSessionSnapshotJson: String? = null,
  val aiConfigJson: String? = null,
)

class BrowserRepository(
  private val context: Context,
  private val dao: BrowserDao,
) {
  val bookmarksFlow: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
  val historyFlow: Flow<List<HistoryEntity>> = dao.observeHistory()

  val preferencesFlow: Flow<BrowserPreferences> =
    context.browserSettingsStore.data.map { prefs ->
      BrowserPreferences(
        toolbarConfigJson = prefs[KEY_TOOLBAR_CONFIG_JSON],
        settingsStoreJson = prefs[KEY_SETTINGS_STORE_JSON],
        designConfigJson = prefs[KEY_DESIGN_CONFIG_JSON],
        sitePermissionsJson = prefs[KEY_SITE_PERMISSIONS_JSON],
        tabSessionSnapshotJson = prefs[KEY_TAB_SESSION_SNAPSHOT_JSON],
        aiConfigJson = prefs[KEY_AI_CONFIG_JSON],
      )
    }

  suspend fun ensureDefaultBookmarks() {
    if (dao.countBookmarks() > 0) return
    val defaults =
      listOf(
        BookmarkEntity("https://duckduckgo.com", "DuckDuckGo", "검색", 1L),
        BookmarkEntity("https://ko.wikipedia.org", "위키백과", "백과", 2L),
        BookmarkEntity("https://www.rust-lang.org", "Rust", "개발", 3L),
        BookmarkEntity("https://source.android.com", "AOSP", "시스템", 4L),
        BookmarkEntity("https://f-droid.org", "F-Droid", "FOSS", 5L),
        BookmarkEntity("https://developer.mozilla.org", "MDN 문서", "문서", 6L),
      )
    defaults.forEach { dao.upsertBookmark(it) }
  }

  suspend fun toggleBookmark(url: String, title: String, currentlyBookmarked: Boolean) {
    if (url.isBlank() || url == "about:mb" || url == "about:blank") return
    if (currentlyBookmarked) {
      dao.deleteBookmark(url)
    } else {
      dao.upsertBookmark(BookmarkEntity(url = url, title = title.ifBlank { url }))
    }
  }

  suspend fun deleteBookmark(url: String) = dao.deleteBookmark(url)

  suspend fun recordVisit(url: String, title: String) {
    if (url.isBlank() || url.startsWith("about:")) return
    val existing = dao.findHistoryByUrl(url)
    val now = System.currentTimeMillis()
    val updated =
      if (existing != null) {
        existing.copy(
          title = title.ifBlank { existing.title },
          visitCount = existing.visitCount + 1,
          lastVisitedMs = now,
        )
      } else {
        HistoryEntity(url = url, title = title.ifBlank { url }, visitCount = 1, lastVisitedMs = now)
      }
    dao.upsertHistory(updated)
  }

  suspend fun deleteHistoryItem(url: String) = dao.deleteHistoryItem(url)

  suspend fun clearHistory() = dao.clearHistory()

  suspend fun setToolbarConfigJson(json: String) {
    context.browserSettingsStore.edit { it[KEY_TOOLBAR_CONFIG_JSON] = json }
  }

  suspend fun setSettingsStoreJson(json: String) {
    context.browserSettingsStore.edit { it[KEY_SETTINGS_STORE_JSON] = json }
  }

  suspend fun setDesignConfigJson(json: String) {
    context.browserSettingsStore.edit { it[KEY_DESIGN_CONFIG_JSON] = json }
  }

  suspend fun setSitePermissionsJson(json: String) {
    context.browserSettingsStore.edit { it[KEY_SITE_PERMISSIONS_JSON] = json }
  }

  suspend fun setTabSessionSnapshotJson(json: String) {
    context.browserSettingsStore.edit { it[KEY_TAB_SESSION_SNAPSHOT_JSON] = json }
  }

  suspend fun setAiConfigJson(json: String) {
    context.browserSettingsStore.edit { it[KEY_AI_CONFIG_JSON] = json }
  }

  private companion object {
    val KEY_TOOLBAR_CONFIG_JSON = stringPreferencesKey("toolbar_config_json")
    val KEY_SETTINGS_STORE_JSON = stringPreferencesKey("settings_store_json")
    val KEY_DESIGN_CONFIG_JSON = stringPreferencesKey("design_config_json")
    val KEY_SITE_PERMISSIONS_JSON = stringPreferencesKey("site_permissions_json")
    val KEY_TAB_SESSION_SNAPSHOT_JSON = stringPreferencesKey("tab_session_snapshot_json")
    val KEY_AI_CONFIG_JSON = stringPreferencesKey("ai_config_json")
  }
}
