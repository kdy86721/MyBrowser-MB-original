package com.mybrowser.core.bridge

/**
 * Pure Kotlin Settings, Design Geometry & Site Permissions Engines for the 4-Pillar Settings Hub.
 * Zero Android dependencies; serializes flat JSON for UniFFI/JNI migration.
 */
// [RUST-MIGRATION-TARGET]
// pub struct RustSettingsStore {
//     state: RwLock<BrowserSettingsRecord>,
// }
class SettingsStoreImpl : SettingsStoreContract {

  private val lock = Any()

  private var themeMode: String = "SYSTEM" // SYSTEM | LIGHT | DARK | AMOLED_DARK
  private var accentPreset: String = "DYNAMIC" // DYNAMIC | EMERALD | COBALT | AMBER | MONOCHROME
  private var dynamicColor: Boolean = true
  private var floatingBarStyle: String = "TRANSLUCENT_GLASS"
  private var autoHideSensitivity: String = "MEDIUM"
  private var webFontZoomPercent: Int = 100 // 75..200
  private var forceWebDark: Boolean = false
  private var hideStatusBar: Boolean = false
  private var autoDualPaneUnfold: Boolean = false
  private var splitGestureEnabled: Boolean = true
  private var splitGestureSensitivity: String = "MEDIUM"
  private var swipeUpOpenTabs: Boolean = true
  private var tabSwipeToCloseEnabled: Boolean = true
  private var tabDragFolderEnabled: Boolean = true
  private var tabViewMode: String = "THUMBNAIL_GRID" // THUMBNAIL_GRID | COMPACT_LIST
  private var defaultTabSortMode: String = "RECENT" // RECENT | CREATED | DOMAIN | FOLDERS_FIRST | PINNED_FIRST
  private var searchEngineId: String = "ddg"
  private var customSearchUrl: String = "https://www.mojeek.com/search?q=%s"
  private var trackerBlockingEnabled: Boolean = true
  private var blockerLevel: String = "STANDARD" // STANDARD | STRICT
  private var blockThirdPartyCookies: Boolean = true
  private var sendDntGpcHeaders: Boolean = true
  private var httpsOnlyMode: Boolean = true
  private var dohProviderId: String = "quad9"
  private var customDohUrl: String = "https://dns.quad9.net/dns-query"
  private var confirmExternalApp: Boolean = true
  private var startupBehavior: String = "SPEED_DIAL"
  private var backgroundTabSleep: Boolean = true
  private var autoCloseDays: Int = 0 // 0 | 1 | 7 | 30
  private var downloadTreeUri: String = ""
  private var confirmBeforeDownload: Boolean = true

  override fun getSettingsJson(): String = synchronized(lock) { serializeLocked() }

  override fun updateSetting(key: String, value: String): String = synchronized(lock) {
    applyKeyValueLocked(key, value)
    serializeLocked()
  }

  override fun restoreSettingsJson(rawJson: String): String = synchronized(lock) {
    if (rawJson.isBlank()) return@synchronized serializeLocked()
    extractString(rawJson, "themeMode")?.let { applyKeyValueLocked("themeMode", it) }
    extractString(rawJson, "accentPreset")?.let { applyKeyValueLocked("accentPreset", it) }
    extractBoolean(rawJson, "dynamicColor")?.let { dynamicColor = it }
    extractString(rawJson, "floatingBarStyle")?.let { applyKeyValueLocked("floatingBarStyle", it) }
    extractString(rawJson, "autoHideSensitivity")?.let {
      applyKeyValueLocked("autoHideSensitivity", it)
    }
    extractInt(rawJson, "webFontZoomPercent")?.let {
      webFontZoomPercent = it.coerceIn(75, 200)
    }
    extractBoolean(rawJson, "forceWebDark")?.let { forceWebDark = it }
    extractBoolean(rawJson, "hideStatusBar")?.let { hideStatusBar = it }
    extractBoolean(rawJson, "autoDualPaneUnfold")?.let { autoDualPaneUnfold = it }
    extractBoolean(rawJson, "splitGestureEnabled")?.let { splitGestureEnabled = it }
    extractString(rawJson, "splitGestureSensitivity")?.let {
      applyKeyValueLocked("splitGestureSensitivity", it)
    }
    extractBoolean(rawJson, "swipeUpOpenTabs")?.let { swipeUpOpenTabs = it }
    extractBoolean(rawJson, "tabSwipeToCloseEnabled")?.let { tabSwipeToCloseEnabled = it }
    extractBoolean(rawJson, "tabDragFolderEnabled")?.let { tabDragFolderEnabled = it }
    extractString(rawJson, "tabViewMode")?.let { applyKeyValueLocked("tabViewMode", it) }
    extractString(rawJson, "defaultTabSortMode")?.let { applyKeyValueLocked("defaultTabSortMode", it) }
    extractString(rawJson, "searchEngineId")?.let { searchEngineId = it }
    extractString(rawJson, "customSearchUrl")?.let { customSearchUrl = it }
    extractBoolean(rawJson, "trackerBlockingEnabled")?.let { trackerBlockingEnabled = it }
    extractString(rawJson, "blockerLevel")?.let { applyKeyValueLocked("blockerLevel", it) }
    extractBoolean(rawJson, "blockThirdPartyCookies")?.let { blockThirdPartyCookies = it }
    extractBoolean(rawJson, "sendDntGpcHeaders")?.let { sendDntGpcHeaders = it }
    extractBoolean(rawJson, "httpsOnlyMode")?.let { httpsOnlyMode = it }
    extractString(rawJson, "dohProviderId")?.let { dohProviderId = it }
    extractString(rawJson, "customDohUrl")?.let { customDohUrl = it }
    extractBoolean(rawJson, "confirmExternalApp")?.let { confirmExternalApp = it }
    extractString(rawJson, "startupBehavior")?.let { applyKeyValueLocked("startupBehavior", it) }
    extractBoolean(rawJson, "backgroundTabSleep")?.let { backgroundTabSleep = it }
    extractInt(rawJson, "autoCloseDays")?.let { applyKeyValueLocked("autoCloseDays", it.toString()) }
    extractString(rawJson, "downloadTreeUri")?.let { downloadTreeUri = it }
    extractBoolean(rawJson, "confirmBeforeDownload")?.let { confirmBeforeDownload = it }
    serializeLocked()
  }

  override fun resetSettings(): String = synchronized(lock) {
    themeMode = "SYSTEM"
    accentPreset = "DYNAMIC"
    dynamicColor = true
    floatingBarStyle = "TRANSLUCENT_GLASS"
    autoHideSensitivity = "MEDIUM"
    webFontZoomPercent = 100
    forceWebDark = false
    hideStatusBar = false
    autoDualPaneUnfold = false
    splitGestureEnabled = true
    splitGestureSensitivity = "MEDIUM"
    swipeUpOpenTabs = true
    tabSwipeToCloseEnabled = true
    tabDragFolderEnabled = true
    tabViewMode = "THUMBNAIL_GRID"
    defaultTabSortMode = "RECENT"
    searchEngineId = "ddg"
    customSearchUrl = "https://www.mojeek.com/search?q=%s"
    trackerBlockingEnabled = true
    blockerLevel = "STANDARD"
    blockThirdPartyCookies = true
    sendDntGpcHeaders = true
    httpsOnlyMode = true
    dohProviderId = "quad9"
    customDohUrl = "https://dns.quad9.net/dns-query"
    confirmExternalApp = true
    startupBehavior = "SPEED_DIAL"
    backgroundTabSleep = true
    autoCloseDays = 0
    downloadTreeUri = ""
    confirmBeforeDownload = true
    serializeLocked()
  }

  private fun applyKeyValueLocked(key: String, value: String) {
    when (key) {
      "themeMode" ->
        if (value in setOf("SYSTEM", "LIGHT", "DARK", "AMOLED_DARK")) themeMode = value
      "accentPreset" ->
        if (value in setOf("DYNAMIC", "EMERALD", "COBALT", "AMBER", "MONOCHROME")) {
          accentPreset = value
          dynamicColor = value == "DYNAMIC"
        }
      "dynamicColor" -> value.toBooleanStrictOrNull()?.let {
        dynamicColor = it
        accentPreset = if (it) "DYNAMIC" else if (accentPreset == "DYNAMIC") "COBALT" else accentPreset
      }
      "floatingBarStyle" ->
        if (value in setOf("TRANSLUCENT_GLASS", "SOLID_PILL")) floatingBarStyle = value
      "autoHideSensitivity" ->
        if (value in setOf("LOW", "MEDIUM", "HIGH")) autoHideSensitivity = value
      "webFontZoomPercent" ->
        value.toIntOrNull()?.let { webFontZoomPercent = it.coerceIn(75, 200) }
      "forceWebDark" -> value.toBooleanStrictOrNull()?.let { forceWebDark = it }
      "hideStatusBar" -> value.toBooleanStrictOrNull()?.let { hideStatusBar = it }
      "autoDualPaneUnfold" -> value.toBooleanStrictOrNull()?.let { autoDualPaneUnfold = it }
      "splitGestureEnabled" -> value.toBooleanStrictOrNull()?.let { splitGestureEnabled = it }
      "splitGestureSensitivity" ->
        if (value.uppercase() in setOf("LOW", "MEDIUM", "HIGH")) {
          splitGestureSensitivity = value.uppercase()
        }
      "swipeUpOpenTabs" -> value.toBooleanStrictOrNull()?.let { swipeUpOpenTabs = it }
      "tabSwipeToCloseEnabled" -> value.toBooleanStrictOrNull()?.let { tabSwipeToCloseEnabled = it }
      "tabDragFolderEnabled" -> value.toBooleanStrictOrNull()?.let { tabDragFolderEnabled = it }
      "tabViewMode" ->
        if (value.uppercase() in setOf("THUMBNAIL_GRID", "COMPACT_LIST")) {
          tabViewMode = value.uppercase()
        }
      "defaultTabSortMode" ->
        if (value.uppercase() in setOf("RECENT", "CREATED", "DOMAIN", "FOLDERS_FIRST", "PINNED_FIRST")) {
          defaultTabSortMode = value.uppercase()
        }
      "searchEngineId" -> if (value.isNotBlank()) searchEngineId = value
      "customSearchUrl" -> customSearchUrl = value.trim()
      "trackerBlockingEnabled" ->
        value.toBooleanStrictOrNull()?.let { trackerBlockingEnabled = it }
      "blockerLevel" ->
        if (value.uppercase() in setOf("STANDARD", "STRICT")) blockerLevel = value.uppercase()
      "blockThirdPartyCookies" ->
        value.toBooleanStrictOrNull()?.let { blockThirdPartyCookies = it }
      "sendDntGpcHeaders" -> value.toBooleanStrictOrNull()?.let { sendDntGpcHeaders = it }
      "httpsOnlyMode" -> value.toBooleanStrictOrNull()?.let { httpsOnlyMode = it }
      "dohProviderId" -> if (value.isNotBlank()) dohProviderId = value
      "customDohUrl" -> customDohUrl = value.trim()
      "confirmExternalApp" -> value.toBooleanStrictOrNull()?.let { confirmExternalApp = it }
      "startupBehavior" ->
        if (value in setOf("RESTORE_TABS", "SPEED_DIAL", "BLANK_PAGE")) startupBehavior = value
      "backgroundTabSleep" -> value.toBooleanStrictOrNull()?.let { backgroundTabSleep = it }
      "autoCloseDays" ->
        value.toIntOrNull()?.let { if (it in setOf(0, 1, 7, 30)) autoCloseDays = it }
      "downloadTreeUri" -> downloadTreeUri = value
      "confirmBeforeDownload" -> value.toBooleanStrictOrNull()?.let { confirmBeforeDownload = it }
    }
  }

  private fun serializeLocked(): String {
    return """{"themeMode":${JsonMini.quote(themeMode)},"accentPreset":${JsonMini.quote(accentPreset)},"dynamicColor":$dynamicColor,"floatingBarStyle":${JsonMini.quote(floatingBarStyle)},"autoHideSensitivity":${JsonMini.quote(autoHideSensitivity)},"webFontZoomPercent":$webFontZoomPercent,"forceWebDark":$forceWebDark,"hideStatusBar":$hideStatusBar,"autoDualPaneUnfold":$autoDualPaneUnfold,"splitGestureEnabled":$splitGestureEnabled,"splitGestureSensitivity":${JsonMini.quote(splitGestureSensitivity)},"swipeUpOpenTabs":$swipeUpOpenTabs,"tabSwipeToCloseEnabled":$tabSwipeToCloseEnabled,"tabDragFolderEnabled":$tabDragFolderEnabled,"tabViewMode":${JsonMini.quote(tabViewMode)},"defaultTabSortMode":${JsonMini.quote(defaultTabSortMode)},"searchEngineId":${JsonMini.quote(searchEngineId)},"customSearchUrl":${JsonMini.quote(customSearchUrl)},"trackerBlockingEnabled":$trackerBlockingEnabled,"blockerLevel":${JsonMini.quote(blockerLevel)},"blockThirdPartyCookies":$blockThirdPartyCookies,"sendDntGpcHeaders":$sendDntGpcHeaders,"httpsOnlyMode":$httpsOnlyMode,"dohProviderId":${JsonMini.quote(dohProviderId)},"customDohUrl":${JsonMini.quote(customDohUrl)},"confirmExternalApp":$confirmExternalApp,"startupBehavior":${JsonMini.quote(startupBehavior)},"backgroundTabSleep":$backgroundTabSleep,"autoCloseDays":$autoCloseDays,"downloadTreeUri":${JsonMini.quote(downloadTreeUri)},"confirmBeforeDownload":$confirmBeforeDownload}"""
  }

  private fun extractString(json: String, key: String): String? =
    Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(json)?.groupValues?.get(1)
      ?.replace("\\\"", "\"")
      ?.replace("\\\\", "\\")

  private fun extractBoolean(json: String, key: String): Boolean? =
    Regex(""""$key"\s*:\s*(true|false)""").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()

  private fun extractInt(json: String, key: String): Int? =
    Regex(""""$key"\s*:\s*(-?\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull()
}

// [RUST-MIGRATION-TARGET]
// pub struct RustDesignConfigEngine {
//     state: RwLock<DesignConfigRecord>,
// }
class DesignConfigImpl : DesignConfigContract {

  private val lock = Any()

  private var iconCornerRadiusDp: Float = 22f     // 0f..24f
  private var barCornerRadiusDp: Float = 28f      // 0f..36f
  private var cardCornerRadiusDp: Float = 20f     // 8f..28f
  private var barHeightDp: Float = 56f            // 44f..68f
  private var barWidthFraction: Float = 0.94f     // 0.60f..1.00f
  private var barHorizontalBias: Float = 0.0f     // -1.0f..1.0f
  private var barBottomOffsetDp: Float = 12f      // 4f..40f
  private var barOpacity: Float = 0.90f           // 0.55f..1.00f
  private var barBlurRadiusDp: Float = 18f        // 0f..32f

  override fun getDesignConfigJson(): String = synchronized(lock) { serializeLocked() }

  override fun updateDesignParam(key: String, value: String): String = synchronized(lock) {
    applyParamLocked(key, value)
    serializeLocked()
  }

  override fun restoreDesignConfigJson(rawJson: String): String = synchronized(lock) {
    if (rawJson.isBlank()) return@synchronized serializeLocked()
    extractFloat(rawJson, "iconCornerRadiusDp")?.let { iconCornerRadiusDp = it.coerceIn(0f, 24f) }
    extractFloat(rawJson, "barCornerRadiusDp")?.let { barCornerRadiusDp = it.coerceIn(0f, 36f) }
    extractFloat(rawJson, "cardCornerRadiusDp")?.let { cardCornerRadiusDp = it.coerceIn(8f, 28f) }
    extractFloat(rawJson, "barHeightDp")?.let { barHeightDp = it.coerceIn(44f, 68f) }
    extractFloat(rawJson, "barWidthFraction")?.let { barWidthFraction = it.coerceIn(0.60f, 1.0f) }
    extractFloat(rawJson, "barHorizontalBias")?.let { barHorizontalBias = it.coerceIn(-1.0f, 1.0f) }
    extractFloat(rawJson, "barBottomOffsetDp")?.let { barBottomOffsetDp = it.coerceIn(4f, 40f) }
    extractFloat(rawJson, "barOpacity")?.let { barOpacity = it.coerceIn(0.55f, 1.0f) }
    extractFloat(rawJson, "barBlurRadiusDp")?.let { barBlurRadiusDp = it.coerceIn(0f, 32f) }

    // Legacy key backwards-compatibility migration
    extractFloat(rawJson, "blurIntensityPercent")?.let {
      if (extractFloat(rawJson, "barBlurRadiusDp") == null) {
        barBlurRadiusDp = (it * 0.32f).coerceIn(0f, 32f)
      }
    }
    extractFloat(rawJson, "barOuterRadiusDp")?.let { barCornerRadiusDp = it.coerceIn(0f, 36f) }
    extractFloat(rawJson, "cardRadiusDp")?.let { cardCornerRadiusDp = it.coerceIn(8f, 28f) }
    extractFloat(rawJson, "barWidthPercent")?.let {
      barWidthFraction = (it / 100f).coerceIn(0.60f, 1.0f)
    }
    extractFloat(rawJson, "bottomOffsetDp")?.let { barBottomOffsetDp = it.coerceIn(4f, 40f) }
    extractFloat(rawJson, "surfaceOpacityPercent")?.let {
      barOpacity = (it / 100f).coerceIn(0.55f, 1.0f)
    }
    serializeLocked()
  }

  override fun resetDesignDefaults(): String = synchronized(lock) {
    iconCornerRadiusDp = 22f
    barCornerRadiusDp = 28f
    cardCornerRadiusDp = 20f
    barHeightDp = 56f
    barWidthFraction = 0.94f
    barHorizontalBias = 0.0f
    barBottomOffsetDp = 12f
    barOpacity = 0.90f
    barBlurRadiusDp = 18f
    serializeLocked()
  }

  private fun applyParamLocked(key: String, value: String) {
    val parsed = value.toFloatOrNull() ?: return
    when (key) {
      "iconCornerRadiusDp" -> iconCornerRadiusDp = parsed.coerceIn(0f, 24f)
      "barCornerRadiusDp", "barOuterRadiusDp" -> barCornerRadiusDp = parsed.coerceIn(0f, 36f)
      "cardCornerRadiusDp", "cardRadiusDp" -> cardCornerRadiusDp = parsed.coerceIn(8f, 28f)
      "barHeightDp" -> barHeightDp = parsed.coerceIn(44f, 68f)
      "barWidthFraction" -> barWidthFraction = parsed.coerceIn(0.60f, 1.0f)
      "barWidthPercent" -> barWidthFraction = (parsed / 100f).coerceIn(0.60f, 1.0f)
      "barHorizontalBias" -> barHorizontalBias = parsed.coerceIn(-1.0f, 1.0f)
      "barBottomOffsetDp", "bottomOffsetDp" -> barBottomOffsetDp = parsed.coerceIn(4f, 40f)
      "barOpacity" -> barOpacity = parsed.coerceIn(0.55f, 1.0f)
      "surfaceOpacityPercent" -> barOpacity = (parsed / 100f).coerceIn(0.55f, 1.0f)
      "barBlurRadiusDp" -> barBlurRadiusDp = parsed.coerceIn(0f, 32f)
      "blurIntensityPercent" -> barBlurRadiusDp = (parsed * 0.32f).coerceIn(0f, 32f)
    }
  }

  private fun serializeLocked(): String {
    return """{"iconCornerRadiusDp":$iconCornerRadiusDp,"barCornerRadiusDp":$barCornerRadiusDp,"cardCornerRadiusDp":$cardCornerRadiusDp,"barHeightDp":$barHeightDp,"barWidthFraction":$barWidthFraction,"barHorizontalBias":$barHorizontalBias,"barBottomOffsetDp":$barBottomOffsetDp,"barOpacity":$barOpacity,"barBlurRadiusDp":$barBlurRadiusDp}"""
  }

  private fun extractFloat(json: String, key: String): Float? =
    Regex(""""$key"\s*:\s*(-?\d+(?:\.\d+)?)""").find(json)?.groupValues?.get(1)?.toFloatOrNull()
}

// [RUST-MIGRATION-TARGET]
// pub struct RustSitePermissionsEngine {
//     global_policy: RwLock<SitePolicyRecord>,
//     overrides: RwLock<BTreeMap<String, SitePolicyRecord>>,
// }
class SitePermissionsImpl : SitePermissionsContract {

  private data class Policy(
    var javascriptEnabled: Boolean = true,
    var popupsAllowed: Boolean = false,
    var desktopSite: Boolean = false,
    var blockImages: Boolean = false,
    var cameraPolicy: String = "ASK",
    var microphonePolicy: String = "ASK",
    var locationPolicy: String = "ASK",
    var clipboardPolicy: String = "ASK",
    var notificationsPolicy: String = "BLOCK",
    var sensorsPolicy: String = "ASK",
    var autoplayMedia: String = "BLOCK",
  ) {
    fun copy(): Policy =
      Policy(
        javascriptEnabled = javascriptEnabled,
        popupsAllowed = popupsAllowed,
        desktopSite = desktopSite,
        blockImages = blockImages,
        cameraPolicy = cameraPolicy,
        microphonePolicy = microphonePolicy,
        locationPolicy = locationPolicy,
        clipboardPolicy = clipboardPolicy,
        notificationsPolicy = notificationsPolicy,
        sensorsPolicy = sensorsPolicy,
        autoplayMedia = autoplayMedia,
      )
  }

  private val lock = Any()
  private val globalPolicy = Policy()
  private val domainOverrides = linkedMapOf<String, Policy>()

  override fun getGlobalPolicyJson(): String = synchronized(lock) { serializePolicy(globalPolicy) }

  override fun setGlobalPolicy(key: String, value: String): String = synchronized(lock) {
    applyPolicyKey(globalPolicy, key, value)
    serializePolicy(globalPolicy)
  }

  override fun getEffectivePolicyJson(domain: String): String = synchronized(lock) {
    val clean = domain.trim().lowercase().removePrefix("www.")
    val p = domainOverrides[clean] ?: globalPolicy
    serializePolicy(p)
  }

  override fun setDomainOverride(domain: String, key: String, value: String): String =
    synchronized(lock) {
      val clean = domain.trim().lowercase().removePrefix("www.")
      if (clean.isNotEmpty() && !clean.startsWith("about:")) {
        val entry = domainOverrides.getOrPut(clean) { globalPolicy.copy() }
        applyPolicyKey(entry, key, value)
      }
      exportStateJsonLocked()
    }

  override fun resetDomainOverrides(domain: String): String = synchronized(lock) {
    val clean = domain.trim().lowercase().removePrefix("www.")
    domainOverrides.remove(clean)
    exportStateJsonLocked()
  }

  override fun listDomainOverridesJson(query: String): String = synchronized(lock) {
    val q = query.trim().lowercase()
    val filtered =
      domainOverrides.entries.filter { q.isEmpty() || it.key.contains(q) }
    val items =
      filtered.joinToString(",") { (dom, p) ->
        """{"domain":${JsonMini.quote(dom)},"policy":${serializePolicy(p)}}"""
      }
    "[$items]"
  }

  override fun exportStateJson(): String = synchronized(lock) { exportStateJsonLocked() }

  override fun restoreStateJson(rawJson: String): String = synchronized(lock) {
    if (rawJson.isBlank()) return@synchronized exportStateJsonLocked()
    val globalMatch = Regex(""""global"\s*:\s*(\{[^{}]*\})""").find(rawJson)?.groupValues?.get(1)
    if (globalMatch != null) {
      parsePolicyInto(globalMatch, globalPolicy)
    }
    domainOverrides.clear()
    val overridesBody =
      Regex(""""overrides"\s*:\s*\[(.*)]""").find(rawJson)?.groupValues?.get(1) ?: ""
    val entryRegex = Regex("""\{"domain"\s*:\s*"([^"]+)"\s*,\s*"policy"\s*:\s*(\{[^{}]*\})\}""")
    entryRegex.findAll(overridesBody).forEach { m ->
      val dom = m.groupValues[1].lowercase()
      val pol = globalPolicy.copy()
      parsePolicyInto(m.groupValues[2], pol)
      domainOverrides[dom] = pol
    }
    exportStateJsonLocked()
  }

  private fun exportStateJsonLocked(): String {
    val items =
      domainOverrides.entries.joinToString(",") { (dom, p) ->
        """{"domain":${JsonMini.quote(dom)},"policy":${serializePolicy(p)}}"""
      }
    return """{"global":${serializePolicy(globalPolicy)},"overrides":[$items]}"""
  }

  private fun applyPolicyKey(p: Policy, key: String, value: String) {
    val upper = value.trim().uppercase()
    when (key) {
      "javascriptEnabled" -> value.toBooleanStrictOrNull()?.let { p.javascriptEnabled = it }
      "popupsAllowed" -> value.toBooleanStrictOrNull()?.let { p.popupsAllowed = it }
      "desktopSite" -> value.toBooleanStrictOrNull()?.let { p.desktopSite = it }
      "blockImages" -> value.toBooleanStrictOrNull()?.let { p.blockImages = it }
      "cameraPolicy" -> if (upper in setOf("ASK", "BLOCK", "ALLOW")) p.cameraPolicy = upper
      "microphonePolicy" ->
        if (upper in setOf("ASK", "BLOCK", "ALLOW")) p.microphonePolicy = upper
      "locationPolicy" -> if (upper in setOf("ASK", "BLOCK", "ALLOW")) p.locationPolicy = upper
      "clipboardPolicy" -> if (upper in setOf("ASK", "BLOCK")) p.clipboardPolicy = upper
      "notificationsPolicy" ->
        if (upper in setOf("ASK", "BLOCK")) p.notificationsPolicy = upper
      "sensorsPolicy" -> if (upper in setOf("ASK", "BLOCK")) p.sensorsPolicy = upper
      "autoplayMedia" -> if (upper in setOf("BLOCK", "ALLOW")) p.autoplayMedia = upper
    }
  }

  private fun parsePolicyInto(obj: String, p: Policy) {
    Regex(""""javascriptEnabled"\s*:\s*(true|false)""").find(obj)?.groupValues?.get(1)
      ?.toBooleanStrictOrNull()?.let { p.javascriptEnabled = it }
    Regex(""""popupsAllowed"\s*:\s*(true|false)""").find(obj)?.groupValues?.get(1)
      ?.toBooleanStrictOrNull()?.let { p.popupsAllowed = it }
    Regex(""""desktopSite"\s*:\s*(true|false)""").find(obj)?.groupValues?.get(1)
      ?.toBooleanStrictOrNull()?.let { p.desktopSite = it }
    Regex(""""blockImages"\s*:\s*(true|false)""").find(obj)?.groupValues?.get(1)
      ?.toBooleanStrictOrNull()?.let { p.blockImages = it }
    Regex(""""cameraPolicy"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "cameraPolicy", it) }
    Regex(""""microphonePolicy"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "microphonePolicy", it) }
    Regex(""""locationPolicy"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "locationPolicy", it) }
    Regex(""""clipboardPolicy"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "clipboardPolicy", it) }
    Regex(""""notificationsPolicy"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "notificationsPolicy", it) }
    Regex(""""sensorsPolicy"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "sensorsPolicy", it) }
    Regex(""""autoplayMedia"\s*:\s*"([^"]+)"""").find(obj)?.groupValues?.get(1)
      ?.let { applyPolicyKey(p, "autoplayMedia", it) }
  }

  private fun serializePolicy(p: Policy): String {
    return """{"javascriptEnabled":${p.javascriptEnabled},"popupsAllowed":${p.popupsAllowed},"desktopSite":${p.desktopSite},"blockImages":${p.blockImages},"cameraPolicy":${JsonMini.quote(p.cameraPolicy)},"microphonePolicy":${JsonMini.quote(p.microphonePolicy)},"locationPolicy":${JsonMini.quote(p.locationPolicy)},"clipboardPolicy":${JsonMini.quote(p.clipboardPolicy)},"notificationsPolicy":${JsonMini.quote(p.notificationsPolicy)},"sensorsPolicy":${JsonMini.quote(p.sensorsPolicy)},"autoplayMedia":${JsonMini.quote(p.autoplayMedia)}}"""
  }
}
