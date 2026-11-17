package com.mybrowser.appui.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Live User-Customizable Visual Geometry & Opacity Tokens.
 * Every field maps 1:1 to Compose Modifiers across the browser chrome.
 */
@Immutable
data class DesignUiConfig(
  val iconCornerRadiusDp: Float = 22f,     // 0f..24f
  val barCornerRadiusDp: Float = 28f,      // 0f..36f
  val cardCornerRadiusDp: Float = 20f,     // 8f..28f
  val barHeightDp: Float = 56f,            // 44f..68f
  val barWidthFraction: Float = 0.94f,     // 0.60f..1.00f
  val barHorizontalBias: Float = 0.0f,     // -1.0f..1.0f
  val barBottomOffsetDp: Float = 12f,      // 4f..40f
  val barOpacity: Float = 0.90f,           // 0.55f..1.00f
  val barBlurRadiusDp: Float = 18f,        // 0f..32f
  val animationSpeedScale: Float = 1.0f,   // 0.5f..2.0f
) {
  fun iconShape(): Shape = RoundedCornerShape(iconCornerRadiusDp.coerceIn(0f, 24f).dp)

  fun barOuterShape(): Shape = RoundedCornerShape(barCornerRadiusDp.coerceIn(0f, 36f).dp)

  fun barInnerShape(): Shape =
    RoundedCornerShape((barCornerRadiusDp - 4f).coerceIn(0f, 36f).dp)

  fun cardOuterShape(): Shape = RoundedCornerShape(cardCornerRadiusDp.coerceIn(8f, 28f).dp)

  fun cardInnerShape(): Shape =
    RoundedCornerShape((cardCornerRadiusDp - 8f).coerceIn(2f, 24f).dp)
}

val LocalDesignConfig = compositionLocalOf { DesignUiConfig() }
val LocalAnimationSpeedScale = compositionLocalOf { 1.0f }

/**
 * Mathematical 8dp Baseline Grid & Fallback Radii Tokens.
 */
object BrowserRadii {
  val FloatingOuter: Dp = 28.dp
  val FloatingInner: Dp = 24.dp
  val CardOuter: Dp = 20.dp
  val CardInner: Dp = 12.dp
  val DialogSheet: Dp = 28.dp

  val FloatingOuterShape: Shape = RoundedCornerShape(FloatingOuter)
  val FloatingInnerShape: Shape = RoundedCornerShape(FloatingInner)
  val CardOuterShape: Shape = RoundedCornerShape(CardOuter)
  val CardInnerShape: Shape = RoundedCornerShape(CardInner)
  val DialogSheetShape: Shape = RoundedCornerShape(DialogSheet)
}

object BrowserSpacing {
  val Grid1: Dp = 8.dp
  val Grid1_5: Dp = 12.dp
  val Grid2: Dp = 16.dp
  val Grid3: Dp = 24.dp
  val MinTouch: Dp = 48.dp
}

/**
 * Centralized Material 3 Expressive Spring Physics tokens.
 * Zero tween(), keyframes(), or snap() allowed anywhere in the browser.
 */
object ExpressiveSprings {
  const val BASE_SPATIAL_DAMPING: Float = 0.76f
  const val BASE_SPATIAL_STIFFNESS: Float = 260f
  const val BASE_MICRO_DAMPING: Float = 0.62f
  const val BASE_MICRO_STIFFNESS: Float = 420f

  val Spatial: SpringSpec<Float> =
    spring(dampingRatio = BASE_SPATIAL_DAMPING, stiffness = BASE_SPATIAL_STIFFNESS)
  val Micro: SpringSpec<Float> =
    spring(dampingRatio = BASE_MICRO_DAMPING, stiffness = BASE_MICRO_STIFFNESS)

  fun <T> spatial(speedScale: Float = 1.0f): SpringSpec<T> {
    val s = speedScale.coerceIn(0.5f, 2.0f)
    return spring(dampingRatio = BASE_SPATIAL_DAMPING, stiffness = BASE_SPATIAL_STIFFNESS * (s * s))
  }

  fun <T> micro(speedScale: Float = 1.0f): SpringSpec<T> {
    val s = speedScale.coerceIn(0.5f, 2.0f)
    return spring(dampingRatio = BASE_MICRO_DAMPING, stiffness = BASE_MICRO_STIFFNESS * (s * s))
  }

  @Composable
  fun <T> currentSpatial(): SpringSpec<T> = spatial(LocalAnimationSpeedScale.current)

  @Composable
  fun <T> currentMicro(): SpringSpec<T> = micro(LocalAnimationSpeedScale.current)
}

/**
 * Material 3 Expressive Spring Physics Specs routed through ExpressiveSprings.
 */
object BrowserMotion {
  fun <T> expressiveSpring(speedScale: Float = 1.0f): SpringSpec<T> =
    ExpressiveSprings.spatial(speedScale)

  fun <T> morphingPillSpring(speedScale: Float = 1.0f): SpringSpec<T> =
    ExpressiveSprings.micro(speedScale)

  fun <T> tabDrawerSpring(speedScale: Float = 1.0f): SpringSpec<T> =
    ExpressiveSprings.spatial(speedScale)

  fun <T> snappySpring(speedScale: Float = 1.0f): SpringSpec<T> =
    ExpressiveSprings.micro(speedScale)
}

fun computeDiffusedAmbientElevationDp(blurRadiusDp: Float, isFocused: Boolean): Float {
  val clampedBlur = blurRadiusDp.coerceIn(0f, 32f)
  return (28f + (clampedBlur / 32f) * 10f + if (isFocused) 6f else 0f)
}

fun computeOutwardHaloBlurDp(blurRadiusDp: Float, isFocused: Boolean): Float {
  val clampedBlur = blurRadiusDp.coerceIn(0f, 32f)
  return ((clampedBlur * 0.72f) + if (isFocused) 6f else 4f).coerceIn(4f, 28f)
}

/**
 * Borderless Frosted Glass & Multi-Layer Diffused Shadow surface for floating browser pills.
 * Strictly renders zero hard BorderStroke, casts wide >=28.dp multi-layer ambient/spot shadows,
 * feathered drawBehind contour bleed, and an outward unbounded frosted blur halo over the web page.
 */
@Composable
fun FrostedFloatingSurface(
  shape: Shape,
  opacity: Float,
  blurRadiusDp: Float,
  modifier: Modifier = Modifier,
  isFocused: Boolean = false,
  tintColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  accentTint: Color = MaterialTheme.colorScheme.primaryContainer,
  content: @Composable BoxScope.() -> Unit,
) {
  val clampedBlur = blurRadiusDp.coerceIn(0f, 32f)
  val resolvedAlpha =
    if (isFocused) (opacity + 0.08f).coerceIn(0.84f, 0.99f)
    else opacity.coerceIn(0.55f, 1.0f)
  val outerAmbientElevation = computeDiffusedAmbientElevationDp(clampedBlur, isFocused).dp
  val primaryElevation = (22f + (clampedBlur / 32f) * 6f + if (isFocused) 4f else 0f).dp
  val haloBlurDp = computeOutwardHaloBlurDp(clampedBlur, isFocused)

  val topColor =
    if (isFocused) {
      tintColor.copy(alpha = resolvedAlpha)
    } else {
      Color(
        red = (tintColor.red * 0.90f + accentTint.red * 0.10f).coerceIn(0f, 1f),
        green = (tintColor.green * 0.90f + accentTint.green * 0.10f).coerceIn(0f, 1f),
        blue = (tintColor.blue * 0.90f + accentTint.blue * 0.10f).coerceIn(0f, 1f),
        alpha = resolvedAlpha,
      )
    }
  val midColor =
    tintColor.copy(alpha = (resolvedAlpha * 0.96f).coerceIn(0.52f, 1.0f))
  val bottomColor =
    tintColor.copy(alpha = (resolvedAlpha * 0.92f).coerceIn(0.50f, 1.0f))

  val haloTint =
    Color(
      red = (tintColor.red * 0.85f + accentTint.red * 0.15f).coerceIn(0f, 1f),
      green = (tintColor.green * 0.85f + accentTint.green * 0.15f).coerceIn(0f, 1f),
      blue = (tintColor.blue * 0.85f + accentTint.blue * 0.15f).coerceIn(0f, 1f),
      alpha = (resolvedAlpha * 0.36f).coerceIn(0.14f, 0.42f),
    )

  val contourPath = remember { Path() }

  Box(
    modifier =
      modifier
        .shadow(
          elevation = outerAmbientElevation,
          shape = shape,
          clip = false,
          ambientColor = Color.Black.copy(alpha = 0.22f),
          spotColor = Color.Transparent,
        )
        .shadow(
          elevation = primaryElevation,
          shape = shape,
          clip = false,
          ambientColor = Color.Black.copy(alpha = 0.20f),
          spotColor = Color.Black.copy(alpha = 0.30f),
        )
        .drawBehind {
          if (size.width > 2f && size.height > 2f) {
            contourPath.reset()
            contourPath.addOutline(shape.createOutline(size, layoutDirection, this))
            // Outward multi-band diffused shadow bleed radiating past the pill contour into the web page
            val shadowBands =
              listOf(
                32.dp.toPx() to 0.025f,
                22.dp.toPx() to 0.055f,
                14.dp.toPx() to 0.095f,
                7.dp.toPx() to 0.145f,
              )
            shadowBands.forEach { (strokePx, alphaVal) ->
              drawPath(
                path = contourPath,
                color = Color.Black.copy(alpha = alphaVal),
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
            }
            // Soft atmospheric surface tint feather bridging bar contour -> webpage
            drawPath(
              path = contourPath,
              color = haloTint.copy(alpha = haloTint.alpha * 0.55f),
              style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
          }
        },
  ) {
    // Outward unbounded frosted blur halo layer (unclipped so blur radiates seamlessly over webpage)
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && haloBlurDp > 0.5f) {
              Modifier.blur(haloBlurDp.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            } else {
              Modifier
            }
          )
          .drawBehind {
            if (size.width > 2f && size.height > 2f) {
              contourPath.reset()
              contourPath.addOutline(shape.createOutline(size, layoutDirection, this))
              drawPath(
                path = contourPath,
                color = haloTint,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
              drawPath(
                path = contourPath,
                color = Color.Black.copy(alpha = if (isFocused) 0.16f else 0.12f),
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
            }
          }
    )

    // Clipped inner frosted glass pill surface (zero BorderStroke, sharp inner content above)
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .clip(shape)
          .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && clampedBlur > 0.5f) {
              Modifier.blur(clampedBlur.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
            } else {
              Modifier
            }
          )
          .background(brush = Brush.verticalGradient(listOf(topColor, midColor, bottomColor)))
    )
    content()
  }
}

@Composable
fun rememberPressScale(
  interactionSource: MutableInteractionSource,
  pressedScale: Float = 0.92f,
): State<Float> {
  val isPressed by interactionSource.collectIsPressedAsState()
  val speedScale = LocalAnimationSpeedScale.current
  return animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = ExpressiveSprings.micro(speedScale),
    label = "SpringPressScale",
  )
}

/**
 * GPU-accelerated tactile icon button meeting the 48dp minimum touch target standard.
 */
@Composable
fun SpringIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  pressedScale: Float = 0.92f,
  shape: Shape = LocalDesignConfig.current.iconShape(),
  content: @Composable BoxScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scaleState = rememberPressScale(interactionSource, pressedScale)

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          val s = scaleState.value
          scaleX = s
          scaleY = s
        }
        .clip(shape)
        .clickable(
          interactionSource = interactionSource,
          indication = androidx.compose.material3.ripple(bounded = true),
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
    content = content,
  )
}

/**
 * GPU-accelerated pressable Surface for setting tiles, Speed Dial items, and tab cards.
 */
@Composable
fun SpringPressSurface(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  selected: Boolean = false,
  shape: Shape = LocalDesignConfig.current.cardInnerShape(),
  selectedColor: Color = MaterialTheme.colorScheme.primaryContainer,
  unselectedColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  selectedBorderColor: Color = MaterialTheme.colorScheme.primary,
  unselectedBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
  tonalElevation: Dp = 0.dp,
  pressedScale: Float = 0.92f,
  content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scaleState = rememberPressScale(interactionSource, pressedScale)

  val speedScale = LocalAnimationSpeedScale.current
  val containerColor by
    animateColorAsState(
      targetValue = if (selected) selectedColor else unselectedColor,
      animationSpec = BrowserMotion.expressiveSpring(speedScale),
      label = "SpringSurfaceColor",
    )
  val borderColor by
    animateColorAsState(
      targetValue = if (selected) selectedBorderColor else unselectedBorderColor,
      animationSpec = BrowserMotion.expressiveSpring(speedScale),
      label = "SpringBorderColor",
    )

  Surface(
    shape = shape,
    color = containerColor,
    tonalElevation = tonalElevation,
    border = BorderStroke(width = if (selected) 1.5.dp else 1.dp, color = borderColor),
    modifier =
      modifier
        .graphicsLayer {
          val s = scaleState.value
          scaleX = s
          scaleY = s
        }
        .clip(shape)
        .clickable(
          interactionSource = interactionSource,
          indication = androidx.compose.material3.ripple(bounded = true),
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
        ),
    content = content,
  )
}
