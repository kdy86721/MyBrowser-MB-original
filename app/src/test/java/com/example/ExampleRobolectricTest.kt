package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.mybrowser.appui.foldable.BrowserPostureMode
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.viewmodel.BrowserViewModel
import com.mybrowser.appui.viewmodel.TabSortMode
import com.mybrowser.appui.viewmodel.TabViewMode
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
}
