package com.mybrowser.core.bridge

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Pure Kotlin URL parser & privacy-preserving search router.
 * Supports DuckDuckGo, Brave Search, Startpage, Kagi, and Custom OpenSearch templates.
 * Zero GMS dependencies.
 */
// [RUST-MIGRATION-TARGET]
// pub struct RustUrlEngine;
// // Replaceable with `url::Url` + `psl` crate in Rust.
class UrlEngineImpl : UrlEngineContract {

  private data class SearchEngineSpec(
    val id: String,
    val label: String,
    val templateUrl: String,
    val homepageUrl: String,
  )

  private val engines =
    listOf(
      SearchEngineSpec(
        id = "ddg",
        label = "DuckDuckGo",
        templateUrl = "https://duckduckgo.com/?q=%s",
        homepageUrl = "https://duckduckgo.com",
      ),
      SearchEngineSpec(
        id = "brave",
        label = "Brave Search",
        templateUrl = "https://search.brave.com/search?q=%s",
        homepageUrl = "https://search.brave.com",
      ),
      SearchEngineSpec(
        id = "startpage",
        label = "Startpage",
        templateUrl = "https://www.startpage.com/do/search?q=%s",
        homepageUrl = "https://www.startpage.com",
      ),
      SearchEngineSpec(
        id = "ecosia",
        label = "Ecosia",
        templateUrl = "https://www.ecosia.org/search?q=%s",
        homepageUrl = "https://www.ecosia.org",
      ),
      SearchEngineSpec(
        id = "naver",
        label = "Naver",
        templateUrl = "https://search.naver.com/search.naver?query=%s",
        homepageUrl = "https://www.naver.com",
      ),
      SearchEngineSpec(
        id = "google",
        label = "Google(웹)",
        templateUrl = "https://www.google.com/search?q=%s",
        homepageUrl = "https://www.google.com",
      ),
      SearchEngineSpec(
        id = "kagi",
        label = "Kagi",
        templateUrl = "https://kagi.com/search?q=%s",
        homepageUrl = "https://kagi.com",
      ),
      SearchEngineSpec(
        id = "custom",
        label = "사용자 지정",
        templateUrl = "https://www.mojeek.com/search?q=%s",
        homepageUrl = "https://www.mojeek.com",
      ),
    )

  override fun normalizeAndResolveInput(
    rawInput: String,
    defaultSearchEngineId: String,
    customSearchTemplate: String,
  ): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty() || trimmed.equals("about:mb", ignoreCase = true)) {
      return buildResultJson(
        resolvedUrl = "about:mb",
        isSearch = false,
        isInternal = true,
        isSecure = true,
        displayHost = "about:mb",
      )
    }

    if (trimmed.equals("about:blank", ignoreCase = true)) {
      return buildResultJson(
        resolvedUrl = "about:blank",
        isSearch = false,
        isInternal = true,
        isSecure = true,
        displayHost = "about:blank",
      )
    }

    if (trimmed.startsWith("about:", ignoreCase = true)) {
      return buildResultJson(
        resolvedUrl = "about:mb",
        isSearch = false,
        isInternal = true,
        isSecure = true,
        displayHost = "about:mb",
      )
    }

    val hasWhitespace = trimmed.any { it.isWhitespace() }
    val explicitScheme =
      trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true)

    val resolvedUrl: String
    val isSearch: Boolean

    if (!hasWhitespace && explicitScheme) {
      resolvedUrl = trimmed
      isSearch = false
    } else if (!hasWhitespace && looksLikeHostOrIp(trimmed)) {
      val scheme = if (isLocalHostOrIp(trimmed)) "http://" else "https://"
      resolvedUrl = "$scheme$trimmed"
      isSearch = false
    } else {
      val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
      val template =
        if (defaultSearchEngineId == "custom" && customSearchTemplate.contains("%s")) {
          customSearchTemplate
        } else {
          (engines.firstOrNull { it.id == defaultSearchEngineId } ?: engines.first()).templateUrl
        }
      resolvedUrl = template.replace("%s", encoded)
      isSearch = true
    }

    val domain = extractRegistrableDomain(resolvedUrl)
    val secure = isSecureOrigin(resolvedUrl)
    return buildResultJson(
      resolvedUrl = resolvedUrl,
      isSearch = isSearch,
      isInternal = false,
      isSecure = secure,
      displayHost = domain.ifEmpty { resolvedUrl },
    )
  }

  override fun extractRegistrableDomain(url: String): String {
    if (url.startsWith("about:", ignoreCase = true)) return url.lowercase()
    return try {
      val normalized = if (url.contains("://")) url else "https://$url"
      val host = URI(normalized).host?.lowercase() ?: return ""
      host.removePrefix("www.")
    } catch (_: Exception) {
      ""
    }
  }

  override fun isSecureOrigin(url: String): Boolean {
    val lower = url.lowercase()
    return lower.startsWith("https://") || lower.startsWith("about:")
  }

  override fun upgradeToHttpsIfNeeded(url: String, httpsOnly: Boolean): String {
    if (!httpsOnly) return url
    if (!url.startsWith("http://", ignoreCase = true)) return url
    val domain = extractRegistrableDomain(url)
    if (isLocalHostOrIp(domain)) return url
    return "https://" + url.substring(7)
  }

  override fun getSearchEnginesJson(): String {
    val items =
      engines.joinToString(",") { spec ->
        """{"id":${JsonMini.quote(spec.id)},"label":${JsonMini.quote(spec.label)},"homepage":${JsonMini.quote(spec.homepageUrl)}}"""
      }
    return "[$items]"
  }

  private fun looksLikeHostOrIp(input: String): Boolean {
    if (input.startsWith("localhost", ignoreCase = true)) return true
    val hostPart = input.substringBefore('/').substringBefore('?').substringBefore(':')
    if (ipv4Regex.matches(hostPart)) return true
    if (!hostPart.contains('.')) return false
    val tld = hostPart.substringAfterLast('.')
    return tld.length in 2..12 && tld.all { it.isLetter() }
  }

  private fun isLocalHostOrIp(input: String): Boolean {
    val hostPart = input.substringBefore('/').substringBefore(':').lowercase()
    return hostPart == "localhost" ||
      hostPart.startsWith("127.") ||
      hostPart.startsWith("192.168.") ||
      hostPart.startsWith("10.")
  }

  private fun buildResultJson(
    resolvedUrl: String,
    isSearch: Boolean,
    isInternal: Boolean,
    isSecure: Boolean,
    displayHost: String,
  ): String {
    return """{"resolvedUrl":${JsonMini.quote(resolvedUrl)},"isSearch":$isSearch,"isInternal":$isInternal,"isSecure":$isSecure,"displayHost":${JsonMini.quote(displayHost)}}"""
  }

  private companion object {
    val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
  }
}
