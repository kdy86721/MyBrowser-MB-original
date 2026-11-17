package com.mybrowser.appui.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Public
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mybrowser.appui.foldable.BrowserPostureMode
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.foldable.rememberFoldablePostureState
import com.mybrowser.appui.network.AiFoldableLayout
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
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
  val rememberedTabHeroBounds = remember { mutableStateMapOf<String, Rect>() }

  val tabDrawerProgress =
    remember {
      Animatable(if (uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) 1f else 0f)
    }
  val isDraggingTabDrawer = remember { mutableStateOf(false) }
  var isReturningToWeb by remember { mutableStateOf(false) }
  var pendingDrawerVelocity by remember { mutableFloatStateOf(0f) }
  var heroFlightTabId by remember { mutableStateOf<String?>(uiState.focusedTab?.id) }

  // Multi-layer Spatial Dive-In state for Overlays (Settings, Library, Reader, QR)
  var activeOverlayOrigin by remember { mutableStateOf(SpatialDiveOrigin()) }
  var presentedNonTabOverlay by remember { mutableStateOf(BrowserOverlay.NONE) }
  var isOverlayCollapsing by remember { mutableStateOf(false) }
  val overlayDiveAnim = remember { Animatable(0f) }

  // Multi-layer Spatial Dive-In state for Site Entries (Library, Omnibox Suggestions, Tabletop Bookmarks)
  var siteDiveKey by remember { mutableIntStateOf(0) }
  var siteDiveActive by remember { mutableStateOf(false) }
  var siteDiveFromOverlay by remember { mutableStateOf(false) }
  var siteDiveOrigin by remember { mutableStateOf(SpatialDiveOrigin()) }
  var siteDiveSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
  val siteDiveAnim = remember(siteDiveKey) { Animatable(0f) }

  // Settings deep-link pillar + Long-press Upward AI Sheet morph state
  var activeSettingsPillar by remember { mutableStateOf(SettingsPillar.DESIGN) }
  val aiMorphProgress = remember { Animatable(if (uiState.isAiSheetOpen) 1f else 0f) }
  val isDraggingAiSheet = remember { mutableStateOf(false) }
  var pendingAiVelocity by remember { mutableFloatStateOf(0f) }
  val aiMaxPx = with(density) { 160.dp.toPx().coerceAtLeast(130f) }

  LaunchedEffect(uiState.tabs) {
    val activeIds = uiState.tabs.map { it.id }.toSet()
    val staleIds = rememberedTabHeroBounds.keys.filter { it !in activeIds }
    staleIds.forEach { rememberedTabHeroBounds.remove(it) }
  }

  val speedScale = uiState.designConfig.animationSpeedScale

  LaunchedEffect(uiState.isAiSheetOpen, speedScale) {
    if (!isDraggingAiSheet.value) {
      val target = if (uiState.isAiSheetOpen) 1f else 0f
      val initialVel = pendingAiVelocity
      pendingAiVelocity = 0f
      aiMorphProgress.animateTo(
        targetValue = target,
        animationSpec = ExpressiveSprings.spatial(speedScale),
        initialVelocity = initialVel,
      )
    }
  }

  LaunchedEffect(siteDiveKey, siteDiveActive) {
    if (siteDiveActive) {
      siteDiveAnim.animateTo(1f, ExpressiveSprings.spatial(speedScale))
      if (siteDiveFromOverlay) {
        overlayDiveAnim.snapTo(0f)
        presentedNonTabOverlay = BrowserOverlay.NONE
        isOverlayCollapsing = false
      }
      siteDiveActive = false
      siteDiveFromOverlay = false
      siteDiveSnapshot = null
    }
  }

  LaunchedEffect(uiState.activeOverlay) {
    if (!isDraggingTabDrawer.value) {
      if (uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) {
        viewModel.captureVisibleTabThumbnails()
        isReturningToWeb = false
        if (heroFlightTabId == null) {
          heroFlightTabId = uiState.focusedTab?.id
        }
        val initialVel = pendingDrawerVelocity
        pendingDrawerVelocity = 0f
        tabDrawerProgress.animateTo(
          targetValue = 1f,
          animationSpec = ExpressiveSprings.spatial(speedScale),
          initialVelocity = initialVel,
        )
      } else {
        isReturningToWeb = true
        if (heroFlightTabId == null) {
          heroFlightTabId = uiState.focusedTab?.id
        }
        tabDrawerProgress.animateTo(
          targetValue = 0f,
          animationSpec = ExpressiveSprings.spatial(speedScale),
        )
        isReturningToWeb = false
        heroFlightTabId = uiState.focusedTab?.id
      }
    }
  }

  val nonTabOverlayTarget =
    if (uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER) BrowserOverlay.NONE
    else uiState.activeOverlay

  LaunchedEffect(nonTabOverlayTarget) {
    if (nonTabOverlayTarget != BrowserOverlay.NONE) {
      isOverlayCollapsing = false
      presentedNonTabOverlay = nonTabOverlayTarget
      overlayDiveAnim.animateTo(1f, ExpressiveSprings.spatial(speedScale))
    } else if (presentedNonTabOverlay != BrowserOverlay.NONE && !isOverlayCollapsing && !siteDiveFromOverlay) {
      isOverlayCollapsing = true
      overlayDiveAnim.animateTo(0f, ExpressiveSprings.spatial(speedScale))
      presentedNonTabOverlay = BrowserOverlay.NONE
      isOverlayCollapsing = false
    }
  }

  val openOverlayWithPillar: (BrowserOverlay, SpatialDiveOrigin, Int, SettingsPillar) -> Unit =
    { overlay, origin, libraryTab, settingsPillar ->
      if (overlay == BrowserOverlay.SETTINGS_HUB) {
        activeSettingsPillar = settingsPillar
      }
      isOverlayCollapsing = false
      activeOverlayOrigin = origin
      presentedNonTabOverlay = overlay
      viewModel.showOverlay(overlay, libraryInitialTab = libraryTab)
    }

  val requestOpenOverlay: (BrowserOverlay, SpatialDiveOrigin, Int) -> Unit =
    { overlay, origin, libraryTab ->
      openOverlayWithPillar(overlay, origin, libraryTab, SettingsPillar.DESIGN)
    }

  val requestDismissOverlay: () -> Unit = {
    if (presentedNonTabOverlay != BrowserOverlay.NONE && !isOverlayCollapsing) {
      isOverlayCollapsing = true
      coroutineScope.launch {
        overlayDiveAnim.animateTo(0f, ExpressiveSprings.spatial(speedScale))
        presentedNonTabOverlay = BrowserOverlay.NONE
        isOverlayCollapsing = false
        viewModel.dismissOverlay()
      }
    } else {
      viewModel.dismissOverlay()
    }
  }

  val launchSiteDive: (url: String, pane: Int, origin: SpatialDiveOrigin, fromOverlay: Boolean) -> Unit =
    { url, pane, origin, fromOverlay ->
      if (origin.isValid()) {
        siteDiveFromOverlay = fromOverlay
        siteDiveOrigin = origin
        siteDiveSnapshot =
          if (!fromOverlay) captureComposeViewBitmap(rootView)?.asImageBitmap()
          else null
        siteDiveKey += 1
        siteDiveActive = true
        viewModel.navigateUrlInPane(url, pane)
      } else {
        viewModel.navigateUrlInPane(url, pane)
        if (fromOverlay) {
          requestDismissOverlay()
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
      uiState.isAiSheetOpen ||
        uiState.activeOverlay != BrowserOverlay.NONE ||
        presentedNonTabOverlay != BrowserOverlay.NONE ||
        uiState.isOmniboxFocused ||
        uiState.isFindInPageVisible ||
        !isInternalPage
  ) {
    when {
      uiState.isAiSheetOpen -> viewModel.closeAiSheet()
      presentedNonTabOverlay != BrowserOverlay.NONE -> requestDismissOverlay()
      uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER -> {
        heroFlightTabId = uiState.focusedTab?.id
        isReturningToWeb = true
        viewModel.dismissOverlay()
      }
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
            viewModel.captureVisibleTabThumbnails()
            isReturningToWeb = false
            heroFlightTabId = paneTab?.id ?: uiState.focusedTab?.id
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

  CompositionLocalProvider(
    LocalDesignConfig provides uiState.designConfig,
    LocalAnimationSpeedScale provides uiState.designConfig.animationSpeedScale,
  ) {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize(1080, 1920)) }

    BoxWithConstraints(
      modifier =
        modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .onGloballyPositioned { coords ->
            rootOrigin = coords.boundsInRoot().topLeft
            rootSize = coords.size
          }
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
      val overlayDiveP = overlayDiveAnim.value.coerceIn(0f, 1f)
      val siteDiveP = if (siteDiveActive) siteDiveAnim.value.coerceIn(0f, 1f) else 0f
      val aiP = aiMorphProgress.value.coerceIn(0f, 1f)
      val showAiSheetLayer = aiP > 0.004f || uiState.isAiSheetOpen

      val showTabSwitcherLayer = drawerP > 0.004f || uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER
      val isTabSwitcherSettled =
        uiState.activeOverlay == BrowserOverlay.TAB_SWITCHER && drawerP >= 0.996f && !isReturningToWeb

      val handleAiSwipeProgress: (Int, Float) -> Unit = { paneIndex, p ->
        if (!isDraggingAiSheet.value) {
          viewModel.prepareAiSheetForPane(paneIndex)
        }
        isDraggingAiSheet.value = true
        coroutineScope.launch { aiMorphProgress.snapTo(p) }
      }

      val handleAiSwipeEnd: (Int, Boolean, Float) -> Unit = { paneIndex, completed, velocityY ->
        isDraggingAiSheet.value = false
        val normVel = (-velocityY / aiMaxPx).coerceIn(-8f, 8f)
        if (completed) {
          pendingAiVelocity = normVel
          viewModel.openAiSheet(paneIndex, fromDragGesture = true)
        } else {
          coroutineScope.launch {
            aiMorphProgress.animateTo(
              targetValue = 0f,
              animationSpec = ExpressiveSprings.spatial(speedScale),
              initialVelocity = normVel,
            )
          }
        }
      }

      val renderPrimaryBrowserSurface: @Composable (Modifier) -> Unit = { surfaceModifier ->
        val overlayDepartModifier =
          if (presentedNonTabOverlay != BrowserOverlay.NONE && overlayDiveP > 0.002f && !(siteDiveActive && siteDiveFromOverlay)) {
            Modifier.spatialDiveBackground(
              progress = overlayDiveP,
              origin = activeOverlayOrigin,
              hostSize = rootSize,
              hostTopLeftInRoot = rootOrigin,
            )
          } else {
            Modifier
          }

        val isAiRightPanelLayout =
          postureState.supportsDualPane && uiState.aiConfig.foldableLayout == AiFoldableLayout.RIGHT_PANEL
        val aiBackdropModifier =
          if (aiP > 0.002f && presentedNonTabOverlay == BrowserOverlay.NONE) {
            val depthScale = if (isAiRightPanelLayout) 1f - 0.015f * aiP else 1f - 0.045f * aiP
            val blurRadiusDp = if (isAiRightPanelLayout) 6f * aiP else 20f * aiP
            Modifier
              .graphicsLayer {
                scaleX = depthScale
                scaleY = depthScale
                val cornerPx = uiState.designConfig.barCornerRadiusDp.dp.toPx() * aiP
                if (cornerPx > 0.5f) {
                  shape = RoundedCornerShape(cornerPx / depthScale)
                  clip = true
                }
              }
              .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusDp > 0.5f) {
                  Modifier.blur(blurRadiusDp.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                } else {
                  Modifier
                }
              )
          } else {
            Modifier
          }

        when (postureState.mode) {
          BrowserPostureMode.TABLETOP_HALF_OPENED -> {
            TabletopPostureLayout(
              uiState = uiState,
              postureState = postureState,
              viewModel = viewModel,
              onAiSwipeProgress = { p -> handleAiSwipeProgress(uiState.activePane, p) },
              onAiSwipeEnd = { completed, velY ->
                handleAiSwipeEnd(uiState.activePane, completed, velY)
              },
              onRequestOpenOverlay = requestOpenOverlay,
              onNavigateWithSiteDive = { url, pane, origin ->
                launchSiteDive(url, pane, origin, false)
              },
              onExecuteActionForPane = executeToolbarActionForPane,
              modifier = surfaceModifier.then(overlayDepartModifier).then(aiBackdropModifier),
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
              isReturningToWeb = isReturningToWeb,
              heroFlightTabId = heroFlightTabId,
              rememberedTabCardBounds = rememberedTabHeroBounds,
              onSwipeUpDragProgress = { p ->
                if (!isDraggingTabDrawer.value) {
                  viewModel.captureVisibleTabThumbnails()
                }
                isDraggingTabDrawer.value = true
                isReturningToWeb = false
                heroFlightTabId = uiState.focusedTab?.id
                coroutineScope.launch { tabDrawerProgress.snapTo(p) }
              },
              onSwipeUpDragEnd = { paneIndex, completed, velocityY ->
                isDraggingTabDrawer.value = false
                val normVel = (-velocityY / heroMaxPx).coerceIn(-8f, 8f)
                if (completed) {
                  isReturningToWeb = false
                  pendingDrawerVelocity = normVel
                  viewModel.setActivePane(paneIndex)
                  viewModel.showOverlay(BrowserOverlay.TAB_SWITCHER)
                } else {
                  isReturningToWeb = true
                  coroutineScope.launch {
                    tabDrawerProgress.animateTo(
                      targetValue = 0f,
                      animationSpec = ExpressiveSprings.spatial(speedScale),
                      initialVelocity = normVel,
                    )
                    isReturningToWeb = false
                  }
                }
              },
              onAiSwipeProgress = handleAiSwipeProgress,
              onAiSwipeEnd = handleAiSwipeEnd,
              onRequestOpenOverlay = requestOpenOverlay,
              onNavigateWithSiteDive = { url, pane, origin ->
                launchSiteDive(url, pane, origin, false)
              },
              onExecuteActionForPane = executeToolbarActionForPane,
              modifier =
                surfaceModifier
                  .then(overlayDepartModifier)
                  .then(aiBackdropModifier)
                  .zIndex(if (drawerP > 0.004f && !isTabSwitcherSettled) 2f else 0f),
            )
          }
        }
      }

      if (presentedNonTabOverlay != BrowserOverlay.NONE && overlayDiveP > 0.002f) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.88f * overlayDiveP))
        )
      }

      if (!siteDiveActive) {
        renderPrimaryBrowserSurface(Modifier)
      }

      // 제스처 & 클릭 양방향 통합 Hero Tab-Fly 서랍 (0f = 전체 화면 웹뷰, 1f = 탭 카드 위치)
      if (showTabSwitcherLayer) {
        val tabLayerDepartModifier =
          if (presentedNonTabOverlay != BrowserOverlay.NONE && overlayDiveP > 0.002f) {
            Modifier.spatialDiveBackground(
              progress = overlayDiveP,
              origin = activeOverlayOrigin,
              hostSize = rootSize,
              hostTopLeftInRoot = rootOrigin,
            )
          } else {
            Modifier
          }

        Box(
          modifier =
            Modifier.fillMaxSize()
              .then(tabLayerDepartModifier)
              .zIndex(if (isTabSwitcherSettled) 3f else 1f)
        ) {
          TabSwitcherOverlay(
            tabs = uiState.tabs,
            folders = uiState.folders,
            tabSortMode = uiState.tabSortMode,
            tabViewMode = uiState.settings.tabViewMode,
            tabThumbnails = viewModel.tabThumbnails,
            tabFavicons = viewModel.tabFavicons,
            drawerProgress = drawerP,
            isReturningToWeb = isReturningToWeb,
            heroArrivalTabId = heroFlightTabId ?: uiState.focusedTab?.id,
            hasUndoableClosedTab = uiState.hasUndoableClosedTab,
            tabSwipeToCloseEnabled = uiState.settings.tabSwipeToCloseEnabled,
            tabDragFolderEnabled = uiState.settings.tabDragFolderEnabled,
            primaryTabId = uiState.primaryTabId,
            secondaryTabId = uiState.secondaryTabId,
            splitMode = uiState.splitMode,
            postureState = postureState,
            onSelectTab = { tabId, pane, origin ->
              if (origin.isValid()) {
                rememberedTabHeroBounds[tabId] = origin.boundsInRoot
              }
              heroFlightTabId = tabId
              isReturningToWeb = true
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
            onReportHeroThumbnailBounds = { updated ->
              val removed = rememberedTabHeroBounds.keys.filter { it !in updated }
              removed.forEach { rememberedTabHeroBounds.remove(it) }
              rememberedTabHeroBounds.putAll(updated)
            },
            onDismiss = {
              heroFlightTabId = uiState.focusedTab?.id
              isReturningToWeb = true
              viewModel.dismissOverlay()
            },
          )
        }
      }

      // Settings / Library / Reader / QR Spatial Dive-In Layer
      if (presentedNonTabOverlay != BrowserOverlay.NONE && overlayDiveP > 0.002f) {
        val overlayDepartModifier =
          if (siteDiveActive && siteDiveFromOverlay && siteDiveP > 0.002f) {
            Modifier.spatialDiveBackground(
              progress = siteDiveP,
              origin = siteDiveOrigin,
              hostSize = rootSize,
              hostTopLeftInRoot = rootOrigin,
            )
          } else {
            Modifier
          }
        SpatialDiveScrim(
          progress = overlayDiveP,
          modifier = Modifier.zIndex(4f),
        )
        SpatialDiveForegroundHost(
          progress = overlayDiveP,
          origin = activeOverlayOrigin,
          hostSize = rootSize,
          hostTopLeftInRoot = rootOrigin,
          modifier = Modifier.zIndex(5f).then(overlayDepartModifier),
        ) {
          when (presentedNonTabOverlay) {
            BrowserOverlay.NONE,
            BrowserOverlay.TAB_SWITCHER -> Unit
            BrowserOverlay.LIBRARY_PANEL -> {
              LibraryOverlay(
                bookmarks = uiState.bookmarks,
                history = uiState.history,
                focusedUrl = uiState.focusedTab?.url,
                initialTab = uiState.libraryInitialTab,
                onToggleCurrentBookmark = viewModel::toggleBookmarkForFocusedTab,
                onOpenUrl = { url, origin ->
                  launchSiteDive(url, uiState.activePane, origin, true)
                },
                onDeleteBookmark = viewModel::deleteBookmark,
                onDeleteHistoryItem = viewModel::deleteHistoryItem,
                onClearHistory = viewModel::clearHistory,
                onDismiss = requestDismissOverlay,
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
                aiConfig = uiState.aiConfig,
                aiProviderPresets = uiState.aiProviderPresets,
                isTestingAiConnection = uiState.isTestingAiConnection,
                aiConnectionTestResult = uiState.aiConnectionTestResult,
                initialPillar = activeSettingsPillar,
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
                onUpdateAiConfig = viewModel::updateAiConfig,
                onResetAiConfigDefaults = viewModel::resetAiConfigDefaults,
                onTestAiConnection = viewModel::testAiConnection,
                onOpenLibrary = { libOrigin ->
                  requestOpenOverlay(BrowserOverlay.LIBRARY_PANEL, libOrigin, 0)
                },
                onDismiss = requestDismissOverlay,
              )
            }
            BrowserOverlay.READER_VIEW -> {
              uiState.readerArticle?.let { article ->
                ReaderModeOverlay(
                  article = article,
                  onDismiss = requestDismissOverlay,
                )
              }
            }
            BrowserOverlay.QR_SCANNER -> {
              QrScannerOverlay(
                onQrDecoded = viewModel::onQrCodeDecoded,
                onDismiss = requestDismissOverlay,
              )
            }
          }
        }
      }

      // Morphing AI CLI Chat Sheet / 1:1.4 Right Panel (Long-Press + Upward Drag)
      if (showAiSheetLayer && presentedNonTabOverlay == BrowserOverlay.NONE) {
        val isAiRightPanelLayout =
          postureState.supportsDualPane && uiState.aiConfig.foldableLayout == AiFoldableLayout.RIGHT_PANEL
        val scrimAlpha = (if (isAiRightPanelLayout) 0.28f else 0.50f) * aiP
        val dismissScrimInteraction = remember { MutableInteractionSource() }
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = scrimAlpha))
              .clickable(
                interactionSource = dismissScrimInteraction,
                indication = null,
                enabled = uiState.isAiSheetOpen,
                onClick = viewModel::closeAiSheet,
              )
              .zIndex(6f),
        )
        AiChatOverlaySheet(
          morphProgress = aiP,
          postureState = postureState,
          designConfig = uiState.designConfig,
          aiConfig = uiState.aiConfig,
          providerPresets = uiState.aiProviderPresets,
          attachedUrl = uiState.aiAttachedUrl,
          attachedTitle = uiState.aiAttachedTitle,
          draftInput = uiState.aiDraftInput,
          messages = uiState.aiMessages,
          isStreaming = uiState.isAiStreaming,
          onDraftChange = viewModel::updateAiDraft,
          onDetachPageContext = viewModel::detachAiPageContext,
          onSendMessage = viewModel::sendAiMessage,
          onStopStreaming = viewModel::stopAiStreaming,
          onClearConversation = viewModel::clearAiConversation,
          onOpenAiSettings = { settingsOrigin ->
            viewModel.closeAiSheet()
            openOverlayWithPillar(BrowserOverlay.SETTINGS_HUB, settingsOrigin, 0, SettingsPillar.AI)
          },
          onDismiss = viewModel::closeAiSheet,
          modifier = Modifier.zIndex(7f),
        )
      }

      // Site Entry Spatial Dive-In Foreground Layer (Library Items, Omnibox Suggestions, Tabletop Bookmarks)
      if (siteDiveActive) {
        if (!siteDiveFromOverlay) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f * siteDiveP))
                .zIndex(4f)
          )
          val snap = siteDiveSnapshot
          Box(
            modifier =
              Modifier.fillMaxSize()
                .spatialDiveBackground(
                  progress = siteDiveP,
                  origin = siteDiveOrigin,
                  hostSize = rootSize,
                  hostTopLeftInRoot = rootOrigin,
                )
                .background(MaterialTheme.colorScheme.background)
                .zIndex(5f)
          ) {
            if (snap != null) {
              Image(
                bitmap = snap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }
        SpatialDiveScrim(
          progress = siteDiveP,
          modifier = Modifier.zIndex(6f),
        )
        SpatialDiveForegroundHost(
          progress = siteDiveP,
          origin = siteDiveOrigin,
          hostSize = rootSize,
          hostTopLeftInRoot = rootOrigin,
          modifier = Modifier.zIndex(7f),
        ) {
          renderPrimaryBrowserSurface(Modifier)
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

@Stable
private class TabCarouselState(
  val paneIndex: Int,
) {
  var animToken by mutableIntStateOf(0)
  var activeAnimJob: Job? = null
  var baseOffsetPx by mutableFloatStateOf(0f)
  var isDragging by mutableStateOf(false)
  var isAnimating by mutableStateOf(false)
  var dragOffsetPx by mutableFloatStateOf(0f)
  var paneWidthPx by mutableFloatStateOf(1080f)
}

@Composable
private fun FloatingBarWebLayout(
  uiState: BrowserUiState,
  postureState: FoldablePostureState,
  forceDualPane: Boolean,
  viewModel: BrowserViewModel,
  tabDrawerProgress: Float,
  isReturningToWeb: Boolean,
  heroFlightTabId: String?,
  rememberedTabCardBounds: Map<String, Rect>,
  onSwipeUpDragProgress: (Float) -> Unit,
  onSwipeUpDragEnd: (paneIndex: Int, completed: Boolean, velocityY: Float) -> Unit,
  onAiSwipeProgress: (paneIndex: Int, progress: Float) -> Unit,
  onAiSwipeEnd: (paneIndex: Int, completed: Boolean, velocityY: Float) -> Unit,
  onRequestOpenOverlay: (BrowserOverlay, SpatialDiveOrigin, Int) -> Unit,
  onNavigateWithSiteDive: (String, Int, SpatialDiveOrigin) -> Unit,
  onExecuteActionForPane: (Int, ToolbarActionId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val speedScale = uiState.designConfig.animationSpeedScale
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
        if (showSecondaryPane) BrowserMotion.expressiveSpring(speedScale)
        else BrowserMotion.snappySpring(speedScale),
      label = "DualPaneSecondaryWeight",
    )
  val primaryWeightFraction = (1f - secondaryWeightFraction).coerceAtLeast(0.32f)

  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val carouselScope = rememberCoroutineScope()
  val gapPx = with(density) { 20.dp.toPx() }

  val primaryCarousel = remember { TabCarouselState(0) }
  val secondaryCarousel = remember { TabCarouselState(1) }
  val primaryCarouselAnim = remember { Animatable(0f) }
  val secondaryCarouselAnim = remember { Animatable(0f) }

  fun handleCarouselDrag(carousel: TabCarouselState, anim: Animatable<Float, *>, dx: Float) {
    if (uiState.tabs.size < 2) return
    if (!carousel.isDragging) {
      carousel.animToken += 1
      carousel.activeAnimJob?.cancel()
      carousel.activeAnimJob = null
      carousel.baseOffsetPx = if (carousel.isAnimating) anim.value else 0f
      viewModel.captureVisibleTabThumbnails()
      carousel.isDragging = true
      carousel.isAnimating = false
    }
    val maxTravel = carousel.paneWidthPx.coerceAtLeast(200f) + gapPx
    carousel.dragOffsetPx = (carousel.baseOffsetPx + dx).coerceIn(-maxTravel, maxTravel)
  }

  fun handleCarouselDragEnd(
    carousel: TabCarouselState,
    anim: Animatable<Float, *>,
    velX: Float,
  ) {
    val w = carousel.paneWidthPx.coerceAtLeast(200f)
    val maxTravel = w + gapPx
    val clampedDx = carousel.dragOffsetPx.coerceIn(-maxTravel, maxTravel)
    if (uiState.tabs.size < 2 || (abs(clampedDx) < 2f && abs(velX) < 160f)) {
      carousel.animToken += 1
      carousel.activeAnimJob?.cancel()
      carousel.activeAnimJob = null
      carousel.isDragging = false
      carousel.isAnimating = false
      carousel.dragOffsetPx = 0f
      carousel.baseOffsetPx = 0f
      carouselScope.launch { anim.snapTo(0f) }
      return
    }
    val dirRight = if (abs(clampedDx) > 8f) clampedDx > 0f else velX > 0f
    val delta = if (dirRight) -1 else 1
    val pastThreshold = abs(clampedDx) >= w * 0.30f
    val fastCommit = (dirRight && velX > 750f) || (!dirRight && velX < -750f)
    val shouldCommit = pastThreshold || fastCommit
    val targetPx = if (dirRight) (w + gapPx) else -(w + gapPx)
    val startOffsetPx = if (shouldCommit) (clampedDx - targetPx) else clampedDx

    val myToken = carousel.animToken + 1
    carousel.animToken = myToken
    carousel.activeAnimJob?.cancel()
    carousel.activeAnimJob =
      carouselScope.launch {
        anim.snapTo(startOffsetPx)
        carousel.isDragging = false
        carousel.isAnimating = true
        if (shouldCommit) {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.stepAdjacentTab(delta, targetPane = carousel.paneIndex)
        }
        try {
          anim.animateTo(
            targetValue = 0f,
            animationSpec = ExpressiveSprings.spatial(speedScale),
            initialVelocity = velX.coerceIn(-6000f, 6000f),
          )
        } finally {
          if (carousel.animToken == myToken) {
            carousel.isAnimating = false
            carousel.dragOffsetPx = 0f
            carousel.baseOffsetPx = 0f
          }
        }
      }
  }

  val primaryCarouselOffsetPx =
    when {
      primaryCarousel.isDragging -> primaryCarousel.dragOffsetPx
      primaryCarousel.isAnimating -> primaryCarouselAnim.value
      else -> 0f
    }
  val secondaryCarouselOffsetPx =
    when {
      secondaryCarousel.isDragging -> secondaryCarousel.dragOffsetPx
      secondaryCarousel.isAnimating -> secondaryCarouselAnim.value
      else -> 0f
    }

  val statusInsetModifier =
    if (uiState.settings.hideStatusBar) Modifier else Modifier.statusBarsPadding()

  var hostOriginInRoot by remember { mutableStateOf(Offset.Zero) }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .then(statusInsetModifier)
        .onGloballyPositioned { coords ->
          hostOriginInRoot = coords.boundsInRoot().topLeft
        }
  ) {
    val totalWidthInt = constraints.maxWidth.coerceAtLeast(1)
    val totalHeightInt = constraints.maxHeight.coerceAtLeast(1)
    val totalWidthPx = totalWidthInt.toFloat()
    val totalHeightPx = totalHeightInt.toFloat()

    val activeHeroRect = (heroFlightTabId ?: uiState.focusedTab?.id)?.let { rememberedTabCardBounds[it] }
    val endWidth =
      if (activeHeroRect != null && activeHeroRect.width > 4f) activeHeroRect.width.coerceAtLeast(16f)
      else (totalWidthPx * 0.44f).coerceAtLeast(120f)
    val endHeight =
      if (activeHeroRect != null && activeHeroRect.height > 4f) activeHeroRect.height.coerceAtLeast(16f)
      else (endWidth / 1.12f).coerceAtMost(totalHeightPx * 0.45f)
    val endLeft =
      if (activeHeroRect != null && activeHeroRect.width > 4f) (activeHeroRect.left - hostOriginInRoot.x)
      else (totalWidthPx - endWidth) * 0.5f
    val endTop =
      if (activeHeroRect != null && activeHeroRect.height > 4f) (activeHeroRect.top - hostOriginInRoot.y)
      else (totalHeightPx - endHeight) * 0.38f

    val p = tabDrawerProgress.coerceIn(0f, 1f)
    val isHeroActive = p > 0.004f
    val arcPhase = sin(p * PI).toFloat()
    val arcLiftPx = arcPhase * with(density) { 12.dp.toPx() }
    val curLeft = endLeft * p
    val curTop = endTop * p - arcLiftPx
    val curWidth = (totalWidthPx + (endWidth - totalWidthPx) * p).coerceAtLeast(1f)
    val curHeight = (totalHeightPx + (endHeight - totalHeightPx) * p).coerceAtLeast(1f)

    val uniformScale =
      if (isHeroActive) {
        maxOf(curWidth / totalWidthPx, curHeight / totalHeightPx).coerceIn(0.12f, 1f)
      } else {
        1f
      }
    val clipWInt =
      if (isHeroActive) (curWidth / uniformScale).roundToInt().coerceIn(1, totalWidthInt)
      else totalWidthInt
    val clipHInt =
      if (isHeroActive) (curHeight / uniformScale).roundToInt().coerceIn(1, totalHeightInt)
      else totalHeightInt

    val visualCornerDp =
      computeHeroWindowCornerDp(
        progress = p,
        barCornerRadiusDp = uiState.designConfig.barCornerRadiusDp,
        cardCornerRadiusDp = uiState.designConfig.cardCornerRadiusDp,
      )
    val preScaleShape =
      if (isHeroActive && visualCornerDp > 0.05f) {
        RoundedCornerShape((visualCornerDp / uniformScale).coerceIn(0f, 180f).dp)
      } else {
        RectangleShape
      }
    val heroAlpha = computeHeroWindowAlpha(progress = p, isReturningToWeb = isReturningToWeb)
    val heroShadowPx =
      if (isHeroActive) with(density) { (22f * arcPhase + 10f * p).dp.toPx() } else 0f

    val viewportHeroModifier =
      Modifier
        .graphicsLayer {
          translationX = if (isHeroActive) curLeft else 0f
          translationY = if (isHeroActive) curTop else 0f
          scaleX = uniformScale
          scaleY = uniformScale
          transformOrigin = TransformOrigin(0f, 0f)
          shape = preScaleShape
          clip = isHeroActive && visualCornerDp > 0.05f
          alpha = heroAlpha
          shadowElevation = heroShadowPx
        }
        .layout { measurable, _ ->
          val fixed = Constraints.fixed(totalWidthInt, totalHeightInt)
          val placeable = measurable.measure(fixed)
          layout(clipWInt, clipHInt) {
            placeable.place(((clipWInt - totalWidthInt) / 2), 0)
          }
        }
        .background(MaterialTheme.colorScheme.background)

    Row(modifier = viewportHeroModifier) {
      // [Left / Single Pane]
      Box(
        modifier =
          Modifier.weight(primaryWeightFraction)
            .fillMaxHeight()
            .clip(RectangleShape),
      ) {
        if (primaryTab != null) {
          BrowserTabContentPane(
            tab = primaryTab,
            paneIndex = 0,
            isFocusedPane = !isDualPane || uiState.activePane == 0,
            showSplitHeader = isDualPane,
            postureState = postureState,
            uiState = uiState,
            viewModel = viewModel,
            carouselOffsetPx = primaryCarouselOffsetPx,
            heroTransitionProgress = p,
            onPaneWidthMeasured = { w -> primaryCarousel.paneWidthPx = w },
            onFocusPane = { viewModel.setActivePane(0) },
            onRequestOpenOverlay = onRequestOpenOverlay,
            modifier = Modifier.fillMaxSize(),
          )
        }

        // 분할 모드: 왼쪽 창 하단 중앙 독립 플로팅 바
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
          androidx.compose.animation.AnimatedVisibility(
            visible = showSecondaryPane,
            enter =
              fadeIn(BrowserMotion.expressiveSpring(speedScale)) +
                scaleIn(BrowserMotion.expressiveSpring(speedScale), initialScale = 0.92f),
            exit =
              fadeOut(BrowserMotion.snappySpring(speedScale)) +
                scaleOut(BrowserMotion.snappySpring(speedScale), targetScale = 0.92f),
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
              expandBarOnUrlEdit = uiState.settings.expandBarOnUrlEdit,
              splitGestureEnabled = uiState.settings.splitGestureEnabled,
              splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
              swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
              supportsDualPane = postureState.supportsDualPane,
              heroDragLiftProgress = p,
              onExpandBar = viewModel::expandFloatingBar,
              onCollapseBar = viewModel::collapseFloatingBar,
              onSwipeUpOpenTabs = { onExecuteActionForPane(0, ToolbarActionId.TABS) },
              onSwipeUpDragProgress = onSwipeUpDragProgress,
              onSwipeUpDragEnd = { completed, velY ->
                onSwipeUpDragEnd(0, completed, velY)
              },
              onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta, targetPane = 0) },
              onHorizontalTabDrag = { dx -> handleCarouselDrag(primaryCarousel, primaryCarouselAnim, dx) },
              onHorizontalTabDragEnd = { _, velX ->
                handleCarouselDragEnd(primaryCarousel, primaryCarouselAnim, velX)
              },
              onTriggerSplitGesture = { dirRight ->
                viewModel.splitActiveTabWithGesture(dirRight, sourcePane = 0)
              },
              onCloseSplitPane = { viewModel.closeSplitKeepPane(keepPaneIndex = 1) },
              onAiSwipeProgress = { p -> onAiSwipeProgress(0, p) },
              onAiSwipeEnd = { completed, velY -> onAiSwipeEnd(0, completed, velY) },
              onFocusOmnibox = { viewModel.focusOmnibox(targetPane = 0) },
              onBlurOmnibox = viewModel::blurOmnibox,
              onOmniboxQueryChange = viewModel::updateOmniboxQuery,
              onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, targetPane = 0) },
              onSelectSuggestion = { url, origin -> onNavigateWithSiteDive(url, 0, origin) },
              onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
              onReloadOrStop = { viewModel.reloadOrStopInPane(0) },
              onExecuteAction = { action -> onExecuteActionForPane(0, action) },
              onOpenSettings = { origin -> onRequestOpenOverlay(BrowserOverlay.SETTINGS_HUB, origin, 0) },
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
              .clip(RectangleShape)
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
            carouselOffsetPx = secondaryCarouselOffsetPx,
            heroTransitionProgress = p,
            onPaneWidthMeasured = { w -> secondaryCarousel.paneWidthPx = w },
            onFocusPane = { viewModel.setActivePane(1) },
            onRequestOpenOverlay = onRequestOpenOverlay,
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
            expandBarOnUrlEdit = uiState.settings.expandBarOnUrlEdit,
            splitGestureEnabled = uiState.settings.splitGestureEnabled,
            splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
            swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
            supportsDualPane = postureState.supportsDualPane,
            heroDragLiftProgress = p,
            onExpandBar = viewModel::expandFloatingBar,
            onCollapseBar = viewModel::collapseFloatingBar,
            onSwipeUpOpenTabs = { onExecuteActionForPane(1, ToolbarActionId.TABS) },
            onSwipeUpDragProgress = onSwipeUpDragProgress,
            onSwipeUpDragEnd = { completed, velY ->
              onSwipeUpDragEnd(1, completed, velY)
            },
            onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta, targetPane = 1) },
            onHorizontalTabDrag = { dx -> handleCarouselDrag(secondaryCarousel, secondaryCarouselAnim, dx) },
            onHorizontalTabDragEnd = { _, velX ->
              handleCarouselDragEnd(secondaryCarousel, secondaryCarouselAnim, velX)
            },
            onTriggerSplitGesture = { dirRight ->
              viewModel.splitActiveTabWithGesture(dirRight, sourcePane = 1)
            },
            onCloseSplitPane = { viewModel.closeSplitKeepPane(keepPaneIndex = 0) },
            onAiSwipeProgress = { p -> onAiSwipeProgress(1, p) },
            onAiSwipeEnd = { completed, velY -> onAiSwipeEnd(1, completed, velY) },
            onFocusOmnibox = { viewModel.focusOmnibox(targetPane = 1) },
            onBlurOmnibox = viewModel::blurOmnibox,
            onOmniboxQueryChange = viewModel::updateOmniboxQuery,
            onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, targetPane = 1) },
            onSelectSuggestion = { url, origin -> onNavigateWithSiteDive(url, 1, origin) },
            onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
            onReloadOrStop = { viewModel.reloadOrStopInPane(1) },
            onExecuteAction = { action -> onExecuteActionForPane(1, action) },
            onOpenSettings = { origin -> onRequestOpenOverlay(BrowserOverlay.SETTINGS_HUB, origin, 0) },
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
        fadeIn(BrowserMotion.expressiveSpring(speedScale)) +
          scaleIn(BrowserMotion.expressiveSpring(speedScale), initialScale = 0.94f),
      exit =
        fadeOut(BrowserMotion.snappySpring(speedScale)) +
          scaleOut(BrowserMotion.snappySpring(speedScale), targetScale = 0.94f),
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
        expandBarOnUrlEdit = uiState.settings.expandBarOnUrlEdit,
        splitGestureEnabled = uiState.settings.splitGestureEnabled,
        splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
        swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
        supportsDualPane = postureState.supportsDualPane,
        heroDragLiftProgress = p,
        onExpandBar = viewModel::expandFloatingBar,
        onCollapseBar = viewModel::collapseFloatingBar,
        onSwipeUpOpenTabs = { onExecuteActionForPane(0, ToolbarActionId.TABS) },
        onSwipeUpDragProgress = onSwipeUpDragProgress,
        onSwipeUpDragEnd = { completed, velY ->
          onSwipeUpDragEnd(0, completed, velY)
        },
        onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta, targetPane = 0) },
        onHorizontalTabDrag = { dx -> handleCarouselDrag(primaryCarousel, primaryCarouselAnim, dx) },
        onHorizontalTabDragEnd = { _, velX ->
          handleCarouselDragEnd(primaryCarousel, primaryCarouselAnim, velX)
        },
        onTriggerSplitGesture = { dirRight ->
          viewModel.splitActiveTabWithGesture(dirRight, sourcePane = 0)
        },
        onAiSwipeProgress = { p -> onAiSwipeProgress(0, p) },
        onAiSwipeEnd = { completed, velY -> onAiSwipeEnd(0, completed, velY) },
        onFocusOmnibox = { viewModel.focusOmnibox(targetPane = 0) },
        onBlurOmnibox = viewModel::blurOmnibox,
        onOmniboxQueryChange = viewModel::updateOmniboxQuery,
        onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, targetPane = 0) },
        onSelectSuggestion = { url, origin -> onNavigateWithSiteDive(url, 0, origin) },
        onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
        onReloadOrStop = { viewModel.reloadOrStopInPane(0) },
        onExecuteAction = { action -> onExecuteActionForPane(0, action) },
        onOpenSettings = { origin -> onRequestOpenOverlay(BrowserOverlay.SETTINGS_HUB, origin, 0) },
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
  onAiSwipeProgress: (Float) -> Unit,
  onAiSwipeEnd: (completed: Boolean, velocityY: Float) -> Unit,
  onRequestOpenOverlay: (BrowserOverlay, SpatialDiveOrigin, Int) -> Unit,
  onNavigateWithSiteDive: (String, Int, SpatialDiveOrigin) -> Unit,
  onExecuteActionForPane: (Int, ToolbarActionId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val topHeight = postureState.hingeTopDp.coerceAtLeast(220.dp)
  val hingeGap = postureState.hingeHeightDp.coerceAtLeast(8.dp)
  val tab = uiState.focusedTab ?: uiState.primaryTab
  val isBookmarked =
    tab?.let { t -> uiState.bookmarks.any { it.url == t.url } } ?: false
  val isDesktop = isDesktopModeForTab(tab, uiState)
  val statusInsetModifier =
    if (uiState.settings.hideStatusBar) Modifier else Modifier.statusBarsPadding()

  Box(modifier = modifier.fillMaxSize().then(statusInsetModifier)) {
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
            onRequestOpenOverlay = onRequestOpenOverlay,
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
        onNavigateBookmark = { url, origin ->
          onNavigateWithSiteDive(url, uiState.activePane, origin)
        },
        onOpenLibrary = { onRequestOpenOverlay(BrowserOverlay.LIBRARY_PANEL, SpatialDiveOrigin(), 0) },
        onOpenInspector = { onRequestOpenOverlay(BrowserOverlay.SETTINGS_HUB, SpatialDiveOrigin(), 0) },
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
        expandBarOnUrlEdit = uiState.settings.expandBarOnUrlEdit,
        splitGestureEnabled = uiState.settings.splitGestureEnabled,
        splitGestureSensitivity = uiState.settings.splitGestureSensitivity,
        swipeUpOpenTabs = uiState.settings.swipeUpOpenTabs,
        supportsDualPane = postureState.supportsDualPane,
        onExpandBar = viewModel::expandFloatingBar,
        onCollapseBar = viewModel::collapseFloatingBar,
        onSwipeUpOpenTabs = { onExecuteActionForPane(uiState.activePane, ToolbarActionId.TABS) },
        onSwipeTabDelta = { delta -> viewModel.stepAdjacentTab(delta) },
        onAiSwipeProgress = onAiSwipeProgress,
        onAiSwipeEnd = onAiSwipeEnd,
        onFocusOmnibox = { viewModel.focusOmnibox(uiState.activePane) },
        onBlurOmnibox = viewModel::blurOmnibox,
        onOmniboxQueryChange = viewModel::updateOmniboxQuery,
        onOmniboxSubmit = { input -> viewModel.submitOmnibox(input, uiState.omniboxTargetPane) },
        onSelectSuggestion = { url, origin ->
          onNavigateWithSiteDive(url, uiState.omniboxTargetPane, origin)
        },
        onSelectSearchEngine = { engineId -> viewModel.updateSetting("searchEngineId", engineId) },
        onReloadOrStop = viewModel::reloadOrStopFocusedPane,
        onExecuteAction = { action -> onExecuteActionForPane(uiState.activePane, action) },
        onOpenSettings = { origin -> onRequestOpenOverlay(BrowserOverlay.SETTINGS_HUB, origin, 0) },
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
  carouselOffsetPx: Float = 0f,
  heroTransitionProgress: Float = 0f,
  onPaneWidthMeasured: (Float) -> Unit = {},
  onFocusPane: () -> Unit,
  onRequestOpenOverlay: (BrowserOverlay, SpatialDiveOrigin, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor =
    if (showSplitHeader && isFocusedPane) MaterialTheme.colorScheme.primary
    else if (showSplitHeader) MaterialTheme.colorScheme.outline
    else MaterialTheme.colorScheme.background

  val designConfig = LocalDesignConfig.current
  val speedScale = designConfig.animationSpeedScale
  val cardShape = designConfig.cardInnerShape()

  Surface(
    border = if (showSplitHeader) BorderStroke(1.5.dp, borderColor) else null,
    shape =
      if (showSplitHeader) cardShape else RectangleShape,
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

      val density = LocalDensity.current
      var paneOrigin by remember { mutableStateOf(Offset.Zero) }
      var paneSize by remember { mutableStateOf(IntSize(1080, 1920)) }
      var bookmarkDiveKey by remember(tab.id) { mutableIntStateOf(0) }
      var bookmarkDiveActive by remember(tab.id) { mutableStateOf(false) }
      var bookmarkDiveOrigin by remember(tab.id) { mutableStateOf(SpatialDiveOrigin()) }
      val bookmarkDiveAnim = remember(tab.id, bookmarkDiveKey) { Animatable(0f) }

      LaunchedEffect(tab.id, bookmarkDiveKey) {
        if (bookmarkDiveActive) {
          bookmarkDiveAnim.animateTo(1f, ExpressiveSprings.spatial(speedScale))
          bookmarkDiveActive = false
        }
      }

      val searchLabel =
        uiState.searchEngines.firstOrNull { it.id == uiState.settings.searchEngineId }?.label
          ?: "DuckDuckGo"
      val dohLabel =
        uiState.dohProviders.firstOrNull { it.id == uiState.settings.dohProviderId }?.label
          ?: "Quad9"

      val renderStartPage: @Composable (Modifier) -> Unit = { mod ->
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
          onNavigateBookmark = { url, origin ->
            onFocusPane()
            if (origin.isValid()) {
              bookmarkDiveOrigin = origin
              bookmarkDiveKey += 1
              bookmarkDiveActive = true
            }
            viewModel.navigateUrlInPane(url, paneIndex)
          },
          onOpenInspector = { origin ->
            onRequestOpenOverlay(BrowserOverlay.SETTINGS_HUB, origin, 0)
          },
          onToggleSplit = { viewModel.toggleSplitMode() },
          onScrollDelta = viewModel::onViewportScrollDelta,
          modifier = mod,
        )
      }

      val gapDp = 20.dp
      val gapPx = with(density) { gapDp.toPx() }
      val hasActiveCarousel = abs(carouselOffsetPx) > 0.5f && uiState.tabs.size > 1
      val adjTab =
        remember(hasActiveCarousel, carouselOffsetPx > 0f, tab.id, uiState.tabs) {
          if (!hasActiveCarousel) {
            null
          } else {
            val tabs = uiState.tabs
            val idx = tabs.indexOfFirst { it.id == tab.id }
            if (idx >= 0 && tabs.size > 1) {
              val delta = if (carouselOffsetPx > 0f) -1 else 1
              val nextIdx = ((idx + delta) % tabs.size + tabs.size) % tabs.size
              tabs[nextIdx]
            } else {
              null
            }
          }
        }
      val paneWidthPx = paneSize.width.toFloat().coerceAtLeast(1f)
      val slideProgress =
        if (hasActiveCarousel) (abs(carouselOffsetPx) / (paneWidthPx + gapPx)).coerceIn(0f, 1f)
        else 0f
      val cornerRamp = (slideProgress * 5f).coerceIn(0f, 1f)
      val slidingCornerDp = (designConfig.cardCornerRadiusDp.coerceIn(10f, 24f) * cornerRamp).dp
      val activeWindowShape =
        if (hasActiveCarousel) RoundedCornerShape(slidingCornerDp) else RectangleShape

      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              paneOrigin = coords.boundsInRoot().topLeft
              paneSize = coords.size
              if (coords.size.width > 0) {
                onPaneWidthMeasured(coords.size.width.toFloat())
              }
            }
            .then(
              if (hasActiveCarousel) {
                Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
              } else {
                Modifier
              }
            ),
      ) {
        // Incoming adjacent tab window pulled in with physical 20.dp gap
        if (hasActiveCarousel && adjTab != null && adjTab.id != tab.id) {
          val adjOffsetPx =
            if (carouselOffsetPx > 0f) {
              carouselOffsetPx - (paneWidthPx + gapPx)
            } else {
              carouselOffsetPx + (paneWidthPx + gapPx)
            }
          Box(
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer { translationX = adjOffsetPx }
                .shadow(
                  elevation = (20f * cornerRamp).dp,
                  shape = activeWindowShape,
                  clip = false,
                )
                .clip(activeWindowShape)
                .background(MaterialTheme.colorScheme.background),
          ) {
            CarouselIncomingTabViewport(
              tab = adjTab,
              thumbnail = viewModel.tabThumbnails[adjTab.id],
              favicon = viewModel.tabFavicons[adjTab.id],
              uiState = uiState,
              viewModel = viewModel,
              renderStartPage = renderStartPage,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        // Current active tab window sliding 1:1 with the finger
        val currentWindowModifier =
          if (hasActiveCarousel) {
            Modifier.fillMaxSize()
              .graphicsLayer { translationX = carouselOffsetPx }
              .shadow(
                elevation = (20f * cornerRamp).dp,
                shape = activeWindowShape,
                clip = false,
              )
              .clip(activeWindowShape)
              .background(MaterialTheme.colorScheme.background)
          } else {
            Modifier.fillMaxSize()
          }

        Box(modifier = currentWindowModifier) {
          when (tab.url) {
            "about:mb" -> {
              renderStartPage(Modifier.fillMaxSize())
            }
            "about:blank" -> {
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(onClick = onFocusPane),
              )
            }
            else -> {
              val renderPolicy =
                remember(tab.url, uiState.settings, uiState.sitePermissions) {
                  viewModel.resolveRenderPolicyForTab(tab.url)
                }
              val diveP = if (bookmarkDiveActive) bookmarkDiveAnim.value.coerceIn(0f, 1f) else 1f
              if (bookmarkDiveActive && diveP < 0.996f) {
                Box(
                  modifier =
                    Modifier.fillMaxSize()
                      .background(Color.Black.copy(alpha = 0.88f * diveP))
                )
                renderStartPage(
                  Modifier.fillMaxSize().spatialDiveBackground(
                    progress = diveP,
                    origin = bookmarkDiveOrigin,
                    hostSize = paneSize,
                    hostTopLeftInRoot = paneOrigin,
                  )
                )
                SpatialDiveScrim(progress = diveP)
                SpatialDiveForegroundHost(
                  progress = diveP,
                  origin = bookmarkDiveOrigin,
                  hostSize = paneSize,
                  hostTopLeftInRoot = paneOrigin,
                  modifier = Modifier.fillMaxSize(),
                ) {
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
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              } else {
                Box(modifier = Modifier.fillMaxSize()) {
                  if (heroTransitionProgress > 0.004f) {
                    val cachedThumb = viewModel.tabThumbnails[tab.id]
                    if (cachedThumb != null) {
                      Image(
                        bitmap = cachedThumb,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                      )
                    }
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
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
            }
          }
        }

        // Physical 20.dp frosted blur & directional drop-shadow seam between windows
        if (hasActiveCarousel && adjTab != null && adjTab.id != tab.id) {
          val seamX =
            if (carouselOffsetPx > 0f) {
              carouselOffsetPx - gapPx
            } else {
              paneWidthPx + carouselOffsetPx
            }
          Box(
            modifier =
              Modifier.width(gapDp)
                .fillMaxHeight()
                .graphicsLayer { translationX = seamX }
                .zIndex(2f),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                      Modifier.blur(18.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    } else {
                      Modifier
                    }
                  )
                  .background(
                    brush =
                      Brush.horizontalGradient(
                        colors =
                          listOf(
                            Color.Black.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.24f),
                          ),
                      ),
                  ),
            )
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
              modifier = Modifier.width(3.dp).height(44.dp),
            ) {}
          }
        }
      }
    }
  }
}

@Composable
private fun CarouselIncomingTabViewport(
  tab: TabUiModel,
  thumbnail: ImageBitmap?,
  favicon: ImageBitmap?,
  uiState: BrowserUiState,
  viewModel: BrowserViewModel,
  renderStartPage: @Composable (Modifier) -> Unit,
  modifier: Modifier = Modifier,
) {
  when (tab.url) {
    "about:mb" -> {
      renderStartPage(modifier)
    }
    "about:blank" -> {
      Box(modifier = modifier.background(MaterialTheme.colorScheme.background))
    }
    else -> {
      val renderPolicy =
        remember(tab.url, uiState.settings, uiState.sitePermissions) {
          viewModel.resolveRenderPolicyForTab(tab.url)
        }
      Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
      ) {
        if (thumbnail != null) {
          Image(
            bitmap = thumbnail,
            contentDescription = tab.title.ifBlank { tab.displayHost },
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Column(
            modifier =
              Modifier.fillMaxSize()
                .padding(horizontal = BrowserSpacing.Grid3),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Surface(
              shape = LocalDesignConfig.current.iconShape(),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.size(56.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (favicon != null) {
                  Image(
                    bitmap = favicon,
                    contentDescription = tab.displayHost,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(30.dp).clip(CircleShape),
                  )
                } else {
                  Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = tab.displayHost,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
            }
            Spacer(Modifier.height(BrowserSpacing.Grid1_5))
            Text(
              text = tab.title.ifBlank { tab.displayHost },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = tab.displayHost,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
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
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}
