use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::RwLock;
use std::time::Instant;
use url::Url;

uniffi::setup_scaffolding!();

#[derive(uniffi::Record, Debug, Clone)]
pub struct EngineAbiInfo {
    pub abi_version: u32,
    pub backend_kind: String,
    pub features_csv: String,
}

#[uniffi::export]
pub fn get_engine_abi_info() -> EngineAbiInfo {
    EngineAbiInfo {
        abi_version: 1,
        backend_kind: "rust-uniffi".to_string(),
        features_csv: "url-router,12b-blocker,rfc1035-doh,tab-folder-fsm,frecency-index,design-cfg".to_string(),
    }
}

#[derive(uniffi::Object)]
pub struct RustTrackerBlocker {
    enabled: AtomicBool,
    level: RwLock<String>,
    rules: RwLock<HashMap<String, u8>>,
    evaluated: AtomicU64,
    blocked_ads: AtomicU64,
    blocked_trackers: AtomicU64,
}

#[uniffi::export]
impl RustTrackerBlocker {
    #[uniffi::constructor]
    pub fn new() -> Self {
        let mut initial = HashMap::new();
        for host in ["doubleclick.net", "googlesyndication.com", "adnxs.com", "criteo.com"] {
            initial.insert(host.to_string(), 1u8);
        }
        for host in ["google-analytics.com", "googletagmanager.com", "hotjar.com", "mixpanel.com"] {
            initial.insert(host.to_string(), 2u8);
        }
        Self {
            enabled: AtomicBool::new(true),
            level: RwLock::new("STANDARD".to_string()),
            rules: RwLock::new(initial),
            evaluated: AtomicU64::new(0),
            blocked_ads: AtomicU64::new(0),
            blocked_trackers: AtomicU64::new(0),
        }
    }

    pub fn set_enabled(&self, enabled: bool) {
        self.enabled.store(enabled, Ordering::Relaxed);
    }

    pub fn set_level(&self, level: String) {
        if let Ok(mut guard) = self.level.write() {
            let norm = level.trim().to_uppercase();
            if norm == "STANDARD" || norm == "STRICT" {
                *guard = norm;
            }
        }
    }

    /// Returns 12-byte big-endian verdict frame matching `:core-bridge` TrackerBlockerContract.
    pub fn should_block_request(&self, request_url: String, page_host: String, _resource_type: i32) -> Vec<u8> {
        let start = Instant::now();
        self.evaluated.fetch_add(1, Ordering::Relaxed);

        if !self.enabled.load(Ordering::Relaxed) {
            return pack_verdict(false, 0, 0, start.elapsed().as_micros() as u32, false);
        }

        let req_host = Url::parse(&request_url)
            .ok()
            .and_then(|u| u.host_str().map(|s| s.trim_start_matches("www.").to_lowercase()))
            .unwrap_or_default();

        let clean_page = page_host.trim_start_matches("www.").to_lowercase();
        let third_party = !req_host.is_empty()
            && !clean_page.is_empty()
            && req_host != clean_page
            && !req_host.ends_with(&format!(".{clean_page}"));

        let category = if let Ok(guard) = self.rules.read() {
            let mut candidate = req_host.as_str();
            let mut found = 0u8;
            loop {
                if let Some(&cat) = guard.get(candidate) {
                    found = cat;
                    break;
                }
                match candidate.find('.') {
                    Some(idx) => candidate = &candidate[idx + 1..],
                    None => break,
                }
            }
            found
        } else {
            0
        };

        let block = category != 0 && (third_party || category == 3);
        if block {
            if category == 1 {
                self.blocked_ads.fetch_add(1, Ordering::Relaxed);
            } else {
                self.blocked_trackers.fetch_add(1, Ordering::Relaxed);
            }
        }

        pack_verdict(
            block,
            if block { category } else { 0 },
            0,
            start.elapsed().as_micros().max(1) as u32,
            third_party,
        )
    }
}

fn pack_verdict(blocked: bool, category: u8, rule_hash: u32, latency_micros: u32, third_party: bool) -> Vec<u8> {
    let mut out = Vec::with_capacity(12);
    out.push(if blocked { 1 } else { 0 });
    out.push(category);
    out.extend_from_slice(&rule_hash.to_be_bytes());
    out.extend_from_slice(&latency_micros.to_be_bytes());
    out.extend_from_slice(&(if third_party { 1u16 } else { 0u16 }).to_be_bytes());
    out
}

#[derive(uniffi::Enum, serde::Serialize, serde::Deserialize, Debug, Clone, PartialEq, Eq)]
pub enum TabSortMode {
    Recent,
    Created,
    Domain,
    FoldersFirst,
    PinnedFirst,
}

#[derive(uniffi::Record, serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct TabRecord {
    pub id: String,
    pub url: String,
    pub title: String,
    pub is_loading: bool,
    pub progress: i32,
    pub blocked_count: i32,
    pub created_at_ms: i64,
    pub last_active_at_ms: i64,
    pub folder_id: Option<String>,
    pub is_pinned: bool,
}

#[derive(uniffi::Record, serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct TabFolderRecord {
    pub id: String,
    pub name: String,
    pub color_index: i32,
    pub is_expanded: bool,
}

#[derive(uniffi::Record, serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct ToolbarConfigRecord {
    pub left_slots: Vec<String>,
    pub right_slots: Vec<String>,
    pub alignment: String,
    pub auto_minimize: bool,
    pub fixed_trailing: String,
}

#[derive(uniffi::Record, serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct BrowserSettingsRecord {
    pub theme_mode: String,
    pub accent_preset: String,
    pub dynamic_color: bool,
    pub floating_bar_style: String,
    pub auto_hide_sensitivity: String,
    pub web_font_zoom_percent: i32,
    pub force_web_dark: bool,
    pub hide_status_bar: bool,
    pub auto_dual_pane_unfold: bool,
    pub split_gesture_enabled: bool,
    pub swipe_up_open_tabs: bool,
    pub blocker_level: String,
    pub send_dnt_gpc_headers: bool,
    pub https_only_mode: bool,
    pub block_third_party_cookies: bool,
    pub confirm_external_app: bool,
    pub startup_behavior: String,
    pub background_tab_sleep: bool,
    pub auto_close_days: i32,
    pub confirm_before_download: bool,
}

#[derive(uniffi::Record, serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct SitePolicyRecord {
    pub javascript_enabled: bool,
    pub popups_allowed: bool,
    pub desktop_site: bool,
    pub block_images: bool,
    pub camera_policy: String,
    pub microphone_policy: String,
    pub location_policy: String,
    pub clipboard_policy: String,
    pub notifications_policy: String,
    pub sensors_policy: String,
    pub autoplay_media: String,
}

#[derive(uniffi::Record, serde::Serialize, serde::Deserialize, Debug, Clone)]
pub struct DesignConfigRecord {
    pub icon_corner_radius_dp: f32,
    pub bar_corner_radius_dp: f32,
    pub card_corner_radius_dp: f32,
    pub bar_height_dp: f32,
    pub bar_width_fraction: f32,
    pub bar_horizontal_bias: f32,
    pub bar_bottom_offset_dp: f32,
    pub bar_opacity: f32,
    pub bar_blur_radius_dp: f32,
}

#[derive(uniffi::Object)]
pub struct RustDesignConfigEngine {
    state: RwLock<DesignConfigRecord>,
}

#[uniffi::export]
impl RustDesignConfigEngine {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self {
            state: RwLock::new(DesignConfigRecord {
                icon_corner_radius_dp: 22.0,
                bar_corner_radius_dp: 28.0,
                card_corner_radius_dp: 20.0,
                bar_height_dp: 56.0,
                bar_width_fraction: 0.94,
                bar_horizontal_bias: 0.0,
                bar_bottom_offset_dp: 12.0,
                bar_opacity: 0.90,
                bar_blur_radius_dp: 18.0,
            }),
        }
    }

    pub fn get_design_config_json(&self) -> String {
        let guard = self.state.read().expect("lock poisoned");
        serde_json::to_string(&*guard).unwrap_or_else(|_| "{}".to_string())
    }
}
