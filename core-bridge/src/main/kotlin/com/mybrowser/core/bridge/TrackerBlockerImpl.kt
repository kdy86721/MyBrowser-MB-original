package com.mybrowser.core.bridge

import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * High-speed, zero-allocation-verdict tracker & ad blocker.
 * Returns a 12-byte big-endian binary frame on the hot `shouldInterceptRequest` path.
 * Supports STANDARD and STRICT blocking levels.
 */
// [RUST-MIGRATION-TARGET]
// pub struct RustTrackerBlocker {
//     enabled: AtomicBool,
//     level: RwLock<String>,
//     domain_rules: DashMap<String, u8>,
//     stats: BlockerTelemetry,
// }
class TrackerBlockerImpl : TrackerBlockerContract {

  private val enabled = AtomicBoolean(true)
  private val level = AtomicReference("STANDARD")
  private val domainRules = ConcurrentHashMap<String, Int>()
  private val strictOnlyRules = ConcurrentHashMap<String, Int>()
  private val totalEvaluated = AtomicLong(0)
  private val blockedAds = AtomicLong(0)
  private val blockedTrackers = AtomicLong(0)
  private val blockedFingerprinting = AtomicLong(0)
  private val blockedTelemetry = AtomicLong(0)

  init {
    seedDefaultRules()
  }

  override fun shouldBlockRequest(
    requestUrl: String,
    pageHost: String,
    resourceType: Int,
  ): ByteArray {
    val startNs = System.nanoTime()
    totalEvaluated.incrementAndGet()

    if (!enabled.get()) {
      return packVerdict(
        blocked = false,
        category = CAT_NONE,
        ruleHash = 0,
        latencyMicros = elapsedMicros(startNs),
        thirdParty = false,
      )
    }

    val isStrict = level.get() == "STRICT"
    val requestHost = extractHost(requestUrl)
    val cleanPageHost = pageHost.lowercase().removePrefix("www.")
    val isThirdParty =
      requestHost.isNotEmpty() &&
        cleanPageHost.isNotEmpty() &&
        requestHost != cleanPageHost &&
        !requestHost.endsWith(".$cleanPageHost")

    val matchedCategory =
      matchDomainHierarchy(requestHost, domainRules)
        ?: (if (isStrict) matchDomainHierarchy(requestHost, strictOnlyRules) else null)
        ?: matchPathKeywords(requestUrl, isStrict)

    val shouldBlock =
      matchedCategory != CAT_NONE &&
        (isThirdParty || matchedCategory == CAT_FINGERPRINTING || isStrict)

    if (shouldBlock) {
      when (matchedCategory) {
        CAT_AD_NETWORK -> blockedAds.incrementAndGet()
        CAT_TRACKER -> blockedTrackers.incrementAndGet()
        CAT_FINGERPRINTING -> blockedFingerprinting.incrementAndGet()
        CAT_TELEMETRY -> blockedTelemetry.incrementAndGet()
      }
    }

    return packVerdict(
      blocked = shouldBlock,
      category = if (shouldBlock) matchedCategory else CAT_NONE,
      ruleHash = if (shouldBlock) requestHost.hashCode() else 0,
      latencyMicros = elapsedMicros(startNs),
      thirdParty = isThirdParty,
    )
  }

  override fun loadFilterListBytes(easylistChunk: ByteArray): Int {
    val text = String(easylistChunk, StandardCharsets.UTF_8)
    text.lineSequence().forEach { rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) return@forEach
      if (line.startsWith("||") && line.endsWith("^")) {
        val domain = line.removePrefix("||").removeSuffix("^").lowercase()
        if (domain.contains('.') && !domain.contains('/')) {
          domainRules[domain] = CAT_TRACKER
        }
      } else if (!line.contains(' ') && line.contains('.')) {
        domainRules[line.lowercase()] = CAT_TRACKER
      }
    }
    return getActiveRuleCount()
  }

  override fun setEnabled(enabled: Boolean) {
    this.enabled.set(enabled)
  }

  override fun isEnabled(): Boolean = enabled.get()

  override fun setLevel(level: String) {
    val normalized = level.trim().uppercase()
    if (normalized == "STANDARD" || normalized == "STRICT") {
      this.level.set(normalized)
    }
  }

  override fun getLevel(): String = level.get()

  override fun getActiveRuleCount(): Int =
    domainRules.size + if (level.get() == "STRICT") strictOnlyRules.size else 0

  override fun getSessionStatsJson(): String {
    val ads = blockedAds.get()
    val trackers = blockedTrackers.get()
    val fp = blockedFingerprinting.get()
    val telemetry = blockedTelemetry.get()
    val totalBlocked = ads + trackers + fp + telemetry
    return """{"enabled":${enabled.get()},"level":${JsonMini.quote(level.get())},"ruleCount":${getActiveRuleCount()},"evaluated":${totalEvaluated.get()},"totalBlocked":$totalBlocked,"ads":$ads,"trackers":$trackers,"fingerprinting":$fp,"telemetry":$telemetry}"""
  }

  override fun resetSessionStats() {
    totalEvaluated.set(0)
    blockedAds.set(0)
    blockedTrackers.set(0)
    blockedFingerprinting.set(0)
    blockedTelemetry.set(0)
  }

  private fun matchDomainHierarchy(host: String, map: ConcurrentHashMap<String, Int>): Int? {
    if (host.isEmpty()) return null
    var candidate = host
    while (candidate.isNotEmpty()) {
      map[candidate]?.let { return it }
      val dotIdx = candidate.indexOf('.')
      if (dotIdx < 0) break
      candidate = candidate.substring(dotIdx + 1)
    }
    return null
  }

  private fun matchPathKeywords(url: String, strict: Boolean): Int {
    val lower = url.lowercase()
    return when {
      lower.contains("fingerprint") || lower.contains("fpjs") -> CAT_FINGERPRINTING
      lower.contains("/pagead/") || lower.contains("/adsense") || lower.contains("adserver") ->
        CAT_AD_NETWORK
      lower.contains("/collect?") || lower.contains("/telemetry") || lower.contains("/beacon") ->
        CAT_TELEMETRY
      strict && (lower.contains("/pixel") || lower.contains("/analytics") || lower.contains("utm_source")) ->
        CAT_TRACKER
      else -> CAT_NONE
    }
  }

  private fun extractHost(url: String): String {
    return try {
      URI(url).host?.lowercase()?.removePrefix("www.") ?: ""
    } catch (_: Exception) {
      ""
    }
  }

  private fun elapsedMicros(startNs: Long): Int {
    val delta = (System.nanoTime() - startNs) / 1_000L
    return delta.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
  }

  private fun packVerdict(
    blocked: Boolean,
    category: Int,
    ruleHash: Int,
    latencyMicros: Int,
    thirdParty: Boolean,
  ): ByteArray {
    val buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
    buffer.put(if (blocked) 1.toByte() else 0.toByte())
    buffer.put(category.toByte())
    buffer.putInt(ruleHash)
    buffer.putInt(latencyMicros)
    buffer.putShort(if (thirdParty) 1.toShort() else 0.toShort())
    return buffer.array()
  }

  private fun seedDefaultRules() {
    val ads =
      listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "ads.pubmatic.com",
        "pubmatic.com",
        "rubiconproject.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "adnxs.com",
        "openx.net",
        "casalemedia.com",
        "amazon-adsystem.com",
        "media.net",
        "moatads.com",
      )
    val trackers =
      listOf(
        "google-analytics.com",
        "googletagmanager.com",
        "analytics.google.com",
        "connect.facebook.net",
        "pixel.facebook.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "chartbeat.com",
        "newrelic.com",
        "omtrdc.net",
        "demdex.net",
        "krxd.net",
      )
    val fingerprinting =
      listOf(
        "fpjs.io",
        "fingerprintjs.com",
        "maxmind.com",
        "iovation.com",
        "threatmetrix.com",
      )
    val telemetry =
      listOf(
        "firebaselogging.googleapis.com",
        "app-measurement.com",
        "crashlytics.com",
        "sentry.io",
        "bugsnag.com",
        "stats.g.doubleclick.net",
      )
    val strictExtraTrackers =
      listOf(
        "clarity.ms",
        "fullstory.com",
        "logrocket.com",
        "heap.io",
        "heapanalytics.com",
        "intercom.io",
        "ads-twitter.com",
        "analytics.twitter.com",
        "analytics.tiktok.com",
        "snap.licdn.com",
        "mc.yandex.ru",
        "bat.bing.com",
      )

    ads.forEach { domainRules[it] = CAT_AD_NETWORK }
    trackers.forEach { domainRules[it] = CAT_TRACKER }
    fingerprinting.forEach { domainRules[it] = CAT_FINGERPRINTING }
    telemetry.forEach { domainRules[it] = CAT_TELEMETRY }
    strictExtraTrackers.forEach { strictOnlyRules[it] = CAT_TRACKER }
  }

  companion object {
    const val CAT_NONE = 0
    const val CAT_AD_NETWORK = 1
    const val CAT_TRACKER = 2
    const val CAT_FINGERPRINTING = 3
    const val CAT_TELEMETRY = 4
  }
}
