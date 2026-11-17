package com.mybrowser.core.bridge

import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Deterministic Tab, Folder & Dual-Pane Split-View State Machine.
 * Manages all tab transitions, expandable folders, undo close, inactive pruning,
 * and ByteArray snapshots.
 */
// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Serialize, Deserialize, Clone)]
// pub struct TabRecord {
//     pub id: String,
//     pub url: String,
//     pub title: String,
//     pub is_loading: bool,
//     pub progress: i32,
//     pub blocked_count: i32,
//     pub created_at_ms: i64,
//     pub last_active_at_ms: i64,
//     pub folder_id: Option<String>,
//     pub is_pinned: bool,
// }
class TabStateMachineImpl : TabStateMachineContract {

  private data class TabRecord(
    val id: String,
    var url: String,
    var title: String,
    var isLoading: Boolean,
    var progress: Int,
    var blockedCount: Int,
    val createdAtMs: Long,
    var lastActiveAtMs: Long,
    var folderId: String? = null,
    var isPinned: Boolean = false,
  )

  private data class FolderRecord(
    val id: String,
    var name: String,
    var colorIndex: Int,
    var isExpanded: Boolean,
  )

  private data class UndoClosedTab(
    val tab: TabRecord,
    val index: Int,
    val detachedFolder: FolderRecord?,
  )

  private val lock = Any()
  private val tabs = mutableListOf<TabRecord>()
  private val folders = mutableListOf<FolderRecord>()
  private var primaryTabId: String = ""
  private var secondaryTabId: String? = null
  private var splitMode: Boolean = false
  private var activePane: Int = 0
  private var sortMode: String = "RECENT"
  private var undoSlot: UndoClosedTab? = null

  init {
    val now = System.currentTimeMillis()
    val firstId = "tab-1"
    tabs +=
      TabRecord(
        id = firstId,
        url = "about:mb",
        title = "시작 페이지",
        isLoading = false,
        progress = 100,
        blockedCount = 0,
        createdAtMs = now,
        lastActiveAtMs = now,
        folderId = null,
      )
    primaryTabId = firstId
  }

  override fun getStateJson(): String = synchronized(lock) { serializeStateJsonLocked() }

  override fun createTab(initialUrl: String, title: String, targetPane: Int): String =
    synchronized(lock) {
      val now = System.currentTimeMillis()
      val id = "tab-${UUID.randomUUID().toString().take(8)}"
      val normalizedUrl = initialUrl.ifBlank { "about:mb" }
      val resolvedTitle =
        title.ifBlank { if (normalizedUrl == "about:mb") "시작 페이지" else normalizedUrl }
      tabs +=
        TabRecord(
          id = id,
          url = normalizedUrl,
          title = resolvedTitle,
          isLoading = normalizedUrl != "about:mb" && normalizedUrl != "about:blank",
          progress = if (normalizedUrl == "about:mb" || normalizedUrl == "about:blank") 100 else 10,
          blockedCount = 0,
          createdAtMs = now,
          lastActiveAtMs = now,
          folderId = null,
        )
      if (targetPane == 1 && splitMode) {
        secondaryTabId = id
        activePane = 1
      } else {
        primaryTabId = id
        activePane = 0
      }
      serializeStateJsonLocked()
    }

  override fun closeTab(tabId: String): String = synchronized(lock) {
    if (tabs.size <= 1) {
      val only = tabs.first()
      only.url = "about:mb"
      only.title = "시작 페이지"
      only.isLoading = false
      only.progress = 100
      only.blockedCount = 0
      only.lastActiveAtMs = System.currentTimeMillis()
      only.folderId = null
      only.isPinned = false
      folders.clear()
      primaryTabId = only.id
      secondaryTabId = null
      splitMode = false
      activePane = 0
      undoSlot = null
      return@synchronized serializeStateJsonLocked()
    }

    val idx = tabs.indexOfFirst { it.id == tabId }
    if (idx >= 0) {
      val removed = tabs.removeAt(idx)
      val folderSnap =
        removed.folderId?.let { fId ->
          val folder = folders.firstOrNull { it.id == fId }
          if (folder != null && tabs.none { it.folderId == fId }) {
            folders.remove(folder)
            folder.copy()
          } else {
            folder?.copy()
          }
        }
      undoSlot = UndoClosedTab(tab = removed.copy(), index = idx, detachedFolder = folderSnap)
    }

    if (primaryTabId == tabId) {
      val candidate = tabs.firstOrNull { it.id != secondaryTabId } ?: tabs.first()
      primaryTabId = candidate.id
      candidate.lastActiveAtMs = System.currentTimeMillis()
    }
    if (secondaryTabId == tabId) {
      secondaryTabId = tabs.firstOrNull { it.id != primaryTabId }?.id
      if (secondaryTabId == null) {
        splitMode = false
        activePane = 0
      }
    }
    serializeStateJsonLocked()
  }

  override fun undoLastClosedTab(): String = synchronized(lock) {
    val undo = undoSlot ?: return@synchronized serializeStateJsonLocked()
    undoSlot = null
    if (tabs.any { it.id == undo.tab.id }) return@synchronized serializeStateJsonLocked()

    if (undo.detachedFolder != null && folders.none { it.id == undo.detachedFolder.id }) {
      folders += undo.detachedFolder.copy()
    }
    val insertIdx = undo.index.coerceIn(0, tabs.size)
    val restoredTab = undo.tab.copy(lastActiveAtMs = System.currentTimeMillis())
    if (restoredTab.folderId != null && folders.none { it.id == restoredTab.folderId }) {
      restoredTab.folderId = null
    }
    tabs.add(insertIdx, restoredTab)
    primaryTabId = restoredTab.id
    activePane = 0
    serializeStateJsonLocked()
  }

  override fun hasUndoableClosedTab(): Boolean = synchronized(lock) { undoSlot != null }

  override fun selectTab(tabId: String, targetPane: Int): String = synchronized(lock) {
    val target = tabs.firstOrNull { it.id == tabId } ?: return@synchronized serializeStateJsonLocked()
    target.lastActiveAtMs = System.currentTimeMillis()
    if (targetPane == 1 && splitMode) {
      if (primaryTabId == tabId) {
        val oldSecondary = secondaryTabId
        secondaryTabId = tabId
        if (oldSecondary != null) primaryTabId = oldSecondary
      } else {
        secondaryTabId = tabId
      }
      activePane = 1
    } else {
      if (splitMode && secondaryTabId == tabId) {
        val oldPrimary = primaryTabId
        primaryTabId = tabId
        secondaryTabId = oldPrimary
      } else {
        primaryTabId = tabId
      }
      activePane = 0
    }
    serializeStateJsonLocked()
  }

  override fun updateTabNavigation(
    tabId: String,
    url: String,
    title: String,
    isLoading: Boolean,
    progress: Int,
  ): String = synchronized(lock) {
    val tab = tabs.firstOrNull { it.id == tabId } ?: return@synchronized serializeStateJsonLocked()
    if (url.isNotBlank()) tab.url = url
    if (title.isNotBlank()) tab.title = title
    tab.isLoading = isLoading
    tab.progress = progress.coerceIn(0, 100)
    tab.lastActiveAtMs = System.currentTimeMillis()
    serializeStateJsonLocked()
  }

  override fun incrementTabBlocked(tabId: String): String = synchronized(lock) {
    tabs.firstOrNull { it.id == tabId }?.let { it.blockedCount += 1 }
    serializeStateJsonLocked()
  }

  override fun togglePinTab(tabId: String): String = synchronized(lock) {
    tabs.firstOrNull { it.id == tabId }?.let { it.isPinned = !it.isPinned }
    serializeStateJsonLocked()
  }

  override fun setTabSortMode(mode: String): String = synchronized(lock) {
    val norm = mode.trim().uppercase()
    if (norm in setOf("RECENT", "CREATED", "DOMAIN", "FOLDERS_FIRST", "PINNED_FIRST")) {
      sortMode = norm
    }
    serializeStateJsonLocked()
  }

  override fun setSplitMode(enabled: Boolean): String = synchronized(lock) {
    splitMode = enabled
    if (enabled) {
      if (secondaryTabId == null || secondaryTabId == primaryTabId) {
        val existingOther = tabs.firstOrNull { it.id != primaryTabId }
        if (existingOther != null) {
          secondaryTabId = existingOther.id
        } else {
          val now = System.currentTimeMillis()
          val newId = "tab-${UUID.randomUUID().toString().take(8)}"
          tabs +=
            TabRecord(
              id = newId,
              url = "about:mb",
              title = "분할 시작 페이지",
              isLoading = false,
              progress = 100,
              blockedCount = 0,
              createdAtMs = now,
              lastActiveAtMs = now,
              folderId = null,
            )
          secondaryTabId = newId
        }
      }
    } else {
      activePane = 0
    }
    serializeStateJsonLocked()
  }

  override fun splitActiveTabToPane(directionRight: Boolean): String = synchronized(lock) {
    val now = System.currentTimeMillis()
    val activeId =
      if (splitMode && activePane == 1 && secondaryTabId != null) {
        secondaryTabId!!
      } else {
        primaryTabId
      }
    val newId = "tab-${UUID.randomUUID().toString().take(8)}"
    tabs +=
      TabRecord(
        id = newId,
        url = "about:mb",
        title = "시작 페이지",
        isLoading = false,
        progress = 100,
        blockedCount = 0,
        createdAtMs = now,
        lastActiveAtMs = now,
        folderId = null,
      )
    splitMode = true
    if (directionRight) {
      primaryTabId = newId
      secondaryTabId = activeId
      activePane = 1
    } else {
      primaryTabId = activeId
      secondaryTabId = newId
      activePane = 0
    }
    serializeStateJsonLocked()
  }

  override fun closeSplitKeepPane(keepPaneIndex: Int): String = synchronized(lock) {
    val keptId =
      if (keepPaneIndex == 1 && secondaryTabId != null) {
        secondaryTabId!!
      } else {
        primaryTabId
      }
    primaryTabId = keptId
    secondaryTabId = null
    splitMode = false
    activePane = 0
    serializeStateJsonLocked()
  }

  override fun setActivePane(paneIndex: Int): String = synchronized(lock) {
    activePane = if (splitMode && paneIndex == 1) 1 else 0
    serializeStateJsonLocked()
  }

  override fun stepAdjacentTab(delta: Int): String = synchronized(lock) {
    if (tabs.size <= 1) return@synchronized serializeStateJsonLocked()
    val currentId =
      if (splitMode && activePane == 1 && secondaryTabId != null) secondaryTabId else primaryTabId
    val idx = tabs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val nextIdx = ((idx + delta) % tabs.size + tabs.size) % tabs.size
    val nextTab = tabs[nextIdx]
    nextTab.lastActiveAtMs = System.currentTimeMillis()
    if (splitMode && activePane == 1) {
      secondaryTabId = nextTab.id
    } else {
      primaryTabId = nextTab.id
    }
    serializeStateJsonLocked()
  }

  override fun mergeTabsIntoFolder(
    sourceTabId: String,
    targetTabId: String,
    folderName: String,
  ): String = synchronized(lock) {
    if (sourceTabId == targetTabId) return@synchronized serializeStateJsonLocked()
    val source = tabs.firstOrNull { it.id == sourceTabId } ?: return@synchronized serializeStateJsonLocked()
    val target = tabs.firstOrNull { it.id == targetTabId } ?: return@synchronized serializeStateJsonLocked()

    val existingFolderId = target.folderId ?: source.folderId
    val folderId =
      if (existingFolderId != null && folders.any { it.id == existingFolderId }) {
        folders.first { it.id == existingFolderId }.isExpanded = true
        existingFolderId
      } else {
        val newFolderId = "folder-${UUID.randomUUID().toString().take(8)}"
        val resolvedName = folderName.ifBlank { "폴더 ${folders.size + 1}" }
        folders +=
          FolderRecord(
            id = newFolderId,
            name = resolvedName,
            colorIndex = folders.size % 5,
            isExpanded = true,
          )
        newFolderId
      }

    source.folderId = folderId
    target.folderId = folderId
    pruneEmptyFoldersLocked()
    serializeStateJsonLocked()
  }

  override fun addTabToFolder(tabId: String, folderId: String): String = synchronized(lock) {
    val tab = tabs.firstOrNull { it.id == tabId } ?: return@synchronized serializeStateJsonLocked()
    val folder =
      folders.firstOrNull { it.id == folderId } ?: return@synchronized serializeStateJsonLocked()
    tab.folderId = folder.id
    folder.isExpanded = true
    pruneEmptyFoldersLocked()
    serializeStateJsonLocked()
  }

  override fun removeTabFromFolder(tabId: String): String = synchronized(lock) {
    val tab = tabs.firstOrNull { it.id == tabId } ?: return@synchronized serializeStateJsonLocked()
    tab.folderId = null
    pruneEmptyFoldersLocked()
    serializeStateJsonLocked()
  }

  override fun toggleFolderExpanded(folderId: String): String = synchronized(lock) {
    folders.firstOrNull { it.id == folderId }?.let { it.isExpanded = !it.isExpanded }
    serializeStateJsonLocked()
  }

  override fun collapseAllFolders(): String = synchronized(lock) {
    folders.forEach { it.isExpanded = false }
    serializeStateJsonLocked()
  }

  override fun renameFolder(folderId: String, name: String): String = synchronized(lock) {
    val trimmed = name.trim()
    if (trimmed.isNotEmpty()) {
      folders.firstOrNull { it.id == folderId }?.let { it.name = trimmed }
    }
    serializeStateJsonLocked()
  }

  override fun setFolderColor(folderId: String, colorIndex: Int): String = synchronized(lock) {
    folders.firstOrNull { it.id == folderId }?.let { it.colorIndex = colorIndex.coerceIn(0, 5) }
    serializeStateJsonLocked()
  }

  override fun pruneInactiveTabs(maxAgeDays: Int, nowEpochMs: Long): Int = synchronized(lock) {
    if (maxAgeDays <= 0 || tabs.size <= 1) return@synchronized 0
    val cutoff = nowEpochMs - maxAgeDays.toLong() * 86_400_000L
    val toRemove =
      tabs.filter { tab ->
        !tab.isPinned &&
          tab.id != primaryTabId &&
          tab.id != secondaryTabId &&
          tab.lastActiveAtMs < cutoff
      }
    if (toRemove.isEmpty()) return@synchronized 0
    val removeIds = toRemove.map { it.id }.toSet()
    tabs.removeAll { it.id in removeIds }
    pruneEmptyFoldersLocked()
    toRemove.size
  }

  override fun serializeSnapshotBytes(): ByteArray = synchronized(lock) {
    serializeStateJsonLocked().toByteArray(StandardCharsets.UTF_8)
  }

  override fun restoreSnapshotBytes(payload: ByteArray): String = synchronized(lock) {
    if (payload.isEmpty()) return@synchronized serializeStateJsonLocked()
    val raw = String(payload, StandardCharsets.UTF_8)
    val parsedTabs = parseTabsFromJson(raw)
    if (parsedTabs.isEmpty()) return@synchronized serializeStateJsonLocked()

    tabs.clear()
    tabs += parsedTabs.map { it.copy(isLoading = false, progress = 100) }

    folders.clear()
    folders += parseFoldersFromJson(raw)
    val validFolderIds = folders.map { it.id }.toSet()
    tabs.forEach { tab ->
      if (tab.folderId != null && tab.folderId !in validFolderIds) {
        tab.folderId = null
      }
    }
    pruneEmptyFoldersLocked()

    val savedPrimary = extractTopString(raw, "primaryTabId")
    val savedSecondary = extractTopString(raw, "secondaryTabId")
    val savedSplit = extractTopBoolean(raw, "splitMode") ?: false
    val savedPane = extractTopInt(raw, "activePane") ?: 0
    val savedSortMode = extractTopString(raw, "sortMode")?.trim()?.uppercase()
    if (savedSortMode in setOf("RECENT", "CREATED", "DOMAIN", "FOLDERS_FIRST", "PINNED_FIRST")) {
      sortMode = savedSortMode!!
    }

    primaryTabId =
      if (savedPrimary != null && tabs.any { it.id == savedPrimary }) savedPrimary
      else tabs.first().id
    secondaryTabId =
      if (savedSecondary != null && savedSecondary != primaryTabId && tabs.any { it.id == savedSecondary }) {
        savedSecondary
      } else {
        null
      }
    splitMode = savedSplit && secondaryTabId != null
    activePane = if (splitMode && savedPane == 1) 1 else 0
    undoSlot = null
    serializeStateJsonLocked()
  }

  private fun pruneEmptyFoldersLocked() {
    folders.removeAll { folder -> tabs.none { it.folderId == folder.id } }
  }

  private fun serializeStateJsonLocked(): String {
    val tabItems =
      tabs.joinToString(",") { t ->
        val folderField = t.folderId?.let { JsonMini.quote(it) } ?: "null"
        """{"id":${JsonMini.quote(t.id)},"url":${JsonMini.quote(t.url)},"title":${JsonMini.quote(t.title)},"isLoading":${t.isLoading},"progress":${t.progress},"blockedCount":${t.blockedCount},"createdAtMs":${t.createdAtMs},"lastActiveAtMs":${t.lastActiveAtMs},"folderId":$folderField,"isPinned":${t.isPinned}}"""
      }
    val folderItems =
      folders.joinToString(",") { f ->
        """{"id":${JsonMini.quote(f.id)},"name":${JsonMini.quote(f.name)},"colorIndex":${f.colorIndex},"isExpanded":${f.isExpanded}}"""
      }
    val sec = secondaryTabId?.let { JsonMini.quote(it) } ?: "null"
    return """{"primaryTabId":${JsonMini.quote(primaryTabId)},"secondaryTabId":$sec,"splitMode":$splitMode,"activePane":$activePane,"sortMode":${JsonMini.quote(sortMode)},"hasUndo":${undoSlot != null},"folders":[$folderItems],"tabs":[$tabItems]}"""
  }

  private fun parseTabsFromJson(raw: String): List<TabRecord> {
    val arrBody = extractArrayBody(raw, "tabs") ?: return emptyList()
    val objRegex = Regex("""\{[^{}]*\}""")
    val now = System.currentTimeMillis()
    return objRegex.findAll(arrBody).mapNotNull { m ->
      val obj = m.value
      val id = extractTopString(obj, "id") ?: return@mapNotNull null
      val url = extractTopString(obj, "url") ?: "about:mb"
      val title = extractTopString(obj, "title") ?: url
      val blocked = extractTopInt(obj, "blockedCount") ?: 0
      val created = extractTopLong(obj, "createdAtMs") ?: now
      val lastActive = extractTopLong(obj, "lastActiveAtMs") ?: created
      val folderId = extractTopString(obj, "folderId")
      val isPinned = extractTopBoolean(obj, "isPinned") ?: false
      TabRecord(
        id = id,
        url = url,
        title = title,
        isLoading = false,
        progress = 100,
        blockedCount = blocked,
        createdAtMs = created,
        lastActiveAtMs = lastActive,
        folderId = folderId,
        isPinned = isPinned,
      )
    }.toList()
  }

  private fun parseFoldersFromJson(raw: String): List<FolderRecord> {
    val arrBody = extractArrayBody(raw, "folders") ?: return emptyList()
    val objRegex = Regex("""\{[^{}]*\}""")
    return objRegex.findAll(arrBody).mapNotNull { m ->
      val obj = m.value
      val id = extractTopString(obj, "id") ?: return@mapNotNull null
      val name = extractTopString(obj, "name") ?: "새 폴더"
      val colorIdx = extractTopInt(obj, "colorIndex") ?: 0
      val expanded = extractTopBoolean(obj, "isExpanded") ?: true
      FolderRecord(id = id, name = name, colorIndex = colorIdx.coerceIn(0, 5), isExpanded = expanded)
    }.toList()
  }

  private fun extractArrayBody(raw: String, key: String): String? {
    val startMatch = Regex(""""$key"\s*:\s*\[""").find(raw) ?: return null
    var depth = 1
    var idx = startMatch.range.last + 1
    var inString = false
    var escaped = false
    val sb = StringBuilder()
    while (idx < raw.length && depth > 0) {
      val ch = raw[idx]
      if (escaped) {
        escaped = false
      } else if (ch == '\\' && inString) {
        escaped = true
      } else if (ch == '"') {
        inString = !inString
      } else if (!inString) {
        if (ch == '[') depth++
        else if (ch == ']') {
          depth--
          if (depth == 0) break
        }
      }
      sb.append(ch)
      idx++
    }
    return sb.toString()
  }

  private fun extractTopString(json: String, key: String): String? =
    Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(json)?.groupValues?.get(1)
      ?.replace("\\\"", "\"")
      ?.replace("\\\\", "\\")

  private fun extractTopBoolean(json: String, key: String): Boolean? =
    Regex(""""$key"\s*:\s*(true|false)""").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()

  private fun extractTopInt(json: String, key: String): Int? =
    Regex(""""$key"\s*:\s*(-?\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull()

  private fun extractTopLong(json: String, key: String): Long? =
    Regex(""""$key"\s*:\s*(-?\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull()
}
