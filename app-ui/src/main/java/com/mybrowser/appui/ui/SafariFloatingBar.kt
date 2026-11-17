package com.mybrowser.appui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.viewmodel.FloatingBarStyle
import com.mybrowser.appui.viewmodel.OmniboxSuggestionUi
import com.mybrowser.appui.viewmodel.SearchEngineOption
import com.mybrowser.appui.viewmodel.SplitGestureSensitivity
import com.mybrowser.appui.viewmodel.TabUiModel
import com.mybrowser.appui.viewmodel.ToolbarActionId
import com.mybrowser.appui.viewmodel.ToolbarConfigUi
import kotlin.math.abs
import kotlinx.coroutines.delay

private enum class SplitPreviewDirection {
  NEUTRAL,
  RIGHT,
  LEFT,
}

/**
 * Safari 스타일 하단 통합 플로팅 바.
 *
 * - 단일 모드: DesignUiConfig(높이·너비비율·수평오프셋·하단여백·곡률·투명도) 실시간 바인딩
 * - 분할 모드(`isSplitCompact == true`): 각 분할 창 하단 중앙에 독립 플로팅 바 렌더링
 * - 제스처:
 *   1) 짧은 탭: 인라인 주소창 포커스
 *   2) 좌우 스와이프: 이전/다음 탭 전환
 *   3) 위로 스와이프: 탭 & 폴더 서랍 열기
 *   4) 아래로 스와이프: 플로팅 바 컴팩트 알약으로 축소
 *   5) 1:1.4 대화면 주소창 1초(1000ms) 길게 누르기 + 좌우 드래그: 즉시 화면 분할
 */
@Composable
fun SafariFloatingBar(
  focusedTab: TabUiModel?,
  tabCount: Int,
  splitMode: Boolean,
  activePane: Int,
  paneIndex: Int = 0,
  isSplitCompact: Boolean = false,
  isPaneFocused: Boolean = true,
  isBookmarked: Boolean,
  isDesktopMode: Boolean = false,
  isCollapsed: Boolean,
  isOmniboxFocused: Boolean,
  omniboxQuery: String,
  suggestions: List<OmniboxSuggestionUi>,
  searchEngines: List<SearchEngineOption>,
  selectedEngineId: String,
  isFindInPageVisible: Boolean,
  findQuery: String,
  findActiveMatch: Int,
  findTotalMatches: Int,
  toolbarConfig: ToolbarConfigUi,
  designConfig: DesignUiConfig = LocalDesignConfig.current,
  floatingBarStyle: FloatingBarStyle = FloatingBarStyle.TRANSLUCENT_GLASS,
  splitGestureEnabled: Boolean = true,
  splitGestureSensitivity: SplitGestureSensitivity = SplitGestureSensitivity.MEDIUM,
  swipeUpOpenTabs: Boolean = true,
  supportsDualPane: Boolean,
  heroDragLiftProgress: Float = 0f,
  onExpandBar: () -> Unit,
  onCollapseBar: () -> Unit = {},
  onSwipeUpOpenTabs: () -> Unit = {},
  onSwipeUpDragProgress: (Float) -> Unit = {},
  onSwipeUpDragEnd: (Boolean) -> Unit = { completed -> if (completed) onSwipeUpOpenTabs() },
  onSwipeTabDelta: (Int) -> Unit = {},
  onTriggerSplitGesture: (directionRight: Boolean) -> Unit = {},
  onCloseSplitPane: () -> Unit = {},
  onFocusOmnibox: () -> Unit,
  onBlurOmnibox: () -> Unit,
  onOmniboxQueryChange: (String) -> Unit,
  onOmniboxSubmit: (String) -> Unit,
  onSelectSearchEngine: (String) -> Unit,
  onReloadOrStop: () -> Unit,
  onExecuteAction: (ToolbarActionId) -> Unit,
  onOpenSettings: () -> Unit,
  onFindQueryChange: (String) -> Unit,
  onFindNext: (Boolean) -> Unit,
  onCloseFindInPage: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (isOmniboxFocused) {
    BackHandler(onBack = onBlurOmnibox)
  } else if (isFindInPageVisible) {
    BackHandler(onBack = onCloseFindInPage)
  }

  val horizontalAlignment: Alignment.Horizontal =
    if (isSplitCompact) {
      Alignment.CenterHorizontally
    } else {
      BiasAlignment.Horizontal(designConfig.barHorizontalBias.coerceIn(-1f, 1f))
    }

  var isSplitArmed by remember { mutableStateOf(false) }
  var splitDragX by remember { mutableFloatStateOf(0f) }
  val density = LocalDensity.current
  val thresholdPx = with(density) { splitGestureSensitivity.swipeThresholdDp.dp.toPx() }
  val splitDirection by remember(isSplitArmed, splitDragX, thresholdPx) {
    derivedStateOf {
      when {
        !isSplitArmed -> SplitPreviewDirection.NEUTRAL
        splitDragX > thresholdPx -> SplitPreviewDirection.RIGHT
        splitDragX < -thresholdPx -> SplitPreviewDirection.LEFT
        else -> SplitPreviewDirection.NEUTRAL
      }
    }
  }

  val widthFraction = designConfig.barWidthFraction.coerceIn(0.60f, 1.0f)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .imePadding()
        .padding(
          start = if (isSplitCompact) 6.dp else BrowserSpacing.Grid1_5,
          end = if (isSplitCompact) 6.dp else BrowserSpacing.Grid1_5,
          top = BrowserSpacing.Grid1,
          bottom = designConfig.barBottomOffsetDp.coerceIn(4f, 40f).dp,
        ),
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
  ) {
    // 1초 길게 누르기 + 스와이프 화면 분할 방향 프리뷰 HUD
    AnimatedVisibility(
      visible = isSplitArmed && supportsDualPane && splitGestureEnabled,
      enter =
        fadeIn(BrowserMotion.expressiveSpring()) +
          scaleIn(BrowserMotion.expressiveSpring(), initialScale = 0.90f),
      exit =
        fadeOut(BrowserMotion.snappySpring()) +
          scaleOut(BrowserMotion.snappySpring(), targetScale = 0.90f),
    ) {
      SplitGesturePreviewHud(
        direction = splitDirection,
        currentHost =
          if (focusedTab == null || focusedTab.url == "about:mb" || focusedTab.url == "about:blank") {
            "현재 탭"
          } else {
            focusedTab.displayHost
          },
        designConfig = designConfig,
      )
    }

    // 인라인 자동완성 추천 덱
    AnimatedVisibility(
      visible = isOmniboxFocused,
      enter =
        fadeIn(BrowserMotion.expressiveSpring()) +
          slideInVertically(BrowserMotion.expressiveSpring()) { it / 5 },
      exit =
        fadeOut(BrowserMotion.snappySpring()) +
          slideOutVertically(BrowserMotion.snappySpring()) { it / 5 },
    ) {
      FrostedFloatingSurface(
        shape = designConfig.cardOuterShape(),
        opacity = (designConfig.barOpacity + 0.06f).coerceIn(0.86f, 0.99f),
        blurRadiusDp = designConfig.barBlurRadiusDp,
        isFocused = true,
        modifier =
          Modifier.widthIn(max = if (isSplitCompact) 480.dp else 680.dp)
            .fillMaxWidth(widthFraction),
      ) {
        Column(
          modifier = Modifier.padding(BrowserSpacing.Grid1_5),
          verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        ) {
          if (suggestions.isNotEmpty()) {
            LazyColumn(
              modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              reverseLayout = true,
            ) {
              items(suggestions, key = { it.url }) { suggestion ->
                SpringPressSurface(
                  onClick = { onOmniboxSubmit(suggestion.url) },
                  shape = designConfig.cardInnerShape(),
                  unselectedColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .padding(horizontal = BrowserSpacing.Grid1_5, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                  ) {
                    Icon(
                      imageVector =
                        if (suggestion.isBookmark) Icons.Outlined.Bookmark else Icons.Outlined.History,
                      contentDescription = null,
                      tint =
                        if (suggestion.isBookmark) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = suggestion.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              }
            }
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
            items(searchEngines, key = { it.id }) { engine ->
              FilterChip(
                selected = engine.id == selectedEngineId,
                onClick = { onSelectSearchEngine(engine.id) },
                shape = designConfig.cardInnerShape(),
                label = { Text(engine.label, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
      }
    }

    // 페이지 내 찾기 플로팅 필
    AnimatedVisibility(
      visible = isFindInPageVisible && !isOmniboxFocused,
      enter =
        fadeIn(BrowserMotion.expressiveSpring()) +
          scaleIn(BrowserMotion.expressiveSpring(), initialScale = 0.94f),
      exit =
        fadeOut(BrowserMotion.snappySpring()) +
          scaleOut(BrowserMotion.snappySpring(), targetScale = 0.94f),
    ) {
      FindInPageFloatingPill(
        query = findQuery,
        activeMatch = findActiveMatch,
        totalMatches = findTotalMatches,
        designConfig = designConfig,
        onQueryChange = onFindQueryChange,
        onPrev = { onFindNext(false) },
        onNext = { onFindNext(true) },
        onClose = onCloseFindInPage,
        modifier =
          Modifier.widthIn(max = if (isSplitCompact) 440.dp else 560.dp)
            .fillMaxWidth(widthFraction),
      )
    }

    // 하단 통합 플로팅 바 (접힘 ↔ 확장 단일 컨테이너 스프링 모핑)
    val shouldShowCollapsed by remember(isCollapsed, isOmniboxFocused) {
      derivedStateOf { isCollapsed && !isOmniboxFocused }
    }

    MorphingFloatingPill(
      focusedTab = focusedTab,
      tabCount = tabCount,
      splitMode = splitMode,
      activePane = activePane,
      paneIndex = paneIndex,
      isSplitCompact = isSplitCompact,
      isPaneFocused = isPaneFocused,
      isBookmarked = isBookmarked,
      isDesktopMode = isDesktopMode,
      isCollapsed = shouldShowCollapsed,
      isOmniboxFocused = isOmniboxFocused,
      omniboxQuery = omniboxQuery,
      toolbarConfig = toolbarConfig,
      designConfig = designConfig,
      floatingBarStyle = floatingBarStyle,
      swipeEnabled = toolbarConfig.swipeBarSwitchTabs,
      swipeUpOpenTabs = swipeUpOpenTabs,
      splitGestureEnabled = splitGestureEnabled,
      supportsDualPane = supportsDualPane,
      splitThresholdPx = thresholdPx,
      heroDragLiftProgress = heroDragLiftProgress,
      onSplitArmChanged = { armed, dragX ->
        isSplitArmed = armed
        splitDragX = dragX
      },
      onTriggerSplitGesture = onTriggerSplitGesture,
      onCloseSplitPane = onCloseSplitPane,
      onSwipeTabDelta = onSwipeTabDelta,
      onSwipeUpDragProgress = onSwipeUpDragProgress,
      onSwipeUpDragEnd = onSwipeUpDragEnd,
      onExpandBar = onExpandBar,
      onCollapseBar = onCollapseBar,
      onFocusOmnibox = onFocusOmnibox,
      onBlurOmnibox = onBlurOmnibox,
      onOmniboxQueryChange = onOmniboxQueryChange,
      onOmniboxSubmit = onOmniboxSubmit,
      onReloadOrStop = onReloadOrStop,
      onExecuteAction = onExecuteAction,
      onOpenSettings = onOpenSettings,
    )
  }
}

@Composable
private fun SplitGesturePreviewHud(
  direction: SplitPreviewDirection,
  currentHost: String,
  designConfig: DesignUiConfig,
) {
  val isLeft = direction == SplitPreviewDirection.LEFT
  val isRight = direction == SplitPreviewDirection.RIGHT
  val guideText =
    when (direction) {
      SplitPreviewDirection.RIGHT -> "오른쪽 분할 · 현재 탭 → 오른쪽 / 새 탭 ← 왼쪽"
      SplitPreviewDirection.LEFT -> "왼쪽 분할 · 현재 탭 ← 왼쪽 / 새 탭 → 오른쪽"
      SplitPreviewDirection.NEUTRAL -> "좌우로 밀어 1:1.4 화면 분할"
    }

  Surface(
    shape = designConfig.cardOuterShape(),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    tonalElevation = 10.dp,
    shadowElevation = 14.dp,
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().testTag("split_gesture_preview_hud"),
  ) {
    Column(
      modifier = Modifier.padding(BrowserSpacing.Grid1_5),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left Preview Pane
        Surface(
          shape = designConfig.cardInnerShape(),
          color =
            if (isLeft) MaterialTheme.colorScheme.primaryContainer
            else if (isRight) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          border =
            BorderStroke(
              width = if (isLeft || isRight) 1.5.dp else 1.dp,
              color =
                if (isLeft) MaterialTheme.colorScheme.primary
                else if (isRight) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.weight(1f).height(46.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector =
                when {
                  isRight -> Icons.Outlined.Add
                  else -> Icons.AutoMirrored.Outlined.ArrowBack
                },
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                when {
                  isRight -> "새 탭"
                  isLeft -> currentHost
                  else -> "왼쪽 창"
                },
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Right Preview Pane
        Surface(
          shape = designConfig.cardInnerShape(),
          color =
            if (isRight) MaterialTheme.colorScheme.primaryContainer
            else if (isLeft) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
          border =
            BorderStroke(
              width = if (isLeft || isRight) 1.5.dp else 1.dp,
              color =
                if (isRight) MaterialTheme.colorScheme.primary
                else if (isLeft) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.weight(1f).height(46.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector =
                when {
                  isLeft -> Icons.Outlined.Add
                  else -> Icons.AutoMirrored.Outlined.ArrowForward
                },
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                when {
                  isRight -> currentHost
                  isLeft -> "새 탭"
                  else -> "오른쪽 창"
                },
              style = MaterialTheme.typography.labelSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Text(
        text = guideText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MorphingFloatingPill(
  focusedTab: TabUiModel?,
  tabCount: Int,
  splitMode: Boolean,
  activePane: Int,
  paneIndex: Int,
  isSplitCompact: Boolean,
  isPaneFocused: Boolean,
  isBookmarked: Boolean,
  isDesktopMode: Boolean,
  isCollapsed: Boolean,
  isOmniboxFocused: Boolean,
  omniboxQuery: String,
  toolbarConfig: ToolbarConfigUi,
  designConfig: DesignUiConfig,
  floatingBarStyle: FloatingBarStyle,
  swipeEnabled: Boolean,
  swipeUpOpenTabs: Boolean,
  splitGestureEnabled: Boolean,
  supportsDualPane: Boolean,
  splitThresholdPx: Float,
  heroDragLiftProgress: Float,
  onSplitArmChanged: (Boolean, Float) -> Unit,
  onTriggerSplitGesture: (Boolean) -> Unit,
  onCloseSplitPane: () -> Unit,
  onSwipeTabDelta: (Int) -> Unit,
  onSwipeUpDragProgress: (Float) -> Unit,
  onSwipeUpDragEnd: (Boolean) -> Unit,
  onExpandBar: () -> Unit,
  onCollapseBar: () -> Unit,
  onFocusOmnibox: () -> Unit,
  onBlurOmnibox: () -> Unit,
  onOmniboxQueryChange: (String) -> Unit,
  onOmniboxSubmit: (String) -> Unit,
  onReloadOrStop: () -> Unit,
  onExecuteAction: (ToolbarActionId) -> Unit,
  onOpenSettings: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(isOmniboxFocused) {
    if (isOmniboxFocused) {
      focusRequester.requestFocus()
    }
  }

  val haptic = LocalHapticFeedback.current
  var isHoldingIdle by remember { mutableStateOf(false) }
  val holdProgressAnim = remember { Animatable(0f) }

  LaunchedEffect(isHoldingIdle) {
    if (isHoldingIdle) {
      holdProgressAnim.snapTo(0f)
      delay(180L)
      holdProgressAnim.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 820, easing = LinearEasing),
      )
    } else {
      holdProgressAnim.snapTo(0f)
    }
  }

  val collapseProgress by
    animateFloatAsState(
      targetValue = if (isCollapsed && !isOmniboxFocused) 1f else 0f,
      animationSpec = BrowserMotion.morphingPillSpring(),
      label = "MorphingPillCollapse",
    )

  val surfaceAlpha =
    if (floatingBarStyle == FloatingBarStyle.TRANSLUCENT_GLASS) {
      designConfig.barOpacity.coerceIn(0.55f, 1f)
    } else {
      1f
    }
  val blurRadius =
    if (floatingBarStyle == FloatingBarStyle.TRANSLUCENT_GLASS) {
      designConfig.barBlurRadiusDp.coerceIn(0f, 32f)
    } else {
      0f
    }

  val barHeight = designConfig.barHeightDp.coerceIn(44f, 68f)
  val animatedHeight = androidx.compose.ui.unit.lerp(barHeight.dp, 40.dp, collapseProgress)
  val animatedCorner =
    androidx.compose.ui.unit.lerp(
      designConfig.barCornerRadiusDp.coerceIn(0f, 36f).dp,
      20.dp,
      collapseProgress,
    )
  val outerShape = androidx.compose.foundation.shape.RoundedCornerShape(animatedCorner)
  val innerShape = designConfig.barInnerShape()

  val targetOmniboxHeight =
    if (isOmniboxFocused) {
      (barHeight - 8f).coerceIn(34f, 60f).dp
    } else {
      (barHeight - 12f).coerceIn(32f, 56f).dp
    }
  val omniboxHeight by
    animateDpAsState(
      targetValue = targetOmniboxHeight,
      animationSpec = BrowserMotion.expressiveSpring(),
      label = "OmniboxHeightMorph",
    )

  val canLongPressSplit = supportsDualPane && splitGestureEnabled

  // 통합 제스처 머신:
  // - 짧은 탭 -> 주소창 포커스
  // - 좌우 스와이프 -> 탭 전환
  // - 위로 드래그 -> 실시간 Hero Tab-Fly 미리보기 + 탭 목록 진입
  // - 아래로 스와이프 -> 알약 축소
  // - 1000ms 길게 누르기 + 좌우 드래그 -> 1:1.4 즉시 분할
  val omniboxGestureModifier =
    remember(
      isOmniboxFocused,
      isCollapsed,
      canLongPressSplit,
      swipeEnabled,
      swipeUpOpenTabs,
      tabCount,
      splitThresholdPx,
    ) {
      if (isOmniboxFocused || isCollapsed) {
        Modifier
      } else {
        Modifier.semantics(mergeDescendants = false) {
          role = Role.Button
          onClick(label = "주소 입력") {
            onFocusOmnibox()
            true
          }
        }.pointerInput(
          isOmniboxFocused,
          isCollapsed,
          canLongPressSplit,
          swipeEnabled,
          swipeUpOpenTabs,
          tabCount,
          splitThresholdPx,
        ) {
          awaitEachGesture {
            val touchSlop = viewConfiguration.touchSlop
            val down = awaitFirstDown(requireUnconsumed = false)
            val downPos = down.position
            val startTime = System.currentTimeMillis()
            val holdDurationMs = 1000L

            var movedEarly = false
            var releasedEarly = false
            var armed = false
            var earlyDragX = 0f
            var earlyDragY = 0f

            if (canLongPressSplit) {
              isHoldingIdle = true
            }

            try {
              while (!movedEarly && !releasedEarly && !armed) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = holdDurationMs - elapsed
                if (remaining <= 0L) {
                  if (canLongPressSplit) {
                    armed = true
                  }
                  break
                }

                val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }
                if (event == null) {
                  if (canLongPressSplit) {
                    armed = true
                  }
                  break
                }

                val change =
                  event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change == null || !change.pressed) {
                  releasedEarly = true
                  break
                }

                val dx = change.position.x - downPos.x
                val dy = change.position.y - downPos.y
                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                  movedEarly = true
                  earlyDragX = dx
                  earlyDragY = dy
                  change.consume()
                  break
                }
              }

              isHoldingIdle = false

              if (releasedEarly) {
                onFocusOmnibox()
                return@awaitEachGesture
              }

              if (movedEarly) {
                var trackingHeroFly = false
                var latestHeroProgress = 0f
                var quickActionDone = false
                val heroMaxPx = 150.dp.toPx().coerceAtLeast(120f)

                fun handleMove(dx: Float, dy: Float) {
                  if (quickActionDone) return
                  if (!trackingHeroFly && abs(dx) > abs(dy) * 1.15f && swipeEnabled && tabCount > 1 && abs(dx) > 52f) {
                    quickActionDone = true
                    onSwipeTabDelta(if (dx > 0f) -1 else 1)
                  } else if (dy < -touchSlop && (trackingHeroFly || abs(dy) >= abs(dx)) && swipeUpOpenTabs) {
                    trackingHeroFly = true
                    latestHeroProgress = ((-dy) / heroMaxPx).coerceIn(0f, 1f)
                    onSwipeUpDragProgress(latestHeroProgress)
                  } else if (!trackingHeroFly && dy > 44f && abs(dy) > abs(dx)) {
                    quickActionDone = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCollapseBar()
                  }
                }

                handleMove(earlyDragX, earlyDragY)
                while (true) {
                  val ev = awaitPointerEvent()
                  val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
                  if (ch == null || !ch.pressed) break
                  handleMove(ch.position.x - downPos.x, ch.position.y - downPos.y)
                  ch.consume()
                }
                if (trackingHeroFly) {
                  val completed = latestHeroProgress >= 0.36f
                  if (completed) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  }
                  onSwipeUpDragEnd(completed)
                }
                return@awaitEachGesture
              }

              if (armed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSplitArmChanged(true, 0f)
                var latestDragX = 0f
                while (true) {
                  val ev = awaitPointerEvent()
                  val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
                  if (ch == null || !ch.pressed) {
                    if (latestDragX > splitThresholdPx) {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onTriggerSplitGesture(true)
                    } else if (latestDragX < -splitThresholdPx) {
                      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                      onTriggerSplitGesture(false)
                    }
                    break
                  }
                  ch.consume()
                  latestDragX = ch.position.x - downPos.x
                  onSplitArmChanged(true, latestDragX)
                }
              }
            } finally {
              isHoldingIdle = false
              onSplitArmChanged(false, 0f)
            }
          }
        }
      }
    }

  // 바 전체 영역 세로 스와이프 (버튼 영역 포함 실시간 Hero Tab-Fly & 축소)
  val barVerticalSwipeModifier =
    if (isOmniboxFocused || isCollapsed) {
      Modifier
    } else {
      Modifier.pointerInput(swipeUpOpenTabs) {
        awaitEachGesture {
          val touchSlop = viewConfiguration.touchSlop
          val down = awaitFirstDown(requireUnconsumed = false)
          val downPos = down.position
          var trackingHeroFly = false
          var latestHeroProgress = 0f
          var collapseDone = false
          val heroMaxPx = 150.dp.toPx().coerceAtLeast(120f)
          try {
            while (!collapseDone) {
              val ev = awaitPointerEvent()
              val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
              if (ch == null || !ch.pressed) break
              if (ch.isConsumed && !trackingHeroFly) break
              val dx = ch.position.x - downPos.x
              val dy = ch.position.y - downPos.y
              if (dy < -touchSlop * 1.5f && (trackingHeroFly || abs(dy) > abs(dx) * 1.3f) && swipeUpOpenTabs) {
                trackingHeroFly = true
                ch.consume()
                latestHeroProgress = ((-dy) / heroMaxPx).coerceIn(0f, 1f)
                onSwipeUpDragProgress(latestHeroProgress)
              } else if (!trackingHeroFly && dy > 48f && abs(dy) > abs(dx) * 1.4f) {
                collapseDone = true
                ch.consume()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCollapseBar()
              }
            }
            if (trackingHeroFly) {
              val completed = latestHeroProgress >= 0.36f
              if (completed) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              onSwipeUpDragEnd(completed)
            }
          } catch (t: Throwable) {
            if (trackingHeroFly) onSwipeUpDragEnd(false)
            throw t
          }
        }
      }
    }

  val maxBarWidth = if (isSplitCompact) 520.dp else 720.dp
  val widthFraction = designConfig.barWidthFraction.coerceIn(0.60f, 1.0f)

  BoxWithConstraints(
    modifier = Modifier.widthIn(max = maxBarWidth).fillMaxWidth(widthFraction),
    contentAlignment = Alignment.Center,
  ) {
    val compactMode = isSplitCompact || maxWidth < 460.dp
    val buttonSize =
      when {
        isSplitCompact && maxWidth < 340.dp -> 32.dp
        isSplitCompact -> 36.dp
        barHeight <= 48f -> 36.dp
        compactMode -> 40.dp
        else -> 42.dp
      }

    val expandedWidth = maxWidth
    val collapsedWidth = (if (isSplitCompact) 168.dp else 208.dp).coerceAtMost(expandedWidth)
    val animatedWidth = androidx.compose.ui.unit.lerp(expandedWidth, collapsedWidth, collapseProgress)

    // 분할 모드에서는 각 창 크기에 맞춰 좌/우 커스텀 슬롯 수를 안전하게 축약
    val visibleLeftSlots =
      if (isSplitCompact) {
        toolbarConfig.leftSlots.take(if (maxWidth < 330.dp) 1 else 2)
      } else {
        toolbarConfig.leftSlots
      }
    val visibleRightSlots =
      if (isSplitCompact) {
        toolbarConfig.rightSlots
          .filterNot { it == ToolbarActionId.SPLIT_VIEW }
          .take(if (maxWidth < 330.dp) 0 else 1)
      } else {
        toolbarConfig.rightSlots
      }

    val expandedContentVisibility = (1f - collapseProgress * 1.8f).coerceIn(0f, 1f)
    val collapsedContentVisibility = ((collapseProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val isInternalPage =
      focusedTab == null || focusedTab.url == "about:mb" || focusedTab.url == "about:blank"

    val pillTestTag =
      if (collapseProgress > 0.5f) {
        "collapsed_floating_pill"
      } else if (isSplitCompact) {
        "safari_floating_bar_pane_$paneIndex"
      } else {
        "safari_floating_bar"
      }

    val collapsedInteraction = remember { MutableInteractionSource() }
    val collapsedPressScale = rememberPressScale(collapsedInteraction, 0.94f)

    FrostedFloatingSurface(
      shape = outerShape,
      opacity = surfaceAlpha,
      blurRadiusDp = blurRadius,
      isFocused = isOmniboxFocused || (isSplitCompact && isPaneFocused),
      modifier =
        Modifier.size(width = animatedWidth, height = animatedHeight)
          .graphicsLayer {
            translationY = -heroDragLiftProgress * 36.dp.toPx()
            alpha = 1f - heroDragLiftProgress * 0.25f
            if (collapseProgress > 0.5f) {
              val s = collapsedPressScale.value
              scaleX = s
              scaleY = s
            }
          }
          .then(
            if (collapseProgress > 0.5f) {
              Modifier.clickable(
                interactionSource = collapsedInteraction,
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onExpandBar,
              )
            } else {
              barVerticalSwipeModifier
            }
          )
          .testTag(pillTestTag),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (focusedTab?.isLoading == true && collapseProgress < 0.5f) {
          LinearProgressIndicator(
            progress = { (focusedTab.progress / 100f).coerceIn(0.05f, 1f) },
            modifier =
              Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(2.dp)
                .padding(horizontal = BrowserSpacing.Grid2),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
          )
        }

        // 1초 길게 누르기 진행 바 (분할 제스처 대기 표시)
        if (holdProgressAnim.value > 0.02f) {
          LinearProgressIndicator(
            progress = { holdProgressAnim.value },
            modifier =
              Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(2.5.dp)
                .padding(horizontal = BrowserSpacing.Grid2),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
          )
        }

        // [확장 상태 레이아웃] (스크롤 축소 시 자연스럽게 scale & fade)
        if (expandedContentVisibility > 0.02f) {
          Row(
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer { alpha = expandedContentVisibility }
                .padding(horizontal = if (isSplitCompact) 6.dp else BrowserSpacing.Grid1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            // 1. [좌측 커스텀 아이콘 슬롯] - 스태거드 스케일 인 (0.8f -> 1f)
            AnimatedVisibility(
              visible = !isOmniboxFocused,
              enter = fadeIn(BrowserMotion.morphingPillSpring()),
              exit = fadeOut(BrowserMotion.snappySpring()),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                visibleLeftSlots.forEachIndexed { idx, action ->
                  val stagger = (1f - collapseProgress * (1.35f + idx * 0.2f)).coerceIn(0f, 1f)
                  val iconScale = 0.8f + 0.2f * stagger
                  Box(
                    modifier =
                      Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = stagger
                      }
                  ) {
                    CustomToolbarSlotButton(
                      action = action,
                      focusedTab = focusedTab,
                      tabCount = tabCount,
                      splitMode = splitMode,
                      isBookmarked = isBookmarked,
                      isDesktopMode = isDesktopMode,
                      supportsDualPane = supportsDualPane,
                      buttonSize = buttonSize,
                      designConfig = designConfig,
                      onClick = { onExecuteAction(action) },
                    )
                  }
                }
              }
            }

            // 2. [중앙 통합 주소창] - 무테두리 반투명 인셋 캡슐
            Surface(
              shape = innerShape,
              color =
                MaterialTheme.colorScheme.surfaceVariant.copy(
                  alpha =
                    if (floatingBarStyle == FloatingBarStyle.TRANSLUCENT_GLASS) 0.58f else 0.86f
                ),
              border =
                if (holdProgressAnim.value > 0.05f) {
                  androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else {
                  null
                },
              modifier =
                Modifier.weight(1f)
                  .height(omniboxHeight)
                  .testTag(if (isSplitCompact) "omnibox_trigger_$paneIndex" else "omnibox_trigger")
                  .clip(innerShape)
                  .then(omniboxGestureModifier),
            ) {
              if (isOmniboxFocused) {
                Row(
                  modifier = Modifier.fillMaxSize().padding(horizontal = BrowserSpacing.Grid1_5),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "검색",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  BasicTextField(
                    value = omniboxQuery,
                    onValueChange = onOmniboxQueryChange,
                    singleLine = true,
                    textStyle =
                      MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                      ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions =
                      KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                      ),
                    keyboardActions = KeyboardActions(onGo = { onOmniboxSubmit(omniboxQuery) }),
                    decorationBox = { innerTextField ->
                      Box(contentAlignment = Alignment.CenterStart) {
                        if (omniboxQuery.isEmpty()) {
                          Text(
                            text = "검색 또는 주소 입력",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                        innerTextField()
                      }
                    },
                    modifier =
                      Modifier.weight(1f).focusRequester(focusRequester).testTag("omnibox_input"),
                  )
                  if (omniboxQuery.isNotEmpty()) {
                    SpringIconButton(
                      onClick = { onOmniboxQueryChange("") },
                      modifier = Modifier.size(30.dp),
                    ) {
                      Icon(
                        Icons.Outlined.Clear,
                        contentDescription = "주소 지우기",
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                  SpringIconButton(
                    onClick = { onOmniboxSubmit(omniboxQuery) },
                    modifier = Modifier.size(32.dp).testTag("omnibox_submit_button"),
                  ) {
                    Icon(
                      Icons.AutoMirrored.Outlined.ArrowForward,
                      contentDescription = "이동",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  SpringIconButton(
                    onClick = onBlurOmnibox,
                    modifier = Modifier.size(30.dp).testTag("omnibox_close_button"),
                  ) {
                    Icon(
                      Icons.Outlined.Close,
                      contentDescription = "취소",
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              } else {
                Row(
                  modifier =
                    Modifier.fillMaxSize()
                      .padding(horizontal = if (isSplitCompact) 8.dp else BrowserSpacing.Grid1_5),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  if (splitMode) {
                    Surface(
                      shape = designConfig.cardInnerShape(),
                      color =
                        if (isPaneFocused) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ) {
                      Text(
                        text = "P${(if (isSplitCompact) paneIndex else activePane) + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (isPaneFocused) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                      )
                    }
                  }

                  Icon(
                    imageVector =
                      when {
                        isInternalPage -> Icons.Outlined.Shield
                        focusedTab?.isSecure == true -> Icons.Outlined.Lock
                        else -> Icons.Outlined.Public
                      },
                    contentDescription = "보안 상태",
                    tint =
                      if (focusedTab?.isSecure == true) MaterialTheme.colorScheme.secondary
                      else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(15.dp),
                  )

                  Text(
                    text = if (isInternalPage) "검색 또는 주소 입력" else focusedTab!!.displayHost,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                      if (isInternalPage) MaterialTheme.colorScheme.onSurfaceVariant
                      else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )

                  if ((focusedTab?.blockedCount ?: 0) > 0 && !isSplitCompact) {
                    Surface(
                      shape = designConfig.cardInnerShape(),
                      color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                      Text(
                        text = "${focusedTab?.blockedCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }

                  if (!isInternalPage) {
                    SpringIconButton(
                      onClick = onReloadOrStop,
                      modifier = Modifier.size(30.dp).testTag("reload_button"),
                    ) {
                      Icon(
                        imageVector =
                          if (focusedTab!!.isLoading) Icons.Outlined.Close else Icons.Outlined.Refresh,
                        contentDescription = if (focusedTab.isLoading) "중지" else "새로고침",
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
              }
            }

            // 3. [우측 커스텀 아이콘 슬롯] + [분할 시 창 합치기] + 4. [최우측 설정 아이콘 고정]
            AnimatedVisibility(
              visible = !isOmniboxFocused,
              enter = fadeIn(BrowserMotion.morphingPillSpring()),
              exit = fadeOut(BrowserMotion.snappySpring()),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                visibleRightSlots.forEachIndexed { idx, action ->
                  val stagger = (1f - collapseProgress * (1.35f + idx * 0.2f)).coerceIn(0f, 1f)
                  val iconScale = 0.8f + 0.2f * stagger
                  Box(
                    modifier =
                      Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = stagger
                      }
                  ) {
                    CustomToolbarSlotButton(
                      action = action,
                      focusedTab = focusedTab,
                      tabCount = tabCount,
                      splitMode = splitMode,
                      isBookmarked = isBookmarked,
                      isDesktopMode = isDesktopMode,
                      supportsDualPane = supportsDualPane,
                      buttonSize = buttonSize,
                      designConfig = designConfig,
                      onClick = { onExecuteAction(action) },
                    )
                  }
                }

                if (isSplitCompact) {
                  SpringIconButton(
                    onClick = onCloseSplitPane,
                    modifier = Modifier.size(buttonSize).testTag("merge_split_button_$paneIndex"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.FullscreenExit,
                      contentDescription = "창 합치기",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }

                val settingsStagger = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                val settingsScale = 0.8f + 0.2f * settingsStagger
                Box(
                  modifier =
                    Modifier.graphicsLayer {
                      scaleX = settingsScale
                      scaleY = settingsScale
                      alpha = settingsStagger
                    }
                ) {
                  SpringIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(buttonSize).testTag("fixed_settings_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Settings,
                      contentDescription = "설정",
                      tint = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.size(if (isSplitCompact) 18.dp else 20.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // [스크롤 축소 상태 마이크로 알약 컨텐츠] (도메인 + 보안 아이콘)
        if (collapsedContentVisibility > 0.02f) {
          Row(
            modifier =
              Modifier.fillMaxSize()
                .graphicsLayer {
                  alpha = collapsedContentVisibility
                  val s = 0.86f + 0.14f * collapsedContentVisibility
                  scaleX = s
                  scaleY = s
                }
                .padding(horizontal = BrowserSpacing.Grid2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector =
                if (focusedTab?.isSecure == true) Icons.Outlined.Lock else Icons.Outlined.Shield,
              contentDescription = "보안 상태",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(15.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            Text(
              text = if (isInternalPage) "MyBrowser(MB)" else focusedTab!!.displayHost,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CustomToolbarSlotButton(
  action: ToolbarActionId,
  focusedTab: TabUiModel?,
  tabCount: Int,
  splitMode: Boolean,
  isBookmarked: Boolean,
  isDesktopMode: Boolean,
  supportsDualPane: Boolean,
  buttonSize: androidx.compose.ui.unit.Dp,
  designConfig: DesignUiConfig,
  onClick: () -> Unit,
) {
  val hasWebPage =
    focusedTab != null && focusedTab.url != "about:mb" && focusedTab.url != "about:blank"
  val enabled =
    when (action) {
      ToolbarActionId.BACK -> focusedTab?.canGoBack == true
      ToolbarActionId.FORWARD -> focusedTab?.canGoForward == true
      ToolbarActionId.REFRESH -> hasWebPage
      ToolbarActionId.SHARE -> hasWebPage
      ToolbarActionId.BOOKMARK -> hasWebPage
      ToolbarActionId.FIND_IN_PAGE -> hasWebPage
      ToolbarActionId.SPLIT_VIEW -> supportsDualPane || splitMode
      else -> true
    }

  val tag = "toolbar_slot_${action.id.lowercase()}"

  if (action == ToolbarActionId.TABS) {
    SpringIconButton(
      onClick = onClick,
      modifier = Modifier.size(buttonSize).testTag(tag),
    ) {
      Surface(
        shape = designConfig.iconShape(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
      ) {
        Text(
          text = tabCount.toString(),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
    return
  }

  val icon =
    when (action) {
      ToolbarActionId.BACK -> Icons.AutoMirrored.Outlined.ArrowBack
      ToolbarActionId.FORWARD -> Icons.AutoMirrored.Outlined.ArrowForward
      ToolbarActionId.REFRESH -> Icons.Outlined.Refresh
      ToolbarActionId.BOOKMARK ->
        if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder
      ToolbarActionId.HISTORY -> Icons.Outlined.History
      ToolbarActionId.DESKTOP_MODE -> Icons.Outlined.DesktopWindows
      ToolbarActionId.SHARE -> Icons.Outlined.Share
      ToolbarActionId.QR_SCAN -> Icons.Outlined.QrCodeScanner
      ToolbarActionId.SCREENSHOT -> Icons.Outlined.CameraAlt
      ToolbarActionId.READER_MODE -> Icons.Outlined.ChromeReaderMode
      ToolbarActionId.HOME -> Icons.Outlined.Home
      ToolbarActionId.NEW_TAB -> Icons.Outlined.Add
      ToolbarActionId.SPLIT_VIEW -> Icons.Outlined.Splitscreen
      ToolbarActionId.FIND_IN_PAGE -> Icons.Outlined.FindInPage
      ToolbarActionId.LIBRARY -> Icons.Outlined.Bookmarks
      ToolbarActionId.TABS -> Icons.Outlined.Add
    }

  val tint =
    when {
      !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
      action == ToolbarActionId.SPLIT_VIEW && splitMode -> MaterialTheme.colorScheme.primary
      action == ToolbarActionId.DESKTOP_MODE && isDesktopMode -> MaterialTheme.colorScheme.primary
      action == ToolbarActionId.BOOKMARK && isBookmarked -> MaterialTheme.colorScheme.secondary
      else -> MaterialTheme.colorScheme.onSurface
    }

  SpringIconButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.size(buttonSize).testTag(tag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = action.label,
      tint = tint,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
private fun FindInPageFloatingPill(
  query: String,
  activeMatch: Int,
  totalMatches: Int,
  designConfig: DesignUiConfig,
  onQueryChange: (String) -> Unit,
  onPrev: () -> Unit,
  onNext: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FrostedFloatingSurface(
    shape = designConfig.barOuterShape(),
    opacity = (designConfig.barOpacity + 0.06f).coerceIn(0.86f, 0.99f),
    blurRadiusDp = designConfig.barBlurRadiusDp,
    isFocused = true,
    modifier = modifier.testTag("find_in_page_bar"),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(horizontal = BrowserSpacing.Grid1_5, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
    ) {
      Icon(
        Icons.Outlined.FindInPage,
        contentDescription = "페이지 찾기",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle =
          MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
          Box {
            if (query.isEmpty()) {
              Text(
                "페이지 내 찾기",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            inner()
          }
        },
        modifier = Modifier.weight(1f).testTag("find_in_page_input"),
      )
      if (query.isNotEmpty()) {
        Text(
          text = "$activeMatch/$totalMatches",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      SpringIconButton(onClick = onPrev, enabled = totalMatches > 0, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "이전 결과")
      }
      SpringIconButton(onClick = onNext, enabled = totalMatches > 0, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "다음 결과")
      }
      SpringIconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.Close, contentDescription = "닫기")
      }
    }
  }
}
