package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.mybrowser.appui.foldable.BrowserPostureMode
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.network.AiExecutionMode
import com.mybrowser.appui.network.AiFoldableLayout
import com.mybrowser.appui.ui.ExpressiveSprings
import com.mybrowser.appui.ui.OmniboxLongPressDirection
import com.mybrowser.appui.ui.SpatialDiveOrigin
import com.mybrowser.appui.ui.computeDiffusedAmbientElevationDp
import com.mybrowser.appui.ui.computeHeroWindowAlpha
import com.mybrowser.appui.ui.computeHeroWindowCornerDp
import com.mybrowser.appui.ui.computeOutwardHaloBlurDp
import com.mybrowser.appui.ui.computeSpatialDiveBackgroundCornerDp
import com.mybrowser.appui.ui.computeSpatialDiveCornerDp
import com.mybrowser.appui.ui.computeTabSwitcherBackdropDepth
import com.mybrowser.appui.ui.resolveLongPressDirection
import com.mybrowser.appui.ui.unscaleRectFromCenter
import com.mybrowser.appui.viewmodel.BrowserViewModel
import com.mybrowser.appui.viewmodel.TabSortMode
import com.mybrowser.appui.viewmodel.TabUiModel
import com.mybrowser.appui.viewmodel.TabViewMode
import com.mybrowser.appui.viewmodel.computeKeepAliveTabIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MyBrowser(MB)", appName)
  }

  @Test
  fun `posture state separates passport unfold from short landscape phone`() {
    val passportUnfolded =
      FoldablePostureState(
        mode = BrowserPostureMode.PASSPORT_FLAT,
        windowWidthDp = 640.dp,
        windowHeightDp = 460.dp,
        aspectRatio = 640f / 460f,
      )
    assertTrue(passportUnfolded.supportsDualPane)
    assertTrue(passportUnfolded.isPassportGeometry)

    val shortPhoneLandscape =
      FoldablePostureState(
        mode = BrowserPostureMode.COVER_COMPACT,
        windowWidthDp = 780.dp,
        windowHeightDp = 360.dp,
        aspectRatio = 780f / 360f,
      )
    assertFalse(shortPhoneLandscape.supportsDualPane)
    assertFalse(shortPhoneLandscape.isPassportGeometry)
  }

  @Test
  fun `viewModel keeps single pane on unfold by default and splits via left or right gesture`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BrowserViewModel(app)

    val initialState = vm.uiState.value
    assertFalse(initialState.settings.autoDualPaneUnfold)
    assertFalse(initialState.splitMode)
    val startTabId = initialState.primaryTabId

    // Right swipe after omnibox long-press -> original tab moves right (pane 1), new tab on left
    vm.splitActiveTabWithGesture(directionRight = true, sourcePane = 0)
    val rightSplit = vm.uiState.value
    assertTrue(rightSplit.splitMode)
    assertEquals(1, rightSplit.activePane)
    assertEquals(startTabId, rightSplit.secondaryTabId)
    assertNotNull(rightSplit.primaryTabId)
    assertNotEquals(startTabId, rightSplit.primaryTabId)

    // Close split keeping secondary (original) tab, then test left swipe
    vm.closeSplitKeepPane(keepPaneIndex = 1)
    assertFalse(vm.uiState.value.splitMode)
    assertEquals(startTabId, vm.uiState.value.primaryTabId)

    vm.onWebViewNavigationUpdate(
      tabId = startTabId,
      url = "https://example.org",
      title = "Example",
      isLoading = false,
      progress = 100,
      caps = com.mybrowser.appui.webview.WebViewNavCapabilities(),
    )
    vm.openFindInPage()
    vm.updateFindQuery("test")
    assertTrue(vm.uiState.value.isFindInPageVisible)

    // Left swipe after omnibox long-press -> original tab stays left (pane 0), new tab on right, resets find-in-page
    vm.splitActiveTabWithGesture(directionRight = false, sourcePane = 0)
    val leftSplit = vm.uiState.value
    assertTrue(leftSplit.splitMode)
    assertEquals(0, leftSplit.activePane)
    assertEquals(startTabId, leftSplit.primaryTabId)
    assertNotNull(leftSplit.secondaryTabId)
    assertNotEquals(startTabId, leftSplit.secondaryTabId)
    assertFalse(leftSplit.isFindInPageVisible)
    assertEquals("", leftSplit.findQuery)
  }

  @Test
  fun `tabViewMode and defaultTabSortMode settings update live state and sync sort`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BrowserViewModel(app)

    assertEquals(TabViewMode.THUMBNAIL_GRID, vm.uiState.value.settings.tabViewMode)
    vm.updateSetting("tabViewMode", TabViewMode.COMPACT_LIST.id)
    assertEquals(TabViewMode.COMPACT_LIST, vm.uiState.value.settings.tabViewMode)

    vm.updateSetting("tabViewMode", TabViewMode.THUMBNAIL_GRID.id)
    assertEquals(TabViewMode.THUMBNAIL_GRID, vm.uiState.value.settings.tabViewMode)

    vm.setDefaultTabSortMode(TabSortMode.DOMAIN)
    assertEquals(TabSortMode.DOMAIN, vm.uiState.value.settings.defaultTabSortMode)
    assertEquals(TabSortMode.DOMAIN, vm.uiState.value.tabSortMode)

    vm.updateSetting("defaultTabSortMode", TabSortMode.PINNED_FIRST.id)
    assertEquals(TabSortMode.PINNED_FIRST, vm.uiState.value.settings.defaultTabSortMode)
    assertEquals(TabSortMode.PINNED_FIRST, vm.uiState.value.tabSortMode)
  }

  @Test
  fun `tab favicon cache updates and cleans up on closeTab`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BrowserViewModel(app)

    vm.createNewTab("https://example.org")
    val createdTabId = vm.uiState.value.primaryTabId
    val icon = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)

    vm.onWebViewIconUpdate(createdTabId, icon)
    assertNotNull(vm.tabFavicons[createdTabId])

    vm.closeTab(createdTabId)
    assertNull(vm.tabFavicons[createdTabId])
  }

  @Test
  fun `expressive springs and spatial dive origin validate physics and bounds`() {
    assertEquals(0.78f, ExpressiveSprings.Spatial.dampingRatio, 0.001f)
    assertEquals(165f, ExpressiveSprings.Spatial.stiffness, 0.001f)
    assertEquals(0.65f, ExpressiveSprings.Micro.dampingRatio, 0.001f)
    assertEquals(270f, ExpressiveSprings.Micro.stiffness, 0.001f)

    // Speed multiplier scales stiffness quadratically (base * s^2) and clamps to [0.5, 2.0]
    val slowSpatial = ExpressiveSprings.spatial<Float>(0.2f)
    assertEquals(0.78f, slowSpatial.dampingRatio, 0.001f)
    assertEquals(165f * 0.25f, slowSpatial.stiffness, 0.01f)

    val fastMicro = ExpressiveSprings.micro<Float>(1.5f)
    assertEquals(0.65f, fastMicro.dampingRatio, 0.001f)
    assertEquals(270f * 2.25f, fastMicro.stiffness, 0.01f)

    val cappedSpatial = ExpressiveSprings.spatial<Float>(3.5f)
    assertEquals(165f * 4.0f, cappedSpatial.stiffness, 0.01f)

    val emptyOrigin = SpatialDiveOrigin()
    assertFalse(emptyOrigin.isValid())

    val validOrigin = SpatialDiveOrigin(boundsInRoot = Rect(12f, 24f, 220f, 340f), cornerRadiusDp = 24f)
    assertTrue(validOrigin.isValid())
    assertEquals(24f, validOrigin.cornerRadiusDp, 0.001f)
  }

  @Test
  fun `viewModel propagates animationSpeedScale expandBarOnUrlEdit and forceZoomEnabled`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BrowserViewModel(app)

    assertEquals(1.0f, vm.uiState.value.designConfig.animationSpeedScale, 0.001f)
    assertTrue(vm.uiState.value.settings.expandBarOnUrlEdit)
    assertFalse(vm.uiState.value.settings.forceZoomEnabled)
    assertFalse(vm.resolveRenderPolicyForTab("https://example.org").forceZoomEnabled)

    vm.updateDesignParam("animationSpeedScale", "1.75")
    assertEquals(1.75f, vm.uiState.value.designConfig.animationSpeedScale, 0.001f)

    vm.updateSetting("expandBarOnUrlEdit", "false")
    assertFalse(vm.uiState.value.settings.expandBarOnUrlEdit)

    vm.updateSetting("forceZoomEnabled", "true")
    assertTrue(vm.uiState.value.settings.forceZoomEnabled)
    assertTrue(vm.resolveRenderPolicyForTab("https://example.org").forceZoomEnabled)
  }

  @Test
  fun `floating bar outward halo and spatial dive corners sync cleanly`() {
    // Multi-layer diffused ambient shadow is always >= 28.dp even at 0 blur
    assertTrue(computeDiffusedAmbientElevationDp(blurRadiusDp = 0f, isFocused = false) >= 28f)
    assertEquals(38f, computeDiffusedAmbientElevationDp(blurRadiusDp = 32f, isFocused = false), 0.01f)
    assertEquals(44f, computeDiffusedAmbientElevationDp(blurRadiusDp = 32f, isFocused = true), 0.01f)
    assertTrue(computeOutwardHaloBlurDp(blurRadiusDp = 18f, isFocused = true) > computeOutwardHaloBlurDp(blurRadiusDp = 18f, isFocused = false))

    // Foreground expanding card morphs corners from originCornerDp -> barCornerRadiusDp
    assertEquals(20f, computeSpatialDiveCornerDp(originCornerDp = 20f, barCornerRadiusDp = 32f, progress = 0f), 0.01f)
    assertEquals(26f, computeSpatialDiveCornerDp(originCornerDp = 20f, barCornerRadiusDp = 32f, progress = 0.5f), 0.01f)
    assertEquals(32f, computeSpatialDiveCornerDp(originCornerDp = 20f, barCornerRadiusDp = 32f, progress = 1f), 0.01f)

    // Zooming background corners remain synced to barCornerRadiusDp throughout dive
    assertEquals(0f, computeSpatialDiveBackgroundCornerDp(barCornerRadiusDp = 32f, progress = 0f), 0.01f)
    assertEquals(32f, computeSpatialDiveBackgroundCornerDp(barCornerRadiusDp = 32f, progress = 0.25f), 0.01f)
    assertEquals(32f, computeSpatialDiveBackgroundCornerDp(barCornerRadiusDp = 32f, progress = 1f), 0.01f)
  }

  @Test
  fun `hero tab fly unifies corner sync crossfade and backdrop depth across enter and return`() {
    // 1. Corners engage barCornerRadiusDp immediately at p = 0.01f and interpolate to cardCornerRadiusDp
    assertEquals(0f, computeHeroWindowCornerDp(0f, barCornerRadiusDp = 28f, cardCornerRadiusDp = 20f), 0.01f)
    assertEquals(28f, computeHeroWindowCornerDp(0.01f, barCornerRadiusDp = 28f, cardCornerRadiusDp = 20f), 0.01f)
    assertEquals(24f, computeHeroWindowCornerDp(0.505f, barCornerRadiusDp = 28f, cardCornerRadiusDp = 20f), 0.05f)
    assertEquals(20f, computeHeroWindowCornerDp(1f, barCornerRadiusDp = 28f, cardCornerRadiusDp = 20f), 0.01f)

    // 2. Entry crossfades alpha 1f -> 0f only once progress >= 0.95f; return stays opaque 1f
    assertEquals(1f, computeHeroWindowAlpha(progress = 0.94f, isReturningToWeb = false), 0.001f)
    assertEquals(1f, computeHeroWindowAlpha(progress = 0.95f, isReturningToWeb = false), 0.001f)
    assertEquals(0.5f, computeHeroWindowAlpha(progress = 0.975f, isReturningToWeb = false), 0.02f)
    assertEquals(0f, computeHeroWindowAlpha(progress = 1f, isReturningToWeb = false), 0.001f)
    assertEquals(1f, computeHeroWindowAlpha(progress = 1f, isReturningToWeb = true), 0.001f)
    assertEquals(1f, computeHeroWindowAlpha(progress = 0.5f, isReturningToWeb = true), 0.001f)

    // 3. TabSwitcher backdrop scales 0.92f <-> 1.0f and blurs 0.dp -> 24.dp when returning (1f -> 0f)
    val settledDepth = computeTabSwitcherBackdropDepth(progress = 1f, isReturningToWeb = false)
    assertEquals(1f, settledDepth.scale, 0.001f)
    assertEquals(0f, settledDepth.blurDp, 0.001f)

    val returningMidDepth = computeTabSwitcherBackdropDepth(progress = 0.5f, isReturningToWeb = true)
    assertEquals(0.96f, returningMidDepth.scale, 0.001f)
    assertTrue(returningMidDepth.blurDp > 12f)

    val returningEndDepth = computeTabSwitcherBackdropDepth(progress = 0f, isReturningToWeb = true)
    assertEquals(0.92f, returningEndDepth.scale, 0.001f)
    assertEquals(24f, returningEndDepth.blurDp, 0.01f)

    // 4. Rect unscaling recovers resting 1.0f card thumbnail coordinates from 0.92f backdrop scale
    val root = Rect(0f, 0f, 1000f, 2000f)
    val scaled = Rect(500f - 92f, 1000f - 92f, 500f + 92f, 1000f + 92f)
    val unscaled = unscaleRectFromCenter(scaled, root, 0.92f)
    assertEquals(400f, unscaled.left, 0.1f)
    assertEquals(900f, unscaled.top, 0.1f)
    assertEquals(600f, unscaled.right, 0.1f)
    assertEquals(1100f, unscaled.bottom, 0.1f)
  }

  @Test
  fun `adjacentTabKeepAlive settings update state and clamp count 1 to 3`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BrowserViewModel(app)

    assertTrue(vm.uiState.value.settings.adjacentTabKeepAlive)
    assertEquals(1, vm.uiState.value.settings.adjacentTabKeepAliveCount)

    vm.updateSetting("adjacentTabKeepAliveCount", "3")
    assertEquals(3, vm.uiState.value.settings.adjacentTabKeepAliveCount)

    vm.updateSetting("adjacentTabKeepAliveCount", "9")
    assertEquals(3, vm.uiState.value.settings.adjacentTabKeepAliveCount)

    vm.updateSetting("adjacentTabKeepAliveCount", "0")
    assertEquals(1, vm.uiState.value.settings.adjacentTabKeepAliveCount)

    vm.updateSetting("adjacentTabKeepAlive", "false")
    assertFalse(vm.uiState.value.settings.adjacentTabKeepAlive)

    vm.updateSetting("adjacentTabKeepAlive", "true")
    assertTrue(vm.uiState.value.settings.adjacentTabKeepAlive)
  }

  @Test
  fun `computeKeepAliveTabIds respects window size boundaries split panes and disable toggle`() {
    val tabs =
      (0..6).map { i ->
        TabUiModel(
          id = "tab-$i",
          url = "https://example$i.org",
          title = "Tab $i",
          displayHost = "example$i.org",
          isSecure = true,
          isLoading = false,
          progress = 100,
          blockedCount = 0,
          canGoBack = false,
          canGoForward = false,
        )
      }

    // Single center tab with N = 1 -> indices [2, 3, 4]
    assertEquals(
      setOf("tab-2", "tab-3", "tab-4"),
      computeKeepAliveTabIds(tabs, setOf("tab-3"), keepAliveEnabled = true, keepAliveCount = 1),
    )

    // Left boundary tab-0 with N = 2 -> indices [0, 1, 2]
    assertEquals(
      setOf("tab-0", "tab-1", "tab-2"),
      computeKeepAliveTabIds(tabs, setOf("tab-0"), keepAliveEnabled = true, keepAliveCount = 2),
    )

    // Right boundary tab-6 with N = 3 -> indices [3, 4, 5, 6]
    assertEquals(
      setOf("tab-3", "tab-4", "tab-5", "tab-6"),
      computeKeepAliveTabIds(tabs, setOf("tab-6"), keepAliveEnabled = true, keepAliveCount = 3),
    )

    // Split panes (tab-1 and tab-5) with N = 1 -> [0, 1, 2] U [4, 5, 6]
    assertEquals(
      setOf("tab-0", "tab-1", "tab-2", "tab-4", "tab-5", "tab-6"),
      computeKeepAliveTabIds(tabs, setOf("tab-1", "tab-5"), keepAliveEnabled = true, keepAliveCount = 1),
    )

    // Disabled -> only visible tab ids remain active
    assertEquals(
      setOf("tab-3"),
      computeKeepAliveTabIds(tabs, setOf("tab-3"), keepAliveEnabled = false, keepAliveCount = 3),
    )
  }

  @Test
  fun `resolveLongPressDirection handles split left right and upward AI across postures`() {
    // 1:1.4 Dual-pane posture allows left/right split and upward AI
    assertEquals(
      OmniboxLongPressDirection.NEUTRAL,
      resolveLongPressDirection(dxPx = 20f, dyPx = -10f, canSplit = true, splitThresholdPx = 52f, aiThresholdPx = 44f),
    )
    assertEquals(
      OmniboxLongPressDirection.SPLIT_RIGHT,
      resolveLongPressDirection(dxPx = 70f, dyPx = 5f, canSplit = true, splitThresholdPx = 52f, aiThresholdPx = 44f),
    )
    assertEquals(
      OmniboxLongPressDirection.SPLIT_LEFT,
      resolveLongPressDirection(dxPx = -70f, dyPx = 5f, canSplit = true, splitThresholdPx = 52f, aiThresholdPx = 44f),
    )
    assertEquals(
      OmniboxLongPressDirection.AI_UP,
      resolveLongPressDirection(dxPx = 10f, dyPx = -60f, canSplit = true, splitThresholdPx = 52f, aiThresholdPx = 44f),
    )

    // Compact / cover posture disables horizontal split while preserving AI upward drag
    assertEquals(
      OmniboxLongPressDirection.NEUTRAL,
      resolveLongPressDirection(dxPx = 90f, dyPx = 0f, canSplit = false, splitThresholdPx = 52f, aiThresholdPx = 44f),
    )
    assertEquals(
      OmniboxLongPressDirection.AI_UP,
      resolveLongPressDirection(dxPx = 5f, dyPx = -60f, canSplit = false, splitThresholdPx = 52f, aiThresholdPx = 44f),
    )
  }

  @Test
  fun `viewModel AI config URL auto attachment and drag auto-send work end to end`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = BrowserViewModel(app)

    assertEquals(5, vm.uiState.value.aiProviderPresets.size)
    assertEquals("google_gemini", vm.uiState.value.aiConfig.providerId)
    assertEquals(AiExecutionMode.LOCAL_BINARY_CLI, vm.uiState.value.aiConfig.executionMode)
    assertEquals(AiFoldableLayout.RIGHT_PANEL, vm.uiState.value.aiConfig.foldableLayout)

    vm.updateAiConfig("providerId", "anthropic_claude")
    vm.updateAiConfig("executionMode", AiExecutionMode.LOCALHOST_BRIDGE.id)
    vm.updateAiConfig("foldableLayout", AiFoldableLayout.CENTER_SHEET.id)
    assertEquals("anthropic_claude", vm.uiState.value.aiConfig.providerId)
    assertEquals(AiExecutionMode.LOCALHOST_BRIDGE, vm.uiState.value.aiConfig.executionMode)
    assertEquals(AiFoldableLayout.CENTER_SHEET, vm.uiState.value.aiConfig.foldableLayout)

    val tabId = vm.uiState.value.primaryTabId
    vm.onWebViewNavigationUpdate(
      tabId = tabId,
      url = "https://aosp.example.org/docs",
      title = "AOSP Reference",
      isLoading = false,
      progress = 100,
      caps = com.mybrowser.appui.webview.WebViewNavCapabilities(),
    )

    // Opening AI sheet attaches page URL + title and pre-fills prompt
    vm.openAiSheet(paneIndex = 0, fromDragGesture = false)
    val opened = vm.uiState.value
    assertTrue(opened.isAiSheetOpen)
    assertEquals("https://aosp.example.org/docs", opened.aiAttachedUrl)
    assertEquals("AOSP Reference", opened.aiAttachedTitle)
    assertTrue(opened.aiDraftInput.contains("https://aosp.example.org/docs"))
    assertTrue(opened.aiMessages.isEmpty())

    // Detaching clears page attachment chip and strips URL from draft
    vm.detachAiPageContext()
    assertEquals("", vm.uiState.value.aiAttachedUrl)
    assertFalse(vm.uiState.value.aiDraftInput.contains("https://aosp.example.org/docs"))

    // Auto-send on drag immediately submits message when triggered via drag gesture
    vm.closeAiSheet()
    vm.updateAiConfig("autoSendOnDrag", "true")
    vm.openAiSheet(paneIndex = 0, fromDragGesture = true)
    assertTrue(vm.uiState.value.isAiSheetOpen)
    assertTrue(vm.uiState.value.aiMessages.isNotEmpty())
    assertTrue(vm.uiState.value.aiMessages.first().isUser)
    assertTrue(vm.uiState.value.aiMessages.first().text.contains("https://aosp.example.org/docs"))

    vm.clearAiConversation()
    assertTrue(vm.uiState.value.aiMessages.isEmpty())
    vm.closeAiSheet()
    assertFalse(vm.uiState.value.isAiSheetOpen)
  }
}
