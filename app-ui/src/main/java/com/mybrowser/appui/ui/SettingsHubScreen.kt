package com.mybrowser.appui.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Javascript
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mybrowser.appui.foldable.FoldablePostureState
import com.mybrowser.appui.network.DohLookupResult
import com.mybrowser.appui.network.DohProviderInfo
import com.mybrowser.appui.viewmodel.BlockerLevel
import com.mybrowser.appui.viewmodel.BlockerStatsUi
import com.mybrowser.appui.viewmodel.BrowserSettingsUi
import com.mybrowser.appui.viewmodel.EngineMetaUi
import com.mybrowser.appui.viewmodel.SearchEngineOption
import com.mybrowser.appui.viewmodel.SitePermissionsUiState
import com.mybrowser.appui.viewmodel.SplitGestureSensitivity
import com.mybrowser.appui.viewmodel.StartupBehavior
import com.mybrowser.appui.viewmodel.TabSortMode
import com.mybrowser.appui.viewmodel.ToolbarActionId
import com.mybrowser.appui.viewmodel.ToolbarAlignment
import com.mybrowser.appui.viewmodel.ToolbarConfigUi
import java.util.Locale

enum class SettingsPillar(
  val titleKo: String,
  val icon: ImageVector,
) {
  DESIGN("디자인", Icons.Outlined.Palette),
  CUSTOMIZATION("사용자화", Icons.Outlined.Tune),
  GENERAL_PRIVACY("일반 설정", Icons.Outlined.Security),
  SITE_SETTINGS("사이트 설정", Icons.Outlined.WebAsset),
}

/**
 * 아이콘 중심 4대 설정 허브:
 * - 펼침 1:1.4 화면: 좌측 아이콘 내비게이션 레일 + 우측 20dp 카드 디테일 (2패널 마스터-디테일)
 * - 접힘 커버 화면: 드릴다운 스프링 전환
 */
@Composable
fun SettingsHubOverlay(
  abiInfo: EngineMetaUi,
  postureState: FoldablePostureState,
  blockerStats: BlockerStatsUi,
  settings: BrowserSettingsUi,
  toolbarConfig: ToolbarConfigUi,
  designConfig: DesignUiConfig,
  sitePermissions: SitePermissionsUiState,
  activeTabDomain: String?,
  searchEngines: List<SearchEngineOption>,
  dohProviders: List<DohProviderInfo>,
  isProbingDoh: Boolean,
  lastDohLookup: DohLookupResult?,
  statusBannerMessage: String?,
  onUpdateSetting: (key: String, value: String) -> Unit,
  onUpdateDesignParam: (key: String, value: String) -> Unit,
  onResetDesignDefaults: () -> Unit,
  onUpdateToolbarSlots: (left: List<ToolbarActionId>, right: List<ToolbarActionId>) -> Unit,
  onMoveToolbarSlot: (side: String, fromIdx: Int, toIdx: Int) -> Unit,
  onSelectAlignment: (ToolbarAlignment) -> Unit,
  onToggleAutoMinimize: (Boolean) -> Unit,
  onSetGestures: (swipeTabs: Boolean, pullRefresh: Boolean) -> Unit,
  onResetToolbar: () -> Unit,
  onUpdateFilterList: () -> Unit,
  onClearBrowsingData: (cache: Boolean, history: Boolean, cookies: Boolean) -> Unit,
  onSetGlobalSitePolicy: (key: String, value: String) -> Unit,
  onSetDomainSiteOverride: (domain: String, key: String, value: String) -> Unit,
  onResetDomainOverrides: (domain: String) -> Unit,
  onRunDohProbe: (String) -> Unit,
  onOpenLibrary: (SpatialDiveOrigin) -> Unit,
  onDismiss: () -> Unit,
) {
  val isMasterDetail = postureState.supportsDualPane
  var selectedPillar by remember { mutableStateOf(SettingsPillar.DESIGN) }
  var isDetailOpenOnCover by remember { mutableStateOf(false) }
  var coverDetailOrigin by remember { mutableStateOf(SpatialDiveOrigin()) }
  val coverDiveAnim = remember { Animatable(0f) }

  LaunchedEffect(isMasterDetail, isDetailOpenOnCover, designConfig.animationSpeedScale) {
    val target = if (!isMasterDetail && isDetailOpenOnCover) 1f else 0f
    coverDiveAnim.animateTo(target, ExpressiveSprings.spatial(designConfig.animationSpeedScale))
  }

  BackHandler {
    if (!isMasterDetail && isDetailOpenOnCover) {
      isDetailOpenOnCover = false
    } else {
      onDismiss()
    }
  }

  // True Edge-to-Edge opaque fill behind system bars
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize(),
  ) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = BrowserSpacing.Grid2, vertical = BrowserSpacing.Grid1_5),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        ) {
          if (!isMasterDetail && (isDetailOpenOnCover || coverDiveAnim.value > 0.05f)) {
            SpringIconButton(onClick = { isDetailOpenOnCover = false }) {
              Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "카테고리로 돌아가기")
            }
          }
          Text(
            text =
              if (!isMasterDetail && isDetailOpenOnCover) selectedPillar.titleKo else "설정",
            style = MaterialTheme.typography.titleLarge,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        ) {
          var libButtonRect by remember { mutableStateOf(Rect.Zero) }
          SpringPressSurface(
            onClick = {
              onOpenLibrary(SpatialDiveOrigin(libButtonRect, designConfig.cardCornerRadiusDp))
            },
            shape = designConfig.cardInnerShape(),
            unselectedColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.onGloballyPositioned { libButtonRect = it.boundsInRoot() },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.Outlined.Bookmarks,
                contentDescription = "보관함",
                modifier = Modifier.size(18.dp),
              )
              Text("보관함", style = MaterialTheme.typography.labelLarge)
            }
          }
          SpringIconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("settings_close_button"),
          ) {
            Icon(Icons.Outlined.Close, contentDescription = "설정 닫기")
          }
        }
      }

      if (statusBannerMessage != null) {
        Surface(
          shape = designConfig.cardInnerShape(),
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = BrowserSpacing.Grid2, vertical = 4.dp),
        ) {
          Text(
            text = statusBannerMessage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier =
              Modifier.padding(
                horizontal = BrowserSpacing.Grid1_5,
                vertical = BrowserSpacing.Grid1,
              ),
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

      if (isMasterDetail) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
          SettingsMasterList(
            selectedPillar = selectedPillar,
            showSelectionHighlight = true,
            postureState = postureState,
            abiBackend = abiInfo.backendKind,
            onSelectPillar = { pillar, _ -> selectedPillar = pillar },
            modifier = Modifier.width(232.dp).fillMaxHeight(),
          )
          Box(
            modifier =
              Modifier.width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
          )
          AnimatedContent(
            targetState = selectedPillar,
            transitionSpec = {
              val speed = designConfig.animationSpeedScale
              (fadeIn(ExpressiveSprings.spatial(speed)) +
                scaleIn(ExpressiveSprings.spatial(speed), initialScale = 0.94f)) togetherWith
                (fadeOut(ExpressiveSprings.micro(speed)) +
                  scaleOut(ExpressiveSprings.micro(speed), targetScale = 1.04f))
            },
            label = "SettingsDetailTransition",
            modifier = Modifier.weight(1f).fillMaxHeight(),
          ) { pillar ->
            SettingsDetailPane(
              pillar = pillar,
              blockerStats = blockerStats,
              settings = settings,
              toolbarConfig = toolbarConfig,
              designConfig = designConfig,
              sitePermissions = sitePermissions,
              activeTabDomain = activeTabDomain,
              searchEngines = searchEngines,
              dohProviders = dohProviders,
              isProbingDoh = isProbingDoh,
              lastDohLookup = lastDohLookup,
              onUpdateSetting = onUpdateSetting,
              onUpdateDesignParam = onUpdateDesignParam,
              onResetDesignDefaults = onResetDesignDefaults,
              onUpdateToolbarSlots = onUpdateToolbarSlots,
              onMoveToolbarSlot = onMoveToolbarSlot,
              onSelectAlignment = onSelectAlignment,
              onToggleAutoMinimize = onToggleAutoMinimize,
              onSetGestures = onSetGestures,
              onResetToolbar = onResetToolbar,
              onUpdateFilterList = onUpdateFilterList,
              onClearBrowsingData = onClearBrowsingData,
              onSetGlobalSitePolicy = onSetGlobalSitePolicy,
              onSetDomainSiteOverride = onSetDomainSiteOverride,
              onResetDomainOverrides = onResetDomainOverrides,
              onRunDohProbe = onRunDohProbe,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      } else {
        var coverHostOrigin by remember { mutableStateOf(Offset.Zero) }
        var coverHostSize by remember { mutableStateOf(IntSize.Zero) }
        val diveP = coverDiveAnim.value.coerceIn(0f, 1f)

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .onGloballyPositioned { coords ->
                coverHostOrigin = coords.boundsInRoot().topLeft
                coverHostSize = coords.size
              },
        ) {
          if (diveP < 0.995f) {
            if (diveP > 0.002f) {
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f * diveP))
              )
            }
            SettingsMasterList(
              selectedPillar = selectedPillar,
              showSelectionHighlight = false,
              postureState = postureState,
              abiBackend = abiInfo.backendKind,
              onSelectPillar = { pillar, origin ->
                selectedPillar = pillar
                coverDetailOrigin = origin
                isDetailOpenOnCover = true
              },
              modifier =
                Modifier.fillMaxSize().spatialDiveBackground(
                  progress = diveP,
                  origin = coverDetailOrigin,
                  hostSize = coverHostSize,
                  hostTopLeftInRoot = coverHostOrigin,
                ),
            )
            SpatialDiveScrim(progress = diveP)
          }

          if (diveP > 0.002f || isDetailOpenOnCover) {
            SpatialDiveForegroundHost(
              progress = diveP,
              origin = coverDetailOrigin,
              hostSize = coverHostSize,
              hostTopLeftInRoot = coverHostOrigin,
              surfaceColor = MaterialTheme.colorScheme.background,
            ) {
              SettingsDetailPane(
                pillar = selectedPillar,
                blockerStats = blockerStats,
                settings = settings,
                toolbarConfig = toolbarConfig,
                designConfig = designConfig,
                sitePermissions = sitePermissions,
                activeTabDomain = activeTabDomain,
                searchEngines = searchEngines,
                dohProviders = dohProviders,
                isProbingDoh = isProbingDoh,
                lastDohLookup = lastDohLookup,
                onUpdateSetting = onUpdateSetting,
                onUpdateDesignParam = onUpdateDesignParam,
                onResetDesignDefaults = onResetDesignDefaults,
                onUpdateToolbarSlots = onUpdateToolbarSlots,
                onMoveToolbarSlot = onMoveToolbarSlot,
                onSelectAlignment = onSelectAlignment,
                onToggleAutoMinimize = onToggleAutoMinimize,
                onSetGestures = onSetGestures,
                onResetToolbar = onResetToolbar,
                onUpdateFilterList = onUpdateFilterList,
                onClearBrowsingData = onClearBrowsingData,
                onSetGlobalSitePolicy = onSetGlobalSitePolicy,
                onSetDomainSiteOverride = onSetDomainSiteOverride,
                onResetDomainOverrides = onResetDomainOverrides,
                onRunDohProbe = onRunDohProbe,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsMasterList(
  selectedPillar: SettingsPillar,
  showSelectionHighlight: Boolean,
  postureState: FoldablePostureState,
  abiBackend: String,
  onSelectPillar: (SettingsPillar, SpatialDiveOrigin) -> Unit,
  modifier: Modifier = Modifier,
) {
  val design = LocalDesignConfig.current
  val bottomInset =
    WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
  LazyColumn(
    modifier = modifier,
    contentPadding =
      PaddingValues(
        start = BrowserSpacing.Grid2,
        end = BrowserSpacing.Grid2,
        top = BrowserSpacing.Grid2,
        bottom = BrowserSpacing.Grid2 + bottomInset,
      ),
    verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
  ) {
    items(SettingsPillar.entries, key = { it.name }) { pillar ->
      val active = showSelectionHighlight && pillar == selectedPillar
      var cardBounds by remember { mutableStateOf(Rect.Zero) }
      SpringPressSurface(
        onClick = {
          onSelectPillar(pillar, SpatialDiveOrigin(cardBounds, design.cardCornerRadiusDp))
        },
        selected = active,
        shape = design.cardOuterShape(),
        unselectedColor = MaterialTheme.colorScheme.surface,
        modifier =
          Modifier.fillMaxWidth()
            .onGloballyPositioned { cardBounds = it.boundsInRoot() }
            .testTag("settings_pillar_${pillar.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid2),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
        ) {
          Box(
            modifier =
              Modifier.size(40.dp)
                .clip(design.iconShape())
                .background(
                  if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = pillar.icon,
              contentDescription = pillar.titleKo,
              tint =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
          Text(
            text = pillar.titleKo,
            style = MaterialTheme.typography.titleMedium,
            color =
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    item(key = "telemetry_chip") {
      val ratioStr = String.format(Locale.US, "%.2f", postureState.aspectRatio)
      Surface(
        shape = design.cardInnerShape(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().padding(top = BrowserSpacing.Grid1),
      ) {
        Column(
          modifier = Modifier.padding(BrowserSpacing.Grid1_5),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "${postureState.mode.badgeLabel} · $ratioStr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "순정 AOSP · $abiBackend",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDetailPane(
  pillar: SettingsPillar,
  blockerStats: BlockerStatsUi,
  settings: BrowserSettingsUi,
  toolbarConfig: ToolbarConfigUi,
  designConfig: DesignUiConfig,
  sitePermissions: SitePermissionsUiState,
  activeTabDomain: String?,
  searchEngines: List<SearchEngineOption>,
  dohProviders: List<DohProviderInfo>,
  isProbingDoh: Boolean,
  lastDohLookup: DohLookupResult?,
  onUpdateSetting: (key: String, value: String) -> Unit,
  onUpdateDesignParam: (key: String, value: String) -> Unit,
  onResetDesignDefaults: () -> Unit,
  onUpdateToolbarSlots: (left: List<ToolbarActionId>, right: List<ToolbarActionId>) -> Unit,
  onMoveToolbarSlot: (side: String, fromIdx: Int, toIdx: Int) -> Unit,
  onSelectAlignment: (ToolbarAlignment) -> Unit,
  onToggleAutoMinimize: (Boolean) -> Unit,
  onSetGestures: (swipeTabs: Boolean, pullRefresh: Boolean) -> Unit,
  onResetToolbar: () -> Unit,
  onUpdateFilterList: () -> Unit,
  onClearBrowsingData: (cache: Boolean, history: Boolean, cookies: Boolean) -> Unit,
  onSetGlobalSitePolicy: (key: String, value: String) -> Unit,
  onSetDomainSiteOverride: (domain: String, key: String, value: String) -> Unit,
  onResetDomainOverrides: (domain: String) -> Unit,
  onRunDohProbe: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val bottomInset =
    WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
  LazyColumn(
    modifier = modifier.padding(horizontal = BrowserSpacing.Grid2),
    contentPadding = PaddingValues(top = BrowserSpacing.Grid2, bottom = BrowserSpacing.Grid2 + bottomInset),
    verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid2),
  ) {
    when (pillar) {
      SettingsPillar.DESIGN -> {
        designSettingsItems(
          settings = settings,
          toolbarConfig = toolbarConfig,
          designConfig = designConfig,
          onUpdateSetting = onUpdateSetting,
          onUpdateDesignParam = onUpdateDesignParam,
          onResetDesignDefaults = onResetDesignDefaults,
          onSelectAlignment = onSelectAlignment,
          onToggleAutoMinimize = onToggleAutoMinimize,
        )
      }

      SettingsPillar.CUSTOMIZATION -> {
        item(key = "slot_editor") {
          SettingsGroupCard(title = "하단 바 슬롯 편집 (${ToolbarActionId.entries.size}종 동작)", icon = Icons.Outlined.Tune) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "최대 좌 3 · 우 3 (우측 끝 설정 고정)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              SpringPressSurface(
                onClick = onResetToolbar,
                shape = designConfig.cardInnerShape(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(Icons.Outlined.RestartAlt, contentDescription = "초기화", modifier = Modifier.size(16.dp))
                  Text("초기화", style = MaterialTheme.typography.labelMedium)
                }
              }
            }

            FrostedFloatingSurface(
              shape = designConfig.barOuterShape(),
              opacity = designConfig.barOpacity,
              blurRadiusDp = designConfig.barBlurRadiusDp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  toolbarConfig.leftSlots.forEach {
                    Icon(toolbarActionIcon(it), contentDescription = it.label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  }
                }
                Surface(
                  shape = designConfig.barInnerShape(),
                  color = MaterialTheme.colorScheme.background,
                  modifier = Modifier.weight(1f),
                ) {
                  Text(
                    text = "주소창 고정",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  toolbarConfig.rightSlots.forEach {
                    Icon(toolbarActionIcon(it), contentDescription = it.label, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                  }
                }
                Icon(Icons.Outlined.Settings, contentDescription = "고정 설정", modifier = Modifier.size(16.dp))
              }
            }

            Text("좌측 슬롯 (${toolbarConfig.leftSlots.size}/3)", style = MaterialTheme.typography.labelLarge)
            SlotReorderRow(
              slots = toolbarConfig.leftSlots,
              onMove = { from, to -> onMoveToolbarSlot("LEFT", from, to) },
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              ToolbarActionId.entries.forEach { action ->
                val selected = action in toolbarConfig.leftSlots
                FilterChip(
                  selected = selected,
                  onClick = {
                    val nextLeft =
                      if (selected) toolbarConfig.leftSlots - action
                      else (toolbarConfig.leftSlots + action).take(3)
                    val nextRight = toolbarConfig.rightSlots.filterNot { it in nextLeft }
                    onUpdateToolbarSlots(nextLeft, nextRight)
                  },
                  shape = designConfig.cardInnerShape(),
                  leadingIcon = {
                    Icon(toolbarActionIcon(action), contentDescription = null, modifier = Modifier.size(16.dp))
                  },
                  label = { Text(action.label, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Text("우측 슬롯 (${toolbarConfig.rightSlots.size}/3)", style = MaterialTheme.typography.labelLarge)
            SlotReorderRow(
              slots = toolbarConfig.rightSlots,
              onMove = { from, to -> onMoveToolbarSlot("RIGHT", from, to) },
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              ToolbarActionId.entries.forEach { action ->
                val selected = action in toolbarConfig.rightSlots
                FilterChip(
                  selected = selected,
                  onClick = {
                    val nextRight =
                      if (selected) toolbarConfig.rightSlots - action
                      else (toolbarConfig.rightSlots + action).take(3)
                    val nextLeft = toolbarConfig.leftSlots.filterNot { it in nextRight }
                    onUpdateToolbarSlots(nextLeft, nextRight)
                  },
                  shape = designConfig.cardInnerShape(),
                  leadingIcon = {
                    Icon(toolbarActionIcon(action), contentDescription = null, modifier = Modifier.size(16.dp))
                  },
                  label = { Text(action.label, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
        }

        item(key = "gestures") {
          SettingsGroupCard(title = "제스처 · 분할 · 폴더 동작", icon = Icons.Outlined.Swipe) {
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Splitscreen,
              title = "주소창 1초 길게 눌러 화면 분할",
              checked = settings.splitGestureEnabled,
              onCheckedChange = { onUpdateSetting("splitGestureEnabled", it.toString()) },
            )
            if (settings.splitGestureEnabled) {
              Text("화면 분할 드래그 감도", style = MaterialTheme.typography.labelMedium)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              ) {
                SplitGestureSensitivity.entries.forEach { sens ->
                  IconSegmentTile(
                    icon = Icons.Outlined.Splitscreen,
                    label = sens.label,
                    selected = settings.splitGestureSensitivity == sens,
                    onClick = { onUpdateSetting("splitGestureSensitivity", sens.id) },
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Swipe,
              title = "바 위로 스와이프하여 탭 목록 열기",
              checked = settings.swipeUpOpenTabs,
              onCheckedChange = { onUpdateSetting("swipeUpOpenTabs", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.DeleteOutline,
              title = "탭 카드 좌우 스와이프로 닫기",
              checked = settings.tabSwipeToCloseEnabled,
              onCheckedChange = { onUpdateSetting("tabSwipeToCloseEnabled", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Folder,
              title = "탭 드래그 · 0.5초 호버로 폴더 생성",
              checked = settings.tabDragFolderEnabled,
              onCheckedChange = { onUpdateSetting("tabDragFolderEnabled", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.VerticalAlignBottom,
              title = "스크롤 시 바 축소",
              checked = toolbarConfig.autoMinimize,
              onCheckedChange = onToggleAutoMinimize,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Swipe,
              title = "바 좌우 스와이프 탭 전환",
              checked = toolbarConfig.swipeBarSwitchTabs,
              onCheckedChange = { onSetGestures(it, toolbarConfig.pullDownRefresh) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Refresh,
              title = "당겨서 새로고침",
              checked = toolbarConfig.pullDownRefresh,
              onCheckedChange = { onSetGestures(toolbarConfig.swipeBarSwitchTabs, it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Edit,
              title = "주소 편집 시 바 확장 표시",
              checked = settings.expandBarOnUrlEdit,
              onCheckedChange = { onUpdateSetting("expandBarOnUrlEdit", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              Icon(
                imageVector = Icons.Outlined.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text("탭창 기본 정렬 방식", style = MaterialTheme.typography.labelLarge)
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              TabSortMode.entries.forEach { mode ->
                val modeIcon =
                  when (mode) {
                    TabSortMode.RECENT -> Icons.Outlined.Schedule
                    TabSortMode.CREATED -> Icons.Outlined.AddCircleOutline
                    TabSortMode.DOMAIN -> Icons.Outlined.Public
                    TabSortMode.FOLDERS_FIRST -> Icons.Outlined.Folder
                    TabSortMode.PINNED_FIRST -> Icons.Outlined.PushPin
                  }
                FilterChip(
                  selected = settings.defaultTabSortMode == mode,
                  onClick = { onUpdateSetting("defaultTabSortMode", mode.id) },
                  shape = designConfig.cardInnerShape(),
                  leadingIcon = {
                    Icon(modeIcon, contentDescription = null, modifier = Modifier.size(15.dp))
                  },
                  label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                  modifier = Modifier.testTag("settings_default_sort_${mode.id.lowercase()}"),
                )
              }
            }
          }
        }

        item(key = "search_engine") {
          var customUrlDraft by remember(settings.customSearchUrl) {
            mutableStateOf(settings.customSearchUrl)
          }
          SettingsGroupCard(title = "기본 검색엔진", icon = Icons.Outlined.Search) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              searchEngines.forEach { engine ->
                FilterChip(
                  selected = settings.searchEngineId == engine.id,
                  onClick = { onUpdateSetting("searchEngineId", engine.id) },
                  shape = designConfig.cardInnerShape(),
                  leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                  },
                  label = { Text(engine.label) },
                )
              }
            }
            if (settings.searchEngineId == "custom") {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = customUrlDraft,
                  onValueChange = { customUrlDraft = it },
                  singleLine = true,
                  shape = designConfig.cardInnerShape(),
                  label = { Text("사용자 지정 검색 URL (%s 포함)") },
                  modifier = Modifier.weight(1f),
                )
                Button(
                  onClick = { onUpdateSetting("customSearchUrl", customUrlDraft) },
                  enabled = customUrlDraft.contains("%s"),
                  shape = designConfig.cardInnerShape(),
                ) {
                  Text("저장")
                }
              }
            }
          }
        }
      }

      SettingsPillar.GENERAL_PRIVACY -> {
        item(key = "ad_blocker") {
          SettingsGroupCard(title = "프라이버시 · 추적 차단", icon = Icons.Outlined.Shield) {
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Shield,
              title = "광고 · 추적기 차단 (${blockerStats.ruleCount}개 규칙)",
              checked = settings.trackerBlockingEnabled,
              onCheckedChange = { onUpdateSetting("trackerBlockingEnabled", it.toString()) },
            )
            if (settings.trackerBlockingEnabled) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              ) {
                BlockerLevel.entries.forEach { lvl ->
                  FilterChip(
                    selected = settings.blockerLevel == lvl,
                    onClick = { onUpdateSetting("blockerLevel", lvl.id) },
                    shape = designConfig.cardInnerShape(),
                    label = { Text("차단 강도 · ${lvl.label}") },
                  )
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              MiniStatBox("광고", blockerStats.adsBlocked, Modifier.weight(1f))
              MiniStatBox("추적", blockerStats.trackersBlocked, Modifier.weight(1f))
              MiniStatBox("지문", blockerStats.fingerprintingBlocked, Modifier.weight(1f))
              MiniStatBox("원격", blockerStats.telemetryBlocked, Modifier.weight(1f))
            }
            OutlinedButton(
              onClick = onUpdateFilterList,
              shape = designConfig.cardInnerShape(),
            ) {
              Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text("필터 목록 업데이트")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Cookie,
              title = "서드파티 쿠키 차단",
              checked = settings.blockThirdPartyCookies,
              onCheckedChange = { onUpdateSetting("blockThirdPartyCookies", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.PrivacyTip,
              title = "DNT / Sec-GPC 추적 거부 헤더",
              checked = settings.sendDntGpcHeaders,
              onCheckedChange = { onUpdateSetting("sendDntGpcHeaders", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Lock,
              title = "HTTPS 전용 모드",
              checked = settings.httpsOnlyMode,
              onCheckedChange = { onUpdateSetting("httpsOnlyMode", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.OpenInNew,
              title = "외부 앱 실행 전 확인",
              checked = settings.confirmExternalApp,
              onCheckedChange = { onUpdateSetting("confirmExternalApp", it.toString()) },
            )
          }
        }

        item(key = "doh_network") {
          var probeHost by remember { mutableStateOf("duckduckgo.com") }
          var customDohDraft by remember(settings.customDohUrl) {
            mutableStateOf(settings.customDohUrl)
          }
          SettingsGroupCard(title = "보안 DNS (DNS-over-HTTPS)", icon = Icons.Outlined.Dns) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              dohProviders.forEach { provider ->
                FilterChip(
                  selected = settings.dohProviderId == provider.id,
                  onClick = { onUpdateSetting("dohProviderId", provider.id) },
                  shape = designConfig.cardInnerShape(),
                  label = { Text(provider.label) },
                )
              }
            }
            if (settings.dohProviderId == "custom") {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = customDohDraft,
                  onValueChange = { customDohDraft = it },
                  singleLine = true,
                  shape = designConfig.cardInnerShape(),
                  label = { Text("사용자 지정 DoH 엔드포인트 (https://...)") },
                  modifier = Modifier.weight(1f),
                )
                Button(
                  onClick = { onUpdateSetting("customDohUrl", customDohDraft) },
                  enabled = customDohDraft.startsWith("https://"),
                  shape = designConfig.cardInnerShape(),
                ) {
                  Text("저장")
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = probeHost,
                onValueChange = { probeHost = it },
                singleLine = true,
                shape = designConfig.cardInnerShape(),
                label = { Text("테스트 호스트명") },
                modifier = Modifier.weight(1f),
              )
              Button(
                onClick = { onRunDohProbe(probeHost) },
                enabled = !isProbingDoh,
                shape = designConfig.cardInnerShape(),
                modifier = Modifier.height(52.dp).testTag("doh_probe_button"),
              ) {
                if (isProbingDoh) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                  Text("조회")
                }
              }
            }
            lastDohLookup?.let { res ->
              Surface(
                shape = designConfig.cardInnerShape(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(BrowserSpacing.Grid1_5),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    "${res.providerLabel} · ${res.latencyMs}ms · TX ${res.queryBytes}B / RX ${res.responseBytes}B",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                  if (res.ok && res.records.isNotEmpty()) {
                    res.records.forEach { rec ->
                      Text(
                        "${rec.type} ${rec.address} (${rec.ttlSeconds}s)",
                        style = MaterialTheme.typography.labelMedium,
                      )
                    }
                  } else {
                    Text(
                      res.errorMessage ?: "응답 없음",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }
          }
        }

        item(key = "startup_storage") {
          val context = LocalContext.current
          val safLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
              if (uri != null) {
                try {
                  context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                  )
                } catch (_: Exception) {
                }
                onUpdateSetting("downloadTreeUri", uri.toString())
              }
            }

          SettingsGroupCard(title = "시작 · 탭 관리 · 다운로드", icon = Icons.Outlined.FolderOpen) {
            Text("앱 시작 시 동작", style = MaterialTheme.typography.labelLarge)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              StartupBehavior.entries.forEach { behavior ->
                FilterChip(
                  selected = settings.startupBehavior == behavior,
                  onClick = { onUpdateSetting("startupBehavior", behavior.id) },
                  shape = designConfig.cardInnerShape(),
                  label = { Text(behavior.label) },
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Tab,
              title = "백그라운드 탭 일시정지 (메모리 절약)",
              checked = settings.backgroundTabSleep,
              onCheckedChange = { onUpdateSetting("backgroundTabSleep", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Text("비활성 탭 자동 정리", style = MaterialTheme.typography.labelLarge)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
            ) {
              val dayOptions = listOf(0 to "안 함", 1 to "1일", 7 to "7일", 30 to "30일")
              dayOptions.forEach { (days, label) ->
                FilterChip(
                  selected = settings.autoCloseDays == days,
                  onClick = { onUpdateSetting("autoCloseDays", days.toString()) },
                  shape = designConfig.cardInnerShape(),
                  label = { Text(label) },
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = BrowserSpacing.Grid1)) {
                Text("다운로드 저장 위치 (SAF)", style = MaterialTheme.typography.labelLarge)
                Text(
                  text = settings.downloadTreeUri.ifBlank { "시스템 Downloads 기본 폴더" },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = { safLauncher.launch(null) },
                shape = designConfig.cardInnerShape(),
              ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("폴더 선택")
              }
            }
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Download,
              title = "파일 다운로드 전 확인",
              checked = settings.confirmBeforeDownload,
              onCheckedChange = { onUpdateSetting("confirmBeforeDownload", it.toString()) },
            )
          }
        }

        item(key = "clear_data") {
          var clearCache by remember { mutableStateOf(true) }
          var clearHistory by remember { mutableStateOf(false) }
          var clearCookies by remember { mutableStateOf(true) }

          SettingsGroupCard(title = "인터넷 사용 기록 삭제", icon = Icons.Outlined.DeleteOutline) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                Text("캐시", style = MaterialTheme.typography.labelLarge)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                Text("쿠키", style = MaterialTheme.typography.labelLarge)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it })
                Text("방문 기록", style = MaterialTheme.typography.labelLarge)
              }
            }
            Button(
              onClick = { onClearBrowsingData(clearCache, clearHistory, clearCookies) },
              enabled = clearCache || clearHistory || clearCookies,
              shape = designConfig.cardInnerShape(),
            ) {
              Text("선택 항목 삭제")
            }
          }
        }
      }

      SettingsPillar.SITE_SETTINGS -> {
        val global = sitePermissions.globalPolicy
        item(key = "global_web") {
          SettingsGroupCard(title = "기본 웹 콘텐츠", icon = Icons.Outlined.WebAsset) {
            SettingsIconSwitchRow(
              icon = Icons.Outlined.Javascript,
              title = "자바스크립트 허용",
              checked = global.javascriptEnabled,
              onCheckedChange = { onSetGlobalSitePolicy("javascriptEnabled", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.OpenInNew,
              title = "팝업 · 리디렉션 허용",
              checked = global.popupsAllowed,
              onCheckedChange = { onSetGlobalSitePolicy("popupsAllowed", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.DesktopWindows,
              title = "기본 데스크톱(PC) 모드",
              checked = global.desktopSite,
              onCheckedChange = { onSetGlobalSitePolicy("desktopSite", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.HideImage,
              title = "이미지 로드 차단 (데이터 절약)",
              checked = global.blockImages,
              onCheckedChange = { onSetGlobalSitePolicy("blockImages", it.toString()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.PlayCircleOutline,
              title = "미디어 자동 재생 허용",
              checked = global.autoplayMedia == "ALLOW",
              onCheckedChange = {
                onSetGlobalSitePolicy("autoplayMedia", if (it) "ALLOW" else "BLOCK")
              },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            SettingsIconSwitchRow(
              icon = Icons.Outlined.ZoomIn,
              title = "사이트 강제 확대·축소",
              checked = settings.forceZoomEnabled,
              onCheckedChange = { onUpdateSetting("forceZoomEnabled", it.toString()) },
            )
          }
        }

        item(key = "hardware_permissions") {
          SettingsGroupCard(title = "AOSP 기기 권한 정책", icon = Icons.Outlined.Security) {
            PolicyDualToggleRow(
              icon = Icons.Outlined.CameraAlt,
              label = "카메라",
              current = global.cameraPolicy,
              onSelect = { onSetGlobalSitePolicy("cameraPolicy", it) },
            )
            PolicyDualToggleRow(
              icon = Icons.Outlined.Mic,
              label = "마이크",
              current = global.microphonePolicy,
              onSelect = { onSetGlobalSitePolicy("microphonePolicy", it) },
            )
            PolicyDualToggleRow(
              icon = Icons.Outlined.LocationOn,
              label = "위치 정보",
              current = global.locationPolicy,
              onSelect = { onSetGlobalSitePolicy("locationPolicy", it) },
            )
            PolicyDualToggleRow(
              icon = Icons.Outlined.ContentPaste,
              label = "클립보드 읽기",
              current = global.clipboardPolicy,
              onSelect = { onSetGlobalSitePolicy("clipboardPolicy", it) },
            )
            PolicyDualToggleRow(
              icon = Icons.Outlined.Notifications,
              label = "웹 알림",
              current = global.notificationsPolicy,
              onSelect = { onSetGlobalSitePolicy("notificationsPolicy", it) },
            )
            PolicyDualToggleRow(
              icon = Icons.Outlined.Sensors,
              label = "모션 · 방향 센서",
              current = global.sensorsPolicy,
              onSelect = { onSetGlobalSitePolicy("sensorsPolicy", it) },
            )
          }
        }

        item(key = "domain_exceptions") {
          var domainQuery by remember { mutableStateOf("") }
          var newDomainInput by remember(activeTabDomain) {
            mutableStateOf(
              activeTabDomain?.takeUnless { it.startsWith("about:") } ?: "example.org"
            )
          }

          SettingsGroupCard(title = "도메인별 개별 예외 규칙", icon = Icons.Outlined.Public) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = newDomainInput,
                onValueChange = { newDomainInput = it },
                singleLine = true,
                shape = designConfig.cardInnerShape(),
                label = { Text("도메인 (예: example.org)") },
                modifier = Modifier.weight(1f),
              )
              OutlinedButton(
                onClick = {
                  onSetDomainSiteOverride(newDomainInput, "javascriptEnabled", "true")
                },
                enabled = newDomainInput.contains('.'),
                shape = designConfig.cardInnerShape(),
              ) {
                Text("추가")
              }
            }

            if (sitePermissions.domainOverrides.isNotEmpty()) {
              OutlinedTextField(
                value = domainQuery,
                onValueChange = { domainQuery = it },
                singleLine = true,
                shape = designConfig.cardInnerShape(),
                label = { Text("예외 도메인 검색") },
                modifier = Modifier.fillMaxWidth(),
              )

              val visible =
                remember(sitePermissions.domainOverrides, domainQuery) {
                  sitePermissions.domainOverrides.filter {
                    domainQuery.isBlank() ||
                      it.domain.contains(domainQuery.trim(), ignoreCase = true)
                  }
                }

              visible.forEach { item ->
                Surface(
                  shape = designConfig.cardInnerShape(),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(BrowserSpacing.Grid1_5),
                    verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(item.domain, style = MaterialTheme.typography.labelLarge)
                      SpringIconButton(
                        onClick = { onResetDomainOverrides(item.domain) },
                        modifier = Modifier.size(36.dp),
                      ) {
                        Icon(
                          Icons.Outlined.DeleteOutline,
                          contentDescription = "${item.domain} 삭제",
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
                      FilterChip(
                        selected = item.policy.javascriptEnabled,
                        onClick = {
                          onSetDomainSiteOverride(
                            item.domain,
                            "javascriptEnabled",
                            (!item.policy.javascriptEnabled).toString(),
                          )
                        },
                        shape = designConfig.cardInnerShape(),
                        label = { Text("JS ${if (item.policy.javascriptEnabled) "켜짐" else "꺼짐"}") },
                      )
                      FilterChip(
                        selected = item.policy.desktopSite,
                        onClick = {
                          onSetDomainSiteOverride(
                            item.domain,
                            "desktopSite",
                            (!item.policy.desktopSite).toString(),
                          )
                        },
                        shape = designConfig.cardInnerShape(),
                        label = { Text("PC 모드 ${if (item.policy.desktopSite) "켜짐" else "꺼짐"}") },
                      )
                      FilterChip(
                        selected = item.policy.blockImages,
                        onClick = {
                          onSetDomainSiteOverride(
                            item.domain,
                            "blockImages",
                            (!item.policy.blockImages).toString(),
                          )
                        },
                        shape = designConfig.cardInnerShape(),
                        label = { Text("이미지 ${if (item.policy.blockImages) "차단" else "허용"}") },
                      )
                      FilterChip(
                        selected = item.policy.locationPolicy == "ALLOW",
                        onClick = {
                          val next = if (item.policy.locationPolicy == "ALLOW") "BLOCK" else "ALLOW"
                          onSetDomainSiteOverride(item.domain, "locationPolicy", next)
                        },
                        shape = designConfig.cardInnerShape(),
                        label = {
                          Text("위치 ${if (item.policy.locationPolicy == "ALLOW") "허용" else "차단"}")
                        },
                      )
                    }
                  }
                }
              }
            } else {
              Text(
                text = "등록된 예외 도메인 없음",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun SettingsGroupCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = LocalDesignConfig.current.cardOuterShape(),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(BrowserSpacing.Grid2),
      verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      content()
    }
  }
}

@Composable
internal fun IconSegmentTile(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SpringPressSurface(
    onClick = onClick,
    selected = selected,
    shape = LocalDesignConfig.current.cardInnerShape(),
    modifier = modifier.height(64.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
internal fun SettingsIconSwitchRow(
  icon: ImageVector,
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f).padding(end = BrowserSpacing.Grid1_5),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
    ) {
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(LocalDesignConfig.current.iconShape())
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(title, style = MaterialTheme.typography.labelLarge)
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun SlotReorderRow(
  slots: List<ToolbarActionId>,
  onMove: (fromIdx: Int, toIdx: Int) -> Unit,
) {
  val design = LocalDesignConfig.current
  LazyRow(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
    items(slots.size, key = { idx -> "${slots[idx].id}_$idx" }) { index ->
      val action = slots[index]
      Surface(
        shape = design.cardInnerShape(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SpringIconButton(
            onClick = { onMove(index, index - 1) },
            enabled = index > 0,
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
              contentDescription = "왼쪽으로 이동",
              modifier = Modifier.size(16.dp),
            )
          }
          Icon(
            toolbarActionIcon(action),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.size(4.dp))
          Text(action.label, style = MaterialTheme.typography.labelSmall)
          SpringIconButton(
            onClick = { onMove(index, index + 1) },
            enabled = index < slots.lastIndex,
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              Icons.AutoMirrored.Outlined.KeyboardArrowRight,
              contentDescription = "오른쪽으로 이동",
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PolicyDualToggleRow(
  icon: ImageVector,
  label: String,
  current: String,
  onSelect: (String) -> Unit,
) {
  val design = LocalDesignConfig.current
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1)) {
      FilterChip(
        selected = current == "ASK",
        onClick = { onSelect("ASK") },
        shape = design.cardInnerShape(),
        label = { Text("물어보기", style = MaterialTheme.typography.labelSmall) },
      )
      FilterChip(
        selected = current == "BLOCK",
        onClick = { onSelect("BLOCK") },
        shape = design.cardInnerShape(),
        label = { Text("차단", style = MaterialTheme.typography.labelSmall) },
      )
    }
  }
}

@Composable
private fun MiniStatBox(label: String, count: Long, modifier: Modifier = Modifier) {
  Surface(
    shape = LocalDesignConfig.current.cardInnerShape(),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(count.toString(), style = MaterialTheme.typography.titleMedium)
      Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun toolbarActionIcon(action: ToolbarActionId): ImageVector =
  when (action) {
    ToolbarActionId.BACK -> Icons.AutoMirrored.Outlined.ArrowBack
    ToolbarActionId.FORWARD -> Icons.AutoMirrored.Outlined.ArrowForward
    ToolbarActionId.REFRESH -> Icons.Outlined.Refresh
    ToolbarActionId.BOOKMARK -> Icons.Outlined.Bookmark
    ToolbarActionId.HISTORY -> Icons.Outlined.History
    ToolbarActionId.DESKTOP_MODE -> Icons.Outlined.DesktopWindows
    ToolbarActionId.SHARE -> Icons.Outlined.Share
    ToolbarActionId.QR_SCAN -> Icons.Outlined.QrCodeScanner
    ToolbarActionId.SCREENSHOT -> Icons.Outlined.CameraAlt
    ToolbarActionId.READER_MODE -> Icons.Outlined.ChromeReaderMode
    ToolbarActionId.HOME -> Icons.Outlined.Home
    ToolbarActionId.TABS -> Icons.Outlined.Tab
    ToolbarActionId.NEW_TAB -> Icons.Outlined.Add
    ToolbarActionId.SPLIT_VIEW -> Icons.Outlined.Splitscreen
    ToolbarActionId.FIND_IN_PAGE -> Icons.Outlined.FindInPage
    ToolbarActionId.LIBRARY -> Icons.Outlined.Bookmarks
  }
