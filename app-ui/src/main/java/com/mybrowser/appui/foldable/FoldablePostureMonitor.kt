package com.mybrowser.appui.foldable

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.flow.catch

enum class BrowserPostureMode(val badgeLabel: String) {
  COVER_COMPACT("커버"),
  PASSPORT_FLAT("1:1.4 펼침"),
  BOOK_HALF_OPENED("북 모드"),
  TABLETOP_HALF_OPENED("테이블탑"),
}

@Immutable
data class FoldablePostureState(
  val mode: BrowserPostureMode = BrowserPostureMode.COVER_COMPACT,
  val hasFoldFeature: Boolean = false,
  val isSeparating: Boolean = false,
  val isHorizontalFold: Boolean = false,
  val hingeTopDp: Dp = 0.dp,
  val hingeBottomDp: Dp = 0.dp,
  val hingeLeftDp: Dp = 0.dp,
  val hingeRightDp: Dp = 0.dp,
  val hingeWidthDp: Dp = 0.dp,
  val hingeHeightDp: Dp = 0.dp,
  val windowWidthDp: Dp = 400.dp,
  val windowHeightDp: Dp = 800.dp,
  val aspectRatio: Float = 0.5f,
) {
  val supportsDualPane: Boolean
    get() =
      mode == BrowserPostureMode.PASSPORT_FLAT ||
        mode == BrowserPostureMode.BOOK_HALF_OPENED ||
        windowWidthDp >= 560.dp

  val isPassportGeometry: Boolean
    get() = windowWidthDp >= 520.dp && aspectRatio in 0.62f..1.60f
}

@Composable
fun rememberFoldablePostureState(
  availableWidthDp: Dp,
  availableHeightDp: Dp,
): FoldablePostureState {
  val context = LocalContext.current
  val density = LocalDensity.current
  val activity = context.findActivity()

  val layoutInfo by
    produceState<WindowLayoutInfo?>(initialValue = null, key1 = activity) {
      if (activity != null) {
        WindowInfoTracker.getOrCreate(activity)
          .windowLayoutInfo(activity)
          .catch { /* Fallback gracefully on non-foldable AOSP builds */ }
          .collect { value = it }
      }
    }

  val foldingFeature =
    layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()

  val aspectRatio =
    if (availableHeightDp.value > 0f) availableWidthDp.value / availableHeightDp.value else 1f

  if (foldingFeature != null) {
    val bounds = foldingFeature.bounds
    val hingeLeftDp = with(density) { bounds.left.toDp() }
    val hingeTopDp = with(density) { bounds.top.toDp() }
    val hingeRightDp = with(density) { bounds.right.toDp() }
    val hingeBottomDp = with(density) { bounds.bottom.toDp() }
    val hingeWidthDp = with(density) { bounds.width().toDp() }
    val hingeHeightDp = with(density) { bounds.height().toDp() }
    val isHorizontal = foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL

    val mode =
      when {
        foldingFeature.state == FoldingFeature.State.HALF_OPENED && isHorizontal ->
          BrowserPostureMode.TABLETOP_HALF_OPENED
        foldingFeature.state == FoldingFeature.State.HALF_OPENED && !isHorizontal ->
          BrowserPostureMode.BOOK_HALF_OPENED
        availableWidthDp >= 520.dp -> BrowserPostureMode.PASSPORT_FLAT
        else -> BrowserPostureMode.COVER_COMPACT
      }

    return FoldablePostureState(
      mode = mode,
      hasFoldFeature = true,
      isSeparating = foldingFeature.isSeparating,
      isHorizontalFold = isHorizontal,
      hingeTopDp = hingeTopDp,
      hingeBottomDp = hingeBottomDp,
      hingeLeftDp = hingeLeftDp,
      hingeRightDp = hingeRightDp,
      hingeWidthDp = hingeWidthDp,
      hingeHeightDp = hingeHeightDp,
      windowWidthDp = availableWidthDp,
      windowHeightDp = availableHeightDp,
      aspectRatio = aspectRatio,
    )
  }

  // Fallback geometry detection for 1:1.4 Passport inner screen or unfolded tablets without Hinge HAL
  val mode =
    if (availableWidthDp >= 520.dp) {
      BrowserPostureMode.PASSPORT_FLAT
    } else {
      BrowserPostureMode.COVER_COMPACT
    }

  return FoldablePostureState(
    mode = mode,
    hasFoldFeature = false,
    isSeparating = false,
    isHorizontalFold = false,
    windowWidthDp = availableWidthDp,
    windowHeightDp = availableHeightDp,
    aspectRatio = aspectRatio,
  )
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
