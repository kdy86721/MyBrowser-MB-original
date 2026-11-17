package com.mybrowser.appui.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.data.BookmarkEntity
import com.mybrowser.appui.viewmodel.BlockerStatsUi
import com.mybrowser.appui.viewmodel.TabUiModel

/**
 * 테이블탑 모드 전용 하단 컨트롤 덱 (`HALF_OPENED` + 수평 힌지).
 */
@Composable
fun TabletopDeck(
  focusedTab: TabUiModel?,
  tabs: List<TabUiModel>,
  bookmarks: List<BookmarkEntity>,
  blockerStats: BlockerStatsUi,
  onBack: () -> Unit,
  onForward: () -> Unit,
  onReloadOrStop: () -> Unit,
  onHome: () -> Unit,
  onOpenOmnibox: () -> Unit,
  onSelectTab: (String) -> Unit,
  onNewTab: () -> Unit,
  onNavigateBookmark: (String) -> Unit,
  onOpenLibrary: () -> Unit,
  onOpenInspector: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.fillMaxSize(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            start = BrowserSpacing.Grid2,
            end = BrowserSpacing.Grid2,
            top = BrowserSpacing.Grid1_5,
            bottom = BrowserSpacing.Grid1_5 + navBottom,
          ),
      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
    ) {
      // 상단 덱: 모드 뱃지 + 주소창 + 설정
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        Surface(
          shape = BrowserRadii.CardInnerShape,
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Text(
            text = "테이블탑",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }

        SpringPressSurface(
          onClick = onOpenOmnibox,
          shape = BrowserRadii.FloatingInnerShape,
          unselectedColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.weight(1f).testTag("tabletop_omnibox_trigger"),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = BrowserSpacing.Grid1_5, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          ) {
            Icon(
              imageVector =
                if (focusedTab?.isSecure == true) Icons.Outlined.Lock else Icons.Outlined.Shield,
              contentDescription = "보안 상태",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp),
            )
            val isInternal =
              focusedTab == null ||
                focusedTab.url == "about:mb" ||
                focusedTab.url == "about:blank"
            Text(
              text = if (isInternal) "검색 또는 주소 입력" else focusedTab!!.url,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
          }
        }

        SpringIconButton(onClick = onOpenInspector) {
          Icon(imageVector = Icons.Outlined.Settings, contentDescription = "설정")
        }
      }

      // 탐색 아이콘 바
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          SpringIconButton(
            onClick = onBack,
            enabled = focusedTab?.canGoBack == true,
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
          }
          SpringIconButton(
            onClick = onForward,
            enabled = focusedTab?.canGoForward == true,
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "앞으로")
          }
          SpringIconButton(onClick = onReloadOrStop) {
            Icon(Icons.Outlined.Refresh, contentDescription = "새로고침")
          }
          SpringIconButton(onClick = onHome) {
            Icon(Icons.Outlined.Home, contentDescription = "홈")
          }
          SpringIconButton(onClick = onOpenLibrary) {
            Icon(Icons.Outlined.Bookmarks, contentDescription = "보관함")
          }
        }

        Surface(
          shape = BrowserRadii.CardInnerShape,
          color = MaterialTheme.colorScheme.background,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Text(
            text = "차단 ${blockerStats.totalBlocked}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      // 탭 스트립
      LazyRow(
        contentPadding = PaddingValues(end = BrowserSpacing.Grid1),
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        items(tabs, key = { it.id }) { tab ->
          val active = tab.id == focusedTab?.id
          SpringPressSurface(
            onClick = { onSelectTab(tab.id) },
            selected = active,
            shape = BrowserRadii.CardInnerShape,
            modifier = Modifier.width(156.dp),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              Text(
                text = tab.title,
                style = MaterialTheme.typography.labelLarge,
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
        }

        item(key = "new_tab_chip") {
          SpringPressSurface(
            onClick = onNewTab,
            shape = BrowserRadii.CardInnerShape,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Outlined.Add, contentDescription = "새 탭", modifier = Modifier.size(18.dp))
              Text(text = "새 탭", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }

      // 빠른 북마크 스트립
      LazyRow(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
        items(bookmarks.take(6), key = { it.url }) { bookmark ->
          SpringPressSurface(
            onClick = { onNavigateBookmark(bookmark.url) },
            shape = BrowserRadii.CardInnerShape,
            unselectedColor = MaterialTheme.colorScheme.background,
          ) {
            Text(
              text = bookmark.title,
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      }
    }
  }
}
