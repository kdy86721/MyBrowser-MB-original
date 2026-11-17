package com.mybrowser.core.bridge

/**
 * Pure Kotlin FFI/UniFFI boundary contracts for MyBrowser(MB).
 *
 * STRICT INVARIANT:
 * This module MUST NOT import `android.*`, `androidx.*`, or `com.google.*`.
 * Every method crosses the boundary exclusively with primitive types, `ByteArray`,
 * or flat UTF-8 JSON strings so that `:core-bridge` can be swapped 1:1 with a
 * compiled Rust library (`libmybrowser_core.so`) via UniFFI or JNI without
 * refactoring `:app-ui`.
 */

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct EngineAbiInfo {
//     pub abi_version: u32,
//     pub backend_kind: String, // "kotlin-stub" | "rust-uniffi"
//     pub features_csv: String,
// }
data class EngineAbiInfo(
  val abiVersion: Int,
  val backendKind: String,
  val featuresCsv: String,
)

// [RUST-MIGRATION-TARGET]
// #[uniffi::export(with_foreign)]
// pub trait UrlEngineContract: Send + Sync {
//     fn normalize_and_resolve_input(&self, raw_input: String, default_search_engine_id: String, custom_search_template: String) -> String;
//     fn extract_registrable_domain(&self, url: String) -> String;
//     fn is_secure_origin(&self, url: String) -> bool;
//     fn upgrade_to_https_if_needed(&self, url: String, https_only: bool) -> String;
//     fn get_search_engines_json(&self) -> String;
// }
interface UrlEngineContract {
  fun normalizeAndResolveInput(
    rawInput: String,
    defaultSearchEngineId: String,
    customSearchTemplate: String = "",
  ): String

  fun extractRegistrableDomain(url: String): String
  fun isSecureOrigin(url: String): Boolean
  fun upgradeToHttpsIfNeeded(url: String, httpsOnly: Boolean): String
  fun getSearchEnginesJson(): String
}

// [RUST-MIGRATION-TARGET]
// Binary verdict layout (12 bytes, big-endian) returned by `should_block_request`:
//   [0]      blocked (u8: 0 = allow, 1 = block)
//   [1]      category (u8: 0 = none, 1 = ad, 2 = tracker, 3 = fingerprinting, 4 = telemetry)
//   [2..6]   rule_hash (u32 BE)
//   [6..10]  latency_micros (u32 BE)
//   [10..12] flags (u16 BE: bit0 = third_party)
//
// #[uniffi::export(with_foreign)]
// pub trait TrackerBlockerContract: Send + Sync {
//     fn should_block_request(&self, request_url: String, page_host: String, resource_type: i32) -> Vec<u8>;
//     fn load_filter_list_bytes(&self, easylist_chunk: Vec<u8>) -> i32;
//     fn set_enabled(&self, enabled: bool);
//     fn is_enabled(&self) -> bool;
//     fn set_level(&self, level: String);
//     fn get_level(&self) -> String;
//     fn get_active_rule_count(&self) -> i32;
//     fn get_session_stats_json(&self) -> String;
//     fn reset_session_stats(&self);
// }
interface TrackerBlockerContract {
  fun shouldBlockRequest(requestUrl: String, pageHost: String, resourceType: Int): ByteArray
  fun loadFilterListBytes(easylistChunk: ByteArray): Int
  fun setEnabled(enabled: Boolean)
  fun isEnabled(): Boolean
  fun setLevel(level: String)
  fun getLevel(): String
  fun getActiveRuleCount(): Int
  fun getSessionStatsJson(): String
  fun resetSessionStats()
}

// [RUST-MIGRATION-TARGET]
// #[uniffi::export(with_foreign)]
// pub trait DohResolverContract: Send + Sync {
//     fn build_dns_query_packet(&self, hostname: String, record_type: i32) -> Vec<u8>;
//     fn parse_dns_response_packet(&self, wire_bytes: Vec<u8>) -> String;
//     fn get_doh_providers_json(&self) -> String;
// }
interface DohResolverContract {
  fun buildDnsQueryPacket(hostname: String, recordType: Int): ByteArray
  fun parseDnsResponsePacket(wireBytes: ByteArray): String
  fun getDohProvidersJson(): String
}

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct TabFolderRecord {
//     pub id: String,
//     pub name: String,
//     pub color_index: i32,
//     pub is_expanded: bool,
// }
//
// #[uniffi::export(with_foreign)]
// pub trait TabStateMachineContract: Send + Sync {
//     fn get_state_json(&self) -> String;
//     fn create_tab(&self, initial_url: String, title: String, target_pane: i32) -> String;
//     fn close_tab(&self, tab_id: String) -> String;
//     fn undo_last_closed_tab(&self) -> String;
//     fn has_undoable_closed_tab(&self) -> bool;
//     fn select_tab(&self, tab_id: String, target_pane: i32) -> String;
//     fn update_tab_navigation(&self, tab_id: String, url: String, title: String, is_loading: bool, progress: i32) -> String;
//     fn increment_tab_blocked(&self, tab_id: String) -> String;
//     fn toggle_pin_tab(&self, tab_id: String) -> String;
//     fn set_tab_sort_mode(&self, mode: String) -> String;
//     fn set_split_mode(&self, enabled: bool) -> String;
//     fn split_active_tab_to_pane(&self, direction_right: bool) -> String;
//     fn close_split_keep_pane(&self, keep_pane_index: i32) -> String;
//     fn set_active_pane(&self, pane_index: i32) -> String;
//     fn step_adjacent_tab(&self, delta: i32) -> String;
//     fn merge_tabs_into_folder(&self, source_tab_id: String, target_tab_id: String, folder_name: String) -> String;
//     fn add_tab_to_folder(&self, tab_id: String, folder_id: String) -> String;
//     fn remove_tab_from_folder(&self, tab_id: String) -> String;
//     fn toggle_folder_expanded(&self, folder_id: String) -> String;
//     fn collapse_all_folders(&self) -> String;
//     fn rename_folder(&self, folder_id: String, name: String) -> String;
//     fn set_folder_color(&self, folder_id: String, color_index: i32) -> String;
//     fn prune_inactive_tabs(&self, max_age_days: i32, now_epoch_ms: i64) -> i32;
//     fn serialize_snapshot_bytes(&self) -> Vec<u8>;
//     fn restore_snapshot_bytes(&self, payload: Vec<u8>) -> String;
// }
interface TabStateMachineContract {
  fun getStateJson(): String
  fun createTab(initialUrl: String, title: String, targetPane: Int): String
  fun closeTab(tabId: String): String
  fun undoLastClosedTab(): String
  fun hasUndoableClosedTab(): Boolean
  fun selectTab(tabId: String, targetPane: Int): String
  fun updateTabNavigation(
    tabId: String,
    url: String,
    title: String,
    isLoading: Boolean,
    progress: Int,
  ): String
  fun incrementTabBlocked(tabId: String): String
  fun togglePinTab(tabId: String): String
  fun setTabSortMode(mode: String): String
  fun setSplitMode(enabled: Boolean): String
  fun splitActiveTabToPane(directionRight: Boolean): String
  fun closeSplitKeepPane(keepPaneIndex: Int): String
  fun setActivePane(paneIndex: Int): String
  fun stepAdjacentTab(delta: Int): String
  fun mergeTabsIntoFolder(sourceTabId: String, targetTabId: String, folderName: String): String
  fun addTabToFolder(tabId: String, folderId: String): String
  fun removeTabFromFolder(tabId: String): String
  fun toggleFolderExpanded(folderId: String): String
  fun collapseAllFolders(): String
  fun renameFolder(folderId: String, name: String): String
  fun setFolderColor(folderId: String, colorIndex: Int): String
  fun pruneInactiveTabs(maxAgeDays: Int, nowEpochMs: Long): Int
  fun serializeSnapshotBytes(): ByteArray
  fun restoreSnapshotBytes(payload: ByteArray): String
}

// [RUST-MIGRATION-TARGET]
// #[uniffi::export(with_foreign)]
// pub trait HistoryIndexerContract: Send + Sync {
//     fn score_frecency(&self, visit_count: i32, last_visited_epoch_ms: i64, now_epoch_ms: i64) -> f64;
//     fn rank_suggestions_json(&self, query: String, candidates_json: String, now_epoch_ms: i64) -> String;
// }
interface HistoryIndexerContract {
  fun scoreFrecency(visitCount: Int, lastVisitedEpochMs: Long, nowEpochMs: Long): Double
  fun rankSuggestionsJson(query: String, candidatesJson: String, nowEpochMs: Long): String
}

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct ToolbarConfigRecord {
//     pub left_slots_csv: String,
//     pub right_slots_csv: String,
//     pub alignment: String,
//     pub auto_minimize: bool,
//     pub swipe_bar_switch_tabs: bool,
//     pub pull_down_refresh: bool,
// }
interface ToolbarConfigContract {
  fun getAvailableActionsJson(): String
  fun getConfigJson(): String
  fun updateSlots(leftSlotsCsv: String, rightSlotsCsv: String): String
  fun moveSlot(side: String, fromIndex: Int, toIndex: Int): String
  fun setAlignment(alignment: String): String
  fun setAutoMinimize(enabled: Boolean): String
  fun setGestures(swipeTabs: Boolean, pullRefresh: Boolean): String
  fun restoreConfigJson(rawJson: String): String
  fun resetDefaults(): String
}

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct BrowserSettingsRecord {
//     pub theme_mode: String,              // "SYSTEM" | "LIGHT" | "DARK" | "AMOLED_DARK"
//     pub accent_preset: String,           // "DYNAMIC" | "EMERALD" | "COBALT" | "AMBER" | "MONOCHROME"
//     pub dynamic_color: bool,
//     pub floating_bar_style: String,      // "TRANSLUCENT_GLASS" | "SOLID_PILL"
//     pub auto_hide_sensitivity: String,   // "LOW" | "MEDIUM" | "HIGH"
//     pub web_font_zoom_percent: i32,      // 75..200
//     pub force_web_dark: bool,
//     pub force_zoom_enabled: bool,
//     pub expand_bar_on_url_edit: bool,
//     pub hide_status_bar: bool,
//     pub auto_dual_pane_unfold: bool,
//     pub split_gesture_enabled: bool,
//     pub split_gesture_sensitivity: String, // "LOW" | "MEDIUM" | "HIGH"
//     pub swipe_up_open_tabs: bool,
//     pub tab_swipe_to_close_enabled: bool,
//     pub tab_drag_folder_enabled: bool,
//     pub tab_view_mode: String,           // "THUMBNAIL_GRID" | "COMPACT_LIST"
//     pub default_tab_sort_mode: String,   // "RECENT" | "CREATED" | "DOMAIN" | "FOLDERS_FIRST" | "PINNED_FIRST"
//     pub search_engine_id: String,
//     pub custom_search_url: String,
//     pub tracker_blocking_enabled: bool,
//     pub blocker_level: String,           // "STANDARD" | "STRICT"
//     pub block_third_party_cookies: bool,
//     pub send_dnt_gpc_headers: bool,
//     pub https_only_mode: bool,
//     pub doh_provider_id: String,
//     pub custom_doh_url: String,
//     pub confirm_external_app: bool,
//     pub startup_behavior: String,        // "RESTORE_TABS" | "SPEED_DIAL" | "BLANK_PAGE"
//     pub background_tab_sleep: bool,
//     pub adjacent_tab_keep_alive: bool,
//     pub adjacent_tab_keep_alive_count: i32, // 1 | 2 | 3
//     pub auto_close_days: i32,            // 0 | 1 | 7 | 30
//     pub download_tree_uri: String,
//     pub confirm_before_download: bool,
// }
interface SettingsStoreContract {
  fun getSettingsJson(): String
  fun updateSetting(key: String, value: String): String
  fun restoreSettingsJson(rawJson: String): String
  fun resetSettings(): String
}

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct DesignConfigRecord {
//     pub icon_corner_radius_dp: f32,  // 0.0..24.0
//     pub bar_corner_radius_dp: f32,   // 0.0..36.0
//     pub card_corner_radius_dp: f32,  // 8.0..28.0
//     pub bar_height_dp: f32,          // 44.0..68.0
//     pub bar_width_fraction: f32,     // 0.60..1.00
//     pub bar_horizontal_bias: f32,    // -1.0..1.0
//     pub bar_bottom_offset_dp: f32,   // 4.0..40.0
//     pub bar_opacity: f32,            // 0.55..1.00
//     pub bar_blur_radius_dp: f32,     // 0.0..32.0
//     pub animation_speed_scale: f32,  // 0.50..2.00
// }
interface DesignConfigContract {
  fun getDesignConfigJson(): String
  fun updateDesignParam(key: String, value: String): String
  fun restoreDesignConfigJson(rawJson: String): String
  fun resetDesignDefaults(): String
}

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct SitePolicyRecord {
//     pub javascript_enabled: bool,
//     pub popups_allowed: bool,
//     pub desktop_site: bool,
//     pub block_images: bool,
//     pub camera_policy: String,        // "ASK" | "BLOCK" | "ALLOW"
//     pub microphone_policy: String,    // "ASK" | "BLOCK" | "ALLOW"
//     pub location_policy: String,      // "ASK" | "BLOCK" | "ALLOW"
//     pub clipboard_policy: String,     // "ASK" | "BLOCK"
//     pub notifications_policy: String, // "ASK" | "BLOCK"
//     pub sensors_policy: String,       // "ASK" | "BLOCK"
//     pub autoplay_media: String,       // "BLOCK" | "ALLOW"
// }
interface SitePermissionsContract {
  fun getGlobalPolicyJson(): String
  fun setGlobalPolicy(key: String, value: String): String
  fun getEffectivePolicyJson(domain: String): String
  fun setDomainOverride(domain: String, key: String, value: String): String
  fun resetDomainOverrides(domain: String): String
  fun listDomainOverridesJson(query: String): String
  fun exportStateJson(): String
  fun restoreStateJson(rawJson: String): String
}

// [RUST-MIGRATION-TARGET]
// #[derive(uniffi::Record, Debug, Clone)]
// pub struct AiCliConfigRecord {
//     pub provider_id: String,          // "google_gemini" | "kimi" | "openai_codex" | "anthropic_claude" | "custom"
//     pub execution_mode: String,       // "LOCAL_BINARY_CLI" | "LOCALHOST_BRIDGE" | "DIRECT_API"
//     pub cli_command: String,
//     pub cli_args: String,
//     pub localhost_url: String,
//     pub api_endpoint_url: String,
//     pub model_name: String,
//     pub api_key: String,
//     pub default_prompt_template: String,
//     pub auto_send_on_drag: bool,
//     pub foldable_layout: String,      // "RIGHT_PANEL" | "CENTER_SHEET"
// }
//
// #[uniffi::export(with_foreign)]
// pub trait AiBridgeContract: Send + Sync {
//     fn get_ai_config_json(&self) -> String;
//     fn update_ai_config(&self, key: String, value: String) -> String;
//     fn restore_ai_config_json(&self, raw_json: String) -> String;
//     fn reset_ai_config_defaults(&self) -> String;
//     fn get_provider_presets_json(&self) -> String;
//     fn build_composed_prompt(&self, page_url: String, page_title: String, user_message: String) -> String;
//     fn build_execution_plan_json(&self, page_url: String, page_title: String, user_message: String) -> String;
//     fn parse_stream_chunk_json(&self, provider_id: String, raw_line: String) -> String;
// }
interface AiBridgeContract {
  fun getAiConfigJson(): String
  fun updateAiConfig(key: String, value: String): String
  fun restoreAiConfigJson(rawJson: String): String
  fun resetAiConfigDefaults(): String
  fun getProviderPresetsJson(): String
  fun buildComposedPrompt(pageUrl: String, pageTitle: String, userMessage: String): String
  fun buildExecutionPlanJson(pageUrl: String, pageTitle: String, userMessage: String): String
  fun parseStreamChunkJson(providerId: String, rawLine: String): String
}

internal object JsonMini {
  fun escape(value: String): String {
    val sb = StringBuilder(value.length + 8)
    for (ch in value) {
      when (ch) {
        '\\' -> sb.append("\\\\")
        '"' -> sb.append("\\\"")
        '\n' -> sb.append("\\n")
        '\r' -> sb.append("\\r")
        '\t' -> sb.append("\\t")
        else -> {
          if (ch.code < 0x20) {
            sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
          } else {
            sb.append(ch)
          }
        }
      }
    }
    return sb.toString()
  }

  fun quote(value: String): String = "\"${escape(value)}\""
}
