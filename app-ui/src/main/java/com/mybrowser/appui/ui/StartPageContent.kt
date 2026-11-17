package com.mybrowser.appui.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.data.BookmarkEntity
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.viewmodel.BlockerStatsUi
import java.util.Locale

@Composable
fun StartPageContent(
  paneIndex: Int,
  isFocusedPane: Boolean,
  isSplitMode: Boolean = false,
  postureState: FoldablePostureState,
  blockerStats: BlockerStatsUi,
  searchEngineLabel: String,
  dohProviderLabel: String,
  bookmarks: List<BookmarkEntity>,
  onOpenOmnibox: () -> Unit,
  onNavigateBookmark: (url: String, origin: SpatialDiveOrigin) -> Unit,
  onOpenInspector: (SpatialDiveOrigin) -> Unit,
  onToggleSplit: () -> Unit,
  onScrollDelta: (Int) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val bgColor = MaterialTheme.colorScheme.background
  val scrollState = rememberScrollState()
  var lastOffset by remember { mutableIntStateOf(0) }
  var hasInitializedScroll by remember { mutableStateOf(false) }

  LaunchedEffect(scrollState) {
    snapshotFlow { scrollState.value }
      .collect { current ->
        val dy = current - lastOffset
        if (hasInitializedScroll && dy != 0) {
          onScrollDelta(dy)
        }
        hasInitializedScroll = true
        lastOffset = current
      }
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(bgColor)
        .drawBehind {
          drawRect(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    primaryColor.copy(alpha = if (isFocusedPane) 0.14f else 0.06f),
                    bgColor,
                  ),
                center = Offset(size.width * 0.25f, size.height * 0.15f),
                radius = size.maxDimension * 0.75f,
              )
          )
          drawRect(
            brush =
              Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.09f), bgColor.copy(alpha = 0f)),
                center = Offset(size.width * 0.85f, size.height * 0.75f),
                radius = size.maxDimension * 0.55f,
              )
          )
        }
  ) {
    val columns = if (maxWidth >= 520.dp) 3 else 2

    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(scrollState)
          .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid2),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
      ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
            ) {
              Surface(
                shape = BrowserRadii.CardInnerShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
              ) {
                Text(
                  text = "MB",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
              Column {
                Text(
                  text = "MyBrowser(MB)",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                  text = "순정 AOSP",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
            ) {
              val ratioStr = String.format(Locale.US, "%.2f", postureState.aspectRatio)
              Text(
                text = "${postureState.mode.badgeLabel} · $ratioStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }

          // 통합 검색 런처
          SpringPressSurface(
            onClick = onOpenOmnibox,
            shape = LocalDesignConfig.current.cardOuterShape(),
            unselectedColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().testTag("start_page_omnibox_pane_$paneIndex"),
          ) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = BrowserSpacing.Grid2, vertical = BrowserSpacing.Grid2),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
            ) {
              Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "검색 또는 주소 입력",
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "$searchEngineLabel 검색 또는 주소 입력",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Surface(
                shape = BrowserRadii.CardInnerShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = "P${paneIndex + 1}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
      }

      // 프라이버시 상태 스트립
      var inspectorRect by remember { mutableStateOf(Rect.Zero) }
      val cardCornerDp = LocalDesignConfig.current.cardCornerRadiusDp
      SpringPressSurface(
          onClick = { onOpenInspector(SpatialDiveOrigin(inspectorRect, cardCornerDp)) },
          shape = LocalDesignConfig.current.cardOuterShape(),
          unselectedColor = MaterialTheme.colorScheme.surface,
          modifier =
            Modifier.widthIn(max = 680.dp)
              .fillMaxWidth()
              .onGloballyPositioned { inspectorRect = it.boundsInRoot() },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier =
                  Modifier.size(40.dp)
                    .clip(BrowserRadii.CardInnerShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Shield,
                  contentDescription = "추적 차단",
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(20.dp),
                )
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text =
                    if (blockerStats.enabled) {
                      "차단 ${blockerStats.totalBlocked}건"
                    } else {
                      "차단 일시중지"
                    },
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = "DoH · $dohProviderLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            if (postureState.supportsDualPane) {
              SpringPressSurface(
                onClick = onToggleSplit,
                shape = BrowserRadii.CardInnerShape,
                unselectedColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.testTag("start_page_split_chip"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Splitscreen,
                    contentDescription = if (isSplitMode) "창 합치기" else "화면 분할",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = if (isSplitMode) "합치기" else "분할",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }

      // 자주 방문 그리드
      Column(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
      ) {
          Text(
            text = "자주 방문",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          val chunked = remember(bookmarks, columns) { bookmarks.take(9).chunked(columns) }
          Column(verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5)) {
            chunked.forEach { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
              ) {
                rowItems.forEach { bookmark ->
                  SpeedDialCard(
                    bookmark = bookmark,
                    onClick = { origin -> onNavigateBookmark(bookmark.url, origin) },
                    modifier = Modifier.weight(1f),
                  )
                }
                repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
              }
            }
          }
        }
    }
  }
}

@Composable
private fun SpeedDialCard(
  bookmark: BookmarkEntity,
  onClick: (SpatialDiveOrigin) -> Unit,
  modifier: Modifier = Modifier,
) {
  var tileRect by remember { mutableStateOf(Rect.Zero) }
  val cornerDp = LocalDesignConfig.current.cardCornerRadiusDp
  SpringPressSurface(
    onClick = { onClick(SpatialDiveOrigin(tileRect, cornerDp)) },
    shape = LocalDesignConfig.current.cardOuterShape(),
    unselectedColor = MaterialTheme.colorScheme.surface,
    modifier =
      modifier
        .height(76.dp)
        .onGloballyPositioned { tileRect = it.boundsInRoot() },
  ) {
    Row(
      modifier =
        Modifier.fillMaxSize().padding(horizontal = BrowserSpacing.Grid1_5, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
    ) {
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(BrowserRadii.CardInnerShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Language,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = bookmark.title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = bookmark.category,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }
  }
}
