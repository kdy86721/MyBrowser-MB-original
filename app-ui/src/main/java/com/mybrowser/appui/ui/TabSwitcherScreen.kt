package com.mybrowser.appui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.viewmodel.TabFolderUiModel
import com.mybrowser.appui.viewmodel.TabSortMode
import com.mybrowser.appui.viewmodel.TabUiModel
import com.mybrowser.appui.viewmodel.TabViewMode
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val FolderColorPalette =
  listOf(
    Color(0xFF3B82F6), // 코발트
    Color(0xFF10B981), // 에메랄드
    Color(0xFFF59E0B), // 앰버
    Color(0xFFEC4899), // 로즈
    Color(0xFF8B5CF6), // 바이올렛
    Color(0xFF06B6D4), // 시안
  )

private enum class TabSwitcherFilterMode {
  ALL_TABS,
  FOLDERS_ONLY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherOverlay(
  tabs: List<TabUiModel>,
  folders: List<TabFolderUiModel>,
  tabSortMode: TabSortMode,
  tabViewMode: TabViewMode = TabViewMode.THUMBNAIL_GRID,
  tabThumbnails: Map<String, ImageBitmap> = emptyMap(),
  tabFavicons: Map<String, ImageBitmap> = emptyMap(),
  drawerProgress: Float = 1f,
  heroArrivalTabId: String? = null,
  hasUndoableClosedTab: Boolean,
  tabSwipeToCloseEnabled: Boolean,
  tabDragFolderEnabled: Boolean,
  primaryTabId: String,
  secondaryTabId: String?,
  splitMode: Boolean,
  postureState: FoldablePostureState,
  onSelectTab: (tabId: String, targetPane: Int, origin: SpatialDiveOrigin) -> Unit,
  onCloseTab: (String) -> Unit,
  onUndoCloseTab: () -> Unit,
  onCreateTab: () -> Unit,
  onToggleSplit: () -> Unit,
  onSetTabSortMode: (TabSortMode) -> Unit,
  onTogglePinTab: (String) -> Unit,
  onMergeTabsIntoFolder: (sourceTabId: String, targetTabId: String) -> Unit,
  onAddTabToFolder: (tabId: String, folderId: String) -> Unit,
  onRemoveTabFromFolder: (tabId: String) -> Unit,
  onToggleFolderExpanded: (folderId: String) -> Unit,
  onCollapseAllFolders: () -> Unit,
  onRenameFolder: (folderId: String, newName: String) -> Unit,
  onSetFolderColor: (folderId: String, colorIndex: Int) -> Unit,
  onReportTabCardBounds: (Map<String, Rect>) -> Unit = {},
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val p = drawerProgress.coerceIn(0f, 1f)
  val neighborReveal = ((p - 0.30f) / 0.70f).coerceIn(0f, 1f)
  val neighborScale = 0.88f + 0.12f * neighborReveal
  val heroCardReveal = ((p - 0.80f) / 0.20f).coerceIn(0f, 1f)
  val actionBarReveal = ((p - 0.25f) / 0.50f).coerceIn(0f, 1f)

  val design = LocalDesignConfig.current
  val speedScale = LocalAnimationSpeedScale.current
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current

  val tabBounds = remember { mutableStateMapOf<String, Rect>() }
  val folderBounds = remember { mutableStateMapOf<String, Rect>() }
  var overlayOriginRoot by remember { mutableStateOf(Offset.Zero) }

  var filterMode by remember { mutableStateOf(TabSwitcherFilterMode.ALL_TABS) }
  var searchQuery by remember { mutableStateOf("") }

  var draggedTabId by remember { mutableStateOf<String?>(null) }
  var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
  var hoveredTabTargetId by remember { mutableStateOf<String?>(null) }
  var hoveredFolderTargetId by remember { mutableStateOf<String?>(null) }
  var hoverReadyToMerge by remember { mutableStateOf(false) }

  var editingFolder by remember { mutableStateOf<TabFolderUiModel?>(null) }

  val filteredTabs =
    remember(tabs, searchQuery) {
      val q = searchQuery.trim()
      if (q.isEmpty()) {
        tabs
      } else {
        tabs.filter { tab ->
          tab.title.contains(q, ignoreCase = true) ||
            tab.displayHost.contains(q, ignoreCase = true) ||
            tab.url.contains(q, ignoreCase = true)
        }
      }
    }

  val filteredFolders =
    remember(folders, filteredTabs, searchQuery) {
      val q = searchQuery.trim()
      if (q.isEmpty()) {
        folders
      } else {
        val visibleTabFolderIds = filteredTabs.mapNotNull { it.folderId }.toSet()
        folders.filter { folder ->
          folder.name.contains(q, ignoreCase = true) || folder.id in visibleTabFolderIds
        }
      }
    }

  LaunchedEffect(filteredTabs, filteredFolders, filterMode) {
    val activeTabIds = filteredTabs.map { it.id }.toSet()
    val removedTabKeys = tabBounds.keys.filter { it !in activeTabIds }
    if (removedTabKeys.isNotEmpty()) {
      removedTabKeys.forEach { tabBounds.remove(it) }
      onReportTabCardBounds(tabBounds.toMap())
    }
    val activeFolderIds = filteredFolders.map { it.id }.toSet()
    folderBounds.keys.filter { it !in activeFolderIds }.forEach { folderBounds.remove(it) }
  }

  // 500ms 호버 시 폴더 병합 활성화
  LaunchedEffect(draggedTabId, hoveredTabTargetId, hoveredFolderTargetId) {
    hoverReadyToMerge = false
    val activeDrag = draggedTabId
    val target = hoveredTabTargetId ?: hoveredFolderTargetId
    if (activeDrag != null && target != null) {
      delay(500L)
      hoverReadyToMerge = true
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  fun updateHoverTarget(pointerPos: Offset, sourceTab: TabUiModel) {
    val hitTab =
      tabBounds.entries.firstOrNull { (id, rect) ->
        id != sourceTab.id && rect.contains(pointerPos)
      }?.key
    if (hitTab != null) {
      hoveredTabTargetId = hitTab
      hoveredFolderTargetId = null
      return
    }
    val hitFolder =
      folderBounds.entries.firstOrNull { (folderId, rect) ->
        folderId != sourceTab.folderId && rect.contains(pointerPos)
      }?.key
    hoveredTabTargetId = null
    hoveredFolderTargetId = hitFolder
  }

  val gridCols =
    when (tabViewMode) {
      TabViewMode.COMPACT_LIST -> 1
      TabViewMode.THUMBNAIL_GRID ->
        when {
          postureState.windowWidthDp >= 840.dp -> 4
          postureState.supportsDualPane -> 3
          else -> 2
        }
    }
  val validFolderIds = remember(folders) { folders.map { it.id }.toSet() }

  val pinnedTopTabs =
    remember(filteredTabs, tabSortMode) {
      if (tabSortMode == TabSortMode.PINNED_FIRST) {
        filteredTabs.filter { it.isPinned }.sortedByDescending { it.lastActiveAtMs }
      } else {
        emptyList()
      }
    }
  val pinnedTopIds = remember(pinnedTopTabs) { pinnedTopTabs.map { it.id }.toSet() }

  val ungroupedTabs =
    remember(filteredTabs, validFolderIds, tabSortMode, pinnedTopIds) {
      val base =
        filteredTabs.filter { tab ->
          (tab.folderId == null || tab.folderId !in validFolderIds) && tab.id !in pinnedTopIds
        }
      when (tabSortMode) {
        TabSortMode.RECENT -> base.sortedByDescending { it.lastActiveAtMs }
        TabSortMode.CREATED -> base.sortedBy { it.createdAtMs }
        TabSortMode.DOMAIN -> base.sortedWith(compareBy({ tabDomainLabel(it) }, { -it.lastActiveAtMs }))
        TabSortMode.FOLDERS_FIRST -> base.sortedByDescending { it.lastActiveAtMs }
        TabSortMode.PINNED_FIRST -> base.sortedByDescending { it.lastActiveAtMs }
      }
    }

  val domainGroups =
    remember(ungroupedTabs, tabSortMode) {
      if (tabSortMode == TabSortMode.DOMAIN) {
        ungroupedTabs.groupBy { tabDomainLabel(it) }.entries.toList()
      } else {
        emptyList()
      }
    }

  val pinnedRows = remember(pinnedTopTabs, gridCols) { pinnedTopTabs.chunked(gridCols) }
  val ungroupedRows = remember(ungroupedTabs, gridCols) { ungroupedTabs.chunked(gridCols) }

  // 두 손가락 모으기(Pinch-In) 또는 두 손가락 탭으로 모든 폴더 접기
  val multiTouchCollapseModifier =
    Modifier.pointerInput(folders) {
      awaitEachGesture {
        val touchSlop = viewConfiguration.touchSlop
        val first = awaitFirstDown(requireUnconsumed = false)
        val firstDownPos = first.position
        var secondDownPos: Offset? = null
        var initialDist: Float? = null
        var twoFingerStartMs = 0L
        var movedExceedsTap = false
        var pinched = false

        while (true) {
          val ev = awaitPointerEvent()
          val pressed = ev.changes.filter { it.pressed }
          if (pressed.size >= 2) {
            val p1 = pressed[0].position
            val p2 = pressed[1].position
            val dist = hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()).toFloat()
            if (initialDist == null) {
              initialDist = dist.coerceAtLeast(1f)
              secondDownPos = p2
              twoFingerStartMs = System.currentTimeMillis()
            } else {
              val base = initialDist!!
              if (base > 40f && dist / base < 0.78f && !pinched) {
                pinched = true
                onCollapseAllFolders()
              }
              secondDownPos?.let { s0 ->
                if (hypot((p1.x - firstDownPos.x).toDouble(), (p1.y - firstDownPos.y).toDouble()) >
                    touchSlop * 2 ||
                  hypot((p2.x - s0.x).toDouble(), (p2.y - s0.y).toDouble()) > touchSlop * 2
                ) {
                  movedExceedsTap = true
                }
              }
            }
          } else if (pressed.isEmpty()) {
            if (initialDist != null && !pinched && !movedExceedsTap) {
              if (System.currentTimeMillis() - twoFingerStartMs < 380L) {
                onCollapseAllFolders()
              }
            }
            break
          }
        }
      }
    }

  Surface(
    color = MaterialTheme.colorScheme.background.copy(alpha = (p * 0.96f).coerceIn(0f, 0.96f)),
    modifier = Modifier.fillMaxSize().then(multiTouchCollapseModifier),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().onGloballyPositioned { coords ->
          overlayOriginRoot = coords.boundsInRoot().topLeft
        }
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .statusBarsPadding()
            .padding(
              start = BrowserSpacing.Grid2,
              end = BrowserSpacing.Grid2,
              top = BrowserSpacing.Grid2,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
        ) {
          // 헤더
          Row(
            modifier =
              Modifier.fillMaxWidth().graphicsLayer {
                alpha = neighborReveal
                translationY = -(1f - neighborReveal) * 16.dp.toPx()
              },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(text = "탭 (${tabs.size})", style = MaterialTheme.typography.titleLarge)
              Text(
                text =
                  if (splitMode) "양면 분할 · 폴더 ${folders.size}개"
                  else "${postureState.mode.badgeLabel} · 폴더 ${folders.size}개",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              if (hasUndoableClosedTab) {
                OutlinedButton(
                  onClick = onUndoCloseTab,
                  shape = design.cardInnerShape(),
                  modifier = Modifier.testTag("undo_close_tab_button"),
                ) {
                  Icon(
                    Icons.Outlined.Restore,
                    contentDescription = "방금 닫은 탭 복원",
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(Modifier.size(4.dp))
                  Text("복원", style = MaterialTheme.typography.labelMedium)
                }
              }
              if (folders.any { it.isExpanded }) {
                SpringIconButton(
                  onClick = onCollapseAllFolders,
                  modifier = Modifier.testTag("collapse_all_folders_button"),
                ) {
                  Icon(Icons.Outlined.UnfoldLess, contentDescription = "모든 폴더 접기")
                }
              }
              if (postureState.supportsDualPane) {
                OutlinedButton(
                  onClick = onToggleSplit,
                  shape = design.cardInnerShape(),
                  modifier = Modifier.testTag("tab_switcher_split_button"),
                ) {
                  Icon(
                    Icons.Outlined.Splitscreen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(Modifier.size(6.dp))
                  Text(if (splitMode) "단일" else "분할")
                }
              }
              SpringIconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "닫기")
              }
            }
          }

          // 상단 컨트롤 헤더: 전체 탭 / 탭 폴더 전환 토글 + 컴팩트 탭 검색창
          val isWideHeader = postureState.windowWidthDp >= 540.dp
          if (isWideHeader) {
            Row(
              modifier =
                Modifier.fillMaxWidth().graphicsLayer {
                  alpha = neighborReveal
                },
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TabViewFilterToggle(
                filterMode = filterMode,
                totalTabCount = tabs.size,
                folderCount = folders.size,
                onSelectFilter = { filterMode = it },
              )
              CompactTabSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
              )
            }
          } else {
            Column(
              modifier =
                Modifier.fillMaxWidth().graphicsLayer {
                  alpha = neighborReveal
                },
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TabViewFilterToggle(
                  filterMode = filterMode,
                  totalTabCount = tabs.size,
                  folderCount = folders.size,
                  onSelectFilter = { filterMode = it },
                  modifier = Modifier.fillMaxWidth(),
                )
              }
              CompactTabSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          // 5종 정렬 및 그룹핑 모드 선택 바
          if (filterMode == TabSwitcherFilterMode.ALL_TABS) {
            LazyRow(
              modifier =
                Modifier.fillMaxWidth().graphicsLayer {
                  alpha = neighborReveal
                },
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              items(TabSortMode.entries, key = { it.id }) { mode ->
                val modeIcon =
                  when (mode) {
                    TabSortMode.RECENT -> Icons.Outlined.Schedule
                    TabSortMode.CREATED -> Icons.Outlined.AddCircleOutline
                    TabSortMode.DOMAIN -> Icons.Outlined.Public
                    TabSortMode.FOLDERS_FIRST -> Icons.Outlined.Folder
                    TabSortMode.PINNED_FIRST -> Icons.Outlined.PushPin
                  }
                FilterChip(
                  selected = tabSortMode == mode,
                  onClick = { onSetTabSortMode(mode) },
                  shape = design.cardInnerShape(),
                  leadingIcon = {
                    Icon(modeIcon, contentDescription = null, modifier = Modifier.size(15.dp))
                  },
                  label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.testTag("tab_sort_${mode.name.lowercase()}"),
                )
              }
            }
          }

          // 탭 & 폴더 목록 (2열~4열 썸네일 그리드 또는 1열 컴팩트 리스트)
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
            contentPadding = PaddingValues(bottom = BrowserSpacing.Grid1_5),
          ) {
            // 0. 고정 탭 우선 섹션 (PINNED_FIRST 모드)
            if (filterMode == TabSwitcherFilterMode.ALL_TABS &&
              tabSortMode == TabSortMode.PINNED_FIRST &&
              pinnedRows.isNotEmpty()
            ) {
              item(key = "pinned_section_header") {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                  Icon(
                    Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                  )
                  Text(
                    text = "고정된 탭 (${pinnedTopTabs.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              items(
                count = pinnedRows.size,
                key = { idx -> "pinned_row_${pinnedRows[idx].firstOrNull()?.id ?: idx}" },
              ) { rowIdx ->
                val rowTabs = pinnedRows[rowIdx]
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .animateItem(
                        fadeInSpec = ExpressiveSprings.spatial(speedScale),
                        placementSpec = ExpressiveSprings.spatial(speedScale),
                        fadeOutSpec = ExpressiveSprings.micro(speedScale),
                      ),
                  horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                ) {
                  rowTabs.forEach { tab ->
                    key(tab.id) {
                      val isHeroCard = tab.id == heroArrivalTabId
                      val itemAlpha = if (isHeroCard) heroCardReveal else neighborReveal
                      val itemScale = if (isHeroCard) 1f else neighborScale
                      Box(
                        modifier =
                          Modifier.weight(1f).graphicsLayer {
                            alpha = itemAlpha
                            scaleX = itemScale
                            scaleY = itemScale
                          }
                      ) {
                        TabSwitcherCard(
                          tab = tab,
                          viewMode = tabViewMode,
                          thumbnail = tabThumbnails[tab.id],
                          favicon = tabFavicons[tab.id],
                          isPrimary = tab.id == primaryTabId,
                          isSecondary = splitMode && tab.id == secondaryTabId,
                          splitMode = splitMode,
                          isHeroArrival = isHeroCard && p >= 0.92f,
                          isBeingDragged = draggedTabId == tab.id,
                          isHoverMergeTarget = hoveredTabTargetId == tab.id && hoverReadyToMerge,
                          swipeToCloseEnabled =
                            tabSwipeToCloseEnabled && draggedTabId == null && tabs.size > 1,
                          dragFolderEnabled = tabDragFolderEnabled,
                          accentColor = MaterialTheme.colorScheme.primary,
                          onPositioned = { rect ->
                            tabBounds[tab.id] = rect
                            onReportTabCardBounds(tabBounds.toMap())
                          },
                          onStartDrag = { startCenter ->
                            draggedTabId = tab.id
                            dragPointerRoot = startCenter
                          },
                          onDragDelta = { delta ->
                            dragPointerRoot += delta
                            updateHoverTarget(dragPointerRoot, tab)
                          },
                          onEndDrag = {
                            val activeId = draggedTabId
                            val targetTabId = hoveredTabTargetId
                            val targetFolderId = hoveredFolderTargetId
                            val shouldMerge = hoverReadyToMerge
                            draggedTabId = null
                            hoveredTabTargetId = null
                            hoveredFolderTargetId = null
                            hoverReadyToMerge = false

                            if (activeId != null && shouldMerge) {
                              if (targetTabId != null) {
                                onMergeTabsIntoFolder(activeId, targetTabId)
                              } else if (targetFolderId != null) {
                                onAddTabToFolder(activeId, targetFolderId)
                              }
                            }
                          },
                          onSelectTab = { pane, origin -> onSelectTab(tab.id, pane, origin) },
                          onTogglePin = { onTogglePinTab(tab.id) },
                          onCloseTab = { onCloseTab(tab.id) },
                        )
                      }
                    }
                  }
                  repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                }
              }
            }

            // 1. 탭 폴더 카드 섹션
            items(filteredFolders, key = { it.id }) { folder ->
              val folderTabs =
                filteredTabs.filter { it.folderId == folder.id && it.id !in pinnedTopIds }.let { list ->
                  if (tabSortMode == TabSortMode.CREATED) list.sortedBy { it.createdAtMs }
                  else list.sortedByDescending { it.lastActiveAtMs }
                }
              val showFolder =
                folderTabs.isNotEmpty() ||
                  filterMode == TabSwitcherFilterMode.FOLDERS_ONLY ||
                  tabSortMode == TabSortMode.FOLDERS_FIRST
              if (showFolder) {
                val folderColor =
                  FolderColorPalette[folder.colorIndex.mod(FolderColorPalette.size)]
                val isMergeHovered =
                  hoveredFolderTargetId == folder.id && hoverReadyToMerge

                Surface(
                  shape = design.cardOuterShape(),
                  color = MaterialTheme.colorScheme.surface,
                  border =
                    BorderStroke(
                      width = if (isMergeHovered) 2.dp else 1.5.dp,
                      color =
                        if (isMergeHovered) MaterialTheme.colorScheme.primary
                        else folderColor.copy(alpha = 0.55f),
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .animateItem(
                        fadeInSpec = ExpressiveSprings.spatial(speedScale),
                        placementSpec = ExpressiveSprings.spatial(speedScale),
                        fadeOutSpec = ExpressiveSprings.micro(speedScale),
                      )
                      .graphicsLayer {
                        alpha = neighborReveal
                        scaleX = neighborScale
                        scaleY = neighborScale
                      }
                      .onGloballyPositioned { coords ->
                        folderBounds[folder.id] = coords.boundsInRoot()
                      }
                      .testTag("tab_folder_${folder.id}"),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid1_5),
                    verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                  ) {
                    Row(
                      modifier =
                        Modifier.fillMaxWidth()
                          .clip(design.cardInnerShape())
                          .clickable { onToggleFolderExpanded(folder.id) }
                          .padding(horizontal = 6.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                    ) {
                      Box(
                        modifier =
                          Modifier.size(28.dp)
                            .clip(design.iconShape())
                            .background(folderColor.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center,
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.Folder,
                          contentDescription = null,
                          tint = folderColor,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                      Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                      )
                      Surface(
                        shape = design.cardInnerShape(),
                        color = folderColor.copy(alpha = 0.16f),
                      ) {
                        Text(
                          text = "${folderTabs.size}개",
                          style = MaterialTheme.typography.labelSmall,
                          color = folderColor,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                      }
                      SpringIconButton(
                        onClick = { editingFolder = folder },
                        modifier = Modifier.size(34.dp),
                      ) {
                        Icon(
                          Icons.Outlined.Edit,
                          contentDescription = "폴더 이름 · 색상 변경",
                          modifier = Modifier.size(16.dp),
                        )
                      }
                      Icon(
                        imageVector =
                          if (folder.isExpanded) Icons.Outlined.ExpandLess
                          else Icons.Outlined.ExpandMore,
                        contentDescription = if (folder.isExpanded) "폴더 접기" else "폴더 펼치기",
                      )
                    }

                    AnimatedVisibility(
                      visible = folder.isExpanded,
                      enter =
                        fadeIn(ExpressiveSprings.micro(speedScale)) +
                          expandVertically(ExpressiveSprings.spatial(speedScale)),
                      exit =
                        fadeOut(ExpressiveSprings.micro(speedScale)) +
                          shrinkVertically(ExpressiveSprings.spatial(speedScale)),
                    ) {
                      val rows = folderTabs.chunked(gridCols)
                      Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
                        rows.forEach { rowTabs ->
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                          ) {
                            rowTabs.forEach { tab ->
                              key(tab.id) {
                                Box(modifier = Modifier.weight(1f)) {
                                  TabSwitcherCard(
                                    tab = tab,
                                    viewMode = tabViewMode,
                                    thumbnail = tabThumbnails[tab.id],
                                    favicon = tabFavicons[tab.id],
                                    isPrimary = tab.id == primaryTabId,
                                    isSecondary = splitMode && tab.id == secondaryTabId,
                                    splitMode = splitMode,
                                    isHeroArrival = tab.id == heroArrivalTabId,
                                    isBeingDragged = draggedTabId == tab.id,
                                    isHoverMergeTarget =
                                      hoveredTabTargetId == tab.id && hoverReadyToMerge,
                                    swipeToCloseEnabled =
                                      tabSwipeToCloseEnabled && draggedTabId == null && tabs.size > 1,
                                    dragFolderEnabled = tabDragFolderEnabled,
                                    accentColor = folderColor,
                                    onPositioned = { rect ->
                                      tabBounds[tab.id] = rect
                                      onReportTabCardBounds(tabBounds.toMap())
                                    },
                                    onStartDrag = { startCenter ->
                                      draggedTabId = tab.id
                                      dragPointerRoot = startCenter
                                    },
                                    onDragDelta = { delta ->
                                      dragPointerRoot += delta
                                      updateHoverTarget(dragPointerRoot, tab)
                                    },
                                    onEndDrag = {
                                      val activeId = draggedTabId
                                      val targetTabId = hoveredTabTargetId
                                      val targetFolderId = hoveredFolderTargetId
                                      val shouldMerge = hoverReadyToMerge
                                      val pointer = dragPointerRoot
                                      draggedTabId = null
                                      hoveredTabTargetId = null
                                      hoveredFolderTargetId = null
                                      hoverReadyToMerge = false

                                      if (activeId != null) {
                                        when {
                                          shouldMerge && targetTabId != null ->
                                            onMergeTabsIntoFolder(activeId, targetTabId)
                                          shouldMerge && targetFolderId != null ->
                                            onAddTabToFolder(activeId, targetFolderId)
                                          tab.folderId != null -> {
                                            val parentRect = folderBounds[tab.folderId]
                                            if (parentRect != null && !parentRect.contains(pointer)) {
                                              onRemoveTabFromFolder(activeId)
                                            }
                                          }
                                        }
                                      }
                                    },
                                    onSelectTab = { pane, origin -> onSelectTab(tab.id, pane, origin) },
                                    onTogglePin = { onTogglePinTab(tab.id) },
                                    onCloseTab = { onCloseTab(tab.id) },
                                  )
                                }
                              }
                            }
                            repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            if (filterMode == TabSwitcherFilterMode.FOLDERS_ONLY) {
              if (filteredFolders.isEmpty()) {
                item(key = "empty_folders_card") {
                  Surface(
                    shape = design.cardOuterShape(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = BrowserSpacing.Grid2),
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid3),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                    ) {
                      Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                      )
                      Text(
                        text = if (searchQuery.isBlank()) "탭 폴더 없음" else "검색 결과 없음",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            } else {
              // 2. 도메인별 헤더 그룹 또는 일반 탭 그리드 섹션
              if (tabSortMode == TabSortMode.DOMAIN) {
                domainGroups.forEach { (domainName, groupTabs) ->
                  item(key = "domain_header_$domainName") {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                      Icon(
                        Icons.Outlined.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp),
                      )
                      Text(
                        text = "$domainName (${groupTabs.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  val groupRows = groupTabs.chunked(gridCols)
                  items(
                    count = groupRows.size,
                    key = { idx ->
                      "domain_${domainName}_row_${groupRows[idx].firstOrNull()?.id ?: idx}"
                    },
                  ) { rowIdx ->
                    val rowTabs = groupRows[rowIdx]
                    Row(
                      modifier =
                        Modifier.fillMaxWidth()
                          .animateItem(
                            fadeInSpec = ExpressiveSprings.spatial(speedScale),
                            placementSpec = ExpressiveSprings.spatial(speedScale),
                            fadeOutSpec = ExpressiveSprings.micro(speedScale),
                          ),
                      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                    ) {
                      rowTabs.forEach { tab ->
                        key(tab.id) {
                          val isHeroCard = tab.id == heroArrivalTabId
                          val itemAlpha = if (isHeroCard) heroCardReveal else neighborReveal
                          val itemScale = if (isHeroCard) 1f else neighborScale
                          Box(
                            modifier =
                              Modifier.weight(1f).graphicsLayer {
                                alpha = itemAlpha
                                scaleX = itemScale
                                scaleY = itemScale
                              }
                          ) {
                            TabSwitcherCard(
                              tab = tab,
                              viewMode = tabViewMode,
                              thumbnail = tabThumbnails[tab.id],
                              favicon = tabFavicons[tab.id],
                              isPrimary = tab.id == primaryTabId,
                              isSecondary = splitMode && tab.id == secondaryTabId,
                              splitMode = splitMode,
                              isHeroArrival = isHeroCard && p >= 0.92f,
                              isBeingDragged = draggedTabId == tab.id,
                              isHoverMergeTarget =
                                hoveredTabTargetId == tab.id && hoverReadyToMerge,
                              swipeToCloseEnabled =
                                tabSwipeToCloseEnabled && draggedTabId == null && tabs.size > 1,
                              dragFolderEnabled = tabDragFolderEnabled,
                              accentColor = null,
                              onPositioned = { rect ->
                                tabBounds[tab.id] = rect
                                onReportTabCardBounds(tabBounds.toMap())
                              },
                              onStartDrag = { startCenter ->
                                draggedTabId = tab.id
                                dragPointerRoot = startCenter
                              },
                              onDragDelta = { delta ->
                                dragPointerRoot += delta
                                updateHoverTarget(dragPointerRoot, tab)
                              },
                              onEndDrag = {
                                val activeId = draggedTabId
                                val targetTabId = hoveredTabTargetId
                                val targetFolderId = hoveredFolderTargetId
                                val shouldMerge = hoverReadyToMerge
                                draggedTabId = null
                                hoveredTabTargetId = null
                                hoveredFolderTargetId = null
                                hoverReadyToMerge = false

                                if (activeId != null && shouldMerge) {
                                  if (targetTabId != null) {
                                    onMergeTabsIntoFolder(activeId, targetTabId)
                                  } else if (targetFolderId != null) {
                                    onAddTabToFolder(activeId, targetFolderId)
                                  }
                                }
                              },
                              onSelectTab = { pane, origin -> onSelectTab(tab.id, pane, origin) },
                              onTogglePin = { onTogglePinTab(tab.id) },
                              onCloseTab = { onCloseTab(tab.id) },
                            )
                          }
                        }
                      }
                      repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                    }
                  }
                }
              } else {
                items(
                  count = ungroupedRows.size,
                  key = { idx -> "row_${ungroupedRows[idx].firstOrNull()?.id ?: idx}" },
                ) { rowIndex ->
                  val rowTabs = ungroupedRows[rowIndex]
                  Row(
                    modifier =
                      Modifier.fillMaxWidth()
                        .animateItem(
                          fadeInSpec = ExpressiveSprings.spatial(speedScale),
                          placementSpec = ExpressiveSprings.spatial(speedScale),
                          fadeOutSpec = ExpressiveSprings.micro(speedScale),
                        ),
                    horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                  ) {
                    rowTabs.forEach { tab ->
                      key(tab.id) {
                        val isHeroCard = tab.id == heroArrivalTabId
                        val itemAlpha = if (isHeroCard) heroCardReveal else neighborReveal
                        val itemScale = if (isHeroCard) 1f else neighborScale
                        Box(
                          modifier =
                            Modifier.weight(1f).graphicsLayer {
                              alpha = itemAlpha
                              scaleX = itemScale
                              scaleY = itemScale
                            }
                        ) {
                          TabSwitcherCard(
                            tab = tab,
                            viewMode = tabViewMode,
                            thumbnail = tabThumbnails[tab.id],
                            favicon = tabFavicons[tab.id],
                            isPrimary = tab.id == primaryTabId,
                            isSecondary = splitMode && tab.id == secondaryTabId,
                            splitMode = splitMode,
                            isHeroArrival = isHeroCard && p >= 0.92f,
                            isBeingDragged = draggedTabId == tab.id,
                            isHoverMergeTarget =
                              hoveredTabTargetId == tab.id && hoverReadyToMerge,
                            swipeToCloseEnabled =
                              tabSwipeToCloseEnabled && draggedTabId == null && tabs.size > 1,
                            dragFolderEnabled = tabDragFolderEnabled,
                            accentColor = null,
                            onPositioned = { rect ->
                              tabBounds[tab.id] = rect
                              onReportTabCardBounds(tabBounds.toMap())
                            },
                            onStartDrag = { startCenter ->
                              draggedTabId = tab.id
                              dragPointerRoot = startCenter
                            },
                            onDragDelta = { delta ->
                              dragPointerRoot += delta
                              updateHoverTarget(dragPointerRoot, tab)
                            },
                            onEndDrag = {
                              val activeId = draggedTabId
                              val targetTabId = hoveredTabTargetId
                              val targetFolderId = hoveredFolderTargetId
                              val shouldMerge = hoverReadyToMerge
                              draggedTabId = null
                              hoveredTabTargetId = null
                              hoveredFolderTargetId = null
                              hoverReadyToMerge = false

                              if (activeId != null && shouldMerge) {
                                if (targetTabId != null) {
                                  onMergeTabsIntoFolder(activeId, targetTabId)
                                } else if (targetFolderId != null) {
                                  onAddTabToFolder(activeId, targetFolderId)
                                }
                              }
                            },
                            onSelectTab = { pane, origin -> onSelectTab(tab.id, pane, origin) },
                            onTogglePin = { onTogglePinTab(tab.id) },
                            onCloseTab = { onCloseTab(tab.id) },
                          )
                        }
                      }
                    }
                    repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                  }
                }
              }

              if (filteredTabs.isEmpty()) {
                item(key = "empty_search_tabs") {
                  Surface(
                    shape = design.cardOuterShape(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = BrowserSpacing.Grid2),
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid3),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                    ) {
                      Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                      )
                      Text(
                        text = "검색 결과 없음",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }

          // 하단 통합 액션 바: 외곽 barOuterShape(), 내부 버튼 모두 barInnerShape() ((barCornerRadiusDp - 4f).dp)
          val barInnerButtonShape = design.barInnerShape()
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(bottom = BrowserSpacing.Grid2)
          ) {
            FrostedFloatingSurface(
              shape = design.barOuterShape(),
              opacity = (design.barOpacity + 0.06f).coerceIn(0.86f, 0.98f),
              blurRadiusDp = design.barBlurRadiusDp,
              isFocused = true,
              modifier =
                Modifier.fillMaxWidth()
                  .graphicsLayer {
                    alpha = actionBarReveal
                    translationY = (1f - actionBarReveal) * 28.dp.toPx()
                    val s = 0.94f + 0.06f * actionBarReveal
                    scaleX = s
                    scaleY = s
                  },
            ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              OutlinedButton(
                onClick = {
                  val allModes = TabSortMode.entries
                  val nextMode = allModes[(allModes.indexOf(tabSortMode) + 1) % allModes.size]
                  onSetTabSortMode(nextMode)
                },
                shape = barInnerButtonShape,
                modifier = Modifier.height(44.dp).testTag("tab_switcher_bottom_sort_button"),
              ) {
                Icon(Icons.Outlined.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(tabSortMode.label, style = MaterialTheme.typography.labelMedium)
              }

              Button(
                onClick = onCreateTab,
                shape = barInnerButtonShape,
                modifier = Modifier.weight(1f).height(44.dp).testTag("new_tab_button"),
              ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("새 탭", style = MaterialTheme.typography.labelLarge)
              }

              OutlinedButton(
                onClick = onDismiss,
                shape = barInnerButtonShape,
                modifier = Modifier.height(44.dp).testTag("tab_switcher_bottom_close_button"),
              ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("닫기", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
          }
        }
      }

      // 드래그 중 플로팅 고스트 카드 표시 (간결한 아이콘 · 제목 · 상태 뱃지만 표시)
      val draggedTab = draggedTabId?.let { id -> tabs.firstOrNull { it.id == id } }
      if (draggedTab != null) {
        val halfWidthPx = with(density) { 104.dp.toPx() }
        val yOffsetPx = with(density) { 40.dp.toPx() }
        Surface(
          shape = design.cardOuterShape(),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
          shadowElevation = 14.dp,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
          modifier =
            Modifier.offset {
                IntOffset(
                  (dragPointerRoot.x - overlayOriginRoot.x - halfWidthPx).roundToInt(),
                  (dragPointerRoot.y - overlayOriginRoot.y - yOffsetPx).roundToInt(),
                )
              }
              .width(208.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TabFaviconBadge(
              url = draggedTab.url,
              favicon = tabFavicons[draggedTab.id],
              size = 18.dp,
            )
            Text(
              text = draggedTab.title,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (hoverReadyToMerge) {
              Surface(
                shape = design.cardInnerShape(),
                color = MaterialTheme.colorScheme.primary,
              ) {
                Text(
                  text = "병합",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
        }
      }
    }
  }

  // 폴더 이름 및 색상 편집 다이얼로그
  editingFolder?.let { folder ->
    var nameDraft by remember(folder.id) { mutableStateOf(folder.name) }
    var colorDraft by remember(folder.id) { mutableIntStateOf(folder.colorIndex) }
    AlertDialog(
      onDismissRequest = { editingFolder = null },
      shape = BrowserRadii.DialogSheetShape,
      title = { Text("폴더 편집", style = MaterialTheme.typography.titleMedium) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5)) {
          OutlinedTextField(
            value = nameDraft,
            onValueChange = { nameDraft = it },
            singleLine = true,
            shape = design.cardInnerShape(),
            label = { Text("폴더 이름") },
            modifier = Modifier.fillMaxWidth(),
          )
          Text("폴더 색상", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
            FolderColorPalette.forEachIndexed { idx, color ->
              Surface(
                onClick = { colorDraft = idx },
                shape = CircleShape,
                color = color,
                border =
                  if (colorDraft == idx) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface)
                  else null,
                modifier = Modifier.size(32.dp),
              ) {}
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (nameDraft.isNotBlank()) onRenameFolder(folder.id, nameDraft.trim())
            onSetFolderColor(folder.id, colorDraft)
            editingFolder = null
          },
          shape = design.cardInnerShape(),
        ) {
          Text("저장")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingFolder = null }) { Text("취소") }
      },
    )
  }
}

@Composable
private fun TabViewFilterToggle(
  filterMode: TabSwitcherFilterMode,
  totalTabCount: Int,
  folderCount: Int,
  onSelectFilter: (TabSwitcherFilterMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val design = LocalDesignConfig.current
  Surface(
    shape = design.cardInnerShape(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val allSelected = filterMode == TabSwitcherFilterMode.ALL_TABS
      Surface(
        onClick = { onSelectFilter(TabSwitcherFilterMode.ALL_TABS) },
        shape = design.cardInnerShape(),
        color = if (allSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.height(36.dp).testTag("tab_filter_all"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = design.cardInnerShape(),
            color =
              if (allSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
              else MaterialTheme.colorScheme.surface,
            border =
              BorderStroke(
                width = 1.dp,
                color =
                  if (allSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              ),
          ) {
            Text(
              text = totalTabCount.toString(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color =
                if (allSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
          }
          Text(
            text = "전체 탭",
            style = MaterialTheme.typography.labelMedium,
            color =
              if (allSelected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      val folderSelected = filterMode == TabSwitcherFilterMode.FOLDERS_ONLY
      Surface(
        onClick = { onSelectFilter(TabSwitcherFilterMode.FOLDERS_ONLY) },
        shape = design.cardInnerShape(),
        color = if (folderSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.height(36.dp).testTag("tab_filter_folders"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = "탭 폴더",
            tint =
              if (folderSelected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "폴더 ($folderCount)",
            style = MaterialTheme.typography.labelMedium,
            color =
              if (folderSelected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun CompactTabSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val design = LocalDesignConfig.current
  Surface(
    shape = design.cardInnerShape(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = modifier.height(44.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Search,
        contentDescription = "탭 검색",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle =
          MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
          ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
          Box(contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
              Text(
                text = "탭 검색",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            innerTextField()
          }
        },
        modifier = Modifier.weight(1f).testTag("tab_search_input"),
      )
      if (query.isNotEmpty()) {
        SpringIconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.size(30.dp).testTag("tab_search_clear"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Clear,
            contentDescription = "검색어 지우기",
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun TabFaviconBadge(
  url: String,
  favicon: ImageBitmap?,
  size: androidx.compose.ui.unit.Dp = 16.dp,
) {
  val design = LocalDesignConfig.current
  if (favicon != null) {
    Image(
      bitmap = favicon,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(size).clip(design.cardInnerShape()),
    )
  } else {
    val icon =
      when {
        url == "about:mb" || url == "about:blank" -> Icons.Outlined.Shield
        url.startsWith("https://") -> Icons.Outlined.Lock
        else -> Icons.Outlined.Public
      }
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(size),
    )
  }
}

private fun tabDomainLabel(tab: TabUiModel): String =
  if (tab.url == "about:mb" || tab.url == "about:blank") {
    "내부 페이지"
  } else {
    tab.displayHost.lowercase().removePrefix("www.").ifBlank { "기타" }
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabSwitcherCard(
  tab: TabUiModel,
  viewMode: TabViewMode,
  thumbnail: ImageBitmap?,
  favicon: ImageBitmap?,
  isPrimary: Boolean,
  isSecondary: Boolean,
  splitMode: Boolean,
  isHeroArrival: Boolean = false,
  isBeingDragged: Boolean,
  isHoverMergeTarget: Boolean,
  swipeToCloseEnabled: Boolean,
  dragFolderEnabled: Boolean,
  accentColor: Color?,
  onPositioned: (Rect) -> Unit,
  onStartDrag: (Offset) -> Unit,
  onDragDelta: (Offset) -> Unit,
  onEndDrag: () -> Unit,
  onSelectTab: (Int, SpatialDiveOrigin) -> Unit,
  onTogglePin: () -> Unit,
  onCloseTab: () -> Unit,
) {
  val design = LocalDesignConfig.current
  val speedScale = LocalAnimationSpeedScale.current
  val isActive = isPrimary || isSecondary
  var selfRect by remember { mutableStateOf(Rect.Zero) }
  val heroSettleScale =
    remember(tab.id, isHeroArrival) {
      Animatable(if (isHeroArrival) 0.90f else 1f)
    }
  LaunchedEffect(tab.id, isHeroArrival) {
    if (isHeroArrival) {
      heroSettleScale.animateTo(1f, BrowserMotion.morphingPillSpring(speedScale))
    }
  }

  val latestSwipeEnabled by rememberUpdatedState(swipeToCloseEnabled)
  val latestOnCloseTab by rememberUpdatedState(onCloseTab)
  val dismissState =
    rememberSwipeToDismissBoxState(
      confirmValueChange = { value ->
        if (latestSwipeEnabled &&
          (value == SwipeToDismissBoxValue.StartToEnd ||
            value == SwipeToDismissBoxValue.EndToStart)
        ) {
          latestOnCloseTab()
          true
        } else {
          false
        }
      }
    )
  LaunchedEffect(dismissState.currentValue) {
    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
      delay(280L)
      dismissState.reset()
    }
  }

  val dragModifier =
    if (dragFolderEnabled) {
      Modifier.pointerInput(tab.id) {
        detectDragGesturesAfterLongPress(
          onDragStart = { localOffset ->
            val rootStart = Offset(selfRect.left + localOffset.x, selfRect.top + localOffset.y)
            onStartDrag(rootStart)
          },
          onDrag = { change, dragAmount ->
            change.consume()
            onDragDelta(dragAmount)
          },
          onDragEnd = onEndDrag,
          onDragCancel = onEndDrag,
        )
      }
    } else {
      Modifier
    }

  val cardContent: @Composable () -> Unit = {
    SpringPressSurface(
      onClick = { onSelectTab(0, SpatialDiveOrigin(selfRect, design.cardCornerRadiusDp)) },
      selected = isActive || isHoverMergeTarget || tab.isPinned,
      shape = design.cardOuterShape(),
      unselectedColor = MaterialTheme.colorScheme.surface,
      selectedBorderColor =
        when {
          isHoverMergeTarget -> MaterialTheme.colorScheme.primary
          isActive -> MaterialTheme.colorScheme.primary
          accentColor != null -> accentColor
          else -> MaterialTheme.colorScheme.primary
        },
      modifier =
        Modifier.fillMaxWidth()
          .graphicsLayer {
            alpha = if (isBeingDragged) 0.35f else 1f
            val baseScale = if (isHoverMergeTarget) 1.03f else heroSettleScale.value
            scaleX = baseScale
            scaleY = baseScale
          }
          .onGloballyPositioned { coords ->
            val r = coords.boundsInRoot()
            selfRect = r
            onPositioned(r)
          }
          .then(dragModifier)
          .testTag("tab_card_${tab.id}"),
    ) {
      if (viewMode == TabViewMode.THUMBNAIL_GRID) {
        Column(
          modifier = Modifier.padding(BrowserSpacing.Grid1),
          verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        ) {
          // 카드 상단 바: 파비콘 + P1/P2 + 제목 + 고정 + X 닫기
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            TabFaviconBadge(url = tab.url, favicon = favicon, size = 16.dp)
            if (isPrimary || isSecondary) {
              Surface(
                shape = design.cardInnerShape(),
                color = MaterialTheme.colorScheme.primary,
              ) {
                Text(
                  text = if (isPrimary) "P1" else "P2",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
              }
            }
            Text(
              text = tab.title,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            SpringIconButton(
              onClick = onTogglePin,
              modifier = Modifier.size(30.dp).testTag("pin_tab_${tab.id}"),
            ) {
              Icon(
                imageVector = if (tab.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (tab.isPinned) "${tab.title} 고정 해제" else "${tab.title} 상단 고정",
                tint =
                  if (tab.isPinned) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
            }
            SpringIconButton(
              onClick = onCloseTab,
              modifier = Modifier.size(30.dp).testTag("close_tab_${tab.id}"),
            ) {
              Icon(
                Icons.Outlined.Close,
                contentDescription = "${tab.title} 닫기",
                modifier = Modifier.size(15.dp),
              )
            }
          }

          // 라이브 웹페이지 축소 썸네일 프리뷰
          Surface(
            shape = design.cardInnerShape(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth().aspectRatio(1.12f),
          ) {
            Box(modifier = Modifier.fillMaxSize()) {
              if (thumbnail != null) {
                Image(
                  bitmap = thumbnail,
                  contentDescription = tab.title,
                  contentScale = ContentScale.Crop,
                  alignment = Alignment.TopCenter,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                // 내부 페이지(about:mb / about:blank) 또는 미캡처 탭용 시각적 미니어처 프리뷰
                Column(
                  modifier = Modifier.fillMaxSize().padding(10.dp),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Surface(
                    shape = design.cardInnerShape(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      TabFaviconBadge(url = tab.url, favicon = favicon, size = 12.dp)
                      Text(
                        text = tab.displayHost,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                  Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                  ) {
                    TabFaviconBadge(url = tab.url, favicon = favicon, size = 28.dp)
                  }
                }
              }

              // 하단 호스트 · 차단 수 오버레이 + 분할 시 좌/우 선택 버튼
              Row(
                modifier =
                  Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text =
                    when {
                      isHoverMergeTarget -> "병합"
                      tab.blockedCount > 0 -> "${tab.displayHost} · ${tab.blockedCount}"
                      else -> tab.displayHost
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (isHoverMergeTarget) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                if (splitMode) {
                  Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                      onClick = { onSelectTab(0, SpatialDiveOrigin(selfRect, design.cardCornerRadiusDp)) },
                      shape = design.cardInnerShape(),
                      color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                      Text(
                        "P1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                      )
                    }
                    Surface(
                      onClick = { onSelectTab(1, SpatialDiveOrigin(selfRect, design.cardCornerRadiusDp)) },
                      shape = design.cardInnerShape(),
                      color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                      Text(
                        "P2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        // COMPACT_LIST (단일 열 컴팩트 카드)
        Column(
          modifier = Modifier.padding(BrowserSpacing.Grid1_5),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              modifier = Modifier.weight(1f),
            ) {
              TabFaviconBadge(url = tab.url, favicon = favicon, size = 18.dp)
              if (isPrimary || isSecondary) {
                Surface(
                  shape = design.cardInnerShape(),
                  color = MaterialTheme.colorScheme.primary,
                ) {
                  Text(
                    text = if (isPrimary) "P1" else "P2",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = tab.title,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = tab.displayHost,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (splitMode) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                  onClick = { onSelectTab(0, SpatialDiveOrigin(selfRect, design.cardCornerRadiusDp)) }
                ) { Text("왼쪽") }
                TextButton(
                  onClick = { onSelectTab(1, SpatialDiveOrigin(selfRect, design.cardCornerRadiusDp)) }
                ) { Text("오른쪽") }
              }
            }
            SpringIconButton(
              onClick = onTogglePin,
              modifier = Modifier.size(34.dp).testTag("pin_tab_${tab.id}"),
            ) {
              Icon(
                imageVector = if (tab.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (tab.isPinned) "${tab.title} 고정 해제" else "${tab.title} 상단 고정",
                tint =
                  if (tab.isPinned) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
            SpringIconButton(
              onClick = onCloseTab,
              modifier = Modifier.size(34.dp).testTag("close_tab_${tab.id}"),
            ) {
              Icon(
                Icons.Outlined.Close,
                contentDescription = "${tab.title} 닫기",
                modifier = Modifier.size(17.dp),
              )
            }
          }
        }
      }
    }
  }

  if (swipeToCloseEnabled) {
    SwipeToDismissBox(
      state = dismissState,
      enableDismissFromStartToEnd = true,
      enableDismissFromEndToStart = true,
      backgroundContent = {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .clip(design.cardOuterShape())
              .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
              .padding(horizontal = BrowserSpacing.Grid2),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Icon(
            Icons.Outlined.DeleteOutline,
            contentDescription = "탭 닫기",
            tint = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      },
      content = { cardContent() },
    )
  } else {
    cardContent()
  }
}
