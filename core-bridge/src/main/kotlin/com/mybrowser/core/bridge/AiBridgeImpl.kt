package com.mybrowser.core.bridge

/**
 * Pure Kotlin Multi-AI CLI & Bridge Engine for Pure AOSP (No-GMS).
 * Zero Android dependencies; serializes flat JSON for seamless Rust UniFFI/JNI migration.
 */
// [RUST-MIGRATION-TARGET]
// pub struct RustAiCliBridge {
//     config: RwLock<AiCliConfigRecord>,
// }
class AiBridgeImpl : AiBridgeContract {

  private val lock = Any()

  private var providerId: String = "google_gemini"
  private var executionMode: String = "LOCAL_BINARY_CLI"
  private var cliCommand: String = "gemini"
  private var cliArgs: String = "-p"
  private var localhostUrl: String = "http://127.0.0.1:8787/v1/chat/completions"
  private var apiEndpointUrl: String =
    "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"
  private var modelName: String = "gemini-2.5-flash"
  private var apiKey: String = ""
  private var defaultPromptTemplate: String = "이 페이지 핵심 내용 요약해 줘"
  private var autoSendOnDrag: Boolean = false
  private var foldableLayout: String = "RIGHT_PANEL"

  override fun getAiConfigJson(): String = synchronized(lock) { serializeConfigLocked() }

  override fun updateAiConfig(key: String, value: String): String = synchronized(lock) {
    applyKeyValueLocked(key, value)
    serializeConfigLocked()
  }

  override fun restoreAiConfigJson(rawJson: String): String = synchronized(lock) {
    if (rawJson.isBlank()) return@synchronized serializeConfigLocked()
    extractString(rawJson, "providerId")?.let { id ->
      if (id in PRESETS_BY_ID.keys) providerId = id
    }
    extractString(rawJson, "executionMode")?.let { mode ->
      if (mode in VALID_MODES) executionMode = mode
    }
    extractString(rawJson, "cliCommand")?.let { cliCommand = it }
    extractString(rawJson, "cliArgs")?.let { cliArgs = it }
    extractString(rawJson, "localhostUrl")?.let { localhostUrl = it }
    extractString(rawJson, "apiEndpointUrl")?.let { apiEndpointUrl = it }
    extractString(rawJson, "modelName")?.let { modelName = it }
    extractString(rawJson, "apiKey")?.let { apiKey = it }
    extractString(rawJson, "defaultPromptTemplate")?.let { defaultPromptTemplate = it }
    extractBoolean(rawJson, "autoSendOnDrag")?.let { autoSendOnDrag = it }
    extractString(rawJson, "foldableLayout")?.let { layout ->
      if (layout in VALID_LAYOUTS) foldableLayout = layout
    }
    serializeConfigLocked()
  }

  override fun resetAiConfigDefaults(): String = synchronized(lock) {
    val preset = PRESETS_BY_ID.getValue("google_gemini")
    providerId = preset.id
    executionMode = "LOCAL_BINARY_CLI"
    cliCommand = preset.defaultCliCommand
    cliArgs = preset.defaultCliArgs
    localhostUrl = "http://127.0.0.1:8787/v1/chat/completions"
    apiEndpointUrl = preset.defaultApiUrl
    modelName = preset.defaultModel
    apiKey = ""
    defaultPromptTemplate = "이 페이지 핵심 내용 요약해 줘"
    autoSendOnDrag = false
    foldableLayout = "RIGHT_PANEL"
    serializeConfigLocked()
  }

  override fun getProviderPresetsJson(): String {
    return PRESETS.joinToString(prefix = "[", postfix = "]", separator = ",") { p ->
      """{"id":${JsonMini.quote(p.id)},"shortLabel":${JsonMini.quote(p.shortLabel)},"label":${JsonMini.quote(p.label)},"defaultCliCommand":${JsonMini.quote(p.defaultCliCommand)},"defaultCliArgs":${JsonMini.quote(p.defaultCliArgs)},"defaultApiUrl":${JsonMini.quote(p.defaultApiUrl)},"defaultModel":${JsonMini.quote(p.defaultModel)}}"""
    }
  }

  override fun buildComposedPrompt(
    pageUrl: String,
    pageTitle: String,
    userMessage: String,
  ): String = synchronized(lock) {
    val template = userMessage.trim().ifBlank { defaultPromptTemplate.trim() }
    val cleanUrl = pageUrl.trim()
    val hasWebUrl =
      cleanUrl.isNotBlank() &&
        !cleanUrl.startsWith("about:mb") &&
        !cleanUrl.startsWith("about:blank")
    if (!hasWebUrl) {
      return@synchronized template
    }
    if (template.contains(cleanUrl)) {
      return@synchronized template
    }
    val cleanTitle = pageTitle.trim()
    return@synchronized buildString {
      append(template)
      append("\n\n")
      if (cleanTitle.isNotBlank() && cleanTitle != cleanUrl) {
        append("페이지 제목: ")
        append(cleanTitle)
        append('\n')
      }
      append("URL: ")
      append(cleanUrl)
    }
  }

  override fun buildExecutionPlanJson(
    pageUrl: String,
    pageTitle: String,
    userMessage: String,
  ): String = synchronized(lock) {
    val prompt = buildComposedPrompt(pageUrl, pageTitle, userMessage)
    val preset = PRESETS_BY_ID[providerId] ?: PRESETS.first()
    val resolvedModel = modelName.trim().ifBlank { preset.defaultModel }

    when (executionMode) {
      "LOCAL_BINARY_CLI" -> {
        val cmd = cliCommand.trim()
        if (cmd.isBlank()) {
          return@synchronized errorPlanJson("CLI 실행 명령어가 비어 있습니다")
        }
        val argv = buildList {
          addAll(tokenizeShellArgs(cmd))
          addAll(tokenizeShellArgs(cliArgs))
          add(prompt)
        }
        val argvJson = argv.joinToString(prefix = "[", postfix = "]", separator = ",") { JsonMini.quote(it) }
        val testArgvJson =
          (tokenizeShellArgs(cmd) + listOf("--version"))
            .joinToString(prefix = "[", postfix = "]", separator = ",") { JsonMini.quote(it) }
        """{"ok":true,"kind":"CLI_PROCESS","providerId":${JsonMini.quote(providerId)},"argv":$argvJson,"testArgv":$testArgvJson,"prompt":${JsonMini.quote(prompt)},"error":""}"""
      }

      "LOCALHOST_BRIDGE" -> {
        val rawUrl = localhostUrl.trim()
        if (!rawUrl.startsWith("http://127.0.0.1") &&
          !rawUrl.startsWith("http://localhost") &&
          !rawUrl.startsWith("http://") &&
          !rawUrl.startsWith("https://")
        ) {
          return@synchronized errorPlanJson("로컬 브릿지 URL 형식이 올바르지 않습니다 (http://127.0.0.1:...)")
        }
        val bodyJson = buildOpenAiChatBodyJson(resolvedModel, prompt, stream = true)
        val headersJson = buildString {
          append("""{"Content-Type":"application/json","Accept":"text/event-stream, application/json"""")
          if (apiKey.isNotBlank()) {
            append(""","Authorization":${JsonMini.quote("Bearer ${apiKey.trim()}")}""")
          }
          append('}')
        }
        """{"ok":true,"kind":"HTTP_LOCALHOST","providerId":${JsonMini.quote(providerId)},"url":${JsonMini.quote(rawUrl)},"headers":$headersJson,"body":${JsonMini.quote(bodyJson)},"prompt":${JsonMini.quote(prompt)},"error":""}"""
      }

      "DIRECT_API" -> {
        val key = apiKey.trim()
        if (key.isBlank()) {
          return@synchronized errorPlanJson("직접 API 모드에는 API 키 입력이 필요합니다")
        }
        val rawEndpoint = apiEndpointUrl.trim().ifBlank { preset.defaultApiUrl }
        if (!rawEndpoint.startsWith("https://") && !rawEndpoint.startsWith("http://")) {
          return@synchronized errorPlanJson("API 엔드포인트 URL이 올바르지 않습니다")
        }
        when (providerId) {
          "google_gemini" -> {
            val resolvedUrl =
              rawEndpoint
                .replace("{model}", resolvedModel)
                .let { base ->
                  val withStream =
                    if (base.contains(":generateContent")) {
                      base.replace(":generateContent", ":streamGenerateContent?alt=sse")
                    } else {
                      base
                    }
                  val sep = if (withStream.contains('?')) '&' else '?'
                  if (withStream.contains("key=")) withStream else "${withStream}${sep}key=$key"
                }
            val bodyJson =
              """{"contents":[{"role":"user","parts":[{"text":${JsonMini.quote(prompt)}}]}]}"""
            val headersJson =
              """{"Content-Type":"application/json","Accept":"text/event-stream, application/json"}"""
            """{"ok":true,"kind":"HTTP_API","providerId":${JsonMini.quote(providerId)},"url":${JsonMini.quote(resolvedUrl)},"headers":$headersJson,"body":${JsonMini.quote(bodyJson)},"prompt":${JsonMini.quote(prompt)},"error":""}"""
          }

          "anthropic_claude" -> {
            val bodyJson =
              """{"model":${JsonMini.quote(resolvedModel)},"max_tokens":2048,"stream":true,"messages":[{"role":"user","content":${JsonMini.quote(prompt)}}]}"""
            val headersJson =
              """{"Content-Type":"application/json","Accept":"text/event-stream, application/json","x-api-key":${JsonMini.quote(key)},"anthropic-version":"2023-06-01"}"""
            """{"ok":true,"kind":"HTTP_API","providerId":${JsonMini.quote(providerId)},"url":${JsonMini.quote(rawEndpoint)},"headers":$headersJson,"body":${JsonMini.quote(bodyJson)},"prompt":${JsonMini.quote(prompt)},"error":""}"""
          }

          else -> {
            // OpenAI / Kimi / Custom OpenAI-compatible schema
            val bodyJson = buildOpenAiChatBodyJson(resolvedModel, prompt, stream = true)
            val headersJson =
              """{"Content-Type":"application/json","Accept":"text/event-stream, application/json","Authorization":${JsonMini.quote("Bearer $key")}}"""
            """{"ok":true,"kind":"HTTP_API","providerId":${JsonMini.quote(providerId)},"url":${JsonMini.quote(rawEndpoint)},"headers":$headersJson,"body":${JsonMini.quote(bodyJson)},"prompt":${JsonMini.quote(prompt)},"error":""}"""
          }
        }
      }

      else -> errorPlanJson("알 수 없는 AI 실행 방식입니다")
    }
  }

  override fun parseStreamChunkJson(providerId: String, rawLine: String): String {
    val trimmed = rawLine.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("event:") || trimmed.startsWith(":")) {
      return """{"delta":"","done":false,"error":""}"""
    }
    val payload =
      if (trimmed.startsWith("data:")) trimmed.removePrefix("data:").trim()
      else trimmed

    if (payload == "[DONE]") {
      return """{"delta":"","done":true,"error":""}"""
    }

    if (!payload.startsWith("{")) {
      // Plain CLI line output
      return """{"delta":${JsonMini.quote(rawLine + "\n")},"done":false,"error":""}"""
    }

    // Check explicit error block
    val errMsg =
      Regex(""""error"\s*:\s*\{[^}]*"message"\s*:\s*"((?:\\.|[^"\\])*)"""")
        .find(payload)
        ?.groupValues
        ?.get(1)
        ?.let(::unescapeJsonString)
        ?: Regex(""""error"\s*:\s*"((?:\\.|[^"\\])*)"""")
          .find(payload)
          ?.groupValues
          ?.get(1)
          ?.let(::unescapeJsonString)
    if (!errMsg.isNullOrBlank()) {
      return """{"delta":"","done":true,"error":${JsonMini.quote(errMsg)}}"""
    }

    // Anthropic content_block_delta: {"type":"content_block_delta","delta":{"type":"text_delta","text":"..."}}
    // or Gemini: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
    // or OpenAI SSE / JSON: {"choices":[{"delta":{"content":"..."}}]} / {"choices":[{"message":{"content":"..."}}]}
    // or Generic bridge: {"response":"..."} / {"output":"..."}
    val deltaText =
      extractFirstMatchedJsonText(
        payload,
        listOf(
          Regex(""""delta"\s*:\s*\{[^}]*"content"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""delta"\s*:\s*\{[^}]*"text"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""parts"\s*:\s*\[\s*\{[^}]*"text"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""message"\s*:\s*\{[^}]*"content"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""content"\s*:\s*\[\s*\{[^}]*"text"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""response"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""output"\s*:\s*"((?:\\.|[^"\\])*)""""),
          Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)""""),
        ),
      )

    val isDone =
      payload.contains(""""type":"message_stop"""") ||
        payload.contains(""""type": "message_stop"""") ||
        payload.contains(""""done":true""") ||
        payload.contains(""""done": true""")

    return """{"delta":${JsonMini.quote(deltaText)},"done":$isDone,"error":""}"""
  }

  private fun applyKeyValueLocked(key: String, value: String) {
    when (key) {
      "providerId" -> {
        val next = PRESETS_BY_ID[value.trim()] ?: return
        val prev = PRESETS_BY_ID[providerId]
        providerId = next.id
        if (cliCommand.isBlank() || cliCommand == prev?.defaultCliCommand) {
          cliCommand = next.defaultCliCommand
        }
        if (cliArgs.isBlank() || cliArgs == prev?.defaultCliArgs) {
          cliArgs = next.defaultCliArgs
        }
        if (apiEndpointUrl.isBlank() || apiEndpointUrl == prev?.defaultApiUrl) {
          apiEndpointUrl = next.defaultApiUrl
        }
        if (modelName.isBlank() || modelName == prev?.defaultModel) {
          modelName = next.defaultModel
        }
      }
      "executionMode" -> {
        val norm = value.trim().uppercase()
        if (norm in VALID_MODES) executionMode = norm
      }
      "cliCommand" -> cliCommand = value
      "cliArgs" -> cliArgs = value
      "localhostUrl" -> localhostUrl = value.trim()
      "apiEndpointUrl" -> apiEndpointUrl = value.trim()
      "modelName" -> modelName = value.trim()
      "apiKey" -> apiKey = value.trim()
      "defaultPromptTemplate" -> defaultPromptTemplate = value
      "autoSendOnDrag" -> value.toBooleanStrictOrNull()?.let { autoSendOnDrag = it }
      "foldableLayout" -> {
        val norm = value.trim().uppercase()
        if (norm in VALID_LAYOUTS) foldableLayout = norm
      }
    }
  }

  private fun serializeConfigLocked(): String {
    return """{"providerId":${JsonMini.quote(providerId)},"executionMode":${JsonMini.quote(executionMode)},"cliCommand":${JsonMini.quote(cliCommand)},"cliArgs":${JsonMini.quote(cliArgs)},"localhostUrl":${JsonMini.quote(localhostUrl)},"apiEndpointUrl":${JsonMini.quote(apiEndpointUrl)},"modelName":${JsonMini.quote(modelName)},"apiKey":${JsonMini.quote(apiKey)},"defaultPromptTemplate":${JsonMini.quote(defaultPromptTemplate)},"autoSendOnDrag":$autoSendOnDrag,"foldableLayout":${JsonMini.quote(foldableLayout)}}"""
  }

  private fun buildOpenAiChatBodyJson(model: String, prompt: String, stream: Boolean): String {
    return """{"model":${JsonMini.quote(model)},"stream":$stream,"messages":[{"role":"user","content":${JsonMini.quote(prompt)}}]}"""
  }

  private fun errorPlanJson(message: String): String =
    """{"ok":false,"kind":"ERROR","providerId":${JsonMini.quote(providerId)},"argv":[],"testArgv":[],"url":"","headers":{},"body":"","prompt":"","error":${JsonMini.quote(message)}}"""

  private fun tokenizeShellArgs(raw: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var quoteChar: Char? = null
    for (ch in raw.trim()) {
      when {
        quoteChar != null -> {
          if (ch == quoteChar) quoteChar = null
          else current.append(ch)
        }
        ch == '"' || ch == '\'' -> quoteChar = ch
        ch.isWhitespace() -> {
          if (current.isNotEmpty()) {
            result += current.toString()
            current.clear()
          }
        }
        else -> current.append(ch)
      }
    }
    if (current.isNotEmpty()) result += current.toString()
    return result
  }

  private fun extractFirstMatchedJsonText(payload: String, patterns: List<Regex>): String {
    for (regex in patterns) {
      val match = regex.find(payload)?.groupValues?.get(1)
      if (match != null) return unescapeJsonString(match)
    }
    return ""
  }

  private fun unescapeJsonString(escaped: String): String {
    val sb = StringBuilder(escaped.length)
    var i = 0
    while (i < escaped.length) {
      val c = escaped[i]
      if (c == '\\' && i + 1 < escaped.length) {
        val next = escaped[i + 1]
        when (next) {
          'n' -> { sb.append('\n'); i += 2 }
          'r' -> { sb.append('\r'); i += 2 }
          't' -> { sb.append('\t'); i += 2 }
          '"' -> { sb.append('"'); i += 2 }
          '\\' -> { sb.append('\\'); i += 2 }
          '/' -> { sb.append('/'); i += 2 }
          'u' -> {
            if (i + 5 < escaped.length) {
              val hex = escaped.substring(i + 2, i + 6)
              val code = hex.toIntOrNull(16)
              if (code != null) {
                sb.append(code.toChar())
                i += 6
              } else {
                sb.append(c); i += 1
              }
            } else {
              sb.append(c); i += 1
            }
          }
          else -> { sb.append(next); i += 2 }
        }
      } else {
        sb.append(c)
        i += 1
      }
    }
    return sb.toString()
  }

  private fun extractString(json: String, key: String): String? =
    Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(json)?.groupValues?.get(1)
      ?.let(::unescapeJsonString)

  private fun extractBoolean(json: String, key: String): Boolean? =
    Regex(""""$key"\s*:\s*(true|false)""").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()

  private data class PresetSpec(
    val id: String,
    val shortLabel: String,
    val label: String,
    val defaultCliCommand: String,
    val defaultCliArgs: String,
    val defaultApiUrl: String,
    val defaultModel: String,
  )

  private companion object {
    val VALID_MODES = setOf("LOCAL_BINARY_CLI", "LOCALHOST_BRIDGE", "DIRECT_API")
    val VALID_LAYOUTS = setOf("RIGHT_PANEL", "CENTER_SHEET")

    val PRESETS =
      listOf(
        PresetSpec(
          id = "google_gemini",
          shortLabel = "구글",
          label = "Google (Gemini CLI)",
          defaultCliCommand = "gemini",
          defaultCliArgs = "-p",
          defaultApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
          defaultModel = "gemini-2.5-flash",
        ),
        PresetSpec(
          id = "kimi",
          shortLabel = "키미",
          label = "Kimi (Kimi CLI)",
          defaultCliCommand = "kimi",
          defaultCliArgs = "--prompt",
          defaultApiUrl = "https://api.moonshot.cn/v1/chat/completions",
          defaultModel = "moonshot-v1-8k",
        ),
        PresetSpec(
          id = "openai_codex",
          shortLabel = "오픈AI",
          label = "OpenAI (Codex / CLI)",
          defaultCliCommand = "codex",
          defaultCliArgs = "exec",
          defaultApiUrl = "https://api.openai.com/v1/chat/completions",
          defaultModel = "gpt-4o-mini",
        ),
        PresetSpec(
          id = "anthropic_claude",
          shortLabel = "클로드",
          label = "Anthropic (Claude Code)",
          defaultCliCommand = "claude",
          defaultCliArgs = "-p",
          defaultApiUrl = "https://api.anthropic.com/v1/messages",
          defaultModel = "claude-sonnet-4-20250514",
        ),
        PresetSpec(
          id = "custom",
          shortLabel = "커스텀",
          label = "사용자 지정 CLI / 로컬 엔드포인트",
          defaultCliCommand = "ollama",
          defaultCliArgs = "run llama3",
          defaultApiUrl = "http://127.0.0.1:11434/v1/chat/completions",
          defaultModel = "llama3",
        ),
      )

    val PRESETS_BY_ID = PRESETS.associateBy { it.id }
  }
}
