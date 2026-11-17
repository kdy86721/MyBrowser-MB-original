package com.mybrowser.core.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiBridgeImplTest {

  @Test
  fun `presets switch defaults and restoreAiConfigJson preserves custom fields`() {
    val bridge = AiBridgeImpl()

    val kimi = bridge.updateAiConfig("providerId", "kimi")
    assertTrue(kimi.contains(""""providerId":"kimi""""))
    assertTrue(kimi.contains(""""cliCommand":"kimi""""))
    assertTrue(kimi.contains(""""modelName":"moonshot-v1-8k""""))

    val openai = bridge.updateAiConfig("providerId", "openai_codex")
    assertTrue(openai.contains(""""cliCommand":"codex""""))
    assertTrue(openai.contains(""""cliArgs":"exec""""))

    val claude = bridge.updateAiConfig("providerId", "anthropic_claude")
    assertTrue(claude.contains(""""cliCommand":"claude""""))
    assertTrue(claude.contains(""""modelName":"claude-sonnet-4-20250514""""))

    val custom = bridge.updateAiConfig("providerId", "custom")
    assertTrue(custom.contains(""""cliCommand":"ollama""""))

    bridge.updateAiConfig("executionMode", "LOCALHOST_BRIDGE")
    bridge.updateAiConfig("autoSendOnDrag", "true")
    bridge.updateAiConfig("foldableLayout", "CENTER_SHEET")
    val savedJson = bridge.getAiConfigJson()

    val restoredBridge = AiBridgeImpl()
    val restored = restoredBridge.restoreAiConfigJson(savedJson)
    assertTrue(restored.contains(""""executionMode":"LOCALHOST_BRIDGE""""))
    assertTrue(restored.contains(""""autoSendOnDrag":true"""))
    assertTrue(restored.contains(""""foldableLayout":"CENTER_SHEET""""))
  }

  @Test
  fun `buildComposedPrompt appends title and URL once for web pages`() {
    val bridge = AiBridgeImpl()
    val composed =
      bridge.buildComposedPrompt(
        pageUrl = "https://kotlinlang.org/docs/home.html",
        pageTitle = "Kotlin Docs",
        userMessage = "핵심 정리",
      )
    assertTrue(composed.contains("핵심 정리"))
    assertTrue(composed.contains("페이지 제목: Kotlin Docs"))
    assertTrue(composed.contains("URL: https://kotlinlang.org/docs/home.html"))

    val alreadyHasUrl =
      bridge.buildComposedPrompt(
        pageUrl = "https://kotlinlang.org/docs/home.html",
        pageTitle = "Kotlin Docs",
        userMessage = "요약\nhttps://kotlinlang.org/docs/home.html",
      )
    assertFalse(alreadyHasUrl.contains("페이지 제목:"))

    val internalPage =
      bridge.buildComposedPrompt(
        pageUrl = "about:mb",
        pageTitle = "시작 페이지",
        userMessage = "안녕",
      )
    assertFalse(internalPage.contains("about:mb"))
  }

  @Test
  fun `buildExecutionPlanJson generates CLI localhost and direct API plans and rejects invalid config`() {
    val bridge = AiBridgeImpl()

    // Default LOCAL_BINARY_CLI
    val cliPlan = bridge.buildExecutionPlanJson("https://example.org", "Example", "요약")
    assertTrue(cliPlan.contains(""""ok":true"""))
    assertTrue(cliPlan.contains(""""kind":"CLI_PROCESS""""))
    assertTrue(cliPlan.contains(""""argv":["gemini","-p","""))

    bridge.updateAiConfig("cliCommand", "")
    val invalidCli = bridge.buildExecutionPlanJson("https://example.org", "Example", "요약")
    assertTrue(invalidCli.contains(""""ok":false"""))

    // LOCALHOST_BRIDGE
    bridge.resetAiConfigDefaults()
    bridge.updateAiConfig("executionMode", "LOCALHOST_BRIDGE")
    val localPlan = bridge.buildExecutionPlanJson("https://example.org", "Example", "요약")
    assertTrue(localPlan.contains(""""ok":true"""))
    assertTrue(localPlan.contains(""""kind":"HTTP_LOCALHOST""""))
    assertTrue(localPlan.contains("127.0.0.1:8787"))

    // DIRECT_API validation + Gemini/Anthropic headers
    bridge.updateAiConfig("executionMode", "DIRECT_API")
    val missingKey = bridge.buildExecutionPlanJson("https://example.org", "Example", "요약")
    assertTrue(missingKey.contains(""""ok":false"""))

    bridge.updateAiConfig("apiKey", "test-secret-key")
    val geminiApi = bridge.buildExecutionPlanJson("https://example.org", "Example", "요약")
    assertTrue(geminiApi.contains(""""ok":true"""))
    assertTrue(geminiApi.contains("streamGenerateContent?alt=sse"))
    assertTrue(geminiApi.contains("key=test-secret-key"))

    bridge.updateAiConfig("providerId", "anthropic_claude")
    val claudeApi = bridge.buildExecutionPlanJson("https://example.org", "Example", "요약")
    assertTrue(claudeApi.contains(""""x-api-key":"test-secret-key""""))
  }

  @Test
  fun `parseStreamChunkJson extracts SSE deltas DONE plain CLI output and errors`() {
    val bridge = AiBridgeImpl()

    val openAiChunk =
      bridge.parseStreamChunkJson(
        "openai_codex",
        """data: {"choices":[{"delta":{"content":"안녕 "}}]}""",
      )
    assertTrue(openAiChunk.contains(""""delta":"안녕 """"))
    assertTrue(openAiChunk.contains(""""done":false"""))

    val doneChunk = bridge.parseStreamChunkJson("openai_codex", "data: [DONE]")
    assertTrue(doneChunk.contains(""""done":true"""))

    val geminiChunk =
      bridge.parseStreamChunkJson(
        "google_gemini",
        """data: {"candidates":[{"content":{"parts":[{"text":"제미나이"}]}}]}""",
      )
    assertTrue(geminiChunk.contains(""""delta":"제미나이""""))

    val claudeChunk =
      bridge.parseStreamChunkJson(
        "anthropic_claude",
        """data: {"type":"content_block_delta","delta":{"type":"text_delta","text":"클로드"}}""",
      )
    assertTrue(claudeChunk.contains(""""delta":"클로드""""))

    val cliLine = bridge.parseStreamChunkJson("google_gemini", "plain cli line")
    assertTrue(cliLine.contains("plain cli line\\n"))

    val errorChunk =
      bridge.parseStreamChunkJson(
        "openai_codex",
        """{"error":{"message":"invalid api key"}}""",
      )
    assertTrue(errorChunk.contains(""""error":"invalid api key""""))
    assertTrue(errorChunk.contains(""""done":true"""))
  }
}
