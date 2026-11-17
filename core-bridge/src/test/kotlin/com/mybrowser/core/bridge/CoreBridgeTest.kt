package com.mybrowser.core.bridge

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreBridgeTest {

  @Test
  fun urlEngine_normalizesDomainAndRoutesSearchToAllEngines() {
    val engine = UrlEngineImpl()

    val domainJson = engine.normalizeAndResolveInput("rust-lang.org", "ddg")
    assertTrue(domainJson.contains("\"resolvedUrl\":\"https://rust-lang.org\""))
    assertTrue(domainJson.contains("\"isSearch\":false"))

    val braveJson = engine.normalizeAndResolveInput("foldable aosp browser", "brave")
    assertTrue(braveJson.contains("\"isSearch\":true"))
    assertTrue(braveJson.contains("https://search.brave.com/search?q="))

    val ecosiaJson = engine.normalizeAndResolveInput("privacy", "ecosia")
    assertTrue(ecosiaJson.contains("https://www.ecosia.org/search?q="))

    val naverJson = engine.normalizeAndResolveInput("안드로이드", "naver")
    assertTrue(naverJson.contains("https://search.naver.com/search.naver?query="))

    val googleJson = engine.normalizeAndResolveInput("kotlin", "google")
    assertTrue(googleJson.contains("https://www.google.com/search?q="))

    val startJson = engine.normalizeAndResolveInput("", "ddg")
    assertTrue(startJson.contains("\"resolvedUrl\":\"about:mb\""))
  }

  @Test
  fun trackerBlocker_returns12ByteFrameAndRespectsStrictLevel() {
    val blocker = TrackerBlockerImpl()
    val standardRules = blocker.getActiveRuleCount()
    blocker.setLevel("STRICT")
    assertTrue(blocker.getActiveRuleCount() > standardRules)

    val strictFrame =
      blocker.shouldBlockRequest(
        requestUrl = "https://clarity.ms/tag.js",
        pageHost = "example.org",
        resourceType = 1,
      )
    assertEquals(12, strictFrame.size)
    assertEquals(1.toByte(), strictFrame[0])

    blocker.setLevel("STANDARD")
    val blockedFrame =
      blocker.shouldBlockRequest(
        requestUrl = "https://www.google-analytics.com/analytics.js",
        pageHost = "example.org",
        resourceType = 1,
      )
    assertEquals(12, blockedFrame.size)
    assertEquals(1.toByte(), blockedFrame[0])
    assertEquals(TrackerBlockerImpl.CAT_TRACKER.toByte(), blockedFrame[1])

    val allowedFrame =
      blocker.shouldBlockRequest(
        requestUrl = "https://example.org/assets/app.js",
        pageHost = "example.org",
        resourceType = 1,
      )
    assertEquals(12, allowedFrame.size)
    assertEquals(0.toByte(), allowedFrame[0])

    val buf = ByteBuffer.wrap(blockedFrame).order(ByteOrder.BIG_ENDIAN)
    buf.get()
    buf.get()
    buf.int
    val latencyMicros = buf.int
    assertTrue(latencyMicros >= 1)
  }

  @Test
  fun dohResolver_buildsValidRfc1035DnsQueryWirePacket() {
    val doh = DohResolverImpl()
    val packet = doh.buildDnsQueryPacket("duckduckgo.com", DohResolverImpl.TYPE_A)
    assertTrue(packet.size > 16)
    assertEquals(0x4D.toByte(), packet[0])
    assertEquals(0x42.toByte(), packet[1])
    assertEquals(0x01.toByte(), packet[5])
    assertTrue(doh.getDohProvidersJson().contains("\"id\":\"custom\""))
  }

  @Test
  fun tabStateMachine_supportsSplitModeFoldersUndoAndSnapshotRestore() {
    val fsm = TabStateMachineImpl()
    val createdJson = fsm.createTab("https://rust-lang.org", "Rust", 0)
    assertTrue(createdJson.contains("https://rust-lang.org"))

    val stateAfterCreate = fsm.getStateJson()
    val secondTabId = Regex(""""primaryTabId":"([^"]+)"""").find(stateAfterCreate)!!.groupValues[1]

    // Merge tab-1 and secondTabId into a folder
    val mergedFolderJson = fsm.mergeTabsIntoFolder("tab-1", secondTabId, "개발 자료")
    assertTrue(mergedFolderJson.contains(""""name":"개발 자료""""))
    val folderId = Regex(""""folders":\[\{"id":"([^"]+)"""").find(mergedFolderJson)!!.groupValues[1]

    // Collapse all folders
    val collapsedJson = fsm.collapseAllFolders()
    assertTrue(collapsedJson.contains(""""isExpanded":false"""))

    // Ungroup secondTabId out of folder
    val ungroupedJson = fsm.removeTabFromFolder(secondTabId)
    assertTrue(
      Regex(""""id":"$secondTabId"[^}]*"folderId":null""").containsMatchIn(ungroupedJson)
    )

    // Close & Undo
    fsm.closeTab(secondTabId)
    assertTrue(fsm.hasUndoableClosedTab())
    val undoneJson = fsm.undoLastClosedTab()
    assertTrue(undoneJson.contains(secondTabId))
    assertFalse(fsm.hasUndoableClosedTab())

    // Snapshot serialize & restore
    val snapshot = fsm.serializeSnapshotBytes()
    val restoredFsm = TabStateMachineImpl()
    val restoredJson = restoredFsm.restoreSnapshotBytes(snapshot)
    assertTrue(restoredJson.contains("https://rust-lang.org"))
    assertTrue(restoredJson.contains("tab-1"))
  }

  @Test
  fun tabStateMachine_prunesInactiveTabsExcludingActiveAndPinned() {
    val fsm = TabStateMachineImpl()
    fsm.createTab("https://a.org", "A", 0)
    fsm.createTab("https://b.org", "B", 0)
    fsm.togglePinTab("tab-1")
    val pinnedState = fsm.setTabSortMode("PINNED_FIRST")
    assertTrue(pinnedState.contains(""""sortMode":"PINNED_FIRST""""))
    assertTrue(Regex(""""id":"tab-1"[^}]*"isPinned":true""").containsMatchIn(pinnedState))

    val now = System.currentTimeMillis()
    val prunedNone = fsm.pruneInactiveTabs(7, now)
    assertEquals(0, prunedNone)

    // Future check (+8 days) prunes inactive non-active, non-pinned tab (A) while keeping tab-1 (pinned) and primary (B)
    val prunedOld = fsm.pruneInactiveTabs(7, now + 8L * 86_400_000L)
    assertEquals(1, prunedOld)
  }

  @Test
  fun toolbarConfig_supports14ActionsAndSlotLimits() {
    val cfg = ToolbarConfigImpl()
    val catalog = cfg.getAvailableActionsJson()
    assertTrue(catalog.contains("\"REFRESH\""))
    assertTrue(catalog.contains("\"HISTORY\""))
    assertTrue(catalog.contains("\"DESKTOP_MODE\""))
    assertTrue(catalog.contains("\"QR_SCAN\""))
    assertTrue(catalog.contains("\"SCREENSHOT\""))

    val updated = cfg.updateSlots("BACK,REFRESH,DESKTOP_MODE,HOME", "TABS,QR_SCAN,SCREENSHOT,FIND_IN_PAGE")
    assertTrue(updated.contains(""""leftSlots":["BACK","REFRESH","DESKTOP_MODE"]"""))
    assertTrue(updated.contains(""""rightSlots":["TABS","QR_SCAN","SCREENSHOT"]"""))
  }

  @Test
  fun settingsAndSitePermissions_roundTripAllNewFields() {
    val settings = SettingsStoreImpl()
    settings.updateSetting("themeMode", "DARK")
    settings.updateSetting("accentPreset", "EMERALD")
    settings.updateSetting("webFontZoomPercent", "185")
    settings.updateSetting("hideStatusBar", "true")
    settings.updateSetting("blockerLevel", "STRICT")
    settings.updateSetting("autoCloseDays", "7")
    settings.updateSetting("tabViewMode", "COMPACT_LIST")
    settings.updateSetting("defaultTabSortMode", "FOLDERS_FIRST")
    val updated = settings.updateSetting("confirmBeforeDownload", "false")

    assertTrue(updated.contains(""""themeMode":"DARK""""))
    assertTrue(updated.contains(""""accentPreset":"EMERALD""""))
    assertTrue(updated.contains(""""webFontZoomPercent":185"""))
    assertTrue(updated.contains(""""hideStatusBar":true"""))
    assertTrue(updated.contains(""""blockerLevel":"STRICT""""))
    assertTrue(updated.contains(""""autoCloseDays":7"""))
    assertTrue(updated.contains(""""tabViewMode":"COMPACT_LIST""""))
    assertTrue(updated.contains(""""defaultTabSortMode":"FOLDERS_FIRST""""))
    assertTrue(updated.contains(""""confirmBeforeDownload":false"""))

    val restored = SettingsStoreImpl().restoreSettingsJson(updated)
    assertEquals(updated, restored)

    val perms = SitePermissionsImpl()
    perms.setDomainOverride("example.org", "blockImages", "true")
    perms.setDomainOverride("example.org", "sensorsPolicy", "BLOCK")
    val effective = perms.getEffectivePolicyJson("www.example.org")
    assertTrue(effective.contains(""""blockImages":true"""))
    assertTrue(effective.contains(""""sensorsPolicy":"BLOCK""""))
  }

  @Test
  fun designConfig_clampsFloatRangesAndRoundTrips() {
    val design = DesignConfigImpl()
    design.updateDesignParam("iconCornerRadiusDp", "8")
    design.updateDesignParam("barCornerRadiusDp", "34")
    design.updateDesignParam("cardCornerRadiusDp", "4") // clamps to 8f
    design.updateDesignParam("barHeightDp", "40") // clamps to 44f
    design.updateDesignParam("barWidthFraction", "0.75")
    design.updateDesignParam("barHorizontalBias", "-0.6")
    design.updateDesignParam("barBottomOffsetDp", "24")
    design.updateDesignParam("barOpacity", "0.7")
    val json = design.updateDesignParam("barBlurRadiusDp", "48") // clamps to 32f

    assertTrue(json.contains(""""iconCornerRadiusDp":8.0"""))
    assertTrue(json.contains(""""barCornerRadiusDp":34.0"""))
    assertTrue(json.contains(""""cardCornerRadiusDp":8.0"""))
    assertTrue(json.contains(""""barHeightDp":44.0"""))
    assertTrue(json.contains(""""barWidthFraction":0.75"""))
    assertTrue(json.contains(""""barHorizontalBias":-0.6"""))
    assertTrue(json.contains(""""barBottomOffsetDp":24.0"""))
    assertTrue(json.contains(""""barOpacity":0.7"""))
    assertTrue(json.contains(""""barBlurRadiusDp":32.0"""))

    val restored = DesignConfigImpl().restoreDesignConfigJson(json)
    assertEquals(json, restored)

    val migrated = DesignConfigImpl().restoreDesignConfigJson("""{"blurIntensityPercent":50}""")
    assertTrue(migrated.contains(""""barBlurRadiusDp":16.0"""))
  }

  @Test
  fun settingsStore_defaultsAutoDualPaneUnfoldToFalseAndTabCloseExits2TabSplit() {
    val settings = SettingsStoreImpl()
    assertTrue(settings.getSettingsJson().contains(""""autoDualPaneUnfold":false"""))

    val fsm = TabStateMachineImpl()
    val splitJson = fsm.splitActiveTabToPane(directionRight = true)
    assertTrue(splitJson.contains(""""splitMode":true"""))
    val primaryId = Regex(""""primaryTabId":"([^"]+)"""").find(splitJson)!!.groupValues[1]
    val secondaryId = Regex(""""secondaryTabId":"([^"]+)"""").find(splitJson)!!.groupValues[1]
    assertTrue(primaryId != secondaryId)

    // Closing primary tab when only 2 tabs exist must cleanly collapse splitMode instead of primary == secondary
    val afterCloseJson = fsm.closeTab(primaryId)
    assertTrue(afterCloseJson.contains(""""splitMode":false"""))
    assertTrue(afterCloseJson.contains(""""secondaryTabId":null"""))
    assertTrue(afterCloseJson.contains(""""primaryTabId":"$secondaryId""""))
  }
}
