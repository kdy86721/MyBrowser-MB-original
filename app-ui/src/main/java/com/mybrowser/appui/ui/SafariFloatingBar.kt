package com.mybrowser.appui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mybrowser.appui.viewmodel.FloatingBarStyle
import com.mybrowser.appui.viewmodel.OmniboxSuggestionUi
import com.mybrowser.appui.viewmodel.SearchEngineOption
import com.mybrowser.appui.viewmodel.SplitGestureSensitivity
import com.mybrowser.appui.viewmodel.TabUiModel
import com.mybrowser.appui.viewmodel.ToolbarActionId
import com.mybrowser.appui.viewmodel.ToolbarConfigUi
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.delay

enum class OmniboxLongPressDirection {
  NEUTRAL,
  SPLIT_LEFT,
  SPLIT_RIGHT,
  AI_UP,
}

fun resolveLongPressDirection(
  dxPx: Float,
  dyPx: Float,
  canSplit: Boolean,
  splitThresholdPx: Float,
  aiThresholdPx: Float,
): OmniboxLongPressDirection {
  val absX = abs(dxPx)
  val absY = abs(dyPx)
  if (dyPx < -aiThresholdPx && absY >= absX * 0.85f) {
    return OmniboxLongPressDirection.AI_UP
  }
  if (canSplit && absX > splitThresholdPx && absX >= absY) {
    return if (dxPx > 0f) OmniboxLongPressDirection.SPLIT_RIGHT else OmniboxLongPressDirection.SPLIT_LEFT
  }
  return OmniboxLongPressDirection.NEUTRAL
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
  expandBarOnUrlEdit: Boolean = true,
  splitGestureEnabled: Boolean = true,
  splitGestureSensitivity: SplitGestureSensitivity = SplitGestureSensitivity.MEDIUM,
  swipeUpOpenTabs: Boolean = true,
  supportsDualPane: Boolean,
  heroDragLiftProgress: Float = 0f,
  onExpandBar: () -> Unit,
  onCollapseBar: () -> Unit = {},
  onSwipeUpOpenTabs: () -> Unit = {},
  onSwipeUpDragProgress: (Float) -> Unit = {},
  onSwipeUpDragEnd: (completed: Boolean, velocityYPxPerSec: Float) -> Unit = { completed, _ ->
    if (completed) onSwipeUpOpenTabs()
  },
  onSwipeTabDelta: (Int) -> Unit = {},
  onHorizontalTabDrag: (dragDxPx: Float) -> Unit = {},
  onHorizontalTabDragEnd: (dragDxPx: Float, velocityXPxPerSec: Float) -> Unit = { dx, _ ->
    if (abs(dx) > 52f) onSwipeTabDelta(if (dx > 0f) -1 else 1)
  },
  onTriggerSplitGesture: (directionRight: Boolean) -> Unit = {},
  onCloseSplitPane: () -> Unit = {},
  onAiSwipeProgress: (Float) -> Unit = {},
  onAiSwipeEnd: (completed: Boolean, velocityYPxPerSec: Float) -> Unit = { _, _ -> },
  onFocusOmnibox: () -> Unit,
  onBlurOmnibox: () -> Unit,
  onOmniboxQueryChange: (String) -> Unit,
  onOmniboxSubmit: (String) -> Unit,
  onSelectSuggestion: ((url: String, origin: SpatialDiveOrigin) -> Unit)? = null,
  onSelectSearchEngine: (String) -> Unit,
  onReloadOrStop: () -> Unit,
  onExecuteAction: (ToolbarActionId) -> Unit,
  onOpenSettings: (SpatialDiveOrigin) -> Unit,
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

  var isLongPressArmed by remember { mutableStateOf(false) }
  var longPressDragX by remember { mutableFloatStateOf(0f) }
  var longPressDragY by remember { mutableFloatStateOf(0f) }
  val canLongPressSplit = supportsDualPane && splitGestureEnabled
  val density = LocalDensity.current
  val rawThresholdPx = with(density) { splitGestureSensitivity.swipeThresholdDp.dp.toPx() }
  val thresholdPx =
    if (isSplitCompact) minOf(rawThresholdPx, with(density) { 54.dp.toPx() })
    else rawThresholdPx
  val aiThresholdPx = with(density) { 44.dp.toPx() }
  val longPressDirection by remember(
    isLongPressArmed,
    longPressDragX,
    longPressDragY,
    canLongPressSplit,
    thresholdPx,
    aiThresholdPx,
  ) {
    derivedStateOf {
      if (!isLongPressArmed) {
        OmniboxLongPressDirection.NEUTRAL
      } else {
        resolveLongPressDirection(
          dxPx = longPressDragX,
          dyPx = longPressDragY,
          canSplit = canLongPressSplit,
          splitThresholdPx = thresholdPx,
          aiThresholdPx = aiThresholdPx,
        )
      }
    }
  }
  val haptic = LocalHapticFeedback.current
  LaunchedEffect(isLongPressArmed, longPressDirection) {
    if (isLongPressArmed && longPressDirection != OmniboxLongPressDirection.NEUTRAL) {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }

  val speedScale = designConfig.animationSpeedScale
  val shouldShowCollapsed by remember(isCollapsed, isOmniboxFocused) {
    derivedStateOf { isCollapsed && !isOmniboxFocused }
  }
  val pillProgress = remember { Animatable(if (shouldShowCollapsed) 1f else 0f) }
  var isRestoringPill by remember { mutableStateOf(false) }
  LaunchedEffect(shouldShowCollapsed, speedScale) {
    isRestoringPill = !shouldShowCollapsed
    pillProgress.animateTo(
      targetValue = if (shouldShowCollapsed) 1f else 0f,
      animationSpec = BrowserMotion.morphingPillSpring(speedScale),
    )
  }

  val widthFraction =
    if (isSplitCompact) designConfig.barWidthFraction.coerceIn(0.88f, 1.0f)
    else designConfig.barWidthFraction.coerceIn(0.60f, 1.0f)
  val animatedBottomPadding =
    androidx.compose.ui.unit.lerp(
      designConfig.barBottomOffsetDp.coerceIn(4f, 40f).dp,
      10.dp,
      pillProgress.value.coerceIn(0f, 1f),
    )

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        .padding(
          start = if (isSplitCompact) 6.dp else BrowserSpacing.Grid1_5,
          end = if (isSplitCompact) 6.dp else BrowserSpacing.Grid1_5,
          top = BrowserSpacing.Grid1,
          bottom = animatedBottomPadding,
        ),
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
  ) {
    // 680ms 길게 누르기 + 3방향(좌/우 분할 · 상단 AI 호출) 스프링 방향 가이드 HUD
    AnimatedVisibility(
      visible = isLongPressArmed,
      enter =
        fadeIn(BrowserMotion.expressiveSpring(speedScale)) +
          scaleIn(BrowserMotion.expressiveSpring(speedScale), initialScale = 0.88f) +
          slideInVertically(BrowserMotion.expressiveSpring(speedScale)) { it / 4 },
      exit =
        fadeOut(BrowserMotion.snappySpring(speedScale)) +
          scaleOut(BrowserMotion.snappySpring(speedScale), targetScale = 0.88f),
    ) {
      OmniboxLongPressDirectionHud(
        direction = longPressDirection,
        canSplit = canLongPressSplit,
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
        fadeIn(BrowserMotion.expressiveSpring(speedScale)) +
          slideInVertically(BrowserMotion.expressiveSpring(speedScale)) { it / 5 },
      exit =
        fadeOut(BrowserMotion.snappySpring(speedScale)) +
          slideOutVertically(BrowserMotion.snappySpring(speedScale)) { it / 5 },
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
                var itemBounds by remember { mutableStateOf(Rect.Zero) }
                SpringPressSurface(
                  onClick = {
                    val origin = SpatialDiveOrigin(itemBounds, designConfig.cardCornerRadiusDp)
                    if (onSelectSuggestion != null) {
                      onSelectSuggestion(suggestion.url, origin)
                    } else {
                      onOmniboxSubmit(suggestion.url)
                    }
                  },
                  shape = designConfig.cardInnerShape(),
                  unselectedColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                  modifier =
                    Modifier.fillMaxWidth()
                      .onGloballyPositioned { itemBounds = it.boundsInRoot() },
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
        fadeIn(BrowserMotion.expressiveSpring(speedScale)) +
          scaleIn(BrowserMotion.expressiveSpring(speedScale), initialScale = 0.94f),
      exit =
        fadeOut(BrowserMotion.snappySpring(speedScale)) +
          scaleOut(BrowserMotion.snappySpring(speedScale), targetScale = 0.94f),
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

    // 하단 통합 플로팅 바 (접힘 ↔ 확장 단일 Animatable<Float> 120Hz 스프링 모핑)
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
      pillProgress = pillProgress.value.coerceIn(0f, 1f),
      isRestoringPill = isRestoringPill,
      isCollapsed = shouldShowCollapsed,
      isOmniboxFocused = isOmniboxFocused,
      omniboxQuery = omniboxQuery,
      toolbarConfig = toolbarConfig,
      designConfig = designConfig,
      floatingBarStyle = floatingBarStyle,
      expandBarOnUrlEdit = expandBarOnUrlEdit,
      swipeEnabled = toolbarConfig.swipeBarSwitchTabs,
      swipeUpOpenTabs = swipeUpOpenTabs,
      splitGestureEnabled = splitGestureEnabled,
      supportsDualPane = supportsDualPane,
      isLongPressArmed = isLongPressArmed,
      splitThresholdPx = thresholdPx,
      aiThresholdPx = aiThresholdPx,
      heroDragLiftProgress = heroDragLiftProgress,
      onLongPressStateChanged = { armed, dragX, dragY ->
        isLongPressArmed = armed
        longPressDragX = dragX
        longPressDragY = dragY
      },
      onTriggerSplitGesture = onTriggerSplitGesture,
      onCloseSplitPane = onCloseSplitPane,
      onAiSwipeProgress = onAiSwipeProgress,
      onAiSwipeEnd = onAiSwipeEnd,
      onHorizontalTabDrag = onHorizontalTabDrag,
      onHorizontalTabDragEnd = onHorizontalTabDragEnd,
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
private fun OmniboxLongPressDirectionHud(
  direction: OmniboxLongPressDirection,
  canSplit: Boolean,
  currentHost: String,
  designConfig: DesignUiConfig,
) {
  val isLeft = direction == OmniboxLongPressDirection.SPLIT_LEFT
  val isRight = direction == OmniboxLongPressDirection.SPLIT_RIGHT
  val isAiUp = direction == OmniboxLongPressDirection.AI_UP
  val guideText =
    when (direction) {
      OmniboxLongPressDirection.AI_UP -> "위로 놓으면 AI 채팅 브릿지 실행"
      OmniboxLongPressDirection.SPLIT_RIGHT -> "오른쪽 분할 · 현재 탭 → 오른쪽 / 새 탭 ← 왼쪽"
      OmniboxLongPressDirection.SPLIT_LEFT -> "왼쪽 분할 · 현재 탭 ← 왼쪽 / 새 탭 → 오른쪽"
      OmniboxLongPressDirection.NEUTRAL ->
        if (canSplit) "↑ AI 호출 · ←/→ 화면 분할" else "↑ 위로 드래그하여 AI 호출"
    }

  Surface(
    shape = designConfig.cardOuterShape(),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    tonalElevation = 10.dp,
    shadowElevation = 16.dp,
    border =
      BorderStroke(
        width = 1.5.dp,
        color =
          if (isAiUp) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
          else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
      ),
    modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth().testTag("omnibox_long_press_hud"),
  ) {
    Column(
      modifier = Modifier.padding(BrowserSpacing.Grid1_5),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
    ) {
      // Top AI Call Chip
      Surface(
        shape = designConfig.cardInnerShape(),
        color =
          if (isAiUp) MaterialTheme.colorScheme.tertiaryContainer
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border =
          BorderStroke(
            width = if (isAiUp) 1.5.dp else 1.dp,
            color =
              if (isAiUp) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          ),
        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("hud_ai_up_pill"),
      ) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = "AI 호출",
            tint =
              if (isAiUp) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.size(6.dp))
          Text(
            text = if (isAiUp) "AI 어시스턴트 열기" else "↑ AI 호출",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isAiUp) FontWeight.Bold else FontWeight.Medium,
            color =
              if (isAiUp) MaterialTheme.colorScheme.onTertiaryContainer
              else MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      if (canSplit) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Left Split Pill
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
            modifier = Modifier.weight(1f).height(44.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector =
                  if (isRight) Icons.Outlined.Add else Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  when {
                    isRight -> "새 탭"
                    isLeft -> currentHost
                    else -> "← 좌측 분할"
                  },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          // Right Split Pill
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
            modifier = Modifier.weight(1f).height(44.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector =
                  if (isLeft) Icons.Outlined.Add else Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  when {
                    isRight -> currentHost
                    isLeft -> "새 탭"
                    else -> "우측 분할 →"
                  },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Text(
        text = guideText,
        style = MaterialTheme.typography.labelSmall,
        color =
          if (isAiUp) MaterialTheme.colorScheme.tertiary
          else MaterialTheme.colorScheme.primary,
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
  pillProgress: Float,
  isRestoringPill: Boolean,
  isCollapsed: Boolean,
  isOmniboxFocused: Boolean,
  omniboxQuery: String,
  toolbarConfig: ToolbarConfigUi,
  designConfig: DesignUiConfig,
  floatingBarStyle: FloatingBarStyle,
  expandBarOnUrlEdit: Boolean,
  swipeEnabled: Boolean,
  swipeUpOpenTabs: Boolean,
  splitGestureEnabled: Boolean,
  supportsDualPane: Boolean,
  isLongPressArmed: Boolean,
  splitThresholdPx: Float,
  aiThresholdPx: Float,
  heroDragLiftProgress: Float,
  onLongPressStateChanged: (armed: Boolean, dragX: Float, dragY: Float) -> Unit,
  onTriggerSplitGesture: (Boolean) -> Unit,
  onCloseSplitPane: () -> Unit,
  onAiSwipeProgress: (Float) -> Unit,
  onAiSwipeEnd: (completed: Boolean, velocityYPxPerSec: Float) -> Unit,
  onHorizontalTabDrag: (Float) -> Unit,
  onHorizontalTabDragEnd: (Float, Float) -> Unit,
  onSwipeUpDragProgress: (Float) -> Unit,
  onSwipeUpDragEnd: (completed: Boolean, velocityYPxPerSec: Float) -> Unit,
  onExpandBar: () -> Unit,
  onCollapseBar: () -> Unit,
  onFocusOmnibox: () -> Unit,
  onBlurOmnibox: () -> Unit,
  onOmniboxQueryChange: (String) -> Unit,
  onOmniboxSubmit: (String) -> Unit,
  onReloadOrStop: () -> Unit,
  onExecuteAction: (ToolbarActionId) -> Unit,
  onOpenSettings: (SpatialDiveOrigin) -> Unit,
) {
  val speedScale = designConfig.animationSpeedScale
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(isOmniboxFocused) {
    if (isOmniboxFocused) {
      focusRequester.requestFocus()
    }
  }

  val omniboxFocusAnim = remember { Animatable(if (isOmniboxFocused) 1f else 0f) }
  LaunchedEffect(isOmniboxFocused, expandBarOnUrlEdit, speedScale) {
    omniboxFocusAnim.animateTo(
      targetValue = if (isOmniboxFocused) 1f else 0f,
      animationSpec =
        if (expandBarOnUrlEdit) ExpressiveSprings.spatial(speedScale)
        else ExpressiveSprings.micro(speedScale),
    )
  }

  val haptic = LocalHapticFeedback.current
  var isHoldingIdle by remember { mutableStateOf(false) }
  var holdProgressTarget by remember { mutableFloatStateOf(0f) }
  val holdProgress by
    animateFloatAsState(
      targetValue = holdProgressTarget,
      animationSpec = ExpressiveSprings.micro(speedScale),
      label = "HoldSplitProgress",
    )

  LaunchedEffect(isHoldingIdle) {
    if (isHoldingIdle) {
      holdProgressTarget = 0f
      delay(130L)
      val stepCount = 34
      val stepMs = 550L / stepCount
      for (i in 1..stepCount) {
        if (!isHoldingIdle) break
        holdProgressTarget = i / stepCount.toFloat()
        delay(stepMs)
      }
    } else {
      holdProgressTarget = 0f
    }
  }

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
  val omniboxFocus = omniboxFocusAnim.value.coerceIn(0f, 1f)
  val expandedEditBarDp = if (expandBarOnUrlEdit) 96f else barHeight
  val activeExpandedBarHeight =
    androidx.compose.ui.unit.lerp(barHeight.dp, expandedEditBarDp.dp, omniboxFocus)
  val animatedHeight =
    androidx.compose.ui.unit.lerp(activeExpandedBarHeight, 36.dp, pillProgress)

  val targetEditCornerDp =
    if (expandBarOnUrlEdit) designConfig.cardCornerRadiusDp.coerceIn(14f, 26f)
    else designConfig.barCornerRadiusDp.coerceIn(0f, 36f)
  val activeExpandedCorner =
    androidx.compose.ui.unit.lerp(
      designConfig.barCornerRadiusDp.coerceIn(0f, 36f).dp,
      targetEditCornerDp.dp,
      omniboxFocus,
    )
  val animatedCorner =
    androidx.compose.ui.unit.lerp(
      activeExpandedCorner,
      18.dp, // 50% of 36.dp compact pill height
      pillProgress,
    )
  val outerShape = androidx.compose.foundation.shape.RoundedCornerShape(animatedCorner)
  val innerShape = designConfig.barInnerShape()

  val targetEditOmniboxDp =
    if (expandBarOnUrlEdit) 84f else (barHeight - 8f).coerceIn(34f, 60f)
  val omniboxHeight =
    androidx.compose.ui.unit.lerp(
      androidx.compose.ui.unit.lerp(
        (barHeight - 12f).coerceIn(32f, 56f).dp,
        targetEditOmniboxDp.dp,
        omniboxFocus,
      ),
      30.dp,
      pillProgress,
    )

  val canLongPressSplit = supportsDualPane && splitGestureEnabled
  val isOmniboxGestureDisabled = isOmniboxFocused || isCollapsed || pillProgress > 0.3f

  val currentCanSplit by rememberUpdatedState(canLongPressSplit)
  val currentSwipeEnabled by rememberUpdatedState(swipeEnabled)
  val currentSwipeUpOpenTabs by rememberUpdatedState(swipeUpOpenTabs)
  val currentTabCount by rememberUpdatedState(tabCount)
  val currentSplitThresholdPx by rememberUpdatedState(splitThresholdPx)
  val currentAiThresholdPx by rememberUpdatedState(aiThresholdPx)
  val currentOnFocusOmnibox by rememberUpdatedState(onFocusOmnibox)
  val currentOnHorizontalTabDrag by rememberUpdatedState(onHorizontalTabDrag)
  val currentOnHorizontalTabDragEnd by rememberUpdatedState(onHorizontalTabDragEnd)
  val currentOnSwipeUpDragProgress by rememberUpdatedState(onSwipeUpDragProgress)
  val currentOnSwipeUpDragEnd by rememberUpdatedState(onSwipeUpDragEnd)
  val currentOnCollapseBar by rememberUpdatedState(onCollapseBar)
  val currentOnLongPressStateChanged by rememberUpdatedState(onLongPressStateChanged)
  val currentOnTriggerSplitGesture by rememberUpdatedState(onTriggerSplitGesture)
  val currentOnAiSwipeProgress by rememberUpdatedState(onAiSwipeProgress)
  val currentOnAiSwipeEnd by rememberUpdatedState(onAiSwipeEnd)
  val currentLongPressActive by rememberUpdatedState(isHoldingIdle || isLongPressArmed)

  val armGlowProgress by
    animateFloatAsState(
      targetValue = if (isLongPressArmed) 1f else 0f,
      animationSpec = ExpressiveSprings.micro(speedScale),
      label = "OmniboxArmGlow",
    )

  // 통합 제스처 머신 (pointerInput 키를 최소화해 드래그/홀드 중 리컴포지션 취소 방지):
  // - 짧은 탭 -> 주소창 포커스 (자식 새로고침 버튼 소비 시 제외)
  // - 즉시 드래그(680ms 전):
  //   - 좌우 스와이프 -> 탭 전환 캐러셀
  //   - 위로 스와이프 -> VelocityTracker 기반 1:1 Hero Tab-Fly + 탭 서랍 진입
  //   - 아래로 스와이프 -> 알약 축소
  // - 롱프레스 진입 (680ms 유지 후):
  //   - 강한 햅틱 + 외곽 포인트 글로우 + 3방향 가이드 HUD (좌/우 분할, ↑ AI 호출)
  //   - 좌우 드래그(1:1.4 대화면) -> 화면 분할
  //   - 위쪽 드래그(전체 화면 모드 공통) -> 1:1 실시간 AI 채팅 시트 모핑 호출
  val omniboxGestureModifier =
    remember(isOmniboxGestureDisabled) {
      if (isOmniboxGestureDisabled) {
        Modifier
      } else {
        Modifier.semantics(mergeDescendants = false) {
          role = Role.Button
          onClick(label = "주소 입력") {
            currentOnFocusOmnibox()
            true
          }
        }.pointerInput(isOmniboxGestureDisabled) {
          awaitEachGesture {
            val touchSlop = viewConfiguration.touchSlop
            val holdSlop = maxOf(touchSlop * 1.65f, 18.dp.toPx())
            val down = awaitFirstDown(requireUnconsumed = true)
            // 주소창 내부에서 시작된 터치는 부모 barVerticalSwipeModifier가 가로채지 않도록 소비
            down.consume()
            val downPos = down.position
            val startTime = System.currentTimeMillis()
            val holdDurationMs = 680L
            val velocityTracker = VelocityTracker().apply {
              addPosition(down.uptimeMillis, down.position)
            }

            var movedEarly = false
            var releasedWithoutMove = false
            var releaseWasConsumedByChild = false
            var armed = false
            var earlyDragX = 0f
            var earlyDragY = 0f

            isHoldingIdle = true

            try {
              while (!movedEarly && !releasedWithoutMove && !armed) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = holdDurationMs - elapsed
                if (remaining <= 0L) {
                  armed = true
                  break
                }

                val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }
                if (event == null) {
                  armed = true
                  break
                }

                val change =
                  event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                if (change == null || !change.pressed) {
                  releaseWasConsumedByChild = change?.isConsumed == true
                  releasedWithoutMove = true
                  break
                }
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                val dx = change.position.x - downPos.x
                val dy = change.position.y - downPos.y
                if (hypot(dx, dy) > holdSlop) {
                  movedEarly = true
                  earlyDragX = dx
                  earlyDragY = dy
                  change.consume()
                  break
                }
              }

              isHoldingIdle = false

              if (releasedWithoutMove) {
                if (!releaseWasConsumedByChild) {
                  currentOnFocusOmnibox()
                }
                return@awaitEachGesture
              }

              if (movedEarly) {
                var trackingHeroFly = false
                var trackingCarousel = false
                var latestHeroProgress = 0f
                var latestDx = 0f
                var quickActionDone = false
                val heroMaxPx = 180.dp.toPx().coerceAtLeast(140f)

                try {
                  fun handleMove(dx: Float, dy: Float) {
                    if (quickActionDone) return
                    if (trackingCarousel) {
                      latestDx = dx
                      currentOnHorizontalTabDrag(dx)
                    } else if (!trackingHeroFly && abs(dx) > abs(dy) * 1.15f && currentSwipeEnabled && currentTabCount > 1 && abs(dx) > touchSlop) {
                      trackingCarousel = true
                      latestDx = dx
                      currentOnHorizontalTabDrag(dx)
                    } else if (dy < -touchSlop && (trackingHeroFly || abs(dy) >= abs(dx)) && currentSwipeUpOpenTabs) {
                      trackingHeroFly = true
                      latestHeroProgress = ((-dy) / heroMaxPx).coerceIn(0f, 1f)
                      currentOnSwipeUpDragProgress(latestHeroProgress)
                    } else if (!trackingHeroFly && dy > 44f && abs(dy) > abs(dx)) {
                      quickActionDone = true
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      currentOnCollapseBar()
                    }
                  }

                  handleMove(earlyDragX, earlyDragY)
                  while (true) {
                    val ev = awaitPointerEvent()
                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
                    if (ch == null || !ch.pressed) break
                    velocityTracker.addPosition(ch.uptimeMillis, ch.position)
                    handleMove(ch.position.x - downPos.x, ch.position.y - downPos.y)
                    ch.consume()
                  }
                  if (trackingCarousel) {
                    val velX = velocityTracker.calculateVelocity().x
                    trackingCarousel = false
                    currentOnHorizontalTabDragEnd(latestDx, velX)
                  } else if (trackingHeroFly) {
                    val velY = velocityTracker.calculateVelocity().y
                    val completed = latestHeroProgress >= 0.34f || velY < -750f
                    if (completed) {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    trackingHeroFly = false
                    currentOnSwipeUpDragEnd(completed, velY)
                  }
                } catch (t: Throwable) {
                  if (trackingCarousel) currentOnHorizontalTabDragEnd(0f, 0f)
                  if (trackingHeroFly) currentOnSwipeUpDragEnd(false, 0f)
                  throw t
                }
                return@awaitEachGesture
              }

              if (armed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnLongPressStateChanged(true, 0f, 0f)
                var latestDragX = 0f
                var latestDragY = 0f
                var trackingAiSheet = false
                var latestAiProgress = 0f
                val aiMaxPx = 160.dp.toPx().coerceAtLeast(130f)

                try {
                  while (true) {
                    val ev = awaitPointerEvent()
                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
                    if (ch == null || !ch.pressed) {
                      val velY = velocityTracker.calculateVelocity().y
                      val resolvedDir =
                        resolveLongPressDirection(
                          dxPx = latestDragX,
                          dyPx = latestDragY,
                          canSplit = currentCanSplit,
                          splitThresholdPx = currentSplitThresholdPx,
                          aiThresholdPx = currentAiThresholdPx,
                        )
                      if (trackingAiSheet) {
                        val completed =
                          resolvedDir == OmniboxLongPressDirection.AI_UP ||
                            latestAiProgress >= 0.28f ||
                            velY < -650f
                        if (completed) {
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        trackingAiSheet = false
                        currentOnAiSwipeEnd(completed, velY)
                      } else {
                        when (resolvedDir) {
                          OmniboxLongPressDirection.AI_UP -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnAiSwipeEnd(true, velY)
                          }
                          OmniboxLongPressDirection.SPLIT_RIGHT -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnTriggerSplitGesture(true)
                          }
                          OmniboxLongPressDirection.SPLIT_LEFT -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnTriggerSplitGesture(false)
                          }
                          OmniboxLongPressDirection.NEUTRAL -> Unit
                        }
                      }
                      break
                    }
                    ch.consume()
                    velocityTracker.addPosition(ch.uptimeMillis, ch.position)
                    latestDragX = ch.position.x - downPos.x
                    latestDragY = ch.position.y - downPos.y
                    currentOnLongPressStateChanged(true, latestDragX, latestDragY)

                    if (
                      trackingAiSheet ||
                      (latestDragY < -touchSlop * 1.5f && abs(latestDragY) >= abs(latestDragX) * 0.85f)
                    ) {
                      trackingAiSheet = true
                      latestAiProgress = ((-latestDragY) / aiMaxPx).coerceIn(0f, 1f)
                      currentOnAiSwipeProgress(latestAiProgress)
                    }
                  }
                } catch (t: Throwable) {
                  if (trackingAiSheet) {
                    currentOnAiSwipeEnd(false, 0f)
                  }
                  throw t
                }
              }
            } finally {
              isHoldingIdle = false
              currentOnLongPressStateChanged(false, 0f, 0f)
            }
          }
        }
      }
    }

  // 바 전체 영역 세로 스와이프 (주소창 외 좌/우 버튼 영역 포함 실시간 Hero Tab-Fly & 축소)
  val barVerticalSwipeModifier =
    if (isOmniboxFocused || isCollapsed) {
      Modifier
    } else {
      Modifier.pointerInput(isOmniboxGestureDisabled) {
        awaitEachGesture {
          val touchSlop = viewConfiguration.touchSlop
          val down = awaitFirstDown(requireUnconsumed = true)
          val downPos = down.position
          val velocityTracker = VelocityTracker().apply {
            addPosition(down.uptimeMillis, down.position)
          }
          var trackingHeroFly = false
          var latestHeroProgress = 0f
          var collapseDone = false
          val heroMaxPx = 180.dp.toPx().coerceAtLeast(140f)
          try {
            while (!collapseDone) {
              val ev = awaitPointerEvent()
              val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull()
              if (ch == null || !ch.pressed) break
              if (currentLongPressActive) break
              velocityTracker.addPosition(ch.uptimeMillis, ch.position)
              if (ch.isConsumed && !trackingHeroFly) break
              val dx = ch.position.x - downPos.x
              val dy = ch.position.y - downPos.y
              if (dy < -touchSlop * 1.5f && (trackingHeroFly || abs(dy) > abs(dx) * 1.3f) && currentSwipeUpOpenTabs) {
                trackingHeroFly = true
                ch.consume()
                latestHeroProgress = ((-dy) / heroMaxPx).coerceIn(0f, 1f)
                currentOnSwipeUpDragProgress(latestHeroProgress)
              } else if (!trackingHeroFly && dy > 48f && abs(dy) > abs(dx) * 1.4f) {
                collapseDone = true
                ch.consume()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnCollapseBar()
              }
            }
            if (trackingHeroFly) {
              val velY = velocityTracker.calculateVelocity().y
              val completed = latestHeroProgress >= 0.34f || velY < -750f
              if (completed) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              currentOnSwipeUpDragEnd(completed, velY)
            }
          } catch (t: Throwable) {
            if (trackingHeroFly) currentOnSwipeUpDragEnd(false, 0f)
            throw t
          }
        }
      }
    }

  val maxBarWidth = if (isSplitCompact) 520.dp else 720.dp
  val widthFraction =
    if (isSplitCompact) designConfig.barWidthFraction.coerceIn(0.88f, 1.0f)
    else designConfig.barWidthFraction.coerceIn(0.60f, 1.0f)

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
    val collapsedWidth = 156.dp.coerceAtMost(expandedWidth)
    val animatedWidth = androidx.compose.ui.unit.lerp(expandedWidth, collapsedWidth, pillProgress)

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

    // 축소 시 0..0.60 구간에서 1 -> 0, 복원 시 0.35..1.0(역방향 progress 0.65..0)에서 열림
    val restoreP = (1f - pillProgress).coerceIn(0f, 1f)
    val flankLayoutFactor =
      (if (isRestoringPill) {
        ((restoreP - 0.18f) / 0.52f).coerceIn(0f, 1f)
      } else {
        (1f - pillProgress / 0.58f).coerceIn(0f, 1f)
      }) * (1f - omniboxFocus)

    val expandedDomainAlpha = (1f - ((pillProgress - 0.25f) / 0.45f)).coerceIn(0f, 1f)
    val collapsedDomainAlpha = ((pillProgress - 0.40f) / 0.45f).coerceIn(0f, 1f)
    val isInternalPage =
      focusedTab == null || focusedTab.url == "about:mb" || focusedTab.url == "about:blank"

    val pillTestTag =
      if (pillProgress > 0.5f) {
        "collapsed_floating_pill"
      } else if (isSplitCompact) {
        "safari_floating_bar_pane_$paneIndex"
      } else {
        "safari_floating_bar"
      }

    val collapsedInteraction = remember { MutableInteractionSource() }
    val collapsedPressScale = rememberPressScale(collapsedInteraction, 0.94f)
    val barDissolve = (heroDragLiftProgress / 0.45f).coerceIn(0f, 1f)

    val loadingProgressFraction = (focusedTab?.progress ?: 0) / 100f
    val isWebLoading =
      focusedTab?.isLoading == true &&
        focusedTab.url != "about:mb" &&
        focusedTab.url != "about:blank"

    FrostedFloatingSurface(
      shape = outerShape,
      opacity = surfaceAlpha,
      blurRadiusDp = blurRadius,
      isFocused = isOmniboxFocused || (isSplitCompact && isPaneFocused),
      modifier =
        Modifier.size(width = animatedWidth, height = animatedHeight)
          .graphicsLayer {
            translationY = -barDissolve * 52.dp.toPx()
            alpha = 1f - barDissolve
            val dragScale = 1f - barDissolve * 0.08f
            if (pillProgress > 0.5f) {
              val s = collapsedPressScale.value * dragScale
              scaleX = s
              scaleY = s
            } else {
              scaleX = dragScale
              scaleY = dragScale
            }
          }
          .floatingBarPerimeterProgress(
            isActive = isWebLoading,
            progressFraction = loadingProgressFraction,
            cornerRadiusDp = animatedCorner,
            accentColor = MaterialTheme.colorScheme.primary,
            strokeWidthDp = 2.5.dp,
          )
          .then(
            if (pillProgress > 0.5f) {
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

        // 단일 Row 내부에서 좌/우 슬롯이 중앙 캡슐 아래(zIndex 1f)로 수축·복원
        Row(
          modifier =
            Modifier.fillMaxSize()
              .padding(horizontal = if (isSplitCompact || pillProgress > 0.5f) 4.dp else BrowserSpacing.Grid1),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          // 1. [좌측 커스텀 아이콘 슬롯] - 시차적 안쪽 수축 / 오버슈트 확장
          if (flankLayoutFactor > 0.005f && visibleLeftSlots.isNotEmpty()) {
            Row(
              modifier =
                Modifier.zIndex(1f)
                  .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0))
                    val occupiedW = (placeable.width * flankLayoutFactor).toInt().coerceAtLeast(0)
                    layout(occupiedW, placeable.height) {
                      placeable.placeRelative(0, 0)
                    }
                  },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              visibleLeftSlots.forEachIndexed { idx, action ->
                val motion =
                  computeFlankSlotMotion(
                    pillProgress = pillProgress,
                    isRestoringPill = isRestoringPill,
                    omniboxFocus = omniboxFocus,
                    slotIndex = idx,
                    isLeft = true,
                  )
                Box(
                  modifier =
                    Modifier.graphicsLayer {
                      translationX = motion.translationXFactor * 26.dp.toPx()
                      scaleX = motion.scale
                      scaleY = motion.scale
                      alpha = motion.alpha
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

          // 2. [중앙 통합 주소창 <-> 컴팩트 마이크로 알약] (zIndex 2f로 양측 슬롯 위 레이어)
          val capsuleAlpha =
            if (isOmniboxFocused) {
              0.78f
            } else {
              val expandedCapAlpha = if (floatingBarStyle == FloatingBarStyle.TRANSLUCENT_GLASS) 0.58f else 0.86f
              expandedCapAlpha * (1f - pillProgress)
            }
          val omniboxCornerDp =
            androidx.compose.ui.unit.lerp(
              (designConfig.barCornerRadiusDp - 4f).coerceIn(0f, 36f).dp,
              15.dp,
              pillProgress,
            )
          val omniboxShape =
            androidx.compose.foundation.shape.RoundedCornerShape(omniboxCornerDp)
          Surface(
            shape = omniboxShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = capsuleAlpha.coerceIn(0f, 1f)),
            border =
              if (armGlowProgress > 0.01f) {
                BorderStroke(
                  width = (1.5f + 1.0f * armGlowProgress).dp,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f * armGlowProgress),
                )
              } else {
                null
              },
            modifier =
              Modifier.weight(1f)
                .zIndex(2f)
                .height(omniboxHeight)
                .graphicsLayer {
                  val glowScale = 1f + 0.025f * armGlowProgress
                  scaleX = glowScale
                  scaleY = glowScale
                }
                .then(
                  if (armGlowProgress > 0.01f) {
                    Modifier.shadow(
                      elevation = (14f * armGlowProgress).dp,
                      shape = omniboxShape,
                      ambientColor = MaterialTheme.colorScheme.primary,
                      spotColor = MaterialTheme.colorScheme.primary,
                      clip = false,
                    )
                  } else {
                    Modifier
                  }
                )
                .testTag(if (isSplitCompact) "omnibox_trigger_$paneIndex" else "omnibox_trigger")
                .clip(omniboxShape)
                .floatingBarPerimeterProgress(
                  isActive = holdProgress > 0.02f,
                  progressFraction = holdProgress,
                  cornerRadiusDp = omniboxCornerDp,
                  accentColor = MaterialTheme.colorScheme.primary,
                  strokeWidthDp = 2.dp,
                )
                .then(omniboxGestureModifier),
          ) {
              if (isOmniboxFocused) {
                if (expandBarOnUrlEdit) {
                  @Suppress("DEPRECATION")
                  val clipboardManager = LocalClipboardManager.current
                  val urlScrollState = rememberScrollState()
                  Column(
                    modifier =
                      Modifier.fillMaxSize()
                        .padding(horizontal = BrowserSpacing.Grid1_5, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      modifier = Modifier.weight(1f).fillMaxWidth(),
                      verticalAlignment = Alignment.Top,
                      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "검색",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp).size(18.dp),
                      )
                      Box(
                        modifier =
                          Modifier.weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(urlScrollState),
                        contentAlignment = Alignment.TopStart,
                      ) {
                        BasicTextField(
                          value = omniboxQuery,
                          onValueChange = { next ->
                            if (next.contains('\n')) {
                              onOmniboxSubmit(next.replace("\n", "").trim())
                            } else {
                              onOmniboxQueryChange(next)
                            }
                          },
                          singleLine = false,
                          maxLines = 3,
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
                            Box(contentAlignment = Alignment.TopStart) {
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
                            Modifier.fillMaxWidth()
                              .focusRequester(focusRequester)
                              .testTag("omnibox_input"),
                        )
                      }
                      SpringIconButton(
                        onClick = { onOmniboxSubmit(omniboxQuery) },
                        shape = innerShape,
                        modifier = Modifier.size(32.dp).testTag("omnibox_submit_button"),
                      ) {
                        Icon(
                          Icons.AutoMirrored.Outlined.ArrowForward,
                          contentDescription = "이동",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      if (omniboxQuery.isNotEmpty()) {
                        OmniboxUrlActionChip(
                          icon = Icons.Outlined.ContentCopy,
                          label = "복사",
                          shape = innerShape,
                          onClick = {
                            clipboardManager.setText(AnnotatedString(omniboxQuery))
                          },
                        )
                      }
                      OmniboxUrlActionChip(
                        icon = Icons.Outlined.ContentPaste,
                        label = "붙여넣기",
                        shape = innerShape,
                        onClick = {
                          clipboardManager.getText()?.text?.takeIf { it.isNotEmpty() }?.let { pasted ->
                            onOmniboxQueryChange(omniboxQuery + pasted)
                          }
                        },
                      )
                      if (omniboxQuery.isNotEmpty()) {
                        OmniboxUrlActionChip(
                          icon = Icons.Outlined.Clear,
                          label = "지우기",
                          shape = innerShape,
                          onClick = { onOmniboxQueryChange("") },
                        )
                      }
                      OmniboxUrlActionChip(
                        icon = Icons.Outlined.Close,
                        label = "닫기",
                        shape = innerShape,
                        onClick = onBlurOmnibox,
                        modifier = Modifier.testTag("omnibox_close_button"),
                      )
                    }
                  }
                } else {
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
                        shape = innerShape,
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
                      shape = innerShape,
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
                      shape = innerShape,
                      modifier = Modifier.size(30.dp).testTag("omnibox_close_button"),
                    ) {
                      Icon(
                        Icons.Outlined.Close,
                        contentDescription = "취소",
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                }
              } else {
                Box(modifier = Modifier.fillMaxSize()) {
                  // 확장 상태 중앙 주소창 콘텐츠
                  if (expandedDomainAlpha > 0.01f) {
                    Row(
                      modifier =
                        Modifier.fillMaxSize()
                          .graphicsLayer { alpha = expandedDomainAlpha }
                          .padding(horizontal = if (isSplitCompact) 8.dp else BrowserSpacing.Grid1_5),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      if (splitMode) {
                        Surface(
                          shape = innerShape,
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
                          shape = innerShape,
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
                          shape = innerShape,
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

                  // 축소 상태 컴팩트 마이크로 알약 콘텐츠 (도메인 + 보안 아이콘)
                  if (collapsedDomainAlpha > 0.01f) {
                    Row(
                      modifier =
                        Modifier.fillMaxSize()
                          .graphicsLayer {
                            alpha = collapsedDomainAlpha
                            val s = 0.88f + 0.12f * collapsedDomainAlpha
                            scaleX = s
                            scaleY = s
                          }
                          .padding(horizontal = BrowserSpacing.Grid1_5),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center,
                    ) {
                      Icon(
                        imageVector =
                          if (focusedTab?.isSecure == true) Icons.Outlined.Lock else Icons.Outlined.Shield,
                        contentDescription = "보안 상태",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp),
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

            // 3. [우측 커스텀 아이콘 슬롯] + [분할 시 창 합치기] + 4. [최우측 설정 아이콘 고정]
            if (flankLayoutFactor > 0.005f) {
              Row(
                modifier =
                  Modifier.zIndex(1f)
                    .layout { measurable, constraints ->
                      val placeable = measurable.measure(constraints.copy(minWidth = 0))
                      val occupiedW = (placeable.width * flankLayoutFactor).toInt().coerceAtLeast(0)
                      layout(occupiedW, placeable.height) {
                        placeable.placeRelative(0, 0)
                      }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                visibleRightSlots.forEachIndexed { idx, action ->
                  val motion =
                    computeFlankSlotMotion(
                      pillProgress = pillProgress,
                      isRestoringPill = isRestoringPill,
                      omniboxFocus = omniboxFocus,
                      slotIndex = idx,
                      isLeft = false,
                    )
                  Box(
                    modifier =
                      Modifier.graphicsLayer {
                        translationX = motion.translationXFactor * 26.dp.toPx()
                        scaleX = motion.scale
                        scaleY = motion.scale
                        alpha = motion.alpha
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
                  val mergeMotion =
                    computeFlankSlotMotion(
                      pillProgress = pillProgress,
                      isRestoringPill = isRestoringPill,
                      omniboxFocus = omniboxFocus,
                      slotIndex = visibleRightSlots.size,
                      isLeft = false,
                    )
                  Box(
                    modifier =
                      Modifier.graphicsLayer {
                        translationX = mergeMotion.translationXFactor * 26.dp.toPx()
                        scaleX = mergeMotion.scale
                        scaleY = mergeMotion.scale
                        alpha = mergeMotion.alpha
                      }
                  ) {
                    SpringIconButton(
                      onClick = onCloseSplitPane,
                      shape = innerShape,
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
                }

                val settingsMotion =
                  computeFlankSlotMotion(
                    pillProgress = pillProgress,
                    isRestoringPill = isRestoringPill,
                    omniboxFocus = omniboxFocus,
                    slotIndex = visibleRightSlots.size + if (isSplitCompact) 1 else 0,
                    isLeft = false,
                  )
                Box(
                  modifier =
                    Modifier.graphicsLayer {
                      translationX = settingsMotion.translationXFactor * 28.dp.toPx()
                      scaleX = settingsMotion.scale
                      scaleY = settingsMotion.scale
                      alpha = settingsMotion.alpha
                    }
                ) {
                  var settingsButtonRect by remember { mutableStateOf(Rect.Zero) }
                  SpringIconButton(
                    onClick = {
                      onOpenSettings(
                        SpatialDiveOrigin(
                          boundsInRoot = settingsButtonRect,
                          cornerRadiusDp = (designConfig.barCornerRadiusDp - 4f).coerceAtLeast(0f),
                        )
                      )
                    },
                    shape = innerShape,
                    modifier =
                      Modifier.size(buttonSize)
                        .onGloballyPositioned { coords -> settingsButtonRect = coords.boundsInRoot() }
                        .testTag("fixed_settings_button"),
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
    }
  }
}

@Composable
private fun OmniboxUrlActionChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  shape: androidx.compose.ui.graphics.Shape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = shape,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = modifier.height(26.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
  }
}

private data class FlankSlotMotion(
  val translationXFactor: Float,
  val scale: Float,
  val alpha: Float,
)

private fun computeFlankSlotMotion(
  pillProgress: Float,
  isRestoringPill: Boolean,
  omniboxFocus: Float,
  slotIndex: Int,
  isLeft: Boolean,
): FlankSlotMotion {
  val p = pillProgress.coerceIn(0f, 1f)
  val focusFade = (1f - omniboxFocus).coerceIn(0f, 1f)
  val inwardSign = if (isLeft) 1f else -1f

  if (!isRestoringPill) {
    // Collapsing (0f -> 1f): icons slide inward toward Omnibox, scale 1f -> 0.4f, fade 1f -> 0f during 0f..0.6f
    val start = (slotIndex * 0.05f).coerceIn(0f, 0.25f)
    val end = (0.48f + slotIndex * 0.04f).coerceIn(0.45f, 0.60f)
    val t = ((p - start) / (end - start).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
    return FlankSlotMotion(
      translationXFactor = inwardSign * t,
      scale = (1f - t * 0.6f) * (1f - omniboxFocus * 0.35f),
      alpha = (1f - t) * focusFade,
    )
  }

  // Restoring (1f -> 0f): pill expands first (0f..0.5f), icons pop out sequentially with subtle overshoot 0.5f -> 1.06f -> 1.0f during 0.35f..1.0f
  val restoreP = (1f - p).coerceIn(0f, 1f)
  val start = (0.35f + slotIndex * 0.06f).coerceIn(0.35f, 0.60f)
  val end = (start + 0.40f).coerceAtMost(1.0f)
  val popT = ((restoreP - start) / (end - start).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
  val overshootScale =
    if (popT < 0.72f) {
      0.50f + (1.06f - 0.50f) * (popT / 0.72f)
    } else {
      1.06f - (1.06f - 1.00f) * ((popT - 0.72f) / 0.28f)
    }
  return FlankSlotMotion(
    translationXFactor = inwardSign * (1f - popT),
    scale = overshootScale * (1f - omniboxFocus * 0.35f),
    alpha = (popT * 1.35f).coerceIn(0f, 1f) * focusFade,
  )
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
      shape = designConfig.barInnerShape(),
      modifier = Modifier.size(buttonSize).testTag(tag),
    ) {
      Surface(
        shape = designConfig.barInnerShape(),
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
    shape = designConfig.barInnerShape(),
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
      val findInnerShape = designConfig.barInnerShape()
      SpringIconButton(onClick = onPrev, enabled = totalMatches > 0, shape = findInnerShape, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "이전 결과")
      }
      SpringIconButton(onClick = onNext, enabled = totalMatches > 0, shape = findInnerShape, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "다음 결과")
      }
      SpringIconButton(onClick = onClose, shape = findInnerShape, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.Close, contentDescription = "닫기")
      }
    }
  }
}
