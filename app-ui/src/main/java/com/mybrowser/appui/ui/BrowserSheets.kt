package com.mybrowser.appui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mybrowser.appui.data.BookmarkEntity
import com.mybrowser.appui.data.HistoryEntity
import com.mybrowser.appui.viewmodel.ReaderArticleUi

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

  val navBottom =
    WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .padding(start = BrowserSpacing.Grid2, end = BrowserSpacing.Grid2, top = BrowserSpacing.Grid2),
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
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              contentPadding = PaddingValues(bottom = BrowserSpacing.Grid2 + navBottom),
            ) {
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
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              contentPadding = PaddingValues(bottom = BrowserSpacing.Grid2 + navBottom),
            ) {
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

  val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .padding(start = BrowserSpacing.Grid3, end = BrowserSpacing.Grid3, top = BrowserSpacing.Grid1_5),
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
        contentPadding =
          PaddingValues(
            top = BrowserSpacing.Grid2,
            bottom = BrowserSpacing.Grid2 + navBottom,
          ),
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
