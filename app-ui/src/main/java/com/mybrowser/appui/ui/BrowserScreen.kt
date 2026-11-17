package com.mybrowser.appui.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mybrowser.appui.foldable.BrowserPostureMode
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.foldable.rememberFoldablePostureState
import com.mybrowser.appui.viewmodel.BrowserOverlay
import com.mybrowser.appui.viewmodel.BrowserUiState
import com.mybrowser.appui.viewmodel.BrowserViewModel
import com.mybrowser.appui.viewmodel.TabUiModel
import com.mybrowser.appui.viewmodel.ToolbarActionId
import com.mybrowser.appui.webview.AospWebViewViewport
import com.mybrowser.appui.webview.ExternalAppLaunchRequest
import com.mybrowser.appui.webview.PendingDownloadRequest
import com.mybrowser.appui.webview.WebSitePermissionChallenge
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun MyBrowserRootScreen(
  viewModel: BrowserViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val rootView = LocalView.current
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val rememberedTabCardBounds = remember { mutableStateMapOf<String, Rect>() }

  val tabDrawerProgress = remember {
    Animatable(if (uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) 1f else 0f)
  }
  val isDraggingTabDrawer = remember { mutableStateOf(false) }
  var heroFlightTabId by remember { mutableStateOf<String?>(uiState.focusedTab?.id) }

  LaunchedEffect(uiState.tabs) {
    val activeIds = uiState.tabs.map { it.id }.toSet()
    val staleIds = rememberedTabCardBounds.keys.filter { it !in activeIds }
    staleIds.forEach { rememberedTabCardBounds.remove(it) }
  }

  LaunchedEffect(uiState.activeOverlay) {
    if (!isDraggingTabDrawer.value) {
      if (uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) {
        if (heroFlightTabId == null) {
          heroFlightTabId = uiState.focusedTab?.id
        }
        tabDrawerProgress.animateTo(1f, BrowserMotion.tabDrawerSpring())
      } else {
        tabDrawerProgress.animateTo(0f, BrowserMotion.tabDrawerSpring())
        heroFlightTabId = uiState.focusedTab?.id
      }
    }
  }

  LaunchedEffect(uiState.snackbarNotification?.id) {
    val notif = uiState.snackbarNotification ?: return@LaunchedEffect
    val result =
      snackbarHostState.showSnackbar(
        message = notif.message,
        actionLabel = if (notif.isUndoClose) "실행 취소" else null,
        duration = SnackbarDuration.Short,
      )
    if (result == SnackbarResult.ActionPerformed && notif.isUndoClose) {
      viewModel.undoLastClosedTab()
    } else {
      viewModel.dismissSnackbar()
    }
  }

  val isInternalPage by remember(uiState.focusedTab) {
    derivedStateOf {
      val tab = uiState.focusedTab
      tab == null || tab.url == "about:mb" || tab.url == "about:blank"
    }
  }

  BackHandler(
    enabled =
      uiState.activeOverlay != BrowserOverlay.NONE ||
        uiState.isOmniboxFocused ||
        uiState.isFindInPageVisible ||
        !isInternalPage
  ) {
    when {
      uiState.activeOverlay != BrowserOverlay.NONE -> viewModel.dismissOverlay()
      uiState.isOmniboxFocused -> viewModel.blurOmnibox()
      uiState.isFindInPageVisible -> viewModel.closeFindInPage()
      else -> viewModel.goBackInFocusedPane()
    }
  }

  val executeToolbarActionForPane: (Int, ToolbarActionId) -> Unit =
    remember(
      viewModel,
      uiState.primaryTab,
      uiState.secondaryTab,
      uiState.splitMode,
      context,
      rootView,
    ) {
      { paneIndex, action ->
        val paneTab =
          if (uiState.splitMode && paneIndex == 1) uiState.secondaryTab ?: uiState.primaryTab
          else uiState.primaryTab

        when (action) {
          ToolbarActionId.BACK -> viewModel.goBackInPane(paneIndex)
          ToolbarActionId.FORWARD -> viewModel.goForwardInPane(paneIndex)
          ToolbarActionId.REFRESH -> viewModel.reloadOrStopInPane(paneIndex)
          ToolbarActionId.BOOKMARK -> viewModel.toggleBookmarkForPane(paneIndex)
          ToolbarActionId.HISTORY -> {
            viewModel.setActivePane(paneIndex)
            viewModel.showOverlay(BrowserOverlay.LIBRARY_PANEL, libraryInitialTab = 1)
          }
          ToolbarActionId.DESKTOP_MODE -> viewModel.toggleDesktopModeForPane(paneIndex)
          ToolbarActionId.SHARE -> {
            if (paneTab != null && paneTab.url != "about:mb" && paneTab.url != "about:blank") {
              val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, paneTab.title)
                  putExtra(Intent.EXTRA_TEXT, paneTab.url)
                }
              try {
                context.startActivity(Intent.createChooser(shareIntent, "링크 공유"))
              } catch (_: Throwable) {
              }
            }
          }
          ToolbarActionId.QR_SCAN -> {
            viewModel.setActivePane(paneIndex)
            viewModel.showOverlay(BrowserOverlay.QR_SCANNER)
          }
          ToolbarActionId.SCREENSHOT -> {
            viewModel.captureAndSaveScreenshot(paneIndex) {
              captureComposeViewBitmap(rootView)
            }
          }
          ToolbarActionId.READER_MODE -> {
            viewModel.setActivePane(paneIndex)
            viewModel.toggleReaderMode()
          }
          ToolbarActionId.HOME -> viewModel.goHomeInPane(paneIndex)
          ToolbarActionId.TABS -> {
            viewModel.setActivePane(paneIndex)
            viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER)
          }
          ToolbarActionId.NEW_TAB -> viewModel.createNewTab(targetPane = paneIndex)
          ToolbarActionId.SPLIT_VIEW -> viewModel.toggleSplitMode()
          ToolbarActionId.FIND_IN_PAGE -> {
            viewModel.setActivePane(paneIndex)
            viewModel.openFindInPage()
          }
          ToolbarActionId.LIBRARY -> {
            viewModel.setActivePane(paneIndex)
            viewModel.showOverlay(BrowserOverlay.LIBRARY_PANEL, libraryInitialTab = 0)
          }
        }
      }
    }

  CompositionLocalProvider(LocalDesignConfig provides uiState.designConfig) {
    BoxWithConstraints(
      modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
      val postureState = rememberFoldablePostureState(maxWidth, maxHeight)
      var prevSupportsDual by remember { mutableStateOf(postureState.supportsDualPane) }

      LaunchedEffect(postureState.supportsDualPane, uiState.settings.autoDualPaneUnfold) {
        val transitionedToWide = postureState.supportsDualPane && !prevSupportsDual
        if (transitionedToWide && uiState.settings.autoDualPaneUnfold && !uiState.splitMode) {
          viewModel.toggleSplitMode(forceEnable = true)
        }
        prevSupportsDual = postureState.supportsDualPane
      }

      LaunchedEffect(postureState.supportsDualPane, uiState.activePane) {
        if (!postureState.supportsDualPane && uiState.activePane != 0) {
          viewModel.setActivePane(0)
        }
      }

      val drawerP = tabDrawerProgress.value.coerceIn(0f, 1f)
      val heroMaxPx = with(density) { 180.dp.toPx().coerceAtLeast(140f) }

      when (postureState.mode) {
        BrowserPostureMode.TABLETOP_HALF_OPENED -> {
          TabletopPostureLayout(
            uiState = uiState,
            postureState = postureState,
            viewModel = viewModel,
            onExecuteActionForPane = executeToolbarActionForPane,
          )
        }
        BrowserPostureMode.BOOK_HALF_OPENED,
        BrowserPostureMode.PASSPORT_FLAT,
        BrowserPostureMode.COVER_COMPACT -> {
          FloatingBarWebLayout(
            uiState = uiState,
            postureState = postureState,
            forceDualPane = false,
            viewModel = viewModel,
            tabDrawerProgress = drawerP,
            heroFlightTabId = heroFlightTabId,
            rememberedTabCardBounds = rememberedTabCardBounds,
            onSwipeUpDragProgress = { p ->
              if (!isDraggingTabDrawer.value) {
                viewModel.captureVisibleTabThumbnails()
              }
              isDraggingTabDrawer.value = true
              heroFlightTabId = uiState.focusedTab?.id
              coroutineScope.launch { tabDrawerProgress.snapTo(p) }
            },
            onSwipeUpDragEnd = { paneIndex, completed, velocityY ->
              isDraggingTabDrawer.value = false
              val normVel = (-velocityY / heroMaxPx).coerceIn(-8f, 8f)
              if (completed) {
                viewModel.setActivePane(paneIndex)
                viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER)
                coroutineScope.launch {
                  tabDrawerProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = BrowserMotion.tabDrawerSpring(),
                    initialVelocity = normVel,
                  )
                }
              } else {
                coroutineScope.launch {
                  tabDrawerProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = BrowserMotion.tabDrawerSpring(),
                    initialVelocity = normVel,
                  )
                }
              }
            },
            onExecuteActionForPane = executeToolbarActionForPane,
            modifier =
              Modifier.zIndex(
                if (drawerP in 0.004f..0.92f) 2f else 0f
              ),
          )
        }
      }

      // 제스처 기반 연속 Hero Tab-Fly 서랍 (0f = 웹페이지, 1f = 탭 그리드)
      if (drawerP > 0.004f || uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) {
        Box(
          modifier =
            Modifier.fillMaxSize().zIndex(if (drawerP >= 0.92f) 3f else 1f)
        ) {
          TabSwitcherOverlay(
            tabs = uiState.tabs,
            folders = uiState.folders,
            tabSortMode = uiState.tabSortMode,
            tabViewMode = uiState.settings.tabViewMode,
            tabThumbnails = viewModel.tabThumbnails,
            tabFavicons = viewModel.tabFavicons,
            drawerProgress = drawerP,
            heroArrivalTabId = heroFlightTabId ?: uiState.focusedTab?.id,
            hasUndoableClosedTab = uiState.hasUndoableClosedTab,
            tabSwipeToCloseEnabled = uiState.settings.tabSwipeToCloseEnabled,
            tabDragFolderEnabled = uiState.settings.tabDragFolderEnabled,
            primaryTabId = uiState.primaryTabId,
            secondaryTabId = uiState.secondaryTabId,
            splitMode = uiState.splitMode,
            postureState = postureState,
            onSelectTab = { tabId, pane ->
              heroFlightTabId = tabId
              viewModel.selectTab(tabId, pane)
            },
            onCloseTab = viewModel::closeTab,
            onUndoCloseTab = viewModel::undoLastClosedTab,
            onCreateTab = { viewModel.createNewTab() },
            onToggleSplit = { viewModel.toggleSplitMode() },
            onSetTabSortMode = viewModel::setTabSortMode,
            onTogglePinTab = viewModel::togglePinTab,
            onMergeTabsIntoFolder = { src, tgt -> viewModel.mergeTabsIntoFolder(src, tgt) },
            onAddTabToFolder = viewModel::addTabToFolder,
            onRemoveTabFromFolder = viewModel::removeTabFromFolder,
            onToggleFolderExpanded = viewModel::toggleFolderExpanded,
            onCollapseAllFolders = viewModel::collapseAllFolders,
            onRenameFolder = viewModel::renameFolder,
            onSetFolderColor = viewModel::setFolderColor,
            onReportTabCardBounds = { updated ->
              val removed = rememberedTabCardBounds.keys.filter { it !in updated }
              removed.forEach { rememberedTabCardBounds.remove(it) }
              rememberedTabCardBounds.putAll(updated)
            },
            onDismiss = {
              heroFlightTabId = uiState.focusedTab?.id
              viewModel.dismissOverlay()
            },
          )
        }
      }

      val nonTabOverlay =
        if (uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) BrowserOverlay.NONE
        else uiState.activeOverlay

      AnimatedContent(
        targetState = nonTabOverlay,
        transitionSpec = {
          (fadeIn(BrowserMotion.expressiveSpring()) +
            slideInVertically(BrowserMotion.expressiveSpring()) { it / 16 } +
            scaleIn(BrowserMotion.expressiveSpring(), initialScale = 0.96f)) togetherWith
            (fadeOut(BrowserMotion.snappySpring()) +
              slideOutVertically(BrowserMotion.snappySpring()) { it / 16 } +
              scaleOut(BrowserMotion.snappySpring(), targetScale = 0.96f))
        },
        label = "BrowserOverlayTransition",
        modifier = Modifier.zIndex(4f),
      ) { overlay ->
        when (overlay) {
          BrowserOverlay.NONE,
          BrowserOverlay.TAB_SWITCHER -> Unit
          BrowserOverlay.LIBRARY_PANEL -> {
            LibraryOverlay(
              bookmarks = uiState.bookmarks,
              history = uiState.history,
              focusedUrl = uiState.focusedTab?.url,
              initialTab = uiState.libraryInitialTab,
              onToggleCurrentBookmark = viewModel::toggleBookmarkForFocusedTab,
              onOpenUrl = { url ->
                viewModel.dismissOverlay()
                viewModel.navigateUrlInFocusedPane(url)
              },
              onDeleteBookmark = viewModel::deleteBookmark,
              onDeleteHistoryItem = viewModel::deleteHistoryItem,
              onClearHistory = viewModel::clearHistory,
              onDismiss = viewModel::dismissOverlay,
            )
          }
          BrowserOverlay.SETTINGS_HUB -> {
            SettingsHubOverlay(
              abiInfo = uiState.abiInfo,
              postureState = postureState,
              blockerStats = uiState.blockerStats,
              settings = uiState.settings,
              toolbarConfig = uiState.toolbarConfig,
              designConfig = uiState.designConfig,
              sitePermissions = uiState.sitePermissions,
              activeTabDomain = uiState.focusedTab?.displayHost,
              searchEngines = uiState.searchEngines,
              dohProviders = uiState.dohProviders,
              isProbingDoh = uiState.isProbingDoh,
              lastDohLookup = uiState.lastDohLookup,
              statusBannerMessage = uiState.statusBannerMessage,
              onUpdateSetting = viewModel::updateSetting,
              onUpdateDesignParam = viewModel::updateDesignParam,
              onResetDesignDefaults = viewModel::resetDesignDefaults,
              onUpdateToolbarSlots = viewModel::updateToolbarSlots,
              onMoveToolbarSlot = viewModel::moveToolbarSlot,
              onSelectAlignment = viewModel::setToolbarAlignment,
              onToggleAutoMinimize = viewModel::setToolbarAutoMinimize,
              onSetGestures = viewModel::setToolbarGestures,
              onResetToolbar = viewModel::resetToolbarDefaults,
              onUpdateFilterList = viewModel::updateBuiltInFilterList,
              onClearBrowsingData = viewModel::clearBrowsingData,
              onSetGlobalSitePolicy = viewModel::setGlobalSitePolicy,
              onSetDomainSiteOverride = viewModel::setDomainSiteOverride,
              onResetDomainOverrides = viewModel::resetDomainSiteOverrides,
              onRunDohProbe = viewModel::runDohProbe,
              onOpenLibrary = { viewModel.showOverlay(BrowserOverlay.LIBRARY_PANEL) },
              onDismiss = viewModel::dismissOverlay,
            )
          }
          BrowserOverlay.READER_VIEW -> {
            uiState.readerArticle?.let { article ->
              ReaderModeOverlay(
                article = article,
                onDismiss = viewModel::dismissOverlay,
              )
            }
          }
          BrowserOverlay.QR_SCANNER -> {
            QrScannerOverlay(
              onQrDecoded = viewModel::onQrCodeDecoded,
              onDismiss = viewModel::dismissOverlay,
            )
          }
        }
      }

      // 스낵바 호스트 (탭 닫기 "실행 취소" · 스크린샷 · 다운로드 안내)
      SnackbarHost(
        hostState = snackbarHostState,
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(
              horizontal = BrowserSpacing.Grid2,
              vertical =
                (uiState.designConfig.barHeightDp +
                  uiState.designConfig.barBottomOffsetDp +
                  16f)
                  .dp,
            ),
      )

      uiState.pendingPermissionChallenge?.let { challenge ->
        SitePermissionPromptDialog(
          challenge = challenge,
          onResolve = viewModel::resolvePermissionChallenge,
        )
      }

      uiState.pendingExternalAppRequest?.let { req ->
        ExternalAppConfirmDialog(
          request = req,
          onResolve = viewModel::resolveExternalAppRequest,
        )
      }

      uiState.pendingDownloadRequest?.let { req ->
        DownloadConfirmDialog(
          request = req,
          onResolve = viewModel::resolveDownloadRequest,
        )
      }
    }
  }
}

private fun captureComposeViewBitmap(view: View): Bitmap? {
  if (view.width <= 0 || view.height <= 0) return null
  return try {
    Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { bmp ->
      val canvas = Canvas(bmp)
      view.draw(canvas)
    }
  } catch (_: Throwable) {
    null
  }
}

@Composable
private fun ExternalAppConfirmDialog(
  request: ExternalAppLaunchRequest,
  onResolve: (Boolean) -> Unit,
) {
  AlertDialog(
    onDismissRequest = { onResolve(false) },
    shape = BrowserRadii.DialogSheetShape,
    title = { Text("외부 앱 열기 확인", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
        Text("다른 앱으로 이동하시겠습니까?", style = MaterialTheme.typography.bodyMedium)
        Text(
          text = request.url,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onResolve(true) },
        shape = LocalDesignConfig.current.cardInnerShape(),
      ) {
        Text("열기")
      }
    },
    dismissButton = {
      TextButton(onClick = { onResolve(false) }) { Text("취소") }
    },
  )
}

@Composable
private fun DownloadConfirmDialog(
  request: PendingDownloadRequest,
  onResolve: (Boolean) -> Unit,
) {
  val sizeText =
    if (request.contentLength > 0L) {
      val kb = (request.contentLength / 1024L).coerceAtLeast(1L)
      "${kb}KB"
    } else {
      "크기 알 수 없음"
    }
  AlertDialog(
    onDismissRequest = { onResolve(false) },
    shape = BrowserRadii.DialogSheetShape,
    title = { Text("파일 다운로드", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
        Text(request.filename, style = MaterialTheme.typography.labelLarge)
        Text(
          text = "${request.mimeType} · $sizeText",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onResolve(true) },
        shape = LocalDesignConfig.current.cardInnerShape(),
      ) {
        Text("다운로드")
      }
    },
    dismissButton = {
      TextButton(onClick = { onResolve(false) }) { Text("취소") }
    },
  )
}

@Composable
private fun SitePermissionPromptDialog(
  challenge: WebSitePermissionChallenge,
  onResolve: (granted: Boolean, rememberForDomain: Boolean) -> Unit,
) {
  val context = LocalContext.current
  var rememberChoice by remember(challenge) { mutableStateOf(true) }

  val requiredAndroidPermissions =
    remember(challenge) {
      when (challenge) {
        is WebSitePermissionChallenge.MediaCapture ->
          buildList {
            if (challenge.needsCamera) add(Manifest.permission.CAMERA)
            if (challenge.needsMic) add(Manifest.permission.RECORD_AUDIO)
          }
        is WebSitePermissionChallenge.Geolocation ->
          listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
      }
    }

  val runtimePermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
      val allGranted = results.values.all { it }
      onResolve(allGranted, rememberChoice)
    }

  val title =
    when (challenge) {
      is WebSitePermissionChallenge.MediaCapture -> {
        val parts = buildList {
          if (challenge.needsCamera) add("카메라")
          if (challenge.needsMic) add("마이크")
        }
        "${challenge.domain} · ${parts.joinToString(", ")} 요청"
      }
      is WebSitePermissionChallenge.Geolocation ->
        "${challenge.domain} · 위치 요청"
    }

  val detail =
    when (challenge) {
      is WebSitePermissionChallenge.MediaCapture ->
        "AOSP WebView 로컬 권한 처리"
      is WebSitePermissionChallenge.Geolocation ->
        if (challenge.hasAospLocationProvider) "AOSP LocationManager 사용"
        else "기기 위치 서비스 꺼짐"
    }

  AlertDialog(
    onDismissRequest = { onResolve(false, false) },
    shape = BrowserRadii.DialogSheetShape,
    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
        Text(detail, style = MaterialTheme.typography.bodyMedium)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { rememberChoice = !rememberChoice },
        ) {
          Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
          Text("이 사이트 설정 기억", style = MaterialTheme.typography.labelLarge)
        }
      }
    },
    confirmButton = {
      Button(
        shape = LocalDesignConfig.current.cardInnerShape(),
        onClick = {
          val missing =
            requiredAndroidPermissions.filter { perm ->
              ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            }
          if (missing.isNotEmpty()) {
            runtimePermissionLauncher.launch(missing.toTypedArray())
          } else {
            onResolve(true, rememberChoice)
          }
        },
      ) {
        Text("허용")
      }
    },
    dismissButton = {
      TextButton(onClick = { onResolve(false, rememberChoice) }) {
        Text("차단")
      }
    },
  )
}

private fun isDesktopModeForTab(tab: TabUiModel?, uiState: BrowserUiState): Boolean {
  val domain = tab?.displayHost.orEmpty()
  return uiState.sitePermissions.domainOverrides
    .firstOrNull { it.domain == domain }
    ?.policy
    ?.desktopSite
    ?: uiState.sitePermissions.globalPolicy.desktopSite
}

@Composable
private fun FloatingBarWebLayout(
  uiState: BrowserUiState,
  postureState: FoldablePostureState,
  forceDualPane: Boolean,
  viewModel: BrowserViewModel,
  tabDrawerProgress: Float,
  heroFlightTabId: String?,
  rememberedTabCardBounds: Map<String, Rect>,
  onSwipeUpDragProgress: (Float) -> Unit,
  onSwipeUpDragEnd: (paneIndex: Int, completed: Boolean, velocityY: Float) -> Unit,
  onExecuteActionForPane: (Int, ToolbarActionId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDualPane by
    remember(
      postureState.supportsDualPane,
      forceDualPane,
      uiState.splitMode,
    ) {
      derivedStateOf {
        postureState.supportsDualPane && (forceDualPane || uiState.splitMode)
      }
    }

  val primaryTab = uiState.primaryTab
  val secondaryTab = uiState.secondaryTab ?: uiState.tabs.firstOrNull { it.id != primaryTab?.id }
  val isPrimaryBookmarked by
    remember(primaryTab?.url, uiState.bookmarks) {
      derivedStateOf {
        primaryTab?.let { tab -> uiState.bookmarks.any { it.url == tab.url } } ?: false
      }
    }
  val isSecondaryBookmarked by
    remember(secondaryTab?.url, uiState.bookmarks) {
      derivedStateOf {
        secondaryTab?.let { tab -> uiState.bookmarks.any { it.url == tab.url } } ?: false
      }
    }
  val isPrimaryDesktop = isDesktopModeForTab(primaryTab, uiState)
  val isSecondaryDesktop = isDesktopModeForTab(secondaryTab, uiState)

  var splitRatio by remember { mutableFloatStateOf(0.5f) }
  val showSecondaryPane = isDualPane && secondaryTab != null
  val secondaryWeightFraction by
    animateFloatAsState(
      targetValue = if (showSecondaryPane) (1f - splitRatio).coerceIn(0.32f, 0.68f) else 0f,
      animationSpec =
        if (showSecondaryPane) BrowserMotion.expressiveSpring()
        else BrowserMotion.snappySpring(),
      label = "DualPaneSecondaryWeight",
    )
  val primaryWeightFraction = (1f - secondaryWeightFraction).coerceAtLeast(0.32f)

  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val statusInsetModifier =
    if (uiState.settings.hideStatusBar) Modifier else Modifier.statusBarsPadding()

  BoxWithConstraints(modifier = modifier.fillMaxSize().then(statusInsetModifier)) {
    val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
    val activeHeroRect = (heroFlightTabId ?: uiState.focusedTab?.id)?.let { rememberedTabCardBounds[it] }
    val targetScale =
      if (activeHeroRect != null && activeHeroRect.width > 10f) {
        (activeHeroRect.width / totalWidthPx).coerceIn(0.36f, 0.56f)
      } else {
        0.46f
      }
    val targetDx =
      if (activeHeroRect != null) {
        (activeHeroRect.center.x - (totalWidthPx * 0.5f))
          .coerceIn(-totalWidthPx * 0.65f, totalWidthPx * 0.65f)
      } else {
        0f
      }
    val targetDy =
      if (activeHeroRect != null) {
        (activeHeroRect.center.y - (totalHeightPx * 0.5f))
          .coerceIn(-totalHeightPx * 0.65f, totalHeightPx * 0.65f)
      } else {
        -totalHeightPx * 0.14f
      }

    val p = tabDrawerProgress.coerceIn(0f, 1f)
    val arcPhase = sin(p * PI).toFloat()
    val arcLiftPx = arcPhase * with(density) { 28.dp.toPx() }
    val arcSidePx = arcPhase * (if (targetDx >= 0f) -1f else 1f) * with(density) { 16.dp.toPx() }
    val settleFade = ((p - 0.82f) / 0.12f).coerceIn(0f, 1f)

    val viewportHeroModifier =
      Modifier.fillMaxSize()
        .graphicsLayer {
          val s = 1f - p * (1f - targetScale)
          scaleX = s
          scaleY = s
          translationX = targetDx * p + arcSidePx
          translationY = targetDy * p - arcLiftPx
          alpha = 1f - settleFade
        }
        .clip(
          RoundedCornerShape(
            (p * uiState.designConfig.cardCornerRadiusDp).coerceIn(0f, 28f).dp
          )
        )

    Row(modifier = viewportHeroModifier) {
      // [Left / Single Pane]
      Box(modifier = Modifier.weight(primaryWeightFraction).fillMaxHeight()) {
        if (primaryTab != null) {
          BrowserTabContentPane(
            tab = primaryTab,
            paneIndex = 0,
            isFocusedPane = !isDualPane || uiState.activePane == 0,
            showSplitHeader = isDualPane,
            postureState = postureState,
            uiState = uiState,
            viewModel = viewModel,
            onFocusPane = { viewModel.setActivePane(0) },
            modifier = Modifier.fillMaxSize(),
          )
        }

        // 분할 모드: 왼쪽 창 하단 중앙 독립 플로팅 바
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
          androidx.compose.animation.AnimatedVisibility(
            visible = showSecondaryPane,
            enter =
              fadeIn(BrowserMotion.expressiveSpring()) +
                scaleIn(BrowserMotion.expressiveSpring(), initialScale = 0.92f),
            exit =
              fadeOut(BrowserMotion.snappySpring()) +
                scaleOut(BrowserMotion.snappySpring(), targetScale = 0.92f),
          ) {
            SafariFloatingBar(
              focusedTab = primaryTab,
              tabCount = uiState.tabs.size,
              splitMode = true,
              activePane = uiState.activePane,
              paneIndex = 0,
              isSplitCompact = true,
              isPaneFocused = uiState.activePane == 0,
              isBookmarked = isPrimaryBookmarked,
              isDesktopMode = isPrimaryDesktop,
              isCollapsed = uiState.isFloatingBarCollapsed,
              isOmniboxFocused = uiState.isOmniboxFocused && uiState.omniboxTargetPane == 0,
              omniboxQuery = uiState.omniboxQuery,
              suggestions = uiState.suggestions,
              searchEngines = uiState.searchEngines,
              selectedEngineId = uiState.settings.searchEngineId,
              isFindInPageVisible = uiState.isFindInPageVisible && uiState.activePane == 0,
              findQuery = uiState.findQuery,
              findActiveMatch = uiState.findActiveMatch,
              findTotalMatches = uiState.findTotalMatches,
              toolbarConfig = uiState.toolbarConfig,
              designConfig = uiState.designConfig,
              floatingBarStyle = uiState.settings.floatingBarStyle,
              splitGestureEnabled = uiState.settings.splitGestureEnabled,
              splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
              swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
              supportsDualPane = postureState.supportsDualPane,
              heroDragLiftProgress = p,
              onExpandBar = viewModel::expandFloatingBar,
              onCollapseBar = viewModel::collapseFloatingBar,
              onSwipeUpOpenTabs = {
                viewModel.setActivePane(0)
                viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER)
              },
              onSwipeUpDragProgress = onSwipeUpDragProgress,
              onSwipeUpDragEnd = { completed, velY ->
                onSwipeUpDragEnd(0, completed, velY)
              },
              onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta, targetPane = 0) },
              onTriggerSplitGesture = { dirRight ->
                viewModel.splitActiveTabWithGesture(dirRight, sourcePane = 0)
              },
              onCloseSplitPane = { viewModel.closeSplitKeepPane(keepPaneIndex = 1) },
              onFocusOmnibox = { viewModel.focusOmnibox(targetPane = 0) },
              onBlurOmnibox = viewModel::blurOmnibox,
              onOmniboxQueryChange = viewModel::updateOmniboxQuery,
              onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, targetPane = 0) },
              onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
              onReloadOrStop = { viewModel.reloadOrStopInPane(0) },
              onExecuteAction = { action -> onExecuteActionForPane(0, action) },
              onOpenSettings = { viewModel.showOverlay(BrowserOverlay.SETTINGS_HUB) },
              onFindQueryChange = viewModel::updateFindQuery,
              onFindNext = viewModel::findNextMatch,
              onCloseFindInPage = viewModel::closeFindInPage,
            )
          }
        }
      }

      // [Central Draggable Divider + Right Pane]
      if (secondaryWeightFraction > 0.01f && secondaryTab != null) {
        val hingeWidth =
          if (postureState.hasFoldFeature && !postureState.isHorizontalFold) {
            postureState.hingeWidthDp.coerceAtLeast(10.dp)
          } else {
            10.dp
          }

        Box(
          modifier =
            Modifier.width(hingeWidth)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
              .clickable(onClick = { splitRatio = 0.5f })
              .pointerInput(totalWidthPx) {
                detectHorizontalDragGestures { change, dragAmount ->
                  change.consume()
                  val next = (splitRatio + (dragAmount / totalWidthPx)).coerceIn(0.34f, 0.66f)
                  val snapped =
                    if (abs(next - 0.5f) < 0.015f) {
                      if (abs(splitRatio - 0.5f) >= 0.015f) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      }
                      0.5f
                    } else {
                      next
                    }
                  splitRatio = snapped
                }
              }
              .testTag("dual_pane_divider"),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.width(4.dp).height(36.dp),
          ) {}
        }

        // [Right Pane]
        Box(
          modifier =
            Modifier.weight(secondaryWeightFraction)
              .fillMaxHeight()
              .graphicsLayer { alpha = (secondaryWeightFraction * 2f).coerceIn(0f, 1f) }
        ) {
          BrowserTabContentPane(
            tab = secondaryTab,
            paneIndex = 1,
            isFocusedPane = uiState.activePane == 1,
            showSplitHeader = true,
            postureState = postureState,
            uiState = uiState,
            viewModel = viewModel,
            onFocusPane = { viewModel.setActivePane(1) },
            modifier = Modifier.fillMaxSize(),
          )

          SafariFloatingBar(
            focusedTab = secondaryTab,
            tabCount = uiState.tabs.size,
            splitMode = true,
            activePane = uiState.activePane,
            paneIndex = 1,
            isSplitCompact = true,
            isPaneFocused = uiState.activePane == 1,
            isBookmarked = isSecondaryBookmarked,
            isDesktopMode = isSecondaryDesktop,
            isCollapsed = uiState.isFloatingBarCollapsed,
            isOmniboxFocused = uiState.isOmniboxFocused && uiState.omniboxTargetPane == 1,
            omniboxQuery = uiState.omniboxQuery,
            suggestions = uiState.suggestions,
            searchEngines = uiState.searchEngines,
            selectedEngineId = uiState.settings.searchEngineId,
            isFindInPageVisible = uiState.isFindInPageVisible && uiState.activePane == 1,
            findQuery = uiState.findQuery,
            findActiveMatch = uiState.findActiveMatch,
            findTotalMatches = uiState.findTotalMatches,
            toolbarConfig = uiState.toolbarConfig,
            designConfig = uiState.designConfig,
            floatingBarStyle = uiState.settings.floatingBarStyle,
            splitGestureEnabled = uiState.settings.splitGestureEnabled,
            splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
            swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
            supportsDualPane = postureState.supportsDualPane,
            heroDragLiftProgress = p,
            onExpandBar = viewModel::expandFloatingBar,
            onCollapseBar = viewModel::collapseFloatingBar,
            onSwipeUpOpenTabs = {
              viewModel.setActivePane(1)
              viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER)
            },
            onSwipeUpDragProgress = onSwipeUpDragProgress,
            onSwipeUpDragEnd = { completed, velY ->
              onSwipeUpDragEnd(1, completed, velY)
            },
            onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta, targetPane = 1) },
            onTriggerSplitGesture = { dirRight ->
              viewModel.splitActiveTabWithGesture(dirRight, sourcePane = 1)
            },
            onCloseSplitPane = { viewModel.closeSplitKeepPane(keepPaneIndex = 0) },
            onFocusOmnibox = { viewModel.focusOmnibox(targetPane = 1) },
            onBlurOmnibox = viewModel::blurOmnibox,
            onOmniboxQueryChange = viewModel::updateOmniboxQuery,
            onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, targetPane = 1) },
            onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
            onReloadOrStop = { viewModel.reloadOrStopInPane(1) },
            onExecuteAction = { action -> onExecuteActionForPane(1, action) },
            onOpenSettings = { viewModel.showOverlay(BrowserOverlay.SETTINGS_HUB) },
            onFindQueryChange = viewModel::updateFindQuery,
            onFindNext = viewModel::findNextMatch,
            onCloseFindInPage = viewModel::closeFindInPage,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
          )
        }
      }
    }

    // 단일 모드: 통합 플로팅 바 1개 표기 (barHorizontalBias 적용을 위해 fillMaxWidth)
    AnimatedVisibility(
      visible = !showSecondaryPane,
      enter =
        fadeIn(BrowserMotion.expressiveSpring()) +
          scaleIn(BrowserMotion.expressiveSpring(), initialScale = 0.94f),
      exit =
        fadeOut(BrowserMotion.snappySpring()) +
          scaleOut(BrowserMotion.snappySpring(), targetScale = 0.94f),
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
    ) {
      SafariFloatingBar(
        focusedTab = primaryTab,
        tabCount = uiState.tabs.size,
        splitMode = false,
        activePane = 0,
        paneIndex = 0,
        isSplitCompact = false,
        isPaneFocused = true,
        isBookmarked = isPrimaryBookmarked,
        isDesktopMode = isPrimaryDesktop,
        isCollapsed = uiState.isFloatingBarCollapsed,
        isOmniboxFocused = uiState.isOmniboxFocused,
        omniboxQuery = uiState.omniboxQuery,
        suggestions = uiState.suggestions,
        searchEngines = uiState.searchEngines,
        selectedEngineId = uiState.settings.searchEngineId,
        isFindInPageVisible = uiState.isFindInPageVisible,
        findQuery = uiState.findQuery,
        findActiveMatch = uiState.findActiveMatch,
        findTotalMatches = uiState.findTotalMatches,
        toolbarConfig = uiState.toolbarConfig,
        designConfig = uiState.designConfig,
        floatingBarStyle = uiState.settings.floatingBarStyle,
        splitGestureEnabled = uiState.settings.splitGestureEnabled,
        splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
        swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
        supportsDualPane = postureState.supportsDualPane,
        heroDragLiftProgress = p,
        onExpandBar = viewModel::expandFloatingBar,
        onCollapseBar = viewModel::collapseFloatingBar,
        onSwipeUpOpenTabs = {
          viewModel.setActivePane(0)
          viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER)
        },
        onSwipeUpDragProgress = onSwipeUpDragProgress,
        onSwipeUpDragEnd = { completed, velY ->
          onSwipeUpDragEnd(0, completed, velY)
        },
        onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta, targetPane = 0) },
        onTriggerSplitGesture = { dirRight ->
          viewModel.splitActiveTabWithGesture(dirRight, sourcePane = 0)
        },
        onFocusOmnibox = { viewModel.focusOmnibox(targetPane = 0) },
        onBlurOmnibox = viewModel::blurOmnibox,
        onOmniboxQueryChange = viewModel::updateOmniboxQuery,
        onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, targetPane = 0) },
        onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
        onReloadOrStop = { viewModel.reloadOrStopInPane(0) },
        onExecuteAction = { action -> onExecuteActionForPane(0, action) },
        onOpenSettings = { viewModel.showOverlay(BrowserOverlay.SETTINGS_HUB) },
        onFindQueryChange = viewModel::updateFindQuery,
        onFindNext = viewModel::findNextMatch,
        onCloseFindInPage = viewModel::closeFindInPage,
      )
    }
  }
}

@Composable
private fun TabletopPostureLayout(
  uiState: BrowserUiState,
  postureState: FoldablePostureState,
  viewModel: BrowserViewModel,
  onExecuteActionForPane: (Int, ToolbarActionId) -> Unit,
) {
  val topHeight = postureState.hingeTopDp.coerceAtLeast(220.dp)
  val hingeGap = postureState.hingeHeightDp.coerceAtLeast(8.dp)
  val tab = uiState.focusedTab ?: uiState.primaryTab
  val isBookmarked =
    tab?.let { t -> uiState.bookmarks.any { it.url == t.url } } ?: false
  val isDesktop = isDesktopModeForTab(tab, uiState)
  val statusInsetModifier =
    if (uiState.settings.hideStatusBar) Modifier else Modifier.statusBarsPadding()

  Box(modifier = Modifier.fillMaxSize().then(statusInsetModifier)) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.fillMaxWidth().height(topHeight)) {
        if (tab != null) {
          BrowserTabContentPane(
            tab = tab,
            paneIndex = uiState.activePane,
            isFocusedPane = true,
            showSplitHeader = false,
            postureState = postureState,
            uiState = uiState,
            viewModel = viewModel,
            onFocusPane = {},
          )
        }
      }

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(hingeGap)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
      )

      TabletopDeck(
        focusedTab = tab,
        tabs = uiState.tabs,
        bookmarks = uiState.bookmarks,
        blockerStats = uiState.blockerStats,
        onBack = { viewModel.goBackInFocusedPane() },
        onForward = viewModel::goForwardInFocusedPane,
        onReloadOrStop = viewModel::reloadOrStopFocusedPane,
        onHome = viewModel::goHomeInFocusedPane,
        onOpenOmnibox = { viewModel.focusOmnibox(uiState.activePane) },
        onSelectTab = { tabId -> viewModel.selectTab(tabId, uiState.activePane) },
        onNewTab = { viewModel.createNewTab() },
        onNavigateBookmark = viewModel::navigateUrlInFocusedPane,
        onOpenLibrary = { viewModel.showOverlay(BrowserOverlay.LIBRARY_PANEL) },
        onOpenInspector = { viewModel.showOverlay(BrowserOverlay.SETTINGS_HUB) },
        modifier = Modifier.weight(1f),
      )
    }

    if (uiState.isOmniboxFocused || uiState.isFindInPageVisible) {
      SafariFloatingBar(
        focusedTab = tab,
        tabCount = uiState.tabs.size,
        splitMode = uiState.splitMode,
        activePane = uiState.activePane,
        isBookmarked = isBookmarked,
        isDesktopMode = isDesktop,
        isCollapsed = false,
        isOmniboxFocused = uiState.isOmniboxFocused,
        omniboxQuery = uiState.omniboxQuery,
        suggestions = uiState.suggestions,
        searchEngines = uiState.searchEngines,
        selectedEngineId = uiState.settings.searchEngineId,
        isFindInPageVisible = uiState.isFindInPageVisible,
        findQuery = uiState.findQuery,
        findActiveMatch = uiState.findActiveMatch,
        findTotalMatches = uiState.findTotalMatches,
        toolbarConfig = uiState.toolbarConfig,
        designConfig = uiState.designConfig,
        floatingBarStyle = uiState.settings.floatingBarStyle,
        splitGestureEnabled = uiState.settings.splitGestureEnabled,
        splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
        swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
        supportsDualPane = postureState.supportsDualPane,
        onExpandBar = viewModel::expandFloatingBar,
        onCollapseBar = viewModel::collapseFloatingBar,
        onSwipeUpOpenTabs = { viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER) },
        onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta) },
        onFocusOmnibox = { viewModel.focusOmnibox(uiState.activePane) },
        onBlurOmnibox = viewModel::blurOmnibox,
        onOmniboxQueryChange = viewModel::updateOmniboxQuery,
        onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, uiState.omniboxTargetPane) },
        onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
        onReloadOrStop = viewModel::reloadOrStopFocusedPane,
        onExecuteAction = { action -> onExecuteActionForPane(uiState.activePane, action) },
        onOpenSettings = { viewModel.showOverlay(BrowserOverlay.SETTINGS_HUB) },
        onFindQueryChange = viewModel::updateFindQuery,
        onFindNext = viewModel::findNextMatch,
        onCloseFindInPage = viewModel::closeFindInPage,
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun BrowserTabContentPane(
  tab: TabUiModel,
  paneIndex: Int,
  isFocusedPane: Boolean,
  showSplitHeader: Boolean,
  postureState: FoldablePostureState,
  uiState: BrowserUiState,
  viewModel: BrowserViewModel,
  onFocusPane: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor =
    if (showSplitHeader && isFocusedPane) MaterialTheme.colorScheme.primary
    else if (showSplitHeader) MaterialTheme.colorScheme.outline
    else MaterialTheme.colorScheme.background

  val cardShape = LocalDesignConfig.current.cardInnerShape()

  Surface(
    border = if (showSplitHeader) BorderStroke(1.5.dp, borderColor) else null,
    shape =
      if (showSplitHeader) cardShape else androidx.compose.ui.graphics.RectangleShape,
    modifier = modifier.padding(if (showSplitHeader) 4.dp else 0.dp),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (showSplitHeader) {
        Surface(
          color =
            if (isFocusedPane) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().clickable(onClick = onFocusPane),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = BrowserSpacing.Grid1_5, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          ) {
            Text(
              text = "P${paneIndex + 1}",
              style = MaterialTheme.typography.labelSmall,
              color =
                if (isFocusedPane) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = tab.displayHost,
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (tab.blockedCount > 0) {
              Icon(
                Icons.Outlined.Shield,
                contentDescription = "차단됨",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = tab.blockedCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
            SpringIconButton(
              onClick = { viewModel.closeSplitKeepPane(keepPaneIndex = if (paneIndex == 0) 1 else 0) },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                Icons.Outlined.Close,
                contentDescription = "창 닫기",
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }
      }

      when (tab.url) {
        "about:mb" -> {
          val searchLabel =
            uiState.searchEngines.firstOrNull { it.id == uiState.settings.searchEngineId }?.label
              ?: "DuckDuckGo"
          val dohLabel =
            uiState.dohProviders.firstOrNull { it.id == uiState.settings.dohProviderId }?.label
              ?: "Quad9"

          StartPageContent(
            paneIndex = paneIndex,
            isFocusedPane = isFocusedPane,
            isSplitMode = uiState.splitMode && postureState.supportsDualPane,
            postureState = postureState,
            blockerStats = uiState.blockerStats,
            searchEngineLabel = searchLabel,
            dohProviderLabel = dohLabel,
            bookmarks = uiState.bookmarks,
            onOpenOmnibox = {
              onFocusPane()
              viewModel.focusOmnibox(paneIndex)
            },
            onNavigateBookmark = { url ->
              onFocusPane()
              viewModel.navigateUrlInPane(url, paneIndex)
            },
            onOpenInspector = { viewModel.showOverlay(BrowserOverlay.SETTINGS_HUB) },
            onToggleSplit = { viewModel.toggleSplitMode() },
            onScrollDelta = viewModel::onViewportScrollDelta,
            modifier = Modifier.weight(1f).fillMaxWidth(),
          )
        }
        "about:blank" -> {
          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable(onClick = onFocusPane),
          )
        }
        else -> {
          val renderPolicy =
            remember(tab.url, uiState.settings, uiState.sitePermissions) {
              viewModel.resolveRenderPolicyForTab(tab.url)
            }
          AospWebViewViewport(
            tabId = tab.id,
            targetUrl = tab.url,
            renderPolicy = renderPolicy,
            webViewPool = viewModel.webViewPool,
            onNavigationUpdate = { url, title, isLoading, progress, caps ->
              viewModel.onWebViewNavigationUpdate(tab.id, url, title, isLoading, progress, caps)
            },
            onIconUpdate = { icon -> viewModel.onWebViewIconUpdate(tab.id, icon) },
            onRequestBlocked = { viewModel.onWebViewRequestBlocked(tab.id) },
            onScrollDelta = viewModel::onViewportScrollDelta,
            onPermissionChallenge = viewModel::onPermissionChallenge,
            onExternalAppRequest = viewModel::onExternalAppRequest,
            onDownloadRequest = viewModel::onDownloadRequest,
            modifier = Modifier.weight(1f).fillMaxWidth(),
          )
        }
      }
    }
  }
}
