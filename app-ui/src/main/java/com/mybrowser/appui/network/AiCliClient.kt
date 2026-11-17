package com.mybrowser.appui.network

import androidx.compose.runtime.Immutable
import com.mybrowser.core.bridge.AiBridgeContract
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

enum class AiExecutionMode(val id: String, val label: String) {
  LOCAL_BINARY_CLI("LOCAL_BINARY_CLI", "로컬/Termux CLI"),
  LOCALHOST_BRIDGE("LOCALHOST_BRIDGE", "로컬호스트 브릿지"),
  DIRECT_API("DIRECT_API", "직접 API"),
  ;

  companion object {
    fun fromId(raw: String): AiExecutionMode =
      entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: LOCAL_BINARY_CLI
  }
}

enum class AiFoldableLayout(val id: String, val label: String) {
  RIGHT_PANEL("RIGHT_PANEL", "우측 분할 패널"),
  CENTER_SHEET("CENTER_SHEET", "중앙 확장 시트"),
  ;

  companion object {
    fun fromId(raw: String): AiFoldableLayout =
      entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: RIGHT_PANEL
  }
}

@Immutable
data class AiProviderPresetUi(
  val id: String,
  val shortLabel: String,
  val label: String,
  val defaultCliCommand: String,
  val defaultCliArgs: String,
  val defaultApiUrl: String,
  val defaultModel: String,
)

@Immutable
data class AiConfigUi(
  val providerId: String = "google_gemini",
  val executionMode: AiExecutionMode = AiExecutionMode.LOCAL_BINARY_CLI,
  val cliCommand: String = "gemini",
  val cliArgs: String = "-p",
  val localhostUrl: String = "http://127.0.0.1:8787/v1/chat/completions",
  val apiEndpointUrl: String =
    "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
  val modelName: String = "gemini-2.5-flash",
  val apiKey: String = "",
  val defaultPromptTemplate: String = "이 페이지 핵심 내용 요약해 줘",
  val autoSendOnDrag: Boolean = false,
  val foldableLayout: AiFoldableLayout = AiFoldableLayout.RIGHT_PANEL,
)

@Immutable
data class AiConnectionTestResult(
  val ok: Boolean,
  val statusLabel: String,
  val detailMessage: String,
  val latencyMs: Long,
)

@Immutable
data class AiChatMessageUi(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val attachedUrl: String = "",
  val attachedTitle: String = "",
  val isStreaming: Boolean = false,
  val isError: Boolean = false,
)

sealed interface AiStreamEvent {
  data class Delta(val chunk: String) : AiStreamEvent
  data class Error(val message: String) : AiStreamEvent
  data object Completed : AiStreamEvent
}

/**
 * Pure AOSP (No-GMS) AI CLI / Localhost Bridge / Direct REST executor backed by `:core-bridge`.
 */
class AiCliExecutor(
  private val aiBridge: AiBridgeContract,
) {
  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .build()

  fun listPresets(): List<AiProviderPresetUi> {
    val arr = JSONArray(aiBridge.getProviderPresetsJson())
    return buildList {
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        add(
          AiProviderPresetUi(
            id = obj.getString("id"),
            shortLabel = obj.getString("shortLabel"),
            label = obj.getString("label"),
            defaultCliCommand = obj.getString("defaultCliCommand"),
            defaultCliArgs = obj.getString("defaultCliArgs"),
            defaultApiUrl = obj.getString("defaultApiUrl"),
            defaultModel = obj.getString("defaultModel"),
          )
        )
      }
    }
  }

  fun parseConfig(): AiConfigUi {
    val obj = JSONObject(aiBridge.getAiConfigJson())
    return AiConfigUi(
      providerId = obj.optString("providerId", "google_gemini"),
      executionMode = AiExecutionMode.fromId(obj.optString("executionMode", "LOCAL_BINARY_CLI")),
      cliCommand = obj.optString("cliCommand", "gemini"),
      cliArgs = obj.optString("cliArgs", "-p"),
      localhostUrl = obj.optString("localhostUrl", "http://127.0.0.1:8787/v1/chat/completions"),
      apiEndpointUrl =
        obj.optString(
          "apiEndpointUrl",
          "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
        ),
      modelName = obj.optString("modelName", "gemini-2.5-flash"),
      apiKey = obj.optString("apiKey", ""),
      defaultPromptTemplate = obj.optString("defaultPromptTemplate", "이 페이지 핵심 내용 요약해 줘"),
      autoSendOnDrag = obj.optBoolean("autoSendOnDrag", false),
      foldableLayout = AiFoldableLayout.fromId(obj.optString("foldableLayout", "RIGHT_PANEL")),
    )
  }

  suspend fun testConnection(): AiConnectionTestResult =
    withContext(Dispatchers.IO) {
      val startedAt = System.currentTimeMillis()
      val plan = JSONObject(aiBridge.buildExecutionPlanJson("", "", "ping"))
      if (!plan.optBoolean("ok", false)) {
        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
        return@withContext AiConnectionTestResult(
          ok = false,
          statusLabel = "설정 필요",
          detailMessage = plan.optString("error", "설정을 확인해 주세요"),
          latencyMs = elapsed,
        )
      }

      when (plan.optString("kind")) {
        "CLI_PROCESS" -> {
          val testArr = plan.optJSONArray("testArgv") ?: JSONArray()
          val rawArgv = List(testArr.length()) { idx -> testArr.getString(idx) }
          val cmd = rawArgv.firstOrNull().orEmpty()
          val resolved = resolveCliBinaryPath(cmd)
          val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
          if (resolved == null) {
            return@withContext AiConnectionTestResult(
              ok = false,
              statusLabel = "바이너리 미발견",
              detailMessage = "'$cmd' 실행 파일을 Termux 또는 PATH에서 찾지 못했습니다",
              latencyMs = elapsed,
            )
          }
          try {
            val pb = ProcessBuilder(listOf(resolved) + rawArgv.drop(1))
            applyCliEnvironment(pb)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val out = proc.inputStream.bufferedReader().readText().trim()
            val exitCode = proc.waitFor()
            val totalMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
            if (exitCode == 0) {
              AiConnectionTestResult(
                ok = true,
                statusLabel = "연결됨 (${totalMs}ms)",
                detailMessage = out.lineSequence().firstOrNull()?.take(80) ?: resolved,
                latencyMs = totalMs,
              )
            } else {
              AiConnectionTestResult(
                ok = false,
                statusLabel = "실행 오류 ($exitCode)",
                detailMessage = out.lineSequence().firstOrNull()?.take(100) ?: "종료 코드 $exitCode",
                latencyMs = totalMs,
              )
            }
          } catch (e: Exception) {
            val totalMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
            AiConnectionTestResult(
              ok = false,
              statusLabel = "실행 차단됨",
              detailMessage = e.message ?: "프로세스를 시작할 수 없습니다",
              latencyMs = totalMs,
            )
          }
        }

        "HTTP_LOCALHOST", "HTTP_API" -> {
          val url = plan.optString("url")
          val headersObj = plan.optJSONObject("headers") ?: JSONObject()
          val bodyJson = plan.optString("body")
          try {
            val reqBuilder =
              Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            headersObj.keys().forEach { k ->
              reqBuilder.header(k, headersObj.optString(k))
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
              val totalMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
              if (resp.isSuccessful) {
                AiConnectionTestResult(
                  ok = true,
                  statusLabel = "연결됨 · HTTP ${resp.code} (${totalMs}ms)",
                  detailMessage = url.substringBefore('?'),
                  latencyMs = totalMs,
                )
              } else {
                val errBody = resp.body?.string()?.take(120).orEmpty()
                AiConnectionTestResult(
                  ok = false,
                  statusLabel = "HTTP ${resp.code}",
                  detailMessage = errBody.ifBlank { "엔드포인트 응답 오류" },
                  latencyMs = totalMs,
                )
              }
            }
          } catch (e: Exception) {
            val totalMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L)
            AiConnectionTestResult(
              ok = false,
              statusLabel = "응답 없음",
              detailMessage = e.message ?: "브릿지 서버에 연결할 수 없습니다",
              latencyMs = totalMs,
            )
          }
        }

        else -> {
          AiConnectionTestResult(
            ok = false,
            statusLabel = "오류",
            detailMessage = "지원되지 않는 실행 방식",
            latencyMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(1L),
          )
        }
      }
    }

  fun streamCompletion(
    pageUrl: String,
    pageTitle: String,
    userMessage: String,
  ): Flow<AiStreamEvent> =
    flow {
      val plan = JSONObject(aiBridge.buildExecutionPlanJson(pageUrl, pageTitle, userMessage))
      if (!plan.optBoolean("ok", false)) {
        emit(AiStreamEvent.Error(plan.optString("error", "AI 설정을 확인해 주세요")))
        return@flow
      }

      val providerId = plan.optString("providerId")
      when (plan.optString("kind")) {
        "CLI_PROCESS" -> {
          val argvArr = plan.optJSONArray("argv") ?: JSONArray()
          val rawArgv = List(argvArr.length()) { idx -> argvArr.getString(idx) }
          val cmd = rawArgv.firstOrNull().orEmpty()
          val resolved = resolveCliBinaryPath(cmd)
          if (resolved == null) {
            emit(
              AiStreamEvent.Error(
                "'$cmd' CLI 바이너리를 찾을 수 없습니다. 설정 > AI에서 경로를 확인하거나 로컬호스트 브릿지 모드를 사용해 주세요.",
              )
            )
            return@flow
          }
          var process: Process? = null
          try {
            val pb = ProcessBuilder(listOf(resolved) + rawArgv.drop(1))
            applyCliEnvironment(pb)
            pb.redirectErrorStream(true)
            process = pb.start()
            val reader = process.inputStream.bufferedReader()
            while (true) {
              currentCoroutineContext().ensureActive()
              val line = reader.readLine() ?: break
              val chunkObj = JSONObject(aiBridge.parseStreamChunkJson(providerId, line))
              val err = chunkObj.optString("error")
              if (err.isNotBlank()) {
                emit(AiStreamEvent.Error(err))
                return@flow
              }
              val delta = chunkObj.optString("delta")
              if (delta.isNotEmpty()) {
                emit(AiStreamEvent.Delta(delta))
              }
            }
            val code = process.waitFor()
            if (code != 0) {
              emit(AiStreamEvent.Error("CLI 프로세스가 종료 코드 $code 로 종료되었습니다"))
            } else {
              emit(AiStreamEvent.Completed)
            }
          } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(AiStreamEvent.Error(e.message ?: "CLI 실행 중 오류가 발생했습니다"))
          } finally {
            process?.destroy()
          }
        }

        "HTTP_LOCALHOST", "HTTP_API" -> {
          val url = plan.optString("url")
          val headersObj = plan.optJSONObject("headers") ?: JSONObject()
          val bodyJson = plan.optString("body")
          val reqBuilder =
            Request.Builder()
              .url(url)
              .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
          headersObj.keys().forEach { k ->
            reqBuilder.header(k, headersObj.optString(k))
          }
          try {
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
              if (!resp.isSuccessful) {
                val errBody = resp.body?.string()?.take(240).orEmpty()
                emit(
                  AiStreamEvent.Error(
                    "HTTP ${resp.code}: ${errBody.ifBlank { "요청이 거부되었습니다" }}",
                  )
                )
                return@flow
              }
              val body = resp.body
              if (body == null) {
                emit(AiStreamEvent.Error("응답 본문이 비어 있습니다"))
                return@flow
              }
              val reader = body.charStream().buffered()
              var anyDelta = false
              while (true) {
                currentCoroutineContext().ensureActive()
                val line = reader.readLine() ?: break
                val parsed = JSONObject(aiBridge.parseStreamChunkJson(providerId, line))
                val err = parsed.optString("error")
                if (err.isNotBlank()) {
                  emit(AiStreamEvent.Error(err))
                  return@flow
                }
                val delta = parsed.optString("delta")
                if (delta.isNotEmpty()) {
                  anyDelta = true
                  emit(AiStreamEvent.Delta(delta))
                }
                if (parsed.optBoolean("done", false)) {
                  break
                }
              }
              if (!anyDelta) {
                emit(AiStreamEvent.Error("수신된 응답 텍스트가 없습니다"))
              } else {
                emit(AiStreamEvent.Completed)
              }
            }
          } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(AiStreamEvent.Error(e.message ?: "브릿지 네트워크 오류가 발생했습니다"))
          }
        }

        else -> emit(AiStreamEvent.Error("지원되지 않는 실행 방식입니다"))
      }
    }.flowOn(Dispatchers.IO)

  private fun resolveCliBinaryPath(command: String): String? {
    val clean = command.trim()
    if (clean.isEmpty()) return null
    if (clean.contains('/')) {
      val file = File(clean)
      return if (file.exists() && file.canExecute()) file.absolutePath else null
    }
    val candidateDirs = buildList {
      add("/data/data/com.termux/files/usr/bin")
      add("/data/data/com.termux/files/usr/local/bin")
      System.getenv("PATH")
        ?.split(':')
        ?.filter { it.isNotBlank() }
        ?.let { addAll(it) }
      add("/system/bin")
      add("/system/xbin")
    }
    for (dir in candidateDirs.distinct()) {
      val f = File(dir, clean)
      if (f.exists() && f.canExecute()) return f.absolutePath
    }
    return null
  }

  private fun applyCliEnvironment(pb: ProcessBuilder) {
    val env = pb.environment()
    val termuxBin = "/data/data/com.termux/files/usr/bin"
    val existingPath = env["PATH"].orEmpty()
    if (!existingPath.contains(termuxBin)) {
      env["PATH"] = if (existingPath.isBlank()) termuxBin else "$termuxBin:$existingPath"
    }
    if (env["HOME"].isNullOrBlank()) {
      val termuxHome = File("/data/data/com.termux/files/home")
      if (termuxHome.exists()) {
        env["HOME"] = termuxHome.absolutePath
      }
    }
  }
}
