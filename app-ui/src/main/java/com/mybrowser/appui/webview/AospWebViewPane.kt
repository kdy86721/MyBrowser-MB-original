package com.mybrowser.appui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.LocationManager
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mybrowser.core.bridge.TrackerBlockerContract
import com.mybrowser.core.bridge.UrlEngineContract
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

@Immutable
data class WebViewNavCapabilities(
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
)

@Immutable
data class WebRenderPolicy(
  val fontZoomPercent: Int = 100,
  val forceWebDark: Boolean = false,
  val forceZoomEnabled: Boolean = false,
  val blockThirdPartyCookies: Boolean = true,
  val sendDntGpcHeaders: Boolean = true,
  val httpsOnlyMode: Boolean = true,
  val confirmExternalApp: Boolean = true,
  val javascriptEnabled: Boolean = true,
  val popupsAllowed: Boolean = false,
  val desktopSite: Boolean = false,
  val blockImages: Boolean = false,
  val autoplayMedia: String = "BLOCK",
  val cameraPolicy: String = "ASK",
  val microphonePolicy: String = "ASK",
  val locationPolicy: String = "ASK",
)

@Immutable
data class ExternalAppLaunchRequest(
  val url: String,
  val scheme: String,
)

@Immutable
data class PendingDownloadRequest(
  val url: String,
  val filename: String,
  val mimeType: String,
  val userAgent: String,
  val contentLength: Long,
)

sealed interface WebSitePermissionChallenge {
  val domain: String

  data class MediaCapture(
    override val domain: String,
    val needsCamera: Boolean,
    val needsMic: Boolean,
    val request: PermissionRequest,
  ) : WebSitePermissionChallenge

  data class Geolocation(
    override val domain: String,
    val origin: String,
    val hasAospLocationProvider: Boolean,
    val callback: GeolocationPermissions.Callback,
  ) : WebSitePermissionChallenge
}

/**
 * Pure AOSP WebView Registry.
 * Strictly relies on standard Android SDK `WebView`, `CookieManager`, and `LocationManager`
 * without any GMS / Play Services dependencies.
 */
class AospWebViewPool(
  private val urlEngine: UrlEngineContract,
  private val trackerBlocker: TrackerBlockerContract,
) {
  private val webViews = ConcurrentHashMap<String, WebView>()
  private val lastLoadedUrl = ConcurrentHashMap<String, String>()
  private val appliedPolicies = ConcurrentHashMap<String, WebRenderPolicy>()
  private val scrollListeners = ConcurrentHashMap<String, (Int) -> Unit>()
  private val permissionCallbacks =
    ConcurrentHashMap<String, (WebSitePermissionChallenge) -> Unit>()
  private val externalAppCallbacks =
    ConcurrentHashMap<String, (ExternalAppLaunchRequest) -> Unit>()
  private val downloadCallbacks =
    ConcurrentHashMap<String, (PendingDownloadRequest) -> Unit>()
  private val iconCallbacks = ConcurrentHashMap<String, (Bitmap?) -> Unit>()
  private val pausedTabs = ConcurrentHashMap.newKeySet<String>()

  @SuppressLint("SetJavaScriptEnabled")
  fun getOrCreate(
    context: Context,
    tabId: String,
    renderPolicy: WebRenderPolicy,
    onNavigationUpdate: (url: String, title: String, isLoading: Boolean, progress: Int, caps: WebViewNavCapabilities) -> Unit,
    onRequestBlocked: () -> Unit,
    onScrollDelta: (Int) -> Unit,
    onPermissionChallenge: (WebSitePermissionChallenge) -> Unit,
    onExternalAppRequest: (ExternalAppLaunchRequest) -> Unit = {},
    onDownloadRequest: (PendingDownloadRequest) -> Unit = {},
    onIconUpdate: (Bitmap?) -> Unit = {},
  ): WebView {
    scrollListeners[tabId] = onScrollDelta
    permissionCallbacks[tabId] = onPermissionChallenge
    externalAppCallbacks[tabId] = onExternalAppRequest
    downloadCallbacks[tabId] = onDownloadRequest
    iconCallbacks[tabId] = onIconUpdate

    val existing = webViews[tabId]
    if (existing != null) {
      val prev = appliedPolicies[tabId]
      if (prev != renderPolicy) {
        val needsReload =
          prev?.desktopSite != renderPolicy.desktopSite ||
            prev?.javascriptEnabled != renderPolicy.javascriptEnabled ||
            prev?.forceWebDark != renderPolicy.forceWebDark ||
            prev?.forceZoomEnabled != renderPolicy.forceZoomEnabled ||
            prev?.blockImages != renderPolicy.blockImages
        applyRenderPolicy(existing, renderPolicy)
        appliedPolicies[tabId] = renderPolicy
        val isInternal = lastLoadedUrl[tabId]?.startsWith("about:") == true
        if (needsReload && !existing.url.isNullOrBlank() && !isInternal) {
          existing.reload()
        }
      }
      return existing
    }

    val webView =
      WebView(context).apply {
        layoutParams =
          ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
          val dy = scrollY - oldScrollY
          if (dy != 0) {
            scrollListeners[tabId]?.invoke(dy)
          }
        }
        settings.apply {
          domStorageEnabled = true
          loadWithOverviewMode = true
          useWideViewPort = true
          setSupportZoom(true)
          builtInZoomControls = true
          displayZoomControls = false
          allowFileAccess = false
          allowContentAccess = false
          mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
          // Strict No-GMS: disable Play Services SafeBrowsing check on AOSP builds
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safeBrowsingEnabled = false
          }
        }
        applyRenderPolicy(this, renderPolicy)
        appliedPolicies[tabId] = renderPolicy

        setDownloadListener { dlUrl, ua, contentDisposition, mimetype, contentLength ->
          if (!dlUrl.isNullOrBlank()) {
            val file = URLUtil.guessFileName(dlUrl, contentDisposition, mimetype)
            downloadCallbacks[tabId]?.invoke(
              PendingDownloadRequest(
                url = dlUrl,
                filename = file.ifBlank { "download.bin" },
                mimeType = mimetype ?: "application/octet-stream",
                userAgent = ua ?: "",
                contentLength = contentLength,
              )
            )
          }
        }

        webViewClient =
          object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              view: WebView?,
              request: WebResourceRequest?,
            ): Boolean {
              val reqUrl = request?.url?.toString() ?: return false
              val scheme = request.url?.scheme?.lowercase() ?: ""
              val policy = appliedPolicies[tabId] ?: renderPolicy
              if (scheme !in setOf("http", "https", "about", "data", "javascript", "blob", "")) {
                externalAppCallbacks[tabId]?.invoke(
                  ExternalAppLaunchRequest(url = reqUrl, scheme = scheme)
                )
                return true
              }
              val upgraded = urlEngine.upgradeToHttpsIfNeeded(reqUrl, policy.httpsOnlyMode)
              if (upgraded != reqUrl) {
                loadWithHeaders(view, upgraded, policy.sendDntGpcHeaders)
                return true
              }
              return false
            }

            override fun shouldInterceptRequest(
              view: WebView?,
              request: WebResourceRequest?,
            ): WebResourceResponse? {
              val reqUrl = request?.url?.toString() ?: return null
              val pageUrl = lastLoadedUrl[tabId] ?: ""
              val pageHost = urlEngine.extractRegistrableDomain(pageUrl)
              val verdictFrame =
                trackerBlocker.shouldBlockRequest(
                  requestUrl = reqUrl,
                  pageHost = pageHost,
                  resourceType = if (request.isForMainFrame) 0 else 1,
                )
              val blocked = verdictFrame.isNotEmpty() && verdictFrame[0] == 1.toByte()
              if (blocked) {
                onRequestBlocked()
                return WebResourceResponse(
                  "text/plain",
                  "utf-8",
                  ByteArrayInputStream(ByteArray(0)),
                )
              }
              return null
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              val safeUrl = url ?: return
              if (lastLoadedUrl[tabId]?.startsWith("about:") == true && !safeUrl.startsWith("about:")) {
                return
              }
              lastLoadedUrl[tabId] = safeUrl
              onNavigationUpdate(
                safeUrl,
                view?.title ?: safeUrl,
                true,
                15,
                WebViewNavCapabilities(
                  canGoBack = view?.canGoBack() == true,
                  canGoForward = view?.canGoForward() == true,
                ),
              )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              val safeUrl = url ?: lastLoadedUrl[tabId] ?: ""
              if (lastLoadedUrl[tabId]?.startsWith("about:") == true && !safeUrl.startsWith("about:")) {
                return
              }
              if (safeUrl.isNotBlank()) lastLoadedUrl[tabId] = safeUrl
              val policy = appliedPolicies[tabId] ?: renderPolicy
              if (policy.forceWebDark && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                view?.evaluateJavascript(
                  "(function(){document.documentElement.style.filter='invert(0.92) hue-rotate(180deg)';})();",
                  null,
                )
              }
              if (policy.forceZoomEnabled) {
                view?.evaluateJavascript(
                  FORCE_ZOOM_VIEWPORT_SCRIPT,
                  null,
                )
              }
              onNavigationUpdate(
                safeUrl,
                view?.title ?: safeUrl,
                false,
                100,
                WebViewNavCapabilities(
                  canGoBack = view?.canGoBack() == true,
                  canGoForward = view?.canGoForward() == true,
                ),
              )
            }
          }

        webChromeClient =
          object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              if (lastLoadedUrl[tabId]?.startsWith("about:") == true) return
              val safeUrl = view?.url ?: lastLoadedUrl[tabId] ?: ""
              if (safeUrl.isBlank() || safeUrl == "about:blank") return
              onNavigationUpdate(
                safeUrl,
                view?.title ?: safeUrl,
                newProgress < 100,
                newProgress,
                WebViewNavCapabilities(
                  canGoBack = view?.canGoBack() == true,
                  canGoForward = view?.canGoForward() == true,
                ),
              )
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
              if (lastLoadedUrl[tabId]?.startsWith("about:") == true) return
              val safeUrl = view?.url ?: lastLoadedUrl[tabId] ?: ""
              if (safeUrl.isBlank() || safeUrl == "about:blank") return
              onNavigationUpdate(
                safeUrl,
                title ?: safeUrl,
                (view?.progress ?: 100) < 100,
                view?.progress ?: 100,
                WebViewNavCapabilities(
                  canGoBack = view?.canGoBack() == true,
                  canGoForward = view?.canGoForward() == true,
                ),
              )
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
              if (lastLoadedUrl[tabId]?.startsWith("about:") == true) return
              iconCallbacks[tabId]?.invoke(icon)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
              if (request == null) return
              val resources = request.resources ?: emptyArray()
              val needsCam = resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
              val needsMic = resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
              if (!needsCam && !needsMic) {
                request.deny()
                return
              }
              val domain = urlEngine.extractRegistrableDomain(request.origin?.toString() ?: "")
              val policy = appliedPolicies[tabId] ?: renderPolicy
              val camBlocked = needsCam && policy.cameraPolicy == "BLOCK"
              val micBlocked = needsMic && policy.microphonePolicy == "BLOCK"
              if (camBlocked || micBlocked) {
                request.deny()
                return
              }
              permissionCallbacks[tabId]?.invoke(
                WebSitePermissionChallenge.MediaCapture(
                  domain = domain.ifEmpty { "현재 사이트" },
                  needsCamera = needsCam,
                  needsMic = needsMic,
                  request = request,
                )
              ) ?: request.deny()
            }

            override fun onGeolocationPermissionsShowPrompt(
              origin: String?,
              callback: GeolocationPermissions.Callback?,
            ) {
              if (callback == null) return
              val policy = appliedPolicies[tabId] ?: renderPolicy
              if (policy.locationPolicy == "BLOCK") {
                callback.invoke(origin, false, false)
                return
              }
              val domain = urlEngine.extractRegistrableDomain(origin ?: "")
              // Pure AOSP LocationManager check (zero GMS FusedLocationProviderClient)
              val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
              val hasAospProvider =
                lm?.let {
                  it.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } ?: false
              permissionCallbacks[tabId]?.invoke(
                WebSitePermissionChallenge.Geolocation(
                  domain = domain.ifEmpty { origin ?: "현재 사이트" },
                  origin = origin ?: "",
                  hasAospLocationProvider = hasAospProvider,
                  callback = callback,
                )
              ) ?: callback.invoke(origin, false, false)
            }
          }
      }

    webViews[tabId] = webView
    return webView
  }

  private fun applyRenderPolicy(webView: WebView, policy: WebRenderPolicy) {
    webView.settings.apply {
      javaScriptEnabled = policy.javascriptEnabled
      javaScriptCanOpenWindowsAutomatically = policy.popupsAllowed
      setSupportMultipleWindows(policy.popupsAllowed)
      setSupportZoom(true)
      builtInZoomControls = true
      displayZoomControls = false
      loadsImagesAutomatically = !policy.blockImages
      blockNetworkImage = policy.blockImages
      textZoom = policy.fontZoomPercent.coerceIn(75, 200)
      mediaPlaybackRequiresUserGesture = policy.autoplayMedia == "BLOCK"
      userAgentString =
        if (policy.desktopSite) {
          "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        } else {
          null
        }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isAlgorithmicDarkeningAllowed = policy.forceWebDark
      }
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !policy.blockThirdPartyCookies)
  }

  private fun loadWithHeaders(view: WebView?, url: String, sendDntGpc: Boolean) {
    if (view == null) return
    if (sendDntGpc) {
      view.loadUrl(url, mapOf("DNT" to "1", "Sec-GPC" to "1"))
    } else {
      view.loadUrl(url)
    }
  }

  fun markInternalPage(tabId: String, internalUrl: String) {
    lastLoadedUrl[tabId] = internalUrl
    webViews[tabId]?.stopLoading()
  }

  fun loadUrl(tabId: String, url: String, sendDntGpc: Boolean = true) {
    if (url == "about:mb" || url == "about:blank") {
      markInternalPage(tabId, url)
      return
    }
    lastLoadedUrl[tabId] = url
    webViews[tabId]?.let { wv ->
      loadWithHeaders(wv, url, sendDntGpc)
    }
  }

  fun syncTargetUrlIfNeeded(tabId: String, targetUrl: String, sendDntGpc: Boolean = true) {
    if (targetUrl == "about:mb" || targetUrl == "about:blank") return
    val prevLast = lastLoadedUrl[tabId]
    val wv = webViews[tabId] ?: return
    if ((wv.url.isNullOrBlank() && prevLast != targetUrl) || prevLast?.startsWith("about:") == true) {
      lastLoadedUrl[tabId] = targetUrl
      loadWithHeaders(wv, targetUrl, sendDntGpc)
    }
  }

  fun captureTabBitmap(tabId: String): Bitmap? {
    val wv = webViews[tabId] ?: return null
    val w = wv.width
    val h = wv.height
    if (w <= 0 || h <= 0) return null
    return try {
      val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      wv.draw(canvas)
      bmp
    } catch (_: Throwable) {
      null
    }
  }

  fun captureTabThumbnailBitmap(tabId: String, maxWidthPx: Int = 420): Bitmap? {
    val wv = webViews[tabId] ?: return null
    val w = wv.width
    val h = wv.height
    if (w <= 0 || h <= 0) return null
    return try {
      val scale = (maxWidthPx.toFloat() / w.toFloat()).coerceIn(0.1f, 1f)
      val outW = (w * scale).toInt().coerceAtLeast(1)
      val outH = (h * scale).toInt().coerceAtLeast(1)
      val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.RGB_565)
      val canvas = Canvas(bmp)
      canvas.scale(scale, scale)
      wv.draw(canvas)
      bmp
    } catch (_: Throwable) {
      null
    }
  }

  fun goBack(tabId: String): Boolean {
    val wv = webViews[tabId] ?: return false
    return if (wv.canGoBack()) {
      wv.goBack()
      true
    } else {
      false
    }
  }

  fun goForward(tabId: String): Boolean {
    val wv = webViews[tabId] ?: return false
    return if (wv.canGoForward()) {
      wv.goForward()
      true
    } else {
      false
    }
  }

  fun reload(tabId: String) {
    webViews[tabId]?.reload()
  }

  fun stopLoading(tabId: String) {
    webViews[tabId]?.stopLoading()
  }

  fun findInPage(tabId: String, query: String, onMatchCount: (activeOrdinal: Int, total: Int) -> Unit) {
    val wv = webViews[tabId] ?: return
    if (query.isBlank()) {
      wv.clearMatches()
      onMatchCount(0, 0)
      return
    }
    wv.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
      val displayOrdinal = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
      onMatchCount(displayOrdinal, numberOfMatches)
    }
    wv.findAllAsync(query)
  }

  fun findNext(tabId: String, forward: Boolean) {
    webViews[tabId]?.findNext(forward)
  }

  fun clearFindInPage(tabId: String) {
    webViews[tabId]?.let { wv ->
      wv.clearMatches()
      wv.setFindListener(null)
    }
  }

  fun clearBrowsingData(clearCache: Boolean, clearCookies: Boolean) {
    if (clearCache) {
      webViews.values.forEach { it.clearCache(true) }
    }
    if (clearCookies) {
      CookieManager.getInstance().removeAllCookies(null)
      CookieManager.getInstance().flush()
    }
  }

  fun extractReaderContent(
    tabId: String,
    fallbackTitle: String,
    onResult: (title: String, paragraphs: List<String>) -> Unit,
  ) {
    val wv = webViews[tabId]
    if (wv == null) {
      onResult(fallbackTitle, emptyList())
      return
    }
    val script =
      """
      (function() {
        var root = document.querySelector('article') || document.querySelector('main') || document.body;
        if (!root) return JSON.stringify({title: document.title || '', blocks: []});
        var nodes = root.querySelectorAll('h1,h2,h3,p,li,blockquote');
        var blocks = [];
        for (var i = 0; i < nodes.length && blocks.length < 80; i++) {
          var t = (nodes[i].innerText || '').trim();
          if (t.length > 25) blocks.push(t);
        }
        if (blocks.length === 0 && root.innerText) {
          blocks = root.innerText.split('\n').map(function(s){return s.trim();}).filter(function(s){return s.length > 25;}).slice(0, 60);
        }
        return JSON.stringify({title: document.title || '', blocks: blocks});
      })();
      """.trimIndent()

    wv.evaluateJavascript(script) { raw ->
      try {
        val unquoted =
          if (raw != null && raw.startsWith("\"") && raw.endsWith("\"")) {
            JSONObject("{\"v\":$raw}").getString("v")
          } else {
            raw ?: "{}"
          }
        val obj = JSONObject(unquoted)
        val title = obj.optString("title").ifBlank { fallbackTitle }
        val arr = obj.optJSONArray("blocks")
        val blocks = buildList {
          if (arr != null) {
            for (i in 0 until arr.length()) {
              add(arr.getString(i))
            }
          }
        }
        onResult(title, blocks)
      } catch (_: Exception) {
        onResult(fallbackTitle, emptyList())
      }
    }
  }

  fun getCapabilities(tabId: String): WebViewNavCapabilities {
    val wv = webViews[tabId] ?: return WebViewNavCapabilities()
    return WebViewNavCapabilities(wv.canGoBack(), wv.canGoForward())
  }

  fun syncVisibleTabs(visibleTabIds: Set<String>, pauseBackgroundTabs: Boolean = true) {
    webViews.forEach { (id, wv) ->
      if (id in visibleTabIds || !pauseBackgroundTabs) {
        if (pausedTabs.remove(id)) {
          wv.onResume()
        }
      } else {
        if (pausedTabs.add(id)) {
          wv.onPause()
        }
      }
    }
  }

  fun onAppBackgrounded() {
    webViews.forEach { (id, wv) ->
      if (pausedTabs.add(id)) {
        wv.onPause()
      }
      wv.pauseTimers()
    }
  }

  fun onAppForegrounded(visibleTabIds: Set<String>, pauseBackgroundTabs: Boolean = true) {
    webViews.values.firstOrNull()?.resumeTimers()
    syncVisibleTabs(visibleTabIds, pauseBackgroundTabs)
  }

  fun destroyTab(tabId: String) {
    lastLoadedUrl.remove(tabId)
    appliedPolicies.remove(tabId)
    scrollListeners.remove(tabId)
    permissionCallbacks.remove(tabId)
    externalAppCallbacks.remove(tabId)
    downloadCallbacks.remove(tabId)
    iconCallbacks.remove(tabId)
    pausedTabs.remove(tabId)
    webViews.remove(tabId)?.let { wv ->
      (wv.parent as? ViewGroup)?.removeView(wv)
      wv.stopLoading()
      wv.destroy()
    }
  }

  fun destroyAll() {
    webViews.keys.toList().forEach { destroyTab(it) }
  }

  companion object {
    internal const val FORCE_ZOOM_VIEWPORT_SCRIPT =
      "(function(){var metas=document.querySelectorAll('meta[name=\"viewport\"],meta[name=\"VIEWPORT\"]');" +
        "if(metas.length===0){var m=document.createElement('meta');m.name='viewport';" +
        "m.content='width=device-width,initial-scale=1.0,maximum-scale=5.0,user-scalable=yes';" +
        "if(document.head){document.head.appendChild(m);}}" +
        "else{for(var i=0;i<metas.length;i++){var c=metas[i].getAttribute('content')||'';" +
        "var parts=c.split(',').map(function(p){return p.trim();}).filter(function(p){" +
        "var k=p.split('=')[0].trim().toLowerCase();return k!=='user-scalable'&&k!=='maximum-scale';});" +
        "parts.push('user-scalable=yes');parts.push('maximum-scale=5.0');" +
        "metas[i].setAttribute('content',parts.join(', '));}}})();"
  }
}

@Composable
fun AospWebViewViewport(
  tabId: String,
  targetUrl: String,
  renderPolicy: WebRenderPolicy,
  webViewPool: AospWebViewPool,
  onNavigationUpdate: (url: String, title: String, isLoading: Boolean, progress: Int, caps: WebViewNavCapabilities) -> Unit,
  onRequestBlocked: () -> Unit,
  onScrollDelta: (Int) -> Unit,
  onPermissionChallenge: (WebSitePermissionChallenge) -> Unit,
  onExternalAppRequest: (ExternalAppLaunchRequest) -> Unit,
  onDownloadRequest: (PendingDownloadRequest) -> Unit,
  onIconUpdate: (Bitmap?) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    key(tabId) {
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
          val wv =
            webViewPool.getOrCreate(
              context = ctx,
              tabId = tabId,
              renderPolicy = renderPolicy,
              onNavigationUpdate = onNavigationUpdate,
              onRequestBlocked = onRequestBlocked,
              onScrollDelta = onScrollDelta,
              onPermissionChallenge = onPermissionChallenge,
              onExternalAppRequest = onExternalAppRequest,
              onDownloadRequest = onDownloadRequest,
              onIconUpdate = onIconUpdate,
            )
          (wv.parent as? ViewGroup)?.removeView(wv)
          if (targetUrl != "about:mb" && targetUrl != "about:blank" && wv.url.isNullOrBlank()) {
            webViewPool.loadUrl(tabId, targetUrl, renderPolicy.sendDntGpcHeaders)
          } else {
            webViewPool.syncTargetUrlIfNeeded(tabId, targetUrl, renderPolicy.sendDntGpcHeaders)
          }
          wv
        },
        update = { wv ->
          webViewPool.getOrCreate(
            context = wv.context,
            tabId = tabId,
            renderPolicy = renderPolicy,
            onNavigationUpdate = onNavigationUpdate,
            onRequestBlocked = onRequestBlocked,
            onScrollDelta = onScrollDelta,
            onPermissionChallenge = onPermissionChallenge,
            onExternalAppRequest = onExternalAppRequest,
            onDownloadRequest = onDownloadRequest,
            onIconUpdate = onIconUpdate,
          )
          webViewPool.syncTargetUrlIfNeeded(tabId, targetUrl, renderPolicy.sendDntGpcHeaders)
        },
      )
    }
  }
}
