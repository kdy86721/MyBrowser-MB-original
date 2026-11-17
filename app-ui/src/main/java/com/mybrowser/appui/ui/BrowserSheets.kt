package com.mybrowser.appui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybrowser.appui.data.BookmarkEntity
import com.mybrowser.appui.data.HistoryEntity
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.viewmodel.ReaderArticleUi
import com.mybrowser.appui.viewmodel.TabFolderUiModel
import com.mybrowser.appui.viewmodel.TabSortMode
import com.mybrowser.appui.viewmodel.TabUiModel
import kotlin.math.abs
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherOverlay(
  tabs: List<TabUiModel>,
  folders: List<TabFolderUiModel>,
  tabSortMode: TabSortMode,
  heroArrivalTabId: String? = null,
  hasUndoableClosedTab: Boolean,
  tabSwipeToCloseEnabled: Boolean,
  tabDragFolderEnabled: Boolean,
  primaryTabId: String,
  secondaryTabId: String?,
  splitMode: Boolean,
  postureState: FoldablePostureState,
  onSelectTab: (tabId: String, targetPane: Int) -> Unit,
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

  val design = LocalDesignConfig.current
  val haptic = LocalHapticFeedback.current

  val tabBounds = remember { mutableStateMapOf<String, Rect>() }
  val folderBounds = remember { mutableStateMapOf<String, Rect>() }

  var draggedTabId by remember { mutableStateOf<String?>(null) }
  var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
  var hoveredTabTargetId by remember { mutableStateOf<String?>(null) }
  var hoveredFolderTargetId by remember { mutableStateOf<String?>(null) }
  var hoverReadyToMerge by remember { mutableStateOf(false) }

  var editingFolder by remember { mutableStateOf<TabFolderUiModel?>(null) }

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

  val gridCols = if (postureState.windowWidthDp >= 540.dp) 2 else 1
  val validFolderIds = remember(folders) { folders.map { it.id }.toSet() }

  val pinnedTopTabs = remember(tabs, tabSortMode) {
    if (tabSortMode == TabSortMode.PINNED_FIRST) {
      tabs.filter { it.isPinned }.sortedByDescending { it.lastActiveAtMs }
    } else {
      emptyList()
    }
  }
  val pinnedTopIds = remember(pinnedTopTabs) { pinnedTopTabs.map { it.id }.toSet() }

  val ungroupedTabs = remember(tabs, validFolderIds, tabSortMode, pinnedTopIds) {
    val base =
      tabs.filter { tab ->
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

  val domainGroups = remember(ungroupedTabs, tabSortMode) {
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
                if (hypot((p1.x - firstDownPos.x).toDouble(), (p1.y - firstDownPos.y).toDouble()) > touchSlop * 2 ||
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
    color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
    modifier =
      Modifier.fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .then(multiTouchCollapseModifier),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier.fillMaxSize().padding(BrowserSpacing.Grid2),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier.widthIn(max = 820.dp).fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
        ) {
          // 헤더
          Row(
            modifier = Modifier.fillMaxWidth(),
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

          // 5종 정렬 및 그룹핑 모드 선택 바
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
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

          if (tabDragFolderEnabled || folders.isNotEmpty()) {
            val activeDraggedTab = draggedTabId?.let { id -> tabs.firstOrNull { it.id == id } }
            val hintText =
              when {
                hoverReadyToMerge && hoveredTabTargetId != null -> "놓아서 새 폴더로 병합"
                hoverReadyToMerge && hoveredFolderTargetId != null -> "놓아서 이 폴더에 추가"
                activeDraggedTab?.folderId != null -> "폴더 바깥으로 드래그해 놓으면 그룹 해제"
                tabDragFolderEnabled -> "탭을 길게 끌어 다른 탭 위에 0.5초 올리면 폴더 생성 · 두 손가락 탭/모으기로 접기"
                else -> "두 손가락 탭 또는 모으기로 모든 폴더 접기"
              }
            Surface(
              shape = design.cardInnerShape(),
              color =
                if (hoverReadyToMerge) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = hintText,
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (hoverReadyToMerge) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }

          // 폴더 + 고정 탭 + 도메인/정렬 탭 목록
          LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
            contentPadding = PaddingValues(bottom = BrowserSpacing.Grid1_5),
          ) {
            // 0. 고정 탭 우선 섹션 (PINNED_FIRST 모드)
            if (tabSortMode == TabSortMode.PINNED_FIRST && pinnedRows.isNotEmpty()) {
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
              items(pinnedRows.size, key = { idx -> "pinned_row_${pinnedRows[idx].firstOrNull()?.id ?: idx}" }) { rowIdx ->
                val rowTabs = pinnedRows[rowIdx]
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                ) {
                  rowTabs.forEach { tab ->
                    Box(modifier = Modifier.weight(1f)) {
                      TabGridItemCard(
                        tab = tab,
                        isPrimary = tab.id == primaryTabId,
                        isSecondary = splitMode && tab.id == secondaryTabId,
                        splitMode = splitMode,
                        isHeroArrival = tab.id == heroArrivalTabId,
                        isBeingDragged = draggedTabId == tab.id,
                        isHoverMergeTarget = hoveredTabTargetId == tab.id && hoverReadyToMerge,
                        swipeToCloseEnabled = tabSwipeToCloseEnabled && draggedTabId == null,
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
                        onSelectTab = { pane -> onSelectTab(tab.id, pane) },
                        onTogglePin = { onTogglePinTab(tab.id) },
                        onCloseTab = { onCloseTab(tab.id) },
                      )
                    }
                  }
                  repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                }
              }
            }

            // 1. 탭 폴더 카드 섹션 (도메인별 제외, 또는 모든 모드에서 폴더 유지)
            items(folders, key = { it.id }) { folder ->
              val folderTabs =
                tabs.filter { it.folderId == folder.id && it.id !in pinnedTopIds }.let { list ->
                  if (tabSortMode == TabSortMode.CREATED) list.sortedBy { it.createdAtMs }
                  else list.sortedByDescending { it.lastActiveAtMs }
                }
              if (folderTabs.isNotEmpty() || tabSortMode == TabSortMode.FOLDERS_FIRST) {
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
                        shape = CircleShape,
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

                    AnimatedVisibility(visible = folder.isExpanded) {
                      val rows = folderTabs.chunked(gridCols)
                      Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
                        rows.forEach { rowTabs ->
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                          ) {
                            rowTabs.forEach { tab ->
                              Box(modifier = Modifier.weight(1f)) {
                                TabGridItemCard(
                                  tab = tab,
                                  isPrimary = tab.id == primaryTabId,
                                  isSecondary = splitMode && tab.id == secondaryTabId,
                                  splitMode = splitMode,
                                  isHeroArrival = tab.id == heroArrivalTabId,
                                  isBeingDragged = draggedTabId == tab.id,
                                  isHoverMergeTarget =
                                    hoveredTabTargetId == tab.id && hoverReadyToMerge,
                                  swipeToCloseEnabled = tabSwipeToCloseEnabled && draggedTabId == null,
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
                                  onSelectTab = { pane -> onSelectTab(tab.id, pane) },
                                  onTogglePin = { onTogglePinTab(tab.id) },
                                  onCloseTab = { onCloseTab(tab.id) },
                                )
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
                  key = { idx -> "domain_${domainName}_row_${groupRows[idx].firstOrNull()?.id ?: idx}" },
                ) { rowIdx ->
                  val rowTabs = groupRows[rowIdx]
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                  ) {
                    rowTabs.forEach { tab ->
                      Box(modifier = Modifier.weight(1f)) {
                        TabGridItemCard(
                          tab = tab,
                          isPrimary = tab.id == primaryTabId,
                          isSecondary = splitMode && tab.id == secondaryTabId,
                          splitMode = splitMode,
                          isHeroArrival = tab.id == heroArrivalTabId,
                          isBeingDragged = draggedTabId == tab.id,
                          isHoverMergeTarget = hoveredTabTargetId == tab.id && hoverReadyToMerge,
                          swipeToCloseEnabled = tabSwipeToCloseEnabled && draggedTabId == null,
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
                          onSelectTab = { pane -> onSelectTab(tab.id, pane) },
                          onTogglePin = { onTogglePinTab(tab.id) },
                          onCloseTab = { onCloseTab(tab.id) },
                        )
                      }
                    }
                    repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                  }
                }
              }
            } else {
              items(ungroupedRows.size, key = { idx -> "row_${ungroupedRows[idx].firstOrNull()?.id ?: idx}" }) { rowIndex ->
                val rowTabs = ungroupedRows[rowIndex]
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
                ) {
                  rowTabs.forEach { tab ->
                    Box(modifier = Modifier.weight(1f)) {
                      TabGridItemCard(
                        tab = tab,
                        isPrimary = tab.id == primaryTabId,
                        isSecondary = splitMode && tab.id == secondaryTabId,
                        splitMode = splitMode,
                        isHeroArrival = tab.id == heroArrivalTabId,
                        isBeingDragged = draggedTabId == tab.id,
                        isHoverMergeTarget = hoveredTabTargetId == tab.id && hoverReadyToMerge,
                        swipeToCloseEnabled = tabSwipeToCloseEnabled && draggedTabId == null,
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
                        onSelectTab = { pane -> onSelectTab(tab.id, pane) },
                        onTogglePin = { onTogglePinTab(tab.id) },
                        onCloseTab = { onCloseTab(tab.id) },
                      )
                    }
                  }
                  repeat(gridCols - rowTabs.size) { Spacer(Modifier.weight(1f)) }
                }
              }
            }
          }

          Button(
            onClick = onCreateTab,
            shape = design.cardInnerShape(),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("new_tab_button"),
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("새 탭", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      // 드래그 중 플로팅 고스트 카드 표시
      val draggedTab = draggedTabId?.let { id -> tabs.firstOrNull { it.id == id } }
      if (draggedTab != null) {
        Surface(
          shape = design.cardOuterShape(),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
          shadowElevation = 14.dp,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
          modifier =
            Modifier.offset {
                IntOffset(
                  (dragPointerRoot.x - 110f).roundToInt(),
                  (dragPointerRoot.y - 160f).roundToInt(),
                )
              }
              .width(220.dp),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = draggedTab.title,
              style = MaterialTheme.typography.labelLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (hoverReadyToMerge) "놓아서 병합하기"
                else if (draggedTab.folderId != null) "이동 중 (밖으로 놓아 해제)"
                else "대상 위에 0.5초 머무르면 병합",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
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

private fun tabDomainLabel(tab: TabUiModel): String =
  if (tab.url == "about:mb" || tab.url == "about:blank") {
    "내부 페이지"
  } else {
    tab.displayHost.lowercase().removePrefix("www.").ifBlank { "기타" }
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabGridItemCard(
  tab: TabUiModel,
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
  onSelectTab: (Int) -> Unit,
  onTogglePin: () -> Unit,
  onCloseTab: () -> Unit,
) {
  val design = LocalDesignConfig.current
  val isActive = isPrimary || isSecondary
  var selfRect by remember { mutableStateOf(Rect.Zero) }
  val heroSettleScale = remember(tab.id, isHeroArrival) {
    Animatable(if (isHeroArrival) 0.90f else 1f)
  }
  LaunchedEffect(tab.id, isHeroArrival) {
    if (isHeroArrival) {
      heroSettleScale.animateTo(1f, BrowserMotion.morphingPillSpring())
    }
  }

  val dismissState =
    rememberSwipeToDismissBoxState(
      confirmValueChange = { value ->
        if (swipeToCloseEnabled &&
          (value == SwipeToDismissBoxValue.StartToEnd ||
            value == SwipeToDismissBoxValue.EndToStart)
        ) {
          onCloseTab()
          true
        } else {
          false
        }
      }
    )

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
      onClick = { onSelectTab(0) },
      selected = isActive || isHoverMergeTarget || tab.isPinned,
      shape = design.cardOuterShape(),
      unselectedColor = MaterialTheme.colorScheme.surface,
      selectedBorderColor =
        when {
          isHoverMergeTarget -> MaterialTheme.colorScheme.primary
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
      Column(
        modifier = Modifier.padding(BrowserSpacing.Grid2),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
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
            if (isPrimary || isSecondary) {
              Surface(
                shape = design.cardInnerShape(),
                color = MaterialTheme.colorScheme.primary,
              ) {
                Text(
                  text = if (isPrimary) "P1" else "P2",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = tab.title,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          SpringIconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(36.dp).testTag("pin_tab_${tab.id}"),
          ) {
            Icon(
              imageVector = if (tab.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
              contentDescription = if (tab.isPinned) "${tab.title} 고정 해제" else "${tab.title} 상단 고정",
              tint =
                if (tab.isPinned) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(17.dp),
            )
          }
          SpringIconButton(
            onClick = onCloseTab,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              Icons.Outlined.Close,
              contentDescription = "${tab.title} 닫기",
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Text(
          text = tab.displayHost,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isHoverMergeTarget) "여기에 놓아 병합" else "차단 ${tab.blockedCount}",
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isHoverMergeTarget) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.secondary,
          )
          if (splitMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
              TextButton(onClick = { onSelectTab(0) }) { Text("왼쪽") }
              TextButton(onClick = { onSelectTab(1) }) { Text("오른쪽") }
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

@Composable
fun LibraryOverlay(
  bookmarks: List<BookmarkEntity>,
  history: List<HistoryEntity>,
  focusedUrl: String?,
  initialTab: Int = 0,
  onToggleCurrentBookmark: () -> Unit,
  onOpenUrl: (String) -> Unit,
  onDeleteBookmark: (String) -> Unit,
  onDeleteHistoryItem: (String) -> Unit,
  onClearHistory: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
  val isCurrentBookmarked = focusedUrl != null && bookmarks.any { it.url == focusedUrl }
  val design = LocalDesignConfig.current

  Surface(
    color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(BrowserSpacing.Grid2),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = "보관함", style = MaterialTheme.typography.titleLarge)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          ) {
            if (!focusedUrl.isNullOrBlank() && focusedUrl != "about:mb" && focusedUrl != "about:blank") {
              OutlinedButton(
                onClick = onToggleCurrentBookmark,
                shape = design.cardInnerShape(),
              ) {
                Icon(
                  Icons.Outlined.BookmarkBorder,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(if (isCurrentBookmarked) "저장됨" else "북마크")
              }
            }
            SpringIconButton(onClick = onDismiss) {
              Icon(Icons.Outlined.Close, contentDescription = "닫기")
            }
          }
        }

        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("북마크 (${bookmarks.size})") },
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("방문 기록 (${history.size})") },
          )
        }

        if (selectedTab == 0) {
          if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                "저장된 북마크가 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
              items(bookmarks, key = { it.url }) { b ->
                LibraryEntryCard(
                  title = b.title,
                  subtitle = b.url,
                  badge = b.category,
                  onClick = { onOpenUrl(b.url) },
                  onDelete = { onDeleteBookmark(b.url) },
                )
              }
            }
          }
        } else {
          if (history.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              TextButton(onClick = onClearHistory) { Text("전체 삭제") }
            }
          }
          if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                "방문 기록이 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
              items(history, key = { it.url }) { h ->
                LibraryEntryCard(
                  title = h.title,
                  subtitle = h.url,
                  badge = "${h.visitCount}회",
                  onClick = { onOpenUrl(h.url) },
                  onDelete = { onDeleteHistoryItem(h.url) },
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
private fun LibraryEntryCard(
  title: String,
  subtitle: String,
  badge: String,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val design = LocalDesignConfig.current
  SpringPressSurface(
    onClick = onClick,
    shape = design.cardOuterShape(),
    unselectedColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = BrowserSpacing.Grid2, vertical = BrowserSpacing.Grid1_5),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Surface(
        shape = design.cardInnerShape(),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Text(
          text = badge,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
      SpringIconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
        Icon(
          Icons.Outlined.DeleteOutline,
          contentDescription = "삭제",
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
fun ReaderModeOverlay(
  article: ReaderArticleUi,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val design = LocalDesignConfig.current

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(horizontal = BrowserSpacing.Grid3, vertical = BrowserSpacing.Grid1_5),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = design.cardInnerShape(),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Text(
            text = "읽기 · ${article.domain}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
        SpringIconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "닫기")
        }
      }

      LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(vertical = BrowserSpacing.Grid2),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid2),
      ) {
        item(key = "reader_title") {
          Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        items(article.blocks) { paragraph ->
          Text(
            text = paragraph,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 27.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
          )
        }
      }
    }
  }
}
