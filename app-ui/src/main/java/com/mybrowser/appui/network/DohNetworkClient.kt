package com.mybrowser.appui.network

import androidx.compose.runtime.Immutable
import com.mybrowser.core.bridge.DohResolverContract
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class DohProviderInfo(
  val id: String,
  val label: String,
  val endpointUrl: String,
  val jurisdiction: String,
)

@Immutable
data class DohRecordItem(
  val type: String,
  val address: String,
  val ttlSeconds: Long,
)

@Immutable
data class DohLookupResult(
  val hostname: String,
  val providerLabel: String,
  val queryBytes: Int,
  val responseBytes: Int,
  val latencyMs: Long,
  val ok: Boolean,
  val records: List<DohRecordItem>,
  val errorMessage: String? = null,
)

/**
 * Pure AOSP RFC 8484 DNS-over-HTTPS client backed by `:core-bridge` wire packet engine.
 */
class DohNetworkClient(
  private val dohContract: DohResolverContract,
) {
  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(6, TimeUnit.SECONDS)
      .build()

  fun listProviders(): List<DohProviderInfo> {
    val json = JSONArray(dohContract.getDohProvidersJson())
    return buildList {
      for (i in 0 until json.length()) {
        val obj = json.getJSONObject(i)
        add(
          DohProviderInfo(
            id = obj.getString("id"),
            label = obj.getString("label"),
            endpointUrl = obj.getString("endpointUrl"),
            jurisdiction = obj.getString("jurisdiction"),
          )
        )
      }
    }
  }

  suspend fun resolveHostname(
    hostname: String,
    providerId: String,
    customDohUrl: String = "",
  ): DohLookupResult =
    withContext(Dispatchers.IO) {
      val cleanHost = hostname.trim().removePrefix("https://").removePrefix("http://").substringBefore('/')
      val provider = listProviders().firstOrNull { it.id == providerId } ?: listProviders().first()
      val targetEndpoint =
        if (provider.id == "custom" && customDohUrl.startsWith("https://")) {
          customDohUrl.trim()
        } else {
          provider.endpointUrl
        }
      val queryWire = dohContract.buildDnsQueryPacket(cleanHost, recordType = 1)
      val startedAt = System.currentTimeMillis()

      try {
        val body = queryWire.toRequestBody("application/dns-message".toMediaType())
        val request =
          Request.Builder()
            .url(targetEndpoint)
            .post(body)
            .header("Accept", "application/dns-message")
            .build()

        httpClient.newCall(request).execute().use { response ->
          val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
          if (!response.isSuccessful) {
            return@withContext DohLookupResult(
              hostname = cleanHost,
              providerLabel = provider.label,
              queryBytes = queryWire.size,
              responseBytes = 0,
              latencyMs = elapsed,
              ok = false,
              records = emptyList(),
              errorMessage = "HTTP ${response.code}",
            )
          }
          val responseBytes = response.body?.bytes() ?: ByteArray(0)
          val parsedJson = JSONObject(dohContract.parseDnsResponsePacket(responseBytes))
          val recordsJson = parsedJson.optJSONArray("records") ?: JSONArray()
          val records = buildList {
            for (i in 0 until recordsJson.length()) {
              val r = recordsJson.getJSONObject(i)
              add(
                DohRecordItem(
                  type = r.optString("type", "A"),
                  address = r.optString("address", ""),
                  ttlSeconds = r.optLong("ttl", 0L),
                )
              )
            }
          }
          DohLookupResult(
            hostname = cleanHost,
            providerLabel = provider.label,
            queryBytes = queryWire.size,
            responseBytes = responseBytes.size,
            latencyMs = elapsed,
            ok = parsedJson.optBoolean("ok", false),
            records = records,
            errorMessage = parsedJson.optString("error").takeIf { it.isNotBlank() },
          )
        }
      } catch (e: Exception) {
        DohLookupResult(
          hostname = cleanHost,
          providerLabel = provider.label,
          queryBytes = queryWire.size,
          responseBytes = 0,
          latencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L),
          ok = false,
          records = emptyList(),
          errorMessage = e.message ?: "DoH 네트워크 오류",
        )
      }
    }
}
