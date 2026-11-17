package com.mybrowser.appui.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mybrowser.appui.data.BookmarkEntity
import com.mybrowser.appui.data.BrowserDatabase
import com.mybrowser.appui.data.BrowserRepository
import com.mybrowser.appui.data.HistoryEntity
import com.mybrowser.appui.network.DohLookupResult
import com.mybrowser.appui.network.DohNetworkClient
import com.mybrowser.appui.network.DohProviderInfo
import com.mybrowser.appui.theme.AccentColorPreset
import com.mybrowser.appui.theme.ThemeMode
import com.mybrowser.appui.ui.DesignUiConfig
import com.mybrowser.appui.webview.AospWebViewPool
import com.mybrowser.appui.webview.ExternalAppLaunchRequest
import com.mybrowser.appui.webview.PendingDownloadRequest
import com.mybrowser.appui.webview.WebRenderPolicy
import com.mybrowser.appui.webview.WebSitePermissionChallenge
import com.mybrowser.appui.webview.WebViewNavCapabilities
import com.mybrowser.core.bridge.CoreBridgeRuntime
import com.mybrowser.core.bridge.EngineAbiInfo
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class EngineMetaUi(
  val abiVersion: Int = 1,
  val backendKind: String = "kotlin-ffi-stub",
  val featuresCsv: String = "",
)

@Immutable
data class TabFolderUiModel(
  val id: String,
  val name: String,
  val colorIndex: Int,
  val isExpanded: Boolean,
)

@Immutable
data class TabUiModel(
  val id: String,
  val url: String,
  val title: String,
  val displayHost: String,
  val isSecure: Boolean,
  val isLoading: Boolean,
  val progress: Int,
  val blockedCount: Int,
  val createdAtMs: Long = 0L,
  val lastActiveAtMs: Long = 0L,
  val folderId: String? = null,
  val isPinned: Boolean = false,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
)

enum class TabSortMode(val id: String, val label: String) {
  RECENT("RECENT", "최근 사용순"),
  CREATED("CREATED", "생성순"),
  DOMAIN("DOMAIN", "도메인별"),
  FOLDERS_FIRST("FOLDERS_FIRST", "폴더 우선"),
  PINNED_FIRST("PINNED_FIRST", "고정 탭 우선");

  companion object {
    fun fromId(raw: String): TabSortMode =
      entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: RECENT
  }
}

enum class TabViewMode(val id: String, val label: String) {
  THUMBNAIL_GRID("THUMBNAIL_GRID", "썸네일 그리드"),
  COMPACT_LIST("COMPACT_LIST", "컴팩트 리스트");

  companion object {
    fun fromId(raw: String): TabViewMode =
      entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: THUMBNAIL_GRID
  }
}

@Immutable
data class SearchEngineOption(
  val id: String,
  val label: String,
  val homepage: String,
)

@Immutable
data class BlockerStatsUi(
  val enabled: Boolean = true,
  val level: String = "STANDARD",
  val ruleCount: Int = 0,
  val evaluated: Long = 0,
  val totalBlocked: Long = 0,
  val adsBlocked: Long = 0,
  val trackersBlocked: Long = 0,
  val fingerprintingBlocked: Long = 0,
  val telemetryBlocked: Long = 0,
)

@Immutable
data class OmniboxSuggestionUi(
  val url: String,
  val title: String,
  val isBookmark: Boolean,
)

@Immutable
data class SnackbarNotification(
  val id: Long,
  val message: String,
  val isUndoClose: Boolean = false,
)

enum class ToolbarActionId(val id: String, val label: String) {
  BACK("BACK", "뒤로"),
  FORWARD("FORWARD", "앞으로"),
  REFRESH("REFRESH", "새로고침"),
  NEW_TAB("NEW_TAB", "새 탭"),
  TABS("TABS", "탭"),
  SPLIT_VIEW("SPLIT_VIEW", "분할"),
  BOOKMARK("BOOKMARK", "북마크"),
  HISTORY("HISTORY", "방문 기록"),
  FIND_IN_PAGE("FIND_IN_PAGE", "찾기"),
  DESKTOP_MODE("DESKTOP_MODE", "PC 버전"),
  SHARE("SHARE", "공유"),
  QR_SCAN("QR_SCAN", "QR 스캔"),
  SCREENSHOT("SCREENSHOT", "스크린샷"),
  READER_MODE("READER_MODE", "읽기"),
  HOME("HOME", "홈"),
  LIBRARY("LIBRARY", "보관함");

  companion object {
    fun fromId(raw: String): ToolbarActionId? = entries.firstOrNull { it.id == raw }
  }
}

enum class ToolbarAlignment(val id: String, val label: String, val bias: Float) {
  LEFT_OFFSET("LEFT_OFFSET", "왼쪽", -1.0f),
  CENTER("CENTER", "가운데", 0.0f),
  RIGHT_OFFSET("RIGHT_OFFSET", "오른쪽", 1.0f);

  companion object {
    fun fromId(raw: String): ToolbarAlignment = entries.firstOrNull { it.id == raw } ?: CENTER
  }
}

enum class FloatingBarStyle(val id: String, val label: String) {
  TRANSLUCENT_GLASS("TRANSLUCENT_GLASS", "반투명"),
  SOLID_PILL("SOLID_PILL", "솔리드");

  companion object {
    fun fromId(raw: String): FloatingBarStyle =
      entries.firstOrNull { it.id == raw } ?: TRANSLUCENT_GLASS
  }
}

enum class AutoHideSensitivity(val id: String, val label: String, val dyThreshold: Int) {
  LOW("LOW", "낮음", 64),
  MEDIUM("MEDIUM", "보통", 36),
  HIGH("HIGH", "높음", 18);

  companion object {
    fun fromId(raw: String): AutoHideSensitivity = entries.firstOrNull { it.id == raw } ?: MEDIUM
  }
}

enum class SplitGestureSensitivity(val id: String, val label: String, val swipeThresholdDp: Float) {
  LOW("LOW", "둔감", 78f),
  MEDIUM("MEDIUM", "보통", 52f),
  HIGH("HIGH", "민감", 32f);

  companion object {
    fun fromId(raw: String): SplitGestureSensitivity =
      entries.firstOrNull { it.id == raw } ?: MEDIUM
  }
}

enum class StartupBehavior(val id: String, val label: String) {
  RESTORE_TABS("RESTORE_TABS", "이전 탭 복원"),
  SPEED_DIAL("SPEED_DIAL", "즐겨찾기 홈"),
  BLANK_PAGE("BLANK_PAGE", "새 탭");

  companion object {
    fun fromId(raw: String): StartupBehavior = entries.firstOrNull { it.id == raw } ?: SPEED_DIAL
  }
}

enum class BlockerLevel(val id: String, val label: String) {
  STANDARD("STANDARD", "표준"),
  STRICT("STRICT", "엄격");

  companion object {
    fun fromId(raw: String): BlockerLevel = entries.firstOrNull { it.id == raw } ?: STANDARD
  }
}

@Immutable
data class ToolbarConfigUi(
  val leftSlots: List<ToolbarActionId> =
    listOf(ToolbarActionId.BACK, ToolbarActionId.FORWARD, ToolbarActionId.BOOKMARK),
  val rightSlots: List<ToolbarActionId> =
    listOf(ToolbarActionId.TABS, ToolbarActionId.SPLIT_VIEW),
  val alignment: ToolbarAlignment = ToolbarAlignment.CENTER,
  val autoMinimize: Boolean = true,
  val swipeBarSwitchTabs: Boolean = true,
  val pullDownRefresh: Boolean = true,
)

@Immutable
data class BrowserSettingsUi(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val accentPreset: AccentColorPreset = AccentColorPreset.DYNAMIC,
  val dynamicColor: Boolean = true,
  val floatingBarStyle: FloatingBarStyle = FloatingBarStyle.TRANSLUCENT_GLASS,
  val autoHideSensitivity: AutoHideSensitivity = AutoHideSensitivity.MEDIUM,
  val webFontZoomPercent: Int = 100,
  val forceWebDark: Boolean = false,
  val hideStatusBar: Boolean = false,
  val autoDualPaneUnfold: Boolean = false,
  val splitGestureEnabled: Boolean = true,
  val splitGestureSensitivity: SplitGestureSensitivity = SplitGestureSensitivity.MEDIUM,
  val swipeUpOpenTabs: Boolean = true,
  val tabSwipeToCloseEnabled: Boolean = true,
  val tabDragFolderEnabled: Boolean = true,
  val tabViewMode: TabViewMode = TabViewMode.THUMBNAIL_GRID,
  val defaultTabSortMode: TabSortMode = TabSortMode.RECENT,
  val searchEngineId: String = "ddg",
  val customSearchUrl: String = "https://www.mojeek.com/search?q=%s",
  val trackerBlockingEnabled: Boolean = true,
  val blockerLevel: BlockerLevel = BlockerLevel.STANDARD,
  val blockThirdPartyCookies: Boolean = true,
  val sendDntGpcHeaders: Boolean = true,
  val httpsOnlyMode: Boolean = true,
  val dohProviderId: String = "quad9",
  val customDohUrl: String = "https://dns.quad9.net/dns-query",
  val confirmExternalApp: Boolean = true,
  val startupBehavior: StartupBehavior = StartupBehavior.SPEED_DIAL,
  val backgroundTabSleep: Boolean = true,
  val autoCloseDays: Int = 0,
  val downloadTreeUri: String = "",
  val confirmBeforeDownload: Boolean = true,
)

@Immutable
data class SitePolicyUi(
  val javascriptEnabled: Boolean = true,
  val popupsAllowed: Boolean = false,
  val desktopSite: Boolean = false,
  val blockImages: Boolean = false,
  val cameraPolicy: String = "ASK",
  val microphonePolicy: String = "ASK",
  val locationPolicy: String = "ASK",
  val clipboardPolicy: String = "ASK",
  val notificationsPolicy: String = "BLOCK",
  val sensorsPolicy: String = "ASK",
  val autoplayMedia: String = "BLOCK",
)

@Immutable
data class DomainSiteOverrideUi(
  val domain: String,
  val policy: SitePolicyUi,
)

@Immutable
data class SitePermissionsUiState(
  val globalPolicy: SitePolicyUi = SitePolicyUi(),
  val domainOverrides: List<DomainSiteOverrideUi> = emptyList(),
)

@Immutable
data class ReaderArticleUi(
  val title: String,
  val domain: String,
  val blocks: List<String>,
)

enum class BrowserOverlay {
  NONE,
  TAB_SWITCHER,
  LIBRARY_PANEL,
  SETTINGS_HUB,
  READER_VIEW,
  QR_SCANNER,
}

@Immutable
data class BrowserUiState(
  val tabs: List<TabUiModel> = emptyList(),
  val folders: List<TabFolderUiModel> = emptyList(),
  val tabSortMode: TabSortMode = TabSortMode.RECENT,
  val hasUndoableClosedTab: Boolean = false,
  val primaryTabId: String = "tab-1",
  val secondaryTabId: String? = null,
  val splitMode: Boolean = false,
  val activePane: Int = 0,
  val activeOverlay: BrowserOverlay = BrowserOverlay.NONE,
  val libraryInitialTab: Int = 0,
  val isFloatingBarCollapsed: Boolean = false,
  val isOmniboxFocused: Boolean = false,
  val omniboxQuery: String = "",
  val omniboxTargetPane: Int = 0,
  val suggestions: List<OmniboxSuggestionUi> = emptyList(),
  val isFindInPageVisible: Boolean = false,
  val findQuery: String = "",
  val findActiveMatch: Int = 0,
  val findTotalMatches: Int = 0,
  val readerArticle: ReaderArticleUi? = null,
  val toolbarConfig: ToolbarConfigUi = ToolbarConfigUi(),
  val designConfig: DesignUiConfig = DesignUiConfig(),
  val settings: BrowserSettingsUi = BrowserSettingsUi(),
  val sitePermissions: SitePermissionsUiState = SitePermissionsUiState(),
  val pendingPermissionChallenge: WebSitePermissionChallenge? = null,
  val pendingExternalAppRequest: ExternalAppLaunchRequest? = null,
  val pendingDownloadRequest: PendingDownloadRequest? = null,
  val bookmarks: List<BookmarkEntity> = emptyList(),
  val history: List<HistoryEntity> = emptyList(),
  val blockerStats: BlockerStatsUi = BlockerStatsUi(),
  val searchEngines: List<SearchEngineOption> = emptyList(),
  val dohProviders: List<DohProviderInfo> = emptyList(),
  val isProbingDoh: Boolean = false,
  val lastDohLookup: DohLookupResult? = null,
  val statusBannerMessage: String? = null,
  val snackbarNotification: SnackbarNotification? = null,
  val abiInfo: EngineMetaUi = EngineMetaUi(),
) {
  val primaryTab: TabUiModel?
    get() = tabs.firstOrNull { it.id == primaryTabId } ?: tabs.firstOrNull()

  val secondaryTab: TabUiModel?
    get() = secondaryTabId?.let { secId -> tabs.firstOrNull { it.id == secId } }

  val focusedTab: TabUiModel?
    get() =
      if (splitMode && activePane == 1) {
        secondaryTab ?: primaryTab
      } else {
        primaryTab
      }
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

  val coreBridge = CoreBridgeRuntime()
  private val appContext = application.applicationContext
  private val repository =
    BrowserRepository(
      context = appContext,
      dao = BrowserDatabase.getInstance(appContext).browserDao(),
    )
  private val dohClient = DohNetworkClient(coreBridge.dohResolver)
  val webViewPool = AospWebViewPool(coreBridge.urlEngine, coreBridge.trackerBlocker)

  private val navCapabilitiesMap = mutableMapOf<String, WebViewNavCapabilities>()
  val tabThumbnails = mutableStateMapOf<String, ImageBitmap>()
  val tabFavicons = mutableStateMapOf<String, ImageBitmap>()
  private var accumulatedScrollDyDp = 0f
  private var startupApplied = false
  private var omniboxSearchJob: Job? = null

  private val _uiState =
    MutableStateFlow(
      BrowserUiState(
        searchEngines = parseSearchEngines(coreBridge.urlEngine.getSearchEnginesJson()),
        dohProviders = dohClient.listProviders(),
        abiInfo = coreBridge.abiInfo.toMetaUi(),
      )
    )
  val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

  init {
    syncFromCoreBridge(persistTabs = false)
    syncToolbarConfigFromCore()
    syncDesignConfigFromCore()
    syncSettingsFromCore()
    syncSitePermissionsFromCore()

    viewModelScope.launch { repository.ensureDefaultBookmarks() }

    viewModelScope.launch {
      repository.bookmarksFlow.collect { bookmarks ->
        _uiState.update { it.copy(bookmarks = bookmarks) }
      }
    }

    viewModelScope.launch {
      repository.historyFlow.collect { history ->
        _uiState.update { it.copy(history = history) }
      }
    }

    viewModelScope.launch {
      val prefs = repository.preferencesFlow.first()
      if (!prefs.toolbarConfigJson.isNullOrBlank()) {
        coreBridge.toolbarConfig.restoreConfigJson(prefs.toolbarConfigJson)
        syncToolbarConfigFromCore()
      }
      if (!prefs.designConfigJson.isNullOrBlank()) {
        coreBridge.designConfig.restoreDesignConfigJson(prefs.designConfigJson)
        syncDesignConfigFromCore()
      }
      if (!prefs.settingsStoreJson.isNullOrBlank()) {
        coreBridge.settingsStore.restoreSettingsJson(prefs.settingsStoreJson)
        syncSettingsFromCore()
      }
      if (!prefs.sitePermissionsJson.isNullOrBlank()) {
        coreBridge.sitePermissions.restoreStateJson(prefs.sitePermissionsJson)
        syncSitePermissionsFromCore()
      }

      val currentSettings = _uiState.value.settings
      coreBridge.trackerBlocker.setEnabled(currentSettings.trackerBlockingEnabled)
      coreBridge.trackerBlocker.setLevel(currentSettings.blockerLevel.id)
      refreshBlockerStats()

      if (!startupApplied) {
        startupApplied = true
        when (currentSettings.startupBehavior) {
          StartupBehavior.RESTORE_TABS -> {
            val snap = prefs.tabSessionSnapshotJson
            if (!snap.isNullOrBlank()) {
              coreBridge.tabStateMachine.restoreSnapshotBytes(
                snap.toByteArray(StandardCharsets.UTF_8)
              )
            }
            if (currentSettings.autoCloseDays > 0) {
              coreBridge.tabStateMachine.pruneInactiveTabs(
                currentSettings.autoCloseDays,
                System.currentTimeMillis(),
              )
            }
          }
          StartupBehavior.BLANK_PAGE -> {
            val primaryId = _uiState.value.primaryTabId
            webViewPool.markInternalPage(primaryId, "about:blank")
            coreBridge.tabStateMachine.updateTabNavigation(
              tabId = primaryId,
              url = "about:blank",
              title = "빈 페이지",
              isLoading = false,
              progress = 100,
            )
          }
          StartupBehavior.SPEED_DIAL -> Unit
        }
        coreBridge.tabStateMachine.setTabSortMode(currentSettings.defaultTabSortMode.id)
        syncFromCoreBridge(persistTabs = false)
      }
    }
  }

  fun onHostPause() {
    webViewPool.onAppBackgrounded()
    persistTabSnapshot()
  }

  fun onHostResume() {
    val state = _uiState.value
    if (state.settings.autoCloseDays > 0) {
      val pruned =
        coreBridge.tabStateMachine.pruneInactiveTabs(
          state.settings.autoCloseDays,
          System.currentTimeMillis(),
        )
      if (pruned > 0) {
        syncFromCoreBridge()
      }
    }
    val visible = buildSet {
      add(state.primaryTabId)
      if (state.splitMode) state.secondaryTabId?.let { add(it) }
    }
    webViewPool.onAppForegrounded(visible, state.settings.backgroundTabSleep)
  }

  fun resolveRenderPolicyForTab(tabUrl: String): WebRenderPolicy {
    val domain = coreBridge.urlEngine.extractRegistrableDomain(tabUrl)
    val effectiveJson = JSONObject(coreBridge.sitePermissions.getEffectivePolicyJson(domain))
    val policy = parsePolicyJson(effectiveJson)
    val settings = _uiState.value.settings
    return WebRenderPolicy(
      fontZoomPercent = settings.webFontZoomPercent,
      forceWebDark = settings.forceWebDark,
      blockThirdPartyCookies = settings.blockThirdPartyCookies,
      sendDntGpcHeaders = settings.sendDntGpcHeaders,
      httpsOnlyMode = settings.httpsOnlyMode,
      confirmExternalApp = settings.confirmExternalApp,
      javascriptEnabled = policy.javascriptEnabled,
      popupsAllowed = policy.popupsAllowed,
      desktopSite = policy.desktopSite,
      blockImages = policy.blockImages,
      autoplayMedia = policy.autoplayMedia,
      cameraPolicy = policy.cameraPolicy,
      microphonePolicy = policy.microphonePolicy,
      locationPolicy = policy.locationPolicy,
    )
  }

  fun onViewportScrollDelta(dy: Int) {
    val state = _uiState.value
    if (!state.toolbarConfig.autoMinimize || state.isOmniboxFocused || state.isFindInPageVisible) {
      return
    }
    val density = appContext.resources.displayMetrics.density.coerceAtLeast(1f)
    val dyDp = dy / density
    accumulatedScrollDyDp =
      if ((accumulatedScrollDyDp > 0f && dyDp < 0f) || (accumulatedScrollDyDp < 0f && dyDp > 0f)) {
        dyDp
      } else {
        accumulatedScrollDyDp + dyDp
      }

    val downThresholdDp =
      when (state.settings.autoHideSensitivity) {
        AutoHideSensitivity.LOW -> 58f
        AutoHideSensitivity.MEDIUM -> 36f
        AutoHideSensitivity.HIGH -> 20f
      }
    val upThresholdDp =
      when (state.settings.autoHideSensitivity) {
        AutoHideSensitivity.LOW -> 24f
        AutoHideSensitivity.MEDIUM -> 16f
        AutoHideSensitivity.HIGH -> 10f
      }

    if (accumulatedScrollDyDp > downThresholdDp && !state.isFloatingBarCollapsed) {
      accumulatedScrollDyDp = 0f
      _uiState.update { it.copy(isFloatingBarCollapsed = true) }
    } else if (accumulatedScrollDyDp < -upThresholdDp && state.isFloatingBarCollapsed) {
      accumulatedScrollDyDp = 0f
      _uiState.update { it.copy(isFloatingBarCollapsed = false) }
    }
  }

  fun collapseFloatingBar() {
    accumulatedScrollDyDp = 0f
    _uiState.update { it.copy(isFloatingBarCollapsed = true, isOmniboxFocused = false) }
  }

  fun expandFloatingBar() {
    accumulatedScrollDyDp = 0f
    _uiState.update { it.copy(isFloatingBarCollapsed = false) }
  }

  fun stepAdjacentTab(delta: Int, targetPane: Int = _uiState.value.activePane) {
    if (!_uiState.value.toolbarConfig.swipeBarSwitchTabs) return
    captureVisibleTabThumbnails()
    resetFindInPageState()
    coreBridge.tabStateMachine.setActivePane(targetPane)
    coreBridge.tabStateMachine.stepAdjacentTab(delta)
    syncFromCoreBridge()
  }

  fun focusOmnibox(targetPane: Int = _uiState.value.activePane) {
    val tab =
      if (targetPane == 1 && _uiState.value.splitMode) {
        _uiState.value.secondaryTab
      } else {
        _uiState.value.primaryTab
      }
    val initialQuery =
      tab?.url?.takeUnless { it == "about:mb" || it == "about:blank" } ?: ""
    coreBridge.tabStateMachine.setActivePane(targetPane)
    syncFromCoreBridge(persistTabs = false)
    _uiState.update {
      it.copy(
        isFloatingBarCollapsed = false,
        isOmniboxFocused = true,
        omniboxTargetPane = targetPane,
        omniboxQuery = initialQuery,
      )
    }
    updateOmniboxQuery(initialQuery)
  }

  fun blurOmnibox() {
    _uiState.update { it.copy(isOmniboxFocused = false, suggestions = emptyList()) }
  }

  fun updateOmniboxQuery(query: String) {
    _uiState.update { it.copy(omniboxQuery = query) }
    val state = _uiState.value
    omniboxSearchJob?.cancel()
    omniboxSearchJob =
      viewModelScope.launch {
        val ranked =
          withContext(Dispatchers.Default) {
            val candidatesJson = buildCandidatesJson(state.bookmarks, state.history)
            val rankedJson =
              coreBridge.historyIndexer.rankSuggestionsJson(
                query = query,
                candidatesJson = candidatesJson,
                nowEpochMs = System.currentTimeMillis(),
              )
            parseSuggestions(rankedJson)
          }
        _uiState.update { it.copy(suggestions = ranked) }
      }
  }

  fun submitOmnibox(rawInput: String, targetPane: Int = _uiState.value.omniboxTargetPane) {
    val settings = _uiState.value.settings
    val resolvedJson =
      JSONObject(
        coreBridge.urlEngine.normalizeAndResolveInput(
          rawInput = rawInput,
          defaultSearchEngineId = settings.searchEngineId,
          customSearchTemplate = settings.customSearchUrl,
        )
      )
    val rawResolvedUrl = resolvedJson.getString("resolvedUrl")
    val resolvedUrl =
      coreBridge.urlEngine.upgradeToHttpsIfNeeded(rawResolvedUrl, settings.httpsOnlyMode)
    val displayHost = resolvedJson.getString("displayHost")

    val targetTabId =
      if (targetPane == 1 && _uiState.value.splitMode) {
        _uiState.value.secondaryTabId ?: _uiState.value.primaryTabId
      } else {
        _uiState.value.primaryTabId
      }

    resetFindInPageState()
    coreBridge.tabStateMachine.setActivePane(targetPane)
    if (resolvedUrl == "about:mb" || resolvedUrl == "about:blank") {
      webViewPool.markInternalPage(targetTabId, resolvedUrl)
      tabThumbnails.remove(targetTabId)
      tabFavicons.remove(targetTabId)
      coreBridge.tabStateMachine.updateTabNavigation(
        tabId = targetTabId,
        url = resolvedUrl,
        title = if (resolvedUrl == "about:blank") "빈 페이지" else "시작 페이지",
        isLoading = false,
        progress = 100,
      )
    } else {
      coreBridge.tabStateMachine.updateTabNavigation(
        tabId = targetTabId,
        url = resolvedUrl,
        title = displayHost,
        isLoading = true,
        progress = 15,
      )
      webViewPool.loadUrl(targetTabId, resolvedUrl, settings.sendDntGpcHeaders)
    }

    _uiState.update {
      it.copy(
        isOmniboxFocused = false,
        isFloatingBarCollapsed = false,
        activeOverlay = BrowserOverlay.NONE,
        suggestions = emptyList(),
      )
    }
    syncFromCoreBridge()
  }

  fun navigateUrlInFocusedPane(url: String) {
    submitOmnibox(url, targetPane = _uiState.value.activePane)
  }

  fun navigateUrlInPane(url: String, pane: Int) {
    submitOmnibox(url, targetPane = pane)
  }

  fun onWebViewNavigationUpdate(
    tabId: String,
    url: String,
    title: String,
    isLoading: Boolean,
    progress: Int,
    caps: WebViewNavCapabilities,
  ) {
    navCapabilitiesMap[tabId] = caps
    coreBridge.tabStateMachine.updateTabNavigation(
      tabId = tabId,
      url = url,
      title = title,
      isLoading = isLoading,
      progress = progress,
    )
    if (!isLoading && url.isNotBlank() && !url.startsWith("about:")) {
      viewModelScope.launch { repository.recordVisit(url, title) }
      if (progress >= 100) {
        captureTabThumbnail(tabId)
      }
    }
    syncFromCoreBridge()
  }

  fun onWebViewIconUpdate(tabId: String, icon: Bitmap?) {
    if (icon != null && !icon.isRecycled) {
      tabFavicons[tabId] = icon.asImageBitmap()
    }
  }

  fun captureTabThumbnail(tabId: String) {
    val tab = _uiState.value.tabs.firstOrNull { it.id == tabId }
    if (tab != null && (tab.url == "about:mb" || tab.url == "about:blank")) {
      tabThumbnails.remove(tabId)
      return
    }
    webViewPool.captureTabThumbnailBitmap(tabId)?.let { bmp ->
      tabThumbnails[tabId] = bmp.asImageBitmap()
    }
  }

  fun captureVisibleTabThumbnails() {
    val state = _uiState.value
    captureTabThumbnail(state.primaryTabId)
    if (state.splitMode) {
      state.secondaryTabId?.let { captureTabThumbnail(it) }
    }
  }

  fun onWebViewRequestBlocked(tabId: String) {
    coreBridge.tabStateMachine.incrementTabBlocked(tabId)
    viewModelScope.launch {
      syncFromCoreBridge(persistTabs = false)
    }
  }

  fun onPermissionChallenge(challenge: WebSitePermissionChallenge) {
    _uiState.update { it.copy(pendingPermissionChallenge = challenge) }
  }

  fun resolvePermissionChallenge(granted: Boolean, rememberForDomain: Boolean) {
    val challenge = _uiState.value.pendingPermissionChallenge ?: return
    when (challenge) {
      is WebSitePermissionChallenge.MediaCapture -> {
        if (granted) {
          challenge.request.grant(challenge.request.resources)
        } else {
          challenge.request.deny()
        }
        if (rememberForDomain) {
          val policyStr = if (granted) "ALLOW" else "BLOCK"
          if (challenge.needsCamera) setDomainSiteOverride(challenge.domain, "cameraPolicy", policyStr)
          if (challenge.needsMic) setDomainSiteOverride(challenge.domain, "microphonePolicy", policyStr)
        }
      }
      is WebSitePermissionChallenge.Geolocation -> {
        challenge.callback.invoke(challenge.origin, granted, rememberForDomain)
        if (rememberForDomain) {
          setDomainSiteOverride(
            challenge.domain,
            "locationPolicy",
            if (granted) "ALLOW" else "BLOCK",
          )
        }
      }
    }
    _uiState.update { it.copy(pendingPermissionChallenge = null) }
  }

  fun onExternalAppRequest(req: ExternalAppLaunchRequest) {
    if (_uiState.value.settings.confirmExternalApp) {
      _uiState.update { it.copy(pendingExternalAppRequest = req) }
    } else {
      launchExternalUrl(req.url)
    }
  }

  fun resolveExternalAppRequest(allowed: Boolean) {
    val req = _uiState.value.pendingExternalAppRequest
    _uiState.update { it.copy(pendingExternalAppRequest = null) }
    if (allowed && req != null) {
      launchExternalUrl(req.url)
    }
  }

  private fun launchExternalUrl(url: String) {
    try {
      val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      appContext.startActivity(intent)
    } catch (_: Throwable) {
      emitSnackbar("해당 외부 앱을 찾을 수 없습니다")
    }
  }

  fun onDownloadRequest(req: PendingDownloadRequest) {
    if (_uiState.value.settings.confirmBeforeDownload) {
      _uiState.update { it.copy(pendingDownloadRequest = req) }
    } else {
      startSystemDownload(req)
    }
  }

  fun resolveDownloadRequest(confirmed: Boolean) {
    val req = _uiState.value.pendingDownloadRequest
    _uiState.update { it.copy(pendingDownloadRequest = null) }
    if (confirmed && req != null) {
      startSystemDownload(req)
    }
  }

  private fun startSystemDownload(req: PendingDownloadRequest) {
    try {
      val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
      val request =
        DownloadManager.Request(Uri.parse(req.url)).apply {
          setTitle(req.filename)
          setDescription("MyBrowser(MB) 다운로드")
          setMimeType(req.mimeType)
          if (req.userAgent.isNotBlank()) addRequestHeader("User-Agent", req.userAgent)
          setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
          setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, req.filename)
        }
      dm.enqueue(request)
      emitSnackbar("다운로드 시작 · ${req.filename}")
    } catch (_: Throwable) {
      emitSnackbar("다운로드를 시작할 수 없습니다")
    }
  }

  fun toggleDesktopModeForPane(paneIndex: Int) {
    val tab = tabForPane(paneIndex) ?: return
    val domain = coreBridge.urlEngine.extractRegistrableDomain(tab.url)
    if (domain.isNotBlank() && !domain.startsWith("about:")) {
      val currentDesktop =
        _uiState.value.sitePermissions.domainOverrides
          .firstOrNull { it.domain == domain }
          ?.policy
          ?.desktopSite
          ?: _uiState.value.sitePermissions.globalPolicy.desktopSite
      val next = !currentDesktop
      setDomainSiteOverride(domain, "desktopSite", next.toString())
      emitSnackbar(if (next) "PC 버전으로 전환" else "모바일 버전으로 전환")
    } else {
      val next = !_uiState.value.sitePermissions.globalPolicy.desktopSite
      setGlobalSitePolicy("desktopSite", next.toString())
      emitSnackbar(if (next) "PC 버전 활성화" else "모바일 버전 활성화")
    }
  }

  fun captureAndSaveScreenshot(paneIndex: Int, fallbackViewBitmap: (() -> Bitmap?)? = null) {
    val tab = tabForPane(paneIndex)
    val capturedBitmap =
      (tab?.let { webViewPool.captureTabBitmap(it.id) }) ?: fallbackViewBitmap?.invoke()
    if (capturedBitmap == null) {
      emitSnackbar("캡처할 화면을 준비 중입니다")
      return
    }
    viewModelScope.launch {
      val savedName =
        withContext(Dispatchers.IO) {
          saveBitmapToGallery(capturedBitmap)
        }
      if (savedName != null) {
        emitSnackbar("스크린샷 저장 완료 · $savedName")
      } else {
        emitSnackbar("스크린샷 저장에 실패했습니다")
      }
    }
  }

  private fun saveBitmapToGallery(bitmap: Bitmap): String? {
    val fileName = "MB_${System.currentTimeMillis()}.png"
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values =
          ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
              MediaStore.Images.Media.RELATIVE_PATH,
              "${Environment.DIRECTORY_PICTURES}/MyBrowser",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
          }
        val uri =
          appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        appContext.contentResolver.openOutputStream(uri)?.use { out ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        appContext.contentResolver.update(uri, values, null, null)
        fileName
      } else {
        val dir =
          File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "MyBrowser",
          ).apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        file.name
      }
    } catch (_: Throwable) {
      null
    }
  }

  fun onQrCodeDecoded(rawPayload: String) {
    val clean = rawPayload.trim()
    dismissOverlay()
    if (clean.isNotEmpty()) {
      navigateUrlInFocusedPane(clean)
    }
  }

  fun setActivePane(paneIndex: Int) {
    coreBridge.tabStateMachine.setActivePane(paneIndex)
    syncFromCoreBridge(persistTabs = false)
  }

  fun toggleSplitMode(forceEnable: Boolean? = null) {
    captureVisibleTabThumbnails()
    val next = forceEnable ?: !_uiState.value.splitMode
    coreBridge.tabStateMachine.setSplitMode(next)
    syncFromCoreBridge()
  }

  fun splitActiveTabWithGesture(directionRight: Boolean, sourcePane: Int = _uiState.value.activePane) {
    captureVisibleTabThumbnails()
    resetFindInPageState()
    coreBridge.tabStateMachine.setActivePane(sourcePane)
    coreBridge.tabStateMachine.splitActiveTabToPane(directionRight)
    _uiState.update {
      it.copy(
        isOmniboxFocused = false,
        isFloatingBarCollapsed = false,
        activeOverlay = BrowserOverlay.NONE,
      )
    }
    syncFromCoreBridge()
  }

  fun closeSplitKeepPane(keepPaneIndex: Int) {
    resetFindInPageState()
    coreBridge.tabStateMachine.closeSplitKeepPane(keepPaneIndex)
    _uiState.update { it.copy(isOmniboxFocused = false, isFloatingBarCollapsed = false) }
    syncFromCoreBridge()
  }

  fun createNewTab(initialUrl: String = "about:mb", targetPane: Int = _uiState.value.activePane) {
    captureVisibleTabThumbnails()
    resetFindInPageState()
    coreBridge.tabStateMachine.createTab(
      initialUrl = initialUrl,
      title = if (initialUrl == "about:mb") "시작 페이지" else initialUrl,
      targetPane = targetPane,
    )
    _uiState.update {
      it.copy(
        activeOverlay = BrowserOverlay.NONE,
        isFloatingBarCollapsed = false,
      )
    }
    syncFromCoreBridge()
  }

  fun selectTab(tabId: String, targetPane: Int = _uiState.value.activePane) {
    captureVisibleTabThumbnails()
    resetFindInPageState()
    coreBridge.tabStateMachine.selectTab(tabId, targetPane)
    _uiState.update {
      it.copy(
        activeOverlay = BrowserOverlay.NONE,
        isFloatingBarCollapsed = false,
      )
    }
    syncFromCoreBridge()
  }

  fun closeTab(tabId: String) {
    val hadMultiple = _uiState.value.tabs.size > 1
    resetFindInPageState(clearWebViewMatches = false)
    webViewPool.destroyTab(tabId)
    navCapabilitiesMap.remove(tabId)
    tabThumbnails.remove(tabId)
    tabFavicons.remove(tabId)
    coreBridge.tabStateMachine.closeTab(tabId)
    if (!hadMultiple) {
      _uiState.update { it.copy(activeOverlay = BrowserOverlay.NONE) }
    } else {
      _uiState.update {
        it.copy(
          snackbarNotification =
            SnackbarNotification(
              id = System.currentTimeMillis(),
              message = "탭을 닫았습니다",
              isUndoClose = true,
            )
        )
      }
    }
    syncFromCoreBridge()
  }

  fun undoLastClosedTab() {
    coreBridge.tabStateMachine.undoLastClosedTab()
    _uiState.update { it.copy(snackbarNotification = null) }
    syncFromCoreBridge()
  }

  fun mergeTabsIntoFolder(sourceTabId: String, targetTabId: String, defaultName: String = "새 폴더") {
    if (!_uiState.value.settings.tabDragFolderEnabled) return
    coreBridge.tabStateMachine.mergeTabsIntoFolder(sourceTabId, targetTabId, defaultName)
    syncFromCoreBridge()
    emitSnackbar("탭 폴더가 생성되었습니다")
  }

  fun addTabToFolder(tabId: String, folderId: String) {
    coreBridge.tabStateMachine.addTabToFolder(tabId, folderId)
    syncFromCoreBridge()
  }

  fun removeTabFromFolder(tabId: String) {
    coreBridge.tabStateMachine.removeTabFromFolder(tabId)
    syncFromCoreBridge()
    emitSnackbar("폴더에서 탭을 꺼냈습니다")
  }

  fun toggleFolderExpanded(folderId: String) {
    coreBridge.tabStateMachine.toggleFolderExpanded(folderId)
    syncFromCoreBridge()
  }

  fun collapseAllFolders() {
    if (_uiState.value.folders.any { it.isExpanded }) {
      coreBridge.tabStateMachine.collapseAllFolders()
      syncFromCoreBridge()
      emitSnackbar("모든 폴더를 접었습니다")
    }
  }

  fun renameFolder(folderId: String, newName: String) {
    coreBridge.tabStateMachine.renameFolder(folderId, newName)
    syncFromCoreBridge()
  }

  fun setFolderColor(folderId: String, colorIndex: Int) {
    coreBridge.tabStateMachine.setFolderColor(folderId, colorIndex)
    syncFromCoreBridge()
  }

  fun togglePinTab(tabId: String) {
    val wasPinned = _uiState.value.tabs.firstOrNull { it.id == tabId }?.isPinned == true
    coreBridge.tabStateMachine.togglePinTab(tabId)
    syncFromCoreBridge()
    emitSnackbar(if (wasPinned) "탭 고정을 해제했습니다" else "탭을 상단에 고정했습니다")
  }

  fun setTabSortMode(mode: TabSortMode) {
    coreBridge.tabStateMachine.setTabSortMode(mode.id)
    syncFromCoreBridge()
  }

  fun setDefaultTabSortMode(mode: TabSortMode) {
    updateSetting("defaultTabSortMode", mode.id)
  }

  fun dismissSnackbar() {
    _uiState.update { it.copy(snackbarNotification = null) }
  }

  private fun emitSnackbar(message: String, isUndoClose: Boolean = false) {
    _uiState.update {
      it.copy(
        snackbarNotification =
          SnackbarNotification(
            id = System.currentTimeMillis(),
            message = message,
            isUndoClose = isUndoClose,
          )
      )
    }
  }

  fun goBackInFocusedPane(): Boolean = goBackInPane(_uiState.value.activePane)

  fun goBackInPane(paneIndex: Int): Boolean {
    val tab = tabForPane(paneIndex) ?: return false
    if (tab.url == "about:mb" || tab.url == "about:blank") return false
    coreBridge.tabStateMachine.setActivePane(paneIndex)
    val wentBack = webViewPool.goBack(tab.id)
    if (!wentBack) {
      resetFindInPageState()
      webViewPool.markInternalPage(tab.id, "about:mb")
      coreBridge.tabStateMachine.updateTabNavigation(
        tabId = tab.id,
        url = "about:mb",
        title = "시작 페이지",
        isLoading = false,
        progress = 100,
      )
      syncFromCoreBridge()
    }
    return true
  }

  fun goForwardInFocusedPane() = goForwardInPane(_uiState.value.activePane)

  fun goForwardInPane(paneIndex: Int) {
    val tab = tabForPane(paneIndex) ?: return
    coreBridge.tabStateMachine.setActivePane(paneIndex)
    webViewPool.goForward(tab.id)
  }

  fun reloadOrStopFocusedPane() = reloadOrStopInPane(_uiState.value.activePane)

  fun reloadOrStopInPane(paneIndex: Int) {
    val tab = tabForPane(paneIndex) ?: return
    if (tab.url == "about:mb" || tab.url == "about:blank") return
    coreBridge.tabStateMachine.setActivePane(paneIndex)
    if (tab.isLoading) {
      webViewPool.stopLoading(tab.id)
    } else {
      webViewPool.reload(tab.id)
    }
  }

  fun goHomeInFocusedPane() = goHomeInPane(_uiState.value.activePane)

  fun goHomeInPane(paneIndex: Int) {
    val tab = tabForPane(paneIndex) ?: return
    resetFindInPageState()
    webViewPool.markInternalPage(tab.id, "about:mb")
    tabThumbnails.remove(tab.id)
    tabFavicons.remove(tab.id)
    coreBridge.tabStateMachine.setActivePane(paneIndex)
    coreBridge.tabStateMachine.updateTabNavigation(
      tabId = tab.id,
      url = "about:mb",
      title = "시작 페이지",
      isLoading = false,
      progress = 100,
    )
    syncFromCoreBridge()
  }

  fun toggleBookmarkForPane(paneIndex: Int) {
    val tab = tabForPane(paneIndex) ?: return
    if (tab.url.isBlank() || tab.url == "about:mb" || tab.url == "about:blank") return
    val isBookmarked = _uiState.value.bookmarks.any { it.url == tab.url }
    viewModelScope.launch { repository.toggleBookmark(tab.url, tab.title, isBookmarked) }
    emitSnackbar(if (isBookmarked) "북마크에서 삭제됨" else "북마크에 추가됨")
  }

  private fun tabForPane(paneIndex: Int): TabUiModel? {
    val state = _uiState.value
    return if (state.splitMode && paneIndex == 1) {
      state.secondaryTab ?: state.primaryTab
    } else {
      state.primaryTab
    }
  }

  fun openFindInPage() {
    val tab = _uiState.value.focusedTab ?: return
    if (tab.url == "about:mb" || tab.url == "about:blank") return
    _uiState.update {
      it.copy(
        isFloatingBarCollapsed = false,
        isOmniboxFocused = false,
        isFindInPageVisible = true,
        findQuery = "",
        findActiveMatch = 0,
        findTotalMatches = 0,
      )
    }
  }

  fun updateFindQuery(query: String) {
    val tab = _uiState.value.focusedTab ?: return
    _uiState.update { it.copy(findQuery = query) }
    webViewPool.findInPage(tab.id, query) { active, total ->
      _uiState.update { it.copy(findActiveMatch = active, findTotalMatches = total) }
    }
  }

  fun findNextMatch(forward: Boolean) {
    val tab = _uiState.value.focusedTab ?: return
    webViewPool.findNext(tab.id, forward)
  }

  fun closeFindInPage() {
    resetFindInPageState(clearWebViewMatches = true)
  }

  private fun resetFindInPageState(clearWebViewMatches: Boolean = true) {
    val state = _uiState.value
    if (!state.isFindInPageVisible && state.findQuery.isEmpty()) return
    if (clearWebViewMatches) {
      state.focusedTab?.let { webViewPool.clearFindInPage(it.id) }
    }
    _uiState.update {
      it.copy(
        isFindInPageVisible = false,
        findQuery = "",
        findActiveMatch = 0,
        findTotalMatches = 0,
      )
    }
  }

  fun toggleReaderMode() {
    val tab = _uiState.value.focusedTab ?: return
    if (tab.url == "about:mb" || tab.url == "about:blank") {
      _uiState.update {
        it.copy(
          readerArticle =
            ReaderArticleUi(
              title = "읽기 모드",
              domain = tab.url,
              blocks = listOf("웹 페이지를 연 후 읽기 모드를 선택하면 본문을 추출해 표시합니다."),
            ),
          activeOverlay = BrowserOverlay.READER_VIEW,
        )
      }
      return
    }
    webViewPool.extractReaderContent(tab.id, tab.title) { title, blocks ->
      _uiState.update {
        it.copy(
          readerArticle =
            ReaderArticleUi(
              title = title,
              domain = tab.displayHost,
              blocks = blocks.ifEmpty { listOf("추출할 본문 문단이 없습니다.") },
            ),
          activeOverlay = BrowserOverlay.READER_VIEW,
        )
      }
    }
  }

  fun toggleBookmarkForFocusedTab() {
    val tab = _uiState.value.focusedTab ?: return
    val isBookmarked = _uiState.value.bookmarks.any { it.url == tab.url }
    viewModelScope.launch { repository.toggleBookmark(tab.url, tab.title, isBookmarked) }
  }

  fun deleteBookmark(url: String) {
    viewModelScope.launch { repository.deleteBookmark(url) }
  }

  fun deleteHistoryItem(url: String) {
    viewModelScope.launch { repository.deleteHistoryItem(url) }
  }

  fun clearHistory() {
    viewModelScope.launch { repository.clearHistory() }
  }

  // --- Customization & Toolbar (Pillar 2) ---
  fun updateToolbarSlots(leftSlots: List<ToolbarActionId>, rightSlots: List<ToolbarActionId>) {
    val updatedJson =
      coreBridge.toolbarConfig.updateSlots(
        leftSlotsCsv = leftSlots.joinToString(",") { it.id },
        rightSlotsCsv = rightSlots.joinToString(",") { it.id },
      )
    syncToolbarConfigFromCore()
    viewModelScope.launch { repository.setToolbarConfigJson(updatedJson) }
  }

  fun moveToolbarSlot(side: String, fromIndex: Int, toIndex: Int) {
    val updatedJson = coreBridge.toolbarConfig.moveSlot(side, fromIndex, toIndex)
    syncToolbarConfigFromCore()
    viewModelScope.launch { repository.setToolbarConfigJson(updatedJson) }
  }

  fun setToolbarAlignment(alignment: ToolbarAlignment) {
    val updatedJson = coreBridge.toolbarConfig.setAlignment(alignment.id)
    val designJson = coreBridge.designConfig.updateDesignParam("barHorizontalBias", alignment.bias.toString())
    syncToolbarConfigFromCore()
    syncDesignConfigFromCore()
    viewModelScope.launch {
      repository.setToolbarConfigJson(updatedJson)
      repository.setDesignConfigJson(designJson)
    }
  }

  fun setToolbarAutoMinimize(enabled: Boolean) {
    val updatedJson = coreBridge.toolbarConfig.setAutoMinimize(enabled)
    syncToolbarConfigFromCore()
    if (!enabled) _uiState.update { it.copy(isFloatingBarCollapsed = false) }
    viewModelScope.launch { repository.setToolbarConfigJson(updatedJson) }
  }

  fun setToolbarGestures(swipeTabs: Boolean, pullRefresh: Boolean) {
    val updatedJson = coreBridge.toolbarConfig.setGestures(swipeTabs, pullRefresh)
    syncToolbarConfigFromCore()
    viewModelScope.launch { repository.setToolbarConfigJson(updatedJson) }
  }

  fun resetToolbarDefaults() {
    val updatedJson = coreBridge.toolbarConfig.resetDefaults()
    syncToolbarConfigFromCore()
    viewModelScope.launch { repository.setToolbarConfigJson(updatedJson) }
  }

  // --- Design Config Engine (Pillar 1 Live Geometry Customization) ---
  fun updateDesignParam(key: String, value: String) {
    val updatedJson = coreBridge.designConfig.updateDesignParam(key, value)
    syncDesignConfigFromCore()
    viewModelScope.launch { repository.setDesignConfigJson(updatedJson) }
  }

  fun resetDesignDefaults() {
    val updatedJson = coreBridge.designConfig.resetDesignDefaults()
    syncDesignConfigFromCore()
    viewModelScope.launch { repository.setDesignConfigJson(updatedJson) }
  }

  // --- 4-Pillar Settings Store (Design, Search, General & Privacy) ---
  fun updateSetting(key: String, value: String) {
    val updatedJson = coreBridge.settingsStore.updateSetting(key, value)
    if (key == "trackerBlockingEnabled") {
      coreBridge.trackerBlocker.setEnabled(value.toBooleanStrictOrNull() ?: true)
      refreshBlockerStats()
    } else if (key == "blockerLevel") {
      coreBridge.trackerBlocker.setLevel(value)
      refreshBlockerStats()
    } else if (key == "autoCloseDays") {
      val days = value.toIntOrNull() ?: 0
      if (days > 0) {
        coreBridge.tabStateMachine.pruneInactiveTabs(days, System.currentTimeMillis())
        syncFromCoreBridge()
      }
    } else if (key == "defaultTabSortMode") {
      val mode = TabSortMode.fromId(value)
      coreBridge.tabStateMachine.setTabSortMode(mode.id)
      syncFromCoreBridge()
    }
    syncSettingsFromCore()
    if (key == "backgroundTabSleep") {
      val state = _uiState.value
      val visible = buildSet {
        add(state.primaryTabId)
        if (state.splitMode) state.secondaryTabId?.let { add(it) }
      }
      webViewPool.syncVisibleTabs(visible, state.settings.backgroundTabSleep)
    }
    viewModelScope.launch { repository.setSettingsStoreJson(updatedJson) }
  }

  fun updateBuiltInFilterList() {
    viewModelScope.launch {
      val count =
        withContext(Dispatchers.Default) {
          val extendedRules =
            """
            ||metrics.icloud.com^
            ||ads-twitter.com^
            ||analytics.tiktok.com^
            ||telemetry.mozilla.org^
            ||ad.doubleclick.net^
            ||pixel.wp.com^
            ||sb.scorecardresearch.com^
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
          coreBridge.trackerBlocker.loadFilterListBytes(extendedRules)
        }
      refreshBlockerStats()
      _uiState.update {
        it.copy(statusBannerMessage = "필터 업데이트 완료 · ${count}개 규칙")
      }
    }
  }

  fun clearBrowsingData(clearCache: Boolean, clearHistory: Boolean, clearCookies: Boolean) {
    webViewPool.clearBrowsingData(clearCache = clearCache, clearCookies = clearCookies)
    if (clearHistory) {
      viewModelScope.launch { repository.clearHistory() }
    }
    _uiState.update { it.copy(statusBannerMessage = "선택한 데이터 삭제 완료") }
  }

  fun clearStatusBanner() {
    _uiState.update { it.copy(statusBannerMessage = null) }
  }

  // --- Pillar 4: Site Settings & Per-Domain Overrides ---
  fun setGlobalSitePolicy(key: String, value: String) {
    coreBridge.sitePermissions.setGlobalPolicy(key, value)
    val exported = coreBridge.sitePermissions.exportStateJson()
    syncSitePermissionsFromCore()
    viewModelScope.launch { repository.setSitePermissionsJson(exported) }
  }

  fun setDomainSiteOverride(domain: String, key: String, value: String) {
    val normalized =
      if (domain.contains("://") || domain.contains('/')) {
        coreBridge.urlEngine.extractRegistrableDomain(domain)
      } else {
        domain.trim().lowercase().removePrefix("www.")
      }
    if (normalized.isBlank() || !normalized.contains('.')) return
    val exported = coreBridge.sitePermissions.setDomainOverride(normalized, key, value)
    syncSitePermissionsFromCore()
    viewModelScope.launch { repository.setSitePermissionsJson(exported) }
  }

  fun resetDomainSiteOverrides(domain: String) {
    val exported = coreBridge.sitePermissions.resetDomainOverrides(domain)
    syncSitePermissionsFromCore()
    viewModelScope.launch { repository.setSitePermissionsJson(exported) }
  }

  fun runDohProbe(hostname: String) {
    val clean = hostname.trim().ifEmpty { "duckduckgo.com" }
    val settings = _uiState.value.settings
    _uiState.update { it.copy(isProbingDoh = true) }
    viewModelScope.launch {
      val result =
        dohClient.resolveHostname(
          hostname = clean,
          providerId = settings.dohProviderId,
          customDohUrl = settings.customDohUrl,
        )
      _uiState.update { it.copy(isProbingDoh = false, lastDohLookup = result) }
    }
  }

  fun showOverlay(overlay: BrowserOverlay, libraryInitialTab: Int = 0) {
    if (overlay == BrowserOverlay.TAB_SWITCHER) {
      captureVisibleTabThumbnails()
    }
    if (overlay != BrowserOverlay.NONE) {
      resetFindInPageState()
    }
    refreshBlockerStats()
    _uiState.update {
      it.copy(
        activeOverlay = overlay,
        libraryInitialTab = libraryInitialTab,
        isOmniboxFocused = false,
      )
    }
  }

  fun dismissOverlay() {
    _uiState.update { it.copy(activeOverlay = BrowserOverlay.NONE, statusBannerMessage = null) }
  }

  private fun syncToolbarConfigFromCore() {
    val obj = JSONObject(coreBridge.toolbarConfig.getConfigJson())
    val leftArr = obj.optJSONArray("leftSlots") ?: JSONArray()
    val rightArr = obj.optJSONArray("rightSlots") ?: JSONArray()
    val left = buildList {
      for (i in 0 until leftArr.length()) {
        ToolbarActionId.fromId(leftArr.getString(i))?.let { add(it) }
      }
    }
    val right = buildList {
      for (i in 0 until rightArr.length()) {
        ToolbarActionId.fromId(rightArr.getString(i))?.let { add(it) }
      }
    }
    _uiState.update {
      it.copy(
        toolbarConfig =
          ToolbarConfigUi(
            leftSlots = left,
            rightSlots = right,
            alignment = ToolbarAlignment.fromId(obj.optString("alignment", "CENTER")),
            autoMinimize = obj.optBoolean("autoMinimize", true),
            swipeBarSwitchTabs = obj.optBoolean("swipeBarSwitchTabs", true),
            pullDownRefresh = obj.optBoolean("pullDownRefresh", true),
          )
      )
    }
  }

  private fun syncDesignConfigFromCore() {
    val obj = JSONObject(coreBridge.designConfig.getDesignConfigJson())
    val fallbackBlur =
      if (obj.has("blurIntensityPercent")) {
        (obj.optDouble("blurIntensityPercent", 60.0) * 0.32).toFloat()
      } else {
        18f
      }
    _uiState.update {
      it.copy(
        designConfig =
          DesignUiConfig(
            iconCornerRadiusDp = obj.optDouble("iconCornerRadiusDp", 22.0).toFloat(),
            barCornerRadiusDp = obj.optDouble("barCornerRadiusDp", 28.0).toFloat(),
            cardCornerRadiusDp = obj.optDouble("cardCornerRadiusDp", 20.0).toFloat(),
            barHeightDp = obj.optDouble("barHeightDp", 56.0).toFloat(),
            barWidthFraction = obj.optDouble("barWidthFraction", 0.94).toFloat(),
            barHorizontalBias = obj.optDouble("barHorizontalBias", 0.0).toFloat(),
            barBottomOffsetDp = obj.optDouble("barBottomOffsetDp", 12.0).toFloat(),
            barOpacity = obj.optDouble("barOpacity", 0.90).toFloat(),
            barBlurRadiusDp = obj.optDouble("barBlurRadiusDp", fallbackBlur.toDouble()).toFloat().coerceIn(0f, 32f),
          )
      )
    }
  }

  private fun syncSettingsFromCore() {
    val obj = JSONObject(coreBridge.settingsStore.getSettingsJson())
    _uiState.update {
      it.copy(
        settings =
          BrowserSettingsUi(
            themeMode = ThemeMode.fromId(obj.optString("themeMode", "SYSTEM")),
            accentPreset = AccentColorPreset.fromId(obj.optString("accentPreset", "DYNAMIC")),
            dynamicColor = obj.optBoolean("dynamicColor", true),
            floatingBarStyle =
              FloatingBarStyle.fromId(obj.optString("floatingBarStyle", "TRANSLUCENT_GLASS")),
            autoHideSensitivity =
              AutoHideSensitivity.fromId(obj.optString("autoHideSensitivity", "MEDIUM")),
            webFontZoomPercent = obj.optInt("webFontZoomPercent", 100),
            forceWebDark = obj.optBoolean("forceWebDark", false),
            hideStatusBar = obj.optBoolean("hideStatusBar", false),
            autoDualPaneUnfold = obj.optBoolean("autoDualPaneUnfold", false),
            splitGestureEnabled = obj.optBoolean("splitGestureEnabled", true),
            splitGestureSensitivity =
              SplitGestureSensitivity.fromId(
                obj.optString("splitGestureSensitivity", "MEDIUM")
              ),
            swipeUpOpenTabs = obj.optBoolean("swipeUpOpenTabs", true),
            tabSwipeToCloseEnabled = obj.optBoolean("tabSwipeToCloseEnabled", true),
            tabDragFolderEnabled = obj.optBoolean("tabDragFolderEnabled", true),
            tabViewMode = TabViewMode.fromId(obj.optString("tabViewMode", "THUMBNAIL_GRID")),
            defaultTabSortMode = TabSortMode.fromId(obj.optString("defaultTabSortMode", "RECENT")),
            searchEngineId = obj.optString("searchEngineId", "ddg"),
            customSearchUrl =
              obj.optString("customSearchUrl", "https://www.mojeek.com/search?q=%s"),
            trackerBlockingEnabled = obj.optBoolean("trackerBlockingEnabled", true),
            blockerLevel = BlockerLevel.fromId(obj.optString("blockerLevel", "STANDARD")),
            blockThirdPartyCookies = obj.optBoolean("blockThirdPartyCookies", true),
            sendDntGpcHeaders = obj.optBoolean("sendDntGpcHeaders", true),
            httpsOnlyMode = obj.optBoolean("httpsOnlyMode", true),
            dohProviderId = obj.optString("dohProviderId", "quad9"),
            customDohUrl = obj.optString("customDohUrl", "https://dns.quad9.net/dns-query"),
            confirmExternalApp = obj.optBoolean("confirmExternalApp", true),
            startupBehavior =
              StartupBehavior.fromId(obj.optString("startupBehavior", "SPEED_DIAL")),
            backgroundTabSleep = obj.optBoolean("backgroundTabSleep", true),
            autoCloseDays = obj.optInt("autoCloseDays", 0),
            downloadTreeUri = obj.optString("downloadTreeUri", ""),
            confirmBeforeDownload = obj.optBoolean("confirmBeforeDownload", true),
          )
      )
    }
  }

  private fun syncSitePermissionsFromCore() {
    val exported = JSONObject(coreBridge.sitePermissions.exportStateJson())
    val globalObj = exported.optJSONObject("global") ?: JSONObject()
    val overridesArr = exported.optJSONArray("overrides") ?: JSONArray()
    val overrides = buildList {
      for (i in 0 until overridesArr.length()) {
        val item = overridesArr.getJSONObject(i)
        add(
          DomainSiteOverrideUi(
            domain = item.getString("domain"),
            policy = parsePolicyJson(item.getJSONObject("policy")),
          )
        )
      }
    }
    _uiState.update {
      it.copy(
        sitePermissions =
          SitePermissionsUiState(
            globalPolicy = parsePolicyJson(globalObj),
            domainOverrides = overrides,
          )
      )
    }
  }

  private fun parsePolicyJson(obj: JSONObject): SitePolicyUi =
    SitePolicyUi(
      javascriptEnabled = obj.optBoolean("javascriptEnabled", true),
      popupsAllowed = obj.optBoolean("popupsAllowed", false),
      desktopSite = obj.optBoolean("desktopSite", false),
      blockImages = obj.optBoolean("blockImages", false),
      cameraPolicy = obj.optString("cameraPolicy", "ASK"),
      microphonePolicy = obj.optString("microphonePolicy", "ASK"),
      locationPolicy = obj.optString("locationPolicy", "ASK"),
      clipboardPolicy = obj.optString("clipboardPolicy", "ASK"),
      notificationsPolicy = obj.optString("notificationsPolicy", "BLOCK"),
      sensorsPolicy = obj.optString("sensorsPolicy", "ASK"),
      autoplayMedia = obj.optString("autoplayMedia", "BLOCK"),
    )

  private fun syncFromCoreBridge(persistTabs: Boolean = true) {
    val stateJson = JSONObject(coreBridge.tabStateMachine.getStateJson())
    val primaryId = stateJson.getString("primaryTabId")
    val secondaryId =
      if (stateJson.isNull("secondaryTabId")) null else stateJson.getString("secondaryTabId")
    val splitMode = stateJson.getBoolean("splitMode")
    val activePane = stateJson.getInt("activePane")
    val sortMode = TabSortMode.fromId(stateJson.optString("sortMode", "RECENT"))
    val hasUndo = stateJson.optBoolean("hasUndo", false)

    val foldersJson = stateJson.optJSONArray("folders") ?: JSONArray()
    val folders = buildList {
      for (i in 0 until foldersJson.length()) {
        val f = foldersJson.getJSONObject(i)
        add(
          TabFolderUiModel(
            id = f.getString("id"),
            name = f.getString("name"),
            colorIndex = f.optInt("colorIndex", 0),
            isExpanded = f.optBoolean("isExpanded", true),
          )
        )
      }
    }

    val tabsJson = stateJson.getJSONArray("tabs")
    val tabs = buildList {
      for (i in 0 until tabsJson.length()) {
        val obj = tabsJson.getJSONObject(i)
        val id = obj.getString("id")
        val url = obj.getString("url")
        val caps = navCapabilitiesMap[id] ?: webViewPool.getCapabilities(id)
        val folderRaw = if (obj.isNull("folderId")) null else obj.optString("folderId", "")
        add(
          TabUiModel(
            id = id,
            url = url,
            title = localizeInternalTitle(url, obj.getString("title")),
            displayHost = coreBridge.urlEngine.extractRegistrableDomain(url).ifEmpty { url },
            isSecure = coreBridge.urlEngine.isSecureOrigin(url),
            isLoading = obj.getBoolean("isLoading"),
            progress = obj.getInt("progress"),
            blockedCount = obj.getInt("blockedCount"),
            createdAtMs = obj.optLong("createdAtMs", 0L),
            lastActiveAtMs = obj.optLong("lastActiveAtMs", 0L),
            folderId = folderRaw?.takeIf { it.isNotBlank() && it != "null" },
            isPinned = obj.optBoolean("isPinned", false),
            canGoBack = caps.canGoBack || (url != "about:mb" && url != "about:blank"),
            canGoForward = caps.canGoForward,
          )
        )
      }
    }

    val visibleIds = buildSet {
      add(primaryId)
      if (splitMode && secondaryId != null) add(secondaryId)
    }
    webViewPool.syncVisibleTabs(visibleIds, _uiState.value.settings.backgroundTabSleep)

    _uiState.update {
      it.copy(
        tabs = tabs,
        folders = folders,
        tabSortMode = sortMode,
        hasUndoableClosedTab = hasUndo,
        primaryTabId = primaryId,
        secondaryTabId = secondaryId,
        splitMode = splitMode,
        activePane = activePane,
      )
    }
    refreshBlockerStats()

    if (persistTabs) {
      persistTabSnapshot()
    }
  }

  private fun persistTabSnapshot() {
    val bytes = coreBridge.tabStateMachine.serializeSnapshotBytes()
    val snap = String(bytes, StandardCharsets.UTF_8)
    viewModelScope.launch { repository.setTabSessionSnapshotJson(snap) }
  }

  private fun localizeInternalTitle(url: String, rawTitle: String): String =
    when {
      url == "about:mb" -> "시작 페이지"
      url == "about:blank" -> "빈 페이지"
      rawTitle == "Start Page" -> "시작 페이지"
      rawTitle == "Split Start Page" -> "분할 시작 페이지"
      rawTitle == "Blank Page" -> "빈 페이지"
      else -> rawTitle
    }

  private fun refreshBlockerStats() {
    val stats = JSONObject(coreBridge.trackerBlocker.getSessionStatsJson())
    _uiState.update {
      it.copy(
        blockerStats =
          BlockerStatsUi(
            enabled = stats.optBoolean("enabled", true),
            level = stats.optString("level", "STANDARD"),
            ruleCount = stats.optInt("ruleCount", 0),
            evaluated = stats.optLong("evaluated", 0L),
            totalBlocked = stats.optLong("totalBlocked", 0L),
            adsBlocked = stats.optLong("ads", 0L),
            trackersBlocked = stats.optLong("trackers", 0L),
            fingerprintingBlocked = stats.optLong("fingerprinting", 0L),
            telemetryBlocked = stats.optLong("telemetry", 0L),
          )
      )
    }
  }

  private fun parseSearchEngines(json: String): List<SearchEngineOption> {
    val arr = JSONArray(json)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val id = o.getString("id")
        val rawLabel = o.getString("label")
        add(
          SearchEngineOption(
            id = id,
            label = if (id == "custom") "사용자 지정" else rawLabel,
            homepage = o.getString("homepage"),
          )
        )
      }
    }
  }

  private fun EngineAbiInfo.toMetaUi(): EngineMetaUi =
    EngineMetaUi(
      abiVersion = abiVersion,
      backendKind = backendKind,
      featuresCsv = featuresCsv,
    )

  private fun parseSuggestions(json: String): List<OmniboxSuggestionUi> {
    val arr = JSONArray(json)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
          OmniboxSuggestionUi(
            url = o.getString("url"),
            title = o.getString("title"),
            isBookmark = o.getBoolean("isBookmark"),
          )
        )
      }
    }
  }

  private fun buildCandidatesJson(
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
  ): String {
    val merged = linkedMapOf<String, JSONObject>()
    bookmarks.forEach { b ->
      merged[b.url] =
        JSONObject()
          .put("url", b.url)
          .put("title", b.title)
          .put("visitCount", 4)
          .put("lastVisited", b.createdAtMs)
          .put("isBookmark", true)
    }
    history.forEach { h ->
      val existing = merged[h.url]
      if (existing != null) {
        existing.put("visitCount", h.visitCount).put("lastVisited", h.lastVisitedMs)
      } else {
        merged[h.url] =
          JSONObject()
            .put("url", h.url)
            .put("title", h.title)
            .put("visitCount", h.visitCount)
            .put("lastVisited", h.lastVisitedMs)
            .put("isBookmark", false)
      }
    }
    return JSONArray(merged.values.toList()).toString()
  }

  override fun onCleared() {
    super.onCleared()
    webViewPool.destroyAll()
  }
}
