package com.mybrowser.appui.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.network.AiConfigUi
import com.mybrowser.appui.network.AiConnectionTestResult
import com.mybrowser.appui.network.AiExecutionMode
import com.mybrowser.appui.network.AiFoldableLayout
import com.mybrowser.appui.network.AiProviderPresetUi

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.aiSettingsItems(
  aiConfig: AiConfigUi,
  providerPresets: List<AiProviderPresetUi>,
  designConfig: DesignUiConfig,
  isTestingAiConnection: Boolean,
  aiConnectionTestResult: AiConnectionTestResult?,
  onUpdateAiConfig: (key: String, value: String) -> Unit,
  onResetAiConfigDefaults: () -> Unit,
  onTestAiConnection: () -> Unit,
) {
  item(key = "ai_provider_and_mode") {
    SettingsGroupCard(title = "AI 공급자 & 실행 엔진", icon = Icons.Outlined.AutoAwesome) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val currentPresetLabel =
          providerPresets.firstOrNull { it.id == aiConfig.providerId }?.label ?: "AI 브릿지"
        Text(
          text = currentPresetLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        SpringPressSurface(
          onClick = onResetAiConfigDefaults,
          shape = designConfig.cardInnerShape(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = "AI 설정 초기화", modifier = Modifier.size(16.dp))
            Text("초기화", style = MaterialTheme.typography.labelMedium)
          }
        }
      }

      Text("공급자 프리셋 (5종)", style = MaterialTheme.typography.labelLarge)
      val presetsToRender =
        providerPresets.ifEmpty {
          listOf(
            AiProviderPresetUi("google_gemini", "구글", "Google (Gemini CLI)", "gemini", "-p", "", "gemini-2.5-flash"),
            AiProviderPresetUi("kimi", "키미", "Kimi (Kimi CLI)", "kimi", "--prompt", "", "moonshot-v1-8k"),
            AiProviderPresetUi("openai_codex", "오픈AI", "OpenAI (Codex / CLI)", "codex", "exec", "", "gpt-4o-mini"),
            AiProviderPresetUi("anthropic_claude", "클로드", "Anthropic (Claude Code)", "claude", "-p", "", "claude-sonnet-4-20250514"),
            AiProviderPresetUi("custom", "커스텀", "사용자 지정 CLI / 로컬 엔드포인트", "ollama", "run llama3", "", "llama3"),
          )
        }
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        presetsToRender.forEach { preset ->
          IconSegmentTile(
            icon = providerPresetIcon(preset.id),
            label = preset.shortLabel,
            selected = aiConfig.providerId == preset.id,
            onClick = { onUpdateAiConfig("providerId", preset.id) },
            modifier = Modifier.weight(1f, fill = false).testTag("ai_provider_${preset.id}"),
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      Text("순정 AOSP 실행 방식", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        IconSegmentTile(
          icon = Icons.Outlined.Terminal,
          label = AiExecutionMode.LOCAL_BINARY_CLI.label,
          selected = aiConfig.executionMode == AiExecutionMode.LOCAL_BINARY_CLI,
          onClick = { onUpdateAiConfig("executionMode", AiExecutionMode.LOCAL_BINARY_CLI.id) },
          modifier = Modifier.weight(1f).testTag("ai_mode_local_cli"),
        )
        IconSegmentTile(
          icon = Icons.Outlined.Dns,
          label = AiExecutionMode.LOCALHOST_BRIDGE.label,
          selected = aiConfig.executionMode == AiExecutionMode.LOCALHOST_BRIDGE,
          onClick = { onUpdateAiConfig("executionMode", AiExecutionMode.LOCALHOST_BRIDGE.id) },
          modifier = Modifier.weight(1f).testTag("ai_mode_localhost"),
        )
        IconSegmentTile(
          icon = Icons.Outlined.Link,
          label = AiExecutionMode.DIRECT_API.label,
          selected = aiConfig.executionMode == AiExecutionMode.DIRECT_API,
          onClick = { onUpdateAiConfig("executionMode", AiExecutionMode.DIRECT_API.id) },
          modifier = Modifier.weight(1f).testTag("ai_mode_direct_api"),
        )
      }
    }
  }

  item(key = "ai_connection_bridge") {
    var showSecretKey by remember { mutableStateOf(false) }

    SettingsGroupCard(title = "연결 및 브릿지 설정", icon = Icons.Outlined.Terminal) {
      when (aiConfig.executionMode) {
        AiExecutionMode.LOCAL_BINARY_CLI -> {
          OutlinedTextField(
            value = aiConfig.cliCommand,
            onValueChange = { onUpdateAiConfig("cliCommand", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("CLI 실행 명령 / 바이너리 경로") },
            placeholder = { Text("예: gemini, claude, codex, kimi, ollama") },
            modifier = Modifier.fillMaxWidth().testTag("ai_cli_command_input"),
          )
          OutlinedTextField(
            value = aiConfig.cliArgs,
            onValueChange = { onUpdateAiConfig("cliArgs", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("기본 CLI 인자") },
            placeholder = { Text("예: -p, --prompt, exec") },
            modifier = Modifier.fillMaxWidth().testTag("ai_cli_args_input"),
          )
          Text(
            text = "Termux (/data/data/com.termux/files/usr/bin) 및 시스템 PATH 바이너리를 직접 실행합니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        AiExecutionMode.LOCALHOST_BRIDGE -> {
          OutlinedTextField(
            value = aiConfig.localhostUrl,
            onValueChange = { onUpdateAiConfig("localhostUrl", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("로컬 브릿지 엔드포인트 (OpenAI 호환)") },
            placeholder = { Text("http://127.0.0.1:8787/v1/chat/completions") },
            modifier = Modifier.fillMaxWidth().testTag("ai_localhost_url_input"),
          )
          OutlinedTextField(
            value = aiConfig.modelName,
            onValueChange = { onUpdateAiConfig("modelName", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("모델 이름") },
            placeholder = { Text("gemini-2.5-flash / llama3 / moonshot-v1-8k") },
            modifier = Modifier.fillMaxWidth().testTag("ai_model_input"),
          )
          OutlinedTextField(
            value = aiConfig.apiKey,
            onValueChange = { onUpdateAiConfig("apiKey", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("로컬 브릿지 인증 토큰 (선택)") },
            visualTransformation =
              if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              SpringIconButton(
                onClick = { showSecretKey = !showSecretKey },
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  imageVector =
                    if (showSecretKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showSecretKey) "키 숨기기" else "키 표시",
                  modifier = Modifier.size(18.dp),
                )
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("ai_api_key_input"),
          )
        }

        AiExecutionMode.DIRECT_API -> {
          OutlinedTextField(
            value = aiConfig.apiEndpointUrl,
            onValueChange = { onUpdateAiConfig("apiEndpointUrl", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("API 엔드포인트 URL") },
            placeholder = { Text("https://.../v1/chat/completions") },
            modifier = Modifier.fillMaxWidth().testTag("ai_endpoint_url_input"),
          )
          OutlinedTextField(
            value = aiConfig.modelName,
            onValueChange = { onUpdateAiConfig("modelName", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("모델 이름") },
            modifier = Modifier.fillMaxWidth().testTag("ai_model_input"),
          )
          OutlinedTextField(
            value = aiConfig.apiKey,
            onValueChange = { onUpdateAiConfig("apiKey", it) },
            singleLine = true,
            shape = designConfig.cardInnerShape(),
            label = { Text("API 키 (로컬 DataStore 암호화 보관 권장)") },
            visualTransformation =
              if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              SpringIconButton(
                onClick = { showSecretKey = !showSecretKey },
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  imageVector =
                    if (showSecretKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showSecretKey) "키 숨기기" else "키 표시",
                  modifier = Modifier.size(18.dp),
                )
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("ai_api_key_input"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onTestAiConnection,
          enabled = !isTestingAiConnection,
          shape = designConfig.cardInnerShape(),
          modifier = Modifier.fillMaxWidth().testTag("ai_test_connection_button"),
        ) {
          if (isTestingAiConnection) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
            Text("연결 확인 중...")
          } else {
            Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("연결 테스트")
          }
        }
      }

      if (aiConnectionTestResult != null) {
        val ok = aiConnectionTestResult.ok
        Surface(
          shape = designConfig.cardInnerShape(),
          color =
            if (ok) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
          border =
            BorderStroke(
              1.dp,
              if (ok) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
              else MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("ai_test_result_badge"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid1_5),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(32.dp)
                  .clip(designConfig.iconShape())
                  .background(
                    if (ok) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = aiConnectionTestResult.statusLabel,
                tint = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = aiConnectionTestResult.statusLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
              )
              Text(
                text = aiConnectionTestResult.detailMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  item(key = "ai_gesture_and_foldable") {
    SettingsGroupCard(title = "호출 제스처 · 1:1.4 폴더블 레이아웃", icon = Icons.Outlined.Swipe) {
      OutlinedTextField(
        value = aiConfig.defaultPromptTemplate,
        onValueChange = { onUpdateAiConfig("defaultPromptTemplate", it) },
        singleLine = false,
        maxLines = 3,
        shape = designConfig.cardInnerShape(),
        label = { Text("기본 프롬프트 템플릿") },
        modifier = Modifier.fillMaxWidth().testTag("ai_default_prompt_input"),
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        listOf(
          "이 페이지 핵심 내용 요약해 줘",
          "핵심 쟁점 3가지 bullet 정리",
          "한국어로 자연스럽게 번역·요약해 줘",
        ).forEach { presetPrompt ->
          FilterChip(
            selected = aiConfig.defaultPromptTemplate == presetPrompt,
            onClick = { onUpdateAiConfig("defaultPromptTemplate", presetPrompt) },
            shape = designConfig.cardInnerShape(),
            label = { Text(presetPrompt, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.Bolt,
        title = "드래그 시 자동 전송",
        checked = aiConfig.autoSendOnDrag,
        onCheckedChange = { onUpdateAiConfig("autoSendOnDrag", it.toString()) },
        modifier = Modifier.testTag("ai_auto_send_switch"),
      )
      Text(
        text = "주소창 길게 누르기 후 위쪽 스와이프 시 현재 웹 URL과 기본 프롬프트를 즉시 전송합니다.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      Text("1:1.4 펼침 화면 AI 시트 위치", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        IconSegmentTile(
          icon = Icons.Outlined.Splitscreen,
          label = AiFoldableLayout.RIGHT_PANEL.label,
          selected = aiConfig.foldableLayout == AiFoldableLayout.RIGHT_PANEL,
          onClick = { onUpdateAiConfig("foldableLayout", AiFoldableLayout.RIGHT_PANEL.id) },
          modifier = Modifier.weight(1f).testTag("ai_layout_right_panel"),
        )
        IconSegmentTile(
          icon = Icons.Outlined.WebAsset,
          label = AiFoldableLayout.CENTER_SHEET.label,
          selected = aiConfig.foldableLayout == AiFoldableLayout.CENTER_SHEET,
          onClick = { onUpdateAiConfig("foldableLayout", AiFoldableLayout.CENTER_SHEET.id) },
          modifier = Modifier.weight(1f).testTag("ai_layout_center_sheet"),
        )
      }
    }
  }
}

private fun providerPresetIcon(providerId: String): ImageVector =
  when (providerId) {
    "google_gemini" -> Icons.Outlined.AutoAwesome
    "kimi" -> Icons.Outlined.Bolt
    "openai_codex" -> Icons.Outlined.Terminal
    "anthropic_claude" -> Icons.Outlined.Edit
    else -> Icons.Outlined.Tune
  }
