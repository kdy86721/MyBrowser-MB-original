package com.mybrowser.core.bridge

/**
 * Factory and runtime facade for `:core-bridge`.
 * When Rust FFI (`libmybrowser_core.so` via UniFFI) is linked, swap the concrete
 * implementations below with the generated UniFFI wrappers without touching `:app-ui`.
 */
// [RUST-MIGRATION-TARGET]
// // In Rust UniFFI mode:
// // System.loadLibrary("mybrowser_core")
class CoreBridgeRuntime(
  val urlEngine: UrlEngineContract = UrlEngineImpl(),
  val trackerBlocker: TrackerBlockerContract = TrackerBlockerImpl(),
  val dohResolver: DohResolverContract = DohResolverImpl(),
  val tabStateMachine: TabStateMachineContract = TabStateMachineImpl(),
  val historyIndexer: HistoryIndexerContract = HistoryIndexerImpl(),
  val toolbarConfig: ToolbarConfigContract = ToolbarConfigImpl(),
  val settingsStore: SettingsStoreContract = SettingsStoreImpl(),
  val designConfig: DesignConfigContract = DesignConfigImpl(),
  val sitePermissions: SitePermissionsContract = SitePermissionsImpl(),
  val aiBridge: AiBridgeContract = AiBridgeImpl(),
) {
  val abiInfo: EngineAbiInfo =
    EngineAbiInfo(
      abiVersion = 1,
      backendKind = "kotlin-ffi-stub",
      featuresCsv =
        "url-router,12b-blocker,rfc1035-doh,tab-fsm,frecency-index,toolbar-cfg,design-cfg,5pillar-settings,site-policy,ai-cli-bridge",
    )
}
