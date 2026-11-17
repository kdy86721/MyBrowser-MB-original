package com.mybrowser.appui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.network.AiChatMessageUi
import com.mybrowser.appui.network.AiConfigUi
import com.mybrowser.appui.network.AiFoldableLayout
import com.mybrowser.appui.network.AiProviderPresetUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal sealed interface AiMessageSegment {
  data class Prose(val text: String) : AiMessageSegment
  data class CodeBlock(val language: String, val code: String) : AiMessageSegment
}

internal fun parseAiMessageSegments(raw: String): List<AiMessageSegment> {
  if (!raw.contains("```")) {
    return listOf(AiMessageSegment.Prose(raw))
  }
  val segments = mutableListOf<AiMessageSegment>()
  val lines = raw.split('\n')
  val proseBuffer = StringBuilder()
  val codeBuffer = StringBuilder()
  var inCode = false
  var codeLang = ""

  for (line in lines) {
    val trimmed = line.trimStart()
    if (!inCode && trimmed.startsWith("```")) {
      val prose = proseBuffer.toString().trim()
      if (prose.isNotEmpty()) {
        segments += AiMessageSegment.Prose(prose)
      }
      proseBuffer.clear()
      codeLang = trimmed.removePrefix("```").trim()
      codeBuffer.clear()
      inCode = true
    } else if (inCode && trimmed.startsWith("```")) {
      segments += AiMessageSegment.CodeBlock(language = codeLang, code = codeBuffer.toString().trimEnd())
      codeBuffer.clear()
      codeLang = ""
      inCode = false
    } else if (inCode) {
      if (codeBuffer.isNotEmpty()) codeBuffer.append('\n')
      codeBuffer.append(line)
    } else {
      if (proseBuffer.isNotEmpty()) proseBuffer.append('\n')
      proseBuffer.append(line)
    }
  }

  if (inCode) {
    segments += AiMessageSegment.CodeBlock(language = codeLang, code = codeBuffer.toString())
  } else {
    val prose = proseBuffer.toString().trim()
    if (prose.isNotEmpty()) {
      segments += AiMessageSegment.Prose(prose)
    }
  }
  return if (segments.isEmpty()) listOf(AiMessageSegment.Prose(raw)) else segments
}

/**
 * Morphing AI CLI Chat Bottom Sheet / 1:1.4 Right Panel.
 * Continuously interpolates position, width, height, and barCornerRadiusDp from the floating omnibox.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatOverlaySheet(
  morphProgress: Float,
  postureState: FoldablePostureState,
  designConfig: DesignUiConfig,
  aiConfig: AiConfigUi,
  providerPresets: List<AiProviderPresetUi>,
  attachedUrl: String,
  attachedTitle: String,
  draftInput: String,
  messages: List<AiChatMessageUi>,
  isStreaming: Boolean,
  onDraftChange: (String) -> Unit,
  onDetachPageContext: () -> Unit,
  onSendMessage: (String?) -> Unit,
  onStopStreaming: () -> Unit,
  onClearConversation: () -> Unit,
  onOpenAiSettings: (SpatialDiveOrigin) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = morphProgress > 0.1f, onBack = onDismiss)

  val p = morphProgress.coerceIn(0f, 1f)
  val density = LocalDensity.current
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val isRightSplitPanel =
    postureState.supportsDualPane && aiConfig.foldableLayout == AiFoldableLayout.RIGHT_PANEL

  val syncedCornerDp =
    lerp(
      designConfig.barCornerRadiusDp.coerceIn(8f, 36f).dp,
      designConfig.barCornerRadiusDp.coerceIn(14f, 28f).dp,
      p,
    )
  val sheetShape = RoundedCornerShape(syncedCornerDp)
  val innerShape = designConfig.cardInnerShape()

  val presetLabel =
    providerPresets.firstOrNull { it.id == aiConfig.providerId }?.shortLabel ?: "AI"

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
    contentAlignment = if (isRightSplitPanel) Alignment.CenterEnd else Alignment.BottomCenter,
  ) {
    val maxH = maxHeight
    val maxW = maxWidth

    val targetHeight =
      if (isRightSplitPanel) (maxH - 24.dp).coerceAtLeast(320.dp)
      else (maxH * 0.86f).coerceAtLeast(340.dp)
    val startHeight = designConfig.barHeightDp.coerceIn(44f, 68f).dp
    val animatedHeight = lerp(startHeight, targetHeight, p)

    val targetWidth =
      if (isRightSplitPanel) (maxW * 0.48f).coerceIn(340.dp, 460.dp)
      else minOf(maxW - 16.dp, 680.dp)
    val startWidth = (maxW * designConfig.barWidthFraction.coerceIn(0.60f, 1.0f)).coerceAtMost(targetWidth)
    val animatedWidth = lerp(startWidth, targetWidth, p)

    val bottomSlidePx = with(density) { ((1f - p) * 28.dp.toPx()) }
    val contentRevealAlpha = ((p - 0.18f) / 0.55f).coerceIn(0f, 1f)

    Surface(
      shape = sheetShape,
      color = MaterialTheme.colorScheme.surface.copy(alpha = (0.94f + 0.05f * p).coerceIn(0.9f, 0.99f)),
      tonalElevation = 10.dp,
      shadowElevation = 24.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f * p)),
      modifier =
        Modifier
          .padding(
            end = if (isRightSplitPanel) BrowserSpacing.Grid2 else BrowserSpacing.Grid1,
            start = BrowserSpacing.Grid1,
            bottom = BrowserSpacing.Grid1,
            top = if (isRightSplitPanel) BrowserSpacing.Grid1 else 0.dp,
          )
          .width(animatedWidth)
          .height(animatedHeight)
          .graphicsLayer {
            translationY = bottomSlidePx
            alpha = (0.35f + 0.65f * (p * 3f).coerceIn(0f, 1f))
          }
          .testTag("ai_chat_sheet"),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentRevealAlpha },
      ) {
        // Handle Pill
        Box(
          modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
          )
        }

        // Top Header Bar
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = BrowserSpacing.Grid2, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier =
                Modifier
                  .size(34.dp)
                  .clip(designConfig.iconShape())
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = "AI 어시스턴트",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
            Column {
              Text(
                text = "$presetLabel AI 브릿지",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${aiConfig.executionMode.label} · ${aiConfig.modelName.ifBlank { aiConfig.cliCommand }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            var settingsBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
            SpringIconButton(
              onClick = {
                onOpenAiSettings(SpatialDiveOrigin(settingsBounds, designConfig.cardCornerRadiusDp))
              },
              shape = innerShape,
              modifier =
                Modifier.size(40.dp)
                  .onGloballyPositioned { settingsBounds = it.boundsInRoot() }
                  .testTag("ai_chat_settings_button"),
            ) {
              Icon(Icons.Outlined.Tune, contentDescription = "AI 설정", modifier = Modifier.size(18.dp))
            }
            SpringIconButton(
              onClick = onClearConversation,
              enabled = messages.isNotEmpty() || isStreaming,
              shape = innerShape,
              modifier = Modifier.size(40.dp).testTag("ai_chat_clear_button"),
            ) {
              Icon(Icons.Outlined.DeleteSweep, contentDescription = "대화 초기화", modifier = Modifier.size(18.dp))
            }
            SpringIconButton(
              onClick = onDismiss,
              shape = innerShape,
              modifier = Modifier.size(40.dp).testTag("ai_chat_close_button"),
            ) {
              Icon(Icons.Outlined.Close, contentDescription = "AI 닫기", modifier = Modifier.size(18.dp))
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Message Stream Area
        if (messages.isEmpty()) {
          Box(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(BrowserSpacing.Grid2),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Surface(
                shape = designConfig.cardOuterShape(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(BrowserSpacing.Grid2),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Terminal,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "순정 AOSP 로컬 CLI / 브릿지 대화",
                      style = MaterialTheme.typography.labelLarge,
                    )
                  }
                  Text(
                    text =
                      if (attachedUrl.isNotBlank()) {
                        "현재 페이지 주소가 입력창에 자동 첨부되었습니다. 전송 버튼을 누르거나 질문을 수정하세요."
                      } else {
                        "질문을 입력하면 설정된 AI CLI 또는 로컬 브릿지로 스트리밍 응답을 받아옵니다."
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                listOf(
                  "이 페이지 핵심 내용 요약해 줘",
                  "핵심 개념 3가지 bullet 정리",
                  "한국어로 자연스럽게 번역·요약해 줘",
                ).forEach { quickPrompt ->
                  FilterChip(
                    selected = false,
                    onClick = {
                      val composed =
                        if (attachedUrl.isNotBlank()) "$quickPrompt\n$attachedUrl"
                        else quickPrompt
                      onSendMessage(composed)
                    },
                    shape = innerShape,
                    label = { Text(quickPrompt, style = MaterialTheme.typography.labelSmall) },
                  )
                }
              }
            }
          }
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(BrowserSpacing.Grid2),
            verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
          ) {
            items(messages, key = { it.id }) { message ->
              AiMessageBubble(
                message = message,
                designConfig = designConfig,
              )
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Composer Dock with Attached URL Chip + Pre-filled Multi-line TextField
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = BrowserSpacing.Grid2, vertical = BrowserSpacing.Grid1_5),
          verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        ) {
          if (attachedUrl.isNotBlank()) {
            Surface(
              shape = innerShape,
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth().testTag("ai_attached_url_chip"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Link,
                  contentDescription = "첨부된 페이지",
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  if (attachedTitle.isNotBlank()) {
                    Text(
                      text = attachedTitle,
                      style = MaterialTheme.typography.labelMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Text(
                    text = attachedUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                SpringIconButton(
                  onClick = onDetachPageContext,
                  modifier = Modifier.size(32.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "페이지 첨부 해제",
                    modifier = Modifier.size(15.dp),
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          ) {
            OutlinedTextField(
              value = draftInput,
              onValueChange = onDraftChange,
              placeholder = { Text("AI에게 질문 또는 CLI 명령 입력") },
              minLines = 1,
              maxLines = 4,
              shape = innerShape,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions =
                KeyboardActions(
                  onSend = {
                    if (!isStreaming && draftInput.isNotBlank()) onSendMessage(null)
                  },
                ),
              modifier = Modifier.weight(1f).testTag("ai_chat_input"),
            )

            if (isStreaming) {
              SpringIconButton(
                onClick = onStopStreaming,
                shape = innerShape,
                modifier =
                  Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, innerShape)
                    .testTag("ai_chat_stop_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Stop,
                  contentDescription = "응답 중지",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            } else {
              val canSend = draftInput.isNotBlank()
              SpringIconButton(
                onClick = { onSendMessage(null) },
                enabled = canSend,
                shape = innerShape,
                modifier =
                  Modifier
                    .size(48.dp)
                    .background(
                      if (canSend) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
                      innerShape,
                    )
                    .testTag("ai_chat_send_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Send,
                  contentDescription = "전송",
                  tint =
                    if (canSend) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AiMessageBubble(
  message: AiChatMessageUi,
  designConfig: DesignUiConfig,
) {
  @Suppress("DEPRECATION")
  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val innerShape = designConfig.cardInnerShape()

  if (message.isUser) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      Surface(
        shape = designConfig.cardOuterShape(),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.widthIn(max = 520.dp),
      ) {
        Column(
          modifier = Modifier.padding(BrowserSpacing.Grid1_5),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }
  } else {
    val segments = remember(message.text) { parseAiMessageSegments(message.text) }
    Surface(
      shape = designConfig.cardOuterShape(),
      color =
        if (message.isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
      border =
        BorderStroke(
          1.dp,
          if (message.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(BrowserSpacing.Grid1_5),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        if (message.isStreaming && message.text.isBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
              text = "스트리밍 응답 대기 중...",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          segments.forEach { seg ->
            when (seg) {
              is AiMessageSegment.Prose -> {
                Text(
                  text = seg.text,
                  style = MaterialTheme.typography.bodyMedium,
                  color =
                    if (message.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
              }

              is AiMessageSegment.CodeBlock -> {
                var copied by remember { mutableStateOf(false) }
                Surface(
                  shape = innerShape,
                  color = Color(0xFF141820),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .background(Color(0xFF1E2430))
                          .padding(horizontal = 10.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = seg.language.ifBlank { "코드" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB4C2D8),
                      )
                      SpringPressSurface(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(seg.code))
                          copied = true
                          scope.launch {
                            delay(1400L)
                            copied = false
                          }
                        },
                        shape = RoundedCornerShape(8.dp),
                        unselectedColor = Color(0xFF2B3342),
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                          Icon(
                            imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "코드 복사",
                            tint = if (copied) Color(0xFF7BE0AD) else Color(0xFFE0E7F1),
                            modifier = Modifier.size(13.dp),
                          )
                          Text(
                            text = if (copied) "복사됨" else "코드 복사",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (copied) Color(0xFF7BE0AD) else Color(0xFFE0E7F1),
                          )
                        }
                      }
                    }
                    Text(
                      text = seg.code,
                      style =
                        MaterialTheme.typography.bodySmall.copy(
                          fontFamily = FontFamily.Monospace,
                          color = Color(0xFFE8EEF7),
                        ),
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .horizontalScroll(rememberScrollState())
                          .padding(12.dp),
                    )
                  }
                }
              }
            }
          }
        }

        if (message.isStreaming && message.text.isNotBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.8.dp)
            Text(
              text = "생성 중...",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}
