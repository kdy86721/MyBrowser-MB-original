package com.mybrowser.appui.ui

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Spatial origin captured via Modifier.onGloballyPositioned(boundsInRoot())
 * on clickable triggers (Settings button, Category cards, Tab grid cards, Bookmarks).
 */
@Immutable
data class SpatialDiveOrigin(
  val boundsInRoot: Rect = Rect.Zero,
  val cornerRadiusDp: Float = 20f,
) {
  fun isValid(): Boolean = boundsInRoot.width > 1f && boundsInRoot.height > 1f
}

fun computeSpatialDiveCornerDp(
  originCornerDp: Float,
  barCornerRadiusDp: Float,
  progress: Float,
): Float {
  val p = progress.coerceIn(0f, 1f)
  val start = originCornerDp.coerceIn(0f, 36f)
  val target = barCornerRadiusDp.coerceIn(0f, 36f)
  return lerpFloat(start, target, p)
}

fun computeSpatialDiveBackgroundCornerDp(
  barCornerRadiusDp: Float,
  progress: Float,
): Float {
  val p = progress.coerceIn(0f, 1f)
  if (p <= 0.002f) return 0f
  val target = barCornerRadiusDp.coerceIn(0f, 36f)
  val ramp = (p * 4f).coerceIn(0f, 1f)
  return target * ramp
}

/**
 * Multi-layer Spatial Dive-In background choreography:
 * - Clips all 4 screen corners synced in real time with DesignConfig.barCornerRadiusDp
 * - Scales departing layer outward 1.0f -> 1.14f anchored at originRect.center
 * - Progressive backdrop blur 0.dp -> 26.dp (API 31+ RenderEffect-backed)
 */
@Composable
fun Modifier.spatialDiveBackground(
  progress: Float,
  origin: SpatialDiveOrigin,
  hostSize: IntSize,
  hostTopLeftInRoot: Offset = Offset.Zero,
  targetCornerRadiusDp: Float = LocalDesignConfig.current.barCornerRadiusDp,
): Modifier {
  val p = progress.coerceIn(0f, 1f)
  if (p <= 0.002f) return this

  val hostW = hostSize.width.toFloat().coerceAtLeast(1f)
  val hostH = hostSize.height.toFloat().coerceAtLeast(1f)
  val pivotX =
    if (origin.isValid()) {
      ((origin.boundsInRoot.center.x - hostTopLeftInRoot.x) / hostW).coerceIn(0.05f, 0.95f)
    } else {
      0.5f
    }
  val pivotY =
    if (origin.isValid()) {
      ((origin.boundsInRoot.center.y - hostTopLeftInRoot.y) / hostH).coerceIn(0.05f, 0.95f)
    } else {
      0.5f
    }

  val bgCornerDp = computeSpatialDiveBackgroundCornerDp(targetCornerRadiusDp, p).dp
  val bgCornerShape = RoundedCornerShape(bgCornerDp)

  val blurModifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val blurDp = (26f * p).coerceIn(0f, 26f).dp
      if (blurDp > 0.2.dp) Modifier.blur(blurDp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
      else Modifier
    } else {
      Modifier
    }

  return this
    .clip(bgCornerShape)
    .graphicsLayer {
      val scale = 1f + 0.14f * p
      scaleX = scale
      scaleY = scale
      transformOrigin = TransformOrigin(pivotX, pivotY)
    }
    .then(blurModifier)
}

/**
 * Dark progressive scrim layer paired with spatialDiveBackground (0f -> 0.45f).
 */
@Composable
fun SpatialDiveScrim(
  progress: Float,
  modifier: Modifier = Modifier,
) {
  val p = progress.coerceIn(0f, 1f)
  if (p > 0.002f) {
    Box(
      modifier =
        modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.45f * p))
    )
  }
}

/**
 * Foreground spatial expansion card:
 * Expands directly from origin.boundsInRoot -> full host bounds while morphing all 4 clip corners
 * from origin.cornerRadiusDp -> DesignConfig.barCornerRadiusDp in real time to harmonize with
 * smartphone display bezels, and fading content smoothly across 0.15f..0.65f.
 * Measures inner content at full host constraints to preserve 120Hz fluidity without re-layout thrashing.
 */
@Composable
fun SpatialDiveForegroundHost(
  progress: Float,
  origin: SpatialDiveOrigin,
  hostSize: IntSize,
  hostTopLeftInRoot: Offset = Offset.Zero,
  surfaceColor: Color = MaterialTheme.colorScheme.background,
  targetCornerRadiusDp: Float = LocalDesignConfig.current.barCornerRadiusDp,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val p = progress.coerceIn(0f, 1f)
  if (p <= 0.002f) return

  val density = LocalDensity.current
  val hostWInt = hostSize.width.coerceAtLeast(1)
  val hostHInt = hostSize.height.coerceAtLeast(1)
  val hostW = hostWInt.toFloat()
  val hostH = hostHInt.toFloat()

  val localOrigin =
    if (origin.isValid()) {
      Rect(
        left = (origin.boundsInRoot.left - hostTopLeftInRoot.x).coerceIn(0f, hostW - 1f),
        top = (origin.boundsInRoot.top - hostTopLeftInRoot.y).coerceIn(0f, hostH - 1f),
        right = (origin.boundsInRoot.right - hostTopLeftInRoot.x).coerceIn(1f, hostW),
        bottom = (origin.boundsInRoot.bottom - hostTopLeftInRoot.y).coerceIn(1f, hostH),
      )
    } else {
      Rect(
        left = hostW * 0.25f,
        top = hostH * 0.25f,
        right = hostW * 0.75f,
        bottom = hostH * 0.75f,
      )
    }

  val curLeft = lerpFloat(localOrigin.left, 0f, p)
  val curTop = lerpFloat(localOrigin.top, 0f, p)
  val curRight = lerpFloat(localOrigin.right, hostW, p)
  val curBottom = lerpFloat(localOrigin.bottom, hostH, p)
  val curW = (curRight - curLeft).coerceAtLeast(1f)
  val curH = (curBottom - curTop).coerceAtLeast(1f)

  val startCornerDp = if (origin.isValid()) origin.cornerRadiusDp else targetCornerRadiusDp
  val cornerDp = computeSpatialDiveCornerDp(startCornerDp, targetCornerRadiusDp, p).dp
  val contentAlpha = ((p - 0.15f) / 0.50f).coerceIn(0f, 1f)
  val settled = p >= 0.996f

  Box(modifier = modifier.fillMaxSize()) {
    if (settled) {
      Box(
        modifier = Modifier.fillMaxSize().background(surfaceColor),
        content = content,
      )
    } else {
      val cardShape = RoundedCornerShape(cornerDp)
      Box(
        modifier =
          Modifier
            .offset { IntOffset(curLeft.roundToInt(), curTop.roundToInt()) }
            .size(with(density) { curW.toDp() }, with(density) { curH.toDp() })
            .shadow(
              elevation = (18f * (1f - p * 0.4f)).coerceAtLeast(4f).dp,
              shape = cardShape,
              clip = false,
            )
            .clip(cardShape)
            .background(surfaceColor),
      ) {
        Box(
          modifier =
            Modifier
              .layout { measurable, _ ->
                val fixed = Constraints.fixed(hostWInt, hostHInt)
                val placeable = measurable.measure(fixed)
                val boxW = curW.roundToInt().coerceAtLeast(1)
                val boxH = curH.roundToInt().coerceAtLeast(1)
                layout(boxW, boxH) {
                  placeable.place(
                    ((boxW - hostWInt) * 0.5f).roundToInt(),
                    ((boxH - hostHInt) * 0.5f).roundToInt(),
                  )
                }
              }
              .graphicsLayer {
                alpha = contentAlpha
                val s = 0.84f + 0.16f * p
                scaleX = s
                scaleY = s
              },
          content = content,
        )
      }
    }
  }
}

/**
 * Clockwise perimeter-tracing loading stroke along the outer contour of the Floating Bar,
 * starting at the exact top-center edge and curving smoothly around RoundedCornerShape corners.
 * Fades alpha (1f -> 0f) and stroke width (strokeWidthDp -> 0.dp) via ExpressiveSprings.micro()
 * when loading finishes. Works identically in both expanded and compact pill states.
 */
@Composable
fun Modifier.floatingBarPerimeterProgress(
  isActive: Boolean,
  progressFraction: Float,
  cornerRadiusDp: Dp,
  accentColor: Color,
  strokeWidthDp: Dp = 2.5.dp,
): Modifier {
  var lastNonZeroProgress by remember { mutableFloatStateOf(0f) }
  if (isActive) {
    lastNonZeroProgress = progressFraction.coerceIn(0.04f, 1f)
  }

  val speedScale = LocalAnimationSpeedScale.current
  val targetProgress = if (isActive) progressFraction.coerceIn(0.04f, 1f) else 1f
  val animatedProgress by
    animateFloatAsState(
      targetValue = targetProgress,
      animationSpec = ExpressiveSprings.micro(speedScale),
      label = "PerimeterProgressFraction",
    )

  val activeVisible = isActive && progressFraction < 1f
  val strokeAlpha by
    animateFloatAsState(
      targetValue = if (activeVisible) 1f else 0f,
      animationSpec = ExpressiveSprings.micro(speedScale),
      label = "PerimeterStrokeAlpha",
    )
  val animatedStrokeWidth by
    animateDpAsState(
      targetValue = if (activeVisible) strokeWidthDp else 0.dp,
      animationSpec = ExpressiveSprings.micro(speedScale),
      label = "PerimeterStrokeWidth",
    )

  val fullPath = remember { Path() }
  val segmentPath = remember { Path() }
  val pathMeasure = remember { PathMeasure() }

  return this.drawWithContent {
    drawContent()

    val visAlpha = strokeAlpha.coerceIn(0f, 1f)
    val sw = animatedStrokeWidth.toPx()
    if (visAlpha > 0.008f && sw > 0.15f && size.width > 4f && size.height > 4f) {
      val w = size.width
      val h = size.height
      val inset = sw * 0.5f
      val r = cornerRadiusDp.toPx().coerceIn(0f, minOf((w - sw) * 0.5f, (h - sw) * 0.5f))

      fullPath.reset()
      fullPath.moveTo(w * 0.5f, inset)
      fullPath.lineTo(w - inset - r, inset)
      if (r > 0f) {
        fullPath.arcTo(
          rect = Rect(w - inset - 2f * r, inset, w - inset, inset + 2f * r),
          startAngleDegrees = -90f,
          sweepAngleDegrees = 90f,
          forceMoveTo = false,
        )
      }
      fullPath.lineTo(w - inset, h - inset - r)
      if (r > 0f) {
        fullPath.arcTo(
          rect = Rect(w - inset - 2f * r, h - inset - 2f * r, w - inset, h - inset),
          startAngleDegrees = 0f,
          sweepAngleDegrees = 90f,
          forceMoveTo = false,
        )
      }
      fullPath.lineTo(inset + r, h - inset)
      if (r > 0f) {
        fullPath.arcTo(
          rect = Rect(inset, h - inset - 2f * r, inset + 2f * r, h - inset),
          startAngleDegrees = 90f,
          sweepAngleDegrees = 90f,
          forceMoveTo = false,
        )
      }
      fullPath.lineTo(inset, inset + r)
      if (r > 0f) {
        fullPath.arcTo(
          rect = Rect(inset, inset, inset + 2f * r, inset + 2f * r),
          startAngleDegrees = 180f,
          sweepAngleDegrees = 90f,
          forceMoveTo = false,
        )
      }
      fullPath.lineTo(w * 0.5f, inset)

      pathMeasure.setPath(fullPath, false)
      val totalLength = pathMeasure.length
      val effectiveFraction =
        if (isActive) animatedProgress.coerceIn(0.02f, 1f)
        else maxOf(lastNonZeroProgress, animatedProgress).coerceIn(0.02f, 1f)
      val drawLen = (totalLength * effectiveFraction).coerceAtLeast(0f)

      segmentPath.reset()
      if (drawLen > 0.5f && pathMeasure.getSegment(0f, drawLen, segmentPath, true)) {
        drawPath(
          path = segmentPath,
          color = accentColor.copy(alpha = 0.34f * visAlpha),
          style = Stroke(width = sw * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
          path = segmentPath,
          color = accentColor.copy(alpha = visAlpha),
          style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }
    }
  }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
  start + (stop - start) * fraction
