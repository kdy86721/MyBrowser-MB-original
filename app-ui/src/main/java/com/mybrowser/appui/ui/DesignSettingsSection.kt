package com.mybrowser.appui.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlignHorizontalCenter
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.theme.AccentColorPreset
import com.mybrowser.appui.theme.ThemeMode
import com.mybrowser.appui.viewmodel.AutoHideSensitivity
import com.mybrowser.appui.viewmodel.BrowserSettingsUi
import com.mybrowser.appui.viewmodel.FloatingBarStyle
import com.mybrowser.appui.viewmodel.TabViewMode
import com.mybrowser.appui.viewmodel.ToolbarAlignment
import com.mybrowser.appui.viewmodel.ToolbarConfigUi
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pillar 1 (디자인):
 * 실시간 인터랙티브 프리뷰 + 아이콘·바·카드 곡률, 바 크기/위치/수평 오프셋/투명도,
 * 테마, 강조 색상, 웹 글꼴 배율, 전체 화면(상태 표시줄 숨김) 완전 제어.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.designSettingsItems(
  settings: BrowserSettingsUi,
  toolbarConfig: ToolbarConfigUi,
  designConfig: DesignUiConfig,
  onUpdateSetting: (key: String, value: String) -> Unit,
  onUpdateDesignParam: (key: String, value: String) -> Unit,
  onResetDesignDefaults: () -> Unit,
  onSelectAlignment: (ToolbarAlignment) -> Unit,
  onToggleAutoMinimize: (Boolean) -> Unit,
) {
  item(key = "live_design_preview") {
    var previewSplit by remember { mutableStateOf(false) }

    SettingsGroupCard(title = "실시간 프리뷰", icon = Icons.Outlined.AspectRatio) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
          FilterChip(
            selected = !previewSplit,
            onClick = { previewSplit = false },
            shape = designConfig.cardInnerShape(),
            label = { Text("단일 창", style = MaterialTheme.typography.labelSmall) },
          )
          FilterChip(
            selected = previewSplit,
            onClick = { previewSplit = true },
            shape = designConfig.cardInnerShape(),
            label = { Text("1:1.4 분할", style = MaterialTheme.typography.labelSmall) },
          )
        }
        SpringPressSurface(
          onClick = onResetDesignDefaults,
          shape = designConfig.cardInnerShape(),
          modifier = Modifier.testTag("design_reset_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Outlined.RestartAlt, contentDescription = "초기화", modifier = Modifier.size(16.dp))
            Text("초기화", style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      val barAlignment =
        BiasAlignment(designConfig.barHorizontalBias.coerceIn(-1f, 1f), 1f)
      val barAlpha =
        if (settings.floatingBarStyle == FloatingBarStyle.TRANSLUCENT_GLASS) {
          designConfig.barOpacity.coerceIn(0.55f, 1f)
        } else {
          1f
        }
      val barWidthPct = (designConfig.barWidthFraction * 100f).roundToInt()

      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(176.dp)
            .clip(designConfig.cardOuterShape())
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                )
              )
            )
            .padding(horizontal = 10.dp),
      ) {
        AnimatedContent(
          targetState = previewSplit,
          transitionSpec = {
            fadeIn(BrowserMotion.expressiveSpring(designConfig.animationSpeedScale)) togetherWith
              fadeOut(BrowserMotion.snappySpring(designConfig.animationSpeedScale))
          },
          label = "DesignPreviewSplitMode",
          modifier = Modifier.fillMaxSize(),
        ) { split ->
          if (!split) {
            Box(modifier = Modifier.fillMaxSize()) {
              Surface(
                shape = designConfig.cardOuterShape(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(58.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(30.dp)
                        .clip(designConfig.iconShape())
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      Icons.Outlined.Palette,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Column {
                    Text("카드 · 아이콘 프리뷰", style = MaterialTheme.typography.labelLarge)
                    Text(
                      "카드 ${designConfig.cardCornerRadiusDp.roundToInt()}dp · 아이콘 ${designConfig.iconCornerRadiusDp.roundToInt()}dp",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              Box(
                modifier =
                  Modifier.align(barAlignment)
                    .padding(bottom = (designConfig.barBottomOffsetDp * 0.55f).dp)
                    .fillMaxWidth(designConfig.barWidthFraction.coerceIn(0.6f, 1f)),
              ) {
                MiniPreviewFloatingBar(
                  label = "높이 ${designConfig.barHeightDp.roundToInt()}dp · 너비 $barWidthPct%",
                  barAlpha = barAlpha,
                  designConfig = designConfig,
                )
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxSize().padding(vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              repeat(2) { paneIdx ->
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .fillMaxHeight()
                      .clip(designConfig.cardInnerShape())
                      .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                      .padding(horizontal = 4.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = (designConfig.barBottomOffsetDp * 0.4f).dp)
                        .fillMaxWidth(designConfig.barWidthFraction.coerceIn(0.6f, 1f)),
                  ) {
                    MiniPreviewFloatingBar(
                      label = "P${paneIdx + 1}",
                      barAlpha = barAlpha,
                      designConfig = designConfig,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  item(key = "corner_radii_controls") {
    SettingsGroupCard(title = "곡률 사용자화", icon = Icons.Outlined.RoundedCorner) {
      DesignFloatSliderRow(
        label = "아이콘 모서리 반경",
        valueText =
          if (designConfig.iconCornerRadiusDp <= 0.5f) "직각 0dp"
          else "${designConfig.iconCornerRadiusDp.roundToInt()}dp",
        value = designConfig.iconCornerRadiusDp,
        valueRange = 0f..24f,
        stepSize = 1f,
        onValueChange = { onUpdateDesignParam("iconCornerRadiusDp", it.roundToInt().toString()) },
        testTag = "slider_icon_radius",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      DesignFloatSliderRow(
        label = "플로팅 바 모서리 반경",
        valueText = "${designConfig.barCornerRadiusDp.roundToInt()}dp",
        value = designConfig.barCornerRadiusDp,
        valueRange = 0f..36f,
        stepSize = 2f,
        onValueChange = { onUpdateDesignParam("barCornerRadiusDp", it.roundToInt().toString()) },
        testTag = "slider_bar_radius",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      DesignFloatSliderRow(
        label = "카드 · 컨테이너 곡률",
        valueText = "${designConfig.cardCornerRadiusDp.roundToInt()}dp",
        value = designConfig.cardCornerRadiusDp,
        valueRange = 8f..28f,
        stepSize = 1f,
        onValueChange = { onUpdateDesignParam("cardCornerRadiusDp", it.roundToInt().toString()) },
        testTag = "slider_card_radius",
      )
    }
  }

  item(key = "bar_dimensions_position") {
    SettingsGroupCard(title = "플로팅 바 크기 및 위치", icon = Icons.Outlined.VerticalAlignBottom) {
      DesignFloatSliderRow(
        label = "바 높이",
        valueText = "${designConfig.barHeightDp.roundToInt()}dp",
        value = designConfig.barHeightDp,
        valueRange = 44f..68f,
        stepSize = 2f,
        onValueChange = { onUpdateDesignParam("barHeightDp", it.roundToInt().toString()) },
        testTag = "slider_bar_height",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      val widthPct = (designConfig.barWidthFraction * 100f).roundToInt()
      DesignFloatSliderRow(
        label = "바 너비 비율",
        valueText = "$widthPct%",
        value = designConfig.barWidthFraction,
        valueRange = 0.60f..1.00f,
        stepSize = 0.02f,
        onValueChange = {
          onUpdateDesignParam("barWidthFraction", String.format(Locale.US, "%.2f", it))
        },
        testTag = "slider_bar_width",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      val biasText =
        when {
          designConfig.barHorizontalBias < -0.15f ->
            "왼쪽 ${String.format(Locale.US, "%.2f", designConfig.barHorizontalBias)}"
          designConfig.barHorizontalBias > 0.15f ->
            "오른쪽 +${String.format(Locale.US, "%.2f", designConfig.barHorizontalBias)}"
          else -> "가운데 (0.00)"
        }
      DesignFloatSliderRow(
        label = "수평 위치 오프셋",
        valueText = biasText,
        value = designConfig.barHorizontalBias,
        valueRange = -1.0f..1.0f,
        stepSize = 0.05f,
        onValueChange = {
          onUpdateDesignParam("barHorizontalBias", String.format(Locale.US, "%.2f", it))
        },
        testTag = "slider_bar_horizontal_bias",
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        ToolbarAlignment.entries.forEach { align ->
          val icon =
            when (align) {
              ToolbarAlignment.LEFT_OFFSET -> Icons.AutoMirrored.Outlined.AlignHorizontalLeft
              ToolbarAlignment.CENTER -> Icons.Outlined.AlignHorizontalCenter
              ToolbarAlignment.RIGHT_OFFSET -> Icons.AutoMirrored.Outlined.AlignHorizontalRight
            }
          val isSelected =
            kotlin.math.abs(designConfig.barHorizontalBias - align.bias) < 0.2f
          IconSegmentTile(
            icon = icon,
            label = align.label,
            selected = isSelected,
            onClick = { onSelectAlignment(align) },
            modifier = Modifier.weight(1f),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      DesignFloatSliderRow(
        label = "하단 여백 높이",
        valueText = "${designConfig.barBottomOffsetDp.roundToInt()}dp",
        value = designConfig.barBottomOffsetDp,
        valueRange = 4f..40f,
        stepSize = 2f,
        onValueChange = { onUpdateDesignParam("barBottomOffsetDp", it.roundToInt().toString()) },
        testTag = "slider_bottom_offset",
      )
    }
  }

  item(key = "surface_opacity_blur") {
    SettingsGroupCard(title = "배경 투명도 및 글래스 효과", icon = Icons.Outlined.BlurOn) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        FloatingBarStyle.entries.forEach { style ->
          IconSegmentTile(
            icon = if (style == FloatingBarStyle.TRANSLUCENT_GLASS) Icons.Outlined.BlurOn else Icons.Outlined.Opacity,
            label = style.label,
            selected = settings.floatingBarStyle == style,
            onClick = { onUpdateSetting("floatingBarStyle", style.id) },
            modifier = Modifier.weight(1f),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      val opacityPct = (designConfig.barOpacity * 100f).roundToInt()
      DesignFloatSliderRow(
        label = "바 투명도",
        valueText = "$opacityPct%",
        value = designConfig.barOpacity,
        valueRange = 0.55f..1.00f,
        stepSize = 0.05f,
        onValueChange = {
          onUpdateDesignParam("barOpacity", String.format(Locale.US, "%.2f", it))
        },
        testTag = "slider_surface_opacity",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      DesignFloatSliderRow(
        label = "블러 강도",
        valueText = "${designConfig.barBlurRadiusDp.roundToInt()}dp",
        value = designConfig.barBlurRadiusDp,
        valueRange = 0f..32f,
        stepSize = 2f,
        onValueChange = { onUpdateDesignParam("barBlurRadiusDp", it.roundToInt().toString()) },
        testTag = "slider_blur_radius",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.VerticalAlignBottom,
        title = "스크롤 시 알약 축소",
        checked = toolbarConfig.autoMinimize,
        onCheckedChange = onToggleAutoMinimize,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Text("자동 숨김 감도", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        AutoHideSensitivity.entries.forEach { sens ->
          FilterChip(
            selected = settings.autoHideSensitivity == sens,
            onClick = { onUpdateSetting("autoHideSensitivity", sens.id) },
            shape = designConfig.cardInnerShape(),
            label = { Text(sens.label) },
          )
        }
      }
    }
  }

  item(key = "motion_and_url_edit") {
    SettingsGroupCard(title = "모션 속도 · 주소창 편집", icon = Icons.Outlined.Speed) {
      val speedText =
        when {
          designConfig.animationSpeedScale <= 0.75f ->
            "느리게 ${String.format(Locale.US, "%.1f", designConfig.animationSpeedScale)}x"
          designConfig.animationSpeedScale >= 1.35f ->
            "빠르게 ${String.format(Locale.US, "%.1f", designConfig.animationSpeedScale)}x"
          else ->
            "${String.format(Locale.US, "%.1f", designConfig.animationSpeedScale)}x"
        }
      DesignFloatSliderRow(
        label = "애니메이션 속도",
        valueText = speedText,
        value = designConfig.animationSpeedScale,
        valueRange = 0.5f..2.0f,
        stepSize = 0.1f,
        onValueChange = {
          onUpdateDesignParam("animationSpeedScale", String.format(Locale.US, "%.1f", it))
        },
        testTag = "slider_animation_speed",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.Edit,
        title = "주소 편집 시 바 확장 표시",
        checked = settings.expandBarOnUrlEdit,
        onCheckedChange = { onUpdateSetting("expandBarOnUrlEdit", it.toString()) },
      )
    }
  }

  item(key = "theme_and_typography") {
    SettingsGroupCard(title = "테마 · 강조 색상 · 웹 화면", icon = Icons.Outlined.Palette) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        ThemeMode.entries.forEach { mode ->
          val icon =
            when (mode) {
              ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
              ThemeMode.LIGHT -> Icons.Outlined.LightMode
              ThemeMode.DARK -> Icons.Outlined.DarkMode
              ThemeMode.AMOLED_DARK -> Icons.Outlined.DarkMode
            }
          IconSegmentTile(
            icon = icon,
            label = mode.label,
            selected = settings.themeMode == mode,
            onClick = { onUpdateSetting("themeMode", mode.id) },
            modifier = Modifier.weight(1f),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Text("강조 색상 프리셋", style = MaterialTheme.typography.labelLarge)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        AccentColorPreset.entries.forEach { preset ->
          FilterChip(
            selected = settings.accentPreset == preset,
            onClick = { onUpdateSetting("accentPreset", preset.id) },
            shape = designConfig.cardInnerShape(),
            leadingIcon = {
              Box(
                modifier =
                  Modifier.size(14.dp)
                    .clip(CircleShape)
                    .background(preset.swatch)
              )
            },
            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      DesignFloatSliderRow(
        label = "웹 글꼴 배율",
        valueText = "${settings.webFontZoomPercent}%",
        value = settings.webFontZoomPercent.toFloat(),
        valueRange = 75f..200f,
        stepSize = 5f,
        onValueChange = { onUpdateSetting("webFontZoomPercent", it.roundToInt().toString()) },
        testTag = "slider_web_font_zoom",
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.DarkMode,
        title = "웹페이지 강제 다크",
        checked = settings.forceWebDark,
        onCheckedChange = { onUpdateSetting("forceWebDark", it.toString()) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.ZoomIn,
        title = "사이트 강제 확대·축소",
        checked = settings.forceZoomEnabled,
        onCheckedChange = { onUpdateSetting("forceZoomEnabled", it.toString()) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.Fullscreen,
        title = "상태 표시줄 숨기기 (전체 화면)",
        checked = settings.hideStatusBar,
        onCheckedChange = { onUpdateSetting("hideStatusBar", it.toString()) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      SettingsIconSwitchRow(
        icon = Icons.Outlined.Splitscreen,
        title = "펼침 시 자동 1:1.4 분할",
        checked = settings.autoDualPaneUnfold,
        onCheckedChange = { onUpdateSetting("autoDualPaneUnfold", it.toString()) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Text("탭창 보기 방식", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        TabViewMode.entries.forEach { mode ->
          val icon =
            when (mode) {
              TabViewMode.THUMBNAIL_GRID -> Icons.Outlined.GridView
              TabViewMode.COMPACT_LIST -> Icons.Outlined.ViewAgenda
            }
          IconSegmentTile(
            icon = icon,
            label = mode.label,
            selected = settings.tabViewMode == mode,
            onClick = { onUpdateSetting("tabViewMode", mode.id) },
            modifier = Modifier.weight(1f).testTag("design_tab_view_${mode.id.lowercase()}"),
          )
        }
      }
    }
  }
}

@androidx.compose.runtime.Composable
private fun MiniPreviewFloatingBar(
  label: String,
  barAlpha: Float,
  designConfig: DesignUiConfig,
) {
  FrostedFloatingSurface(
    shape = designConfig.barOuterShape(),
    opacity = barAlpha,
    blurRadiusDp = designConfig.barBlurRadiusDp,
    modifier = Modifier.fillMaxWidth().height((designConfig.barHeightDp * 0.78f).dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier =
          Modifier.size(24.dp)
            .clip(designConfig.barInnerShape())
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp))
      }
      Surface(
        shape = designConfig.barInnerShape(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        modifier = Modifier.weight(1f).height(26.dp),
      ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 8.dp)) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Box(
        modifier =
          Modifier.size(24.dp)
            .clip(designConfig.barInnerShape())
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
      }
    }
  }
}

@androidx.compose.runtime.Composable
private fun DesignFloatSliderRow(
  label: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  stepSize: Float,
  onValueChange: (Float) -> Unit,
  testTag: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.labelLarge)
      Surface(
        shape = LocalDesignConfig.current.cardInnerShape(),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = valueText,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
      }
    }
    Slider(
      value = value.coerceIn(valueRange.start, valueRange.endInclusive),
      onValueChange = { raw ->
        val snapped =
          ((raw / stepSize).roundToInt() * stepSize)
            .coerceIn(valueRange.start, valueRange.endInclusive)
        onValueChange(snapped)
      },
      valueRange = valueRange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
