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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * Material 3 Expressive Spring Physics Specs.
 */
object BrowserMotion {
  fun <T> expressiveSpring(): SpringSpec<T> =
    spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow,
    )

  fun <T> morphingPillSpring(): SpringSpec<T> =
    spring(
      dampingRatio = 0.74f,
      stiffness = 380f,
    )

  fun <T> tabDrawerSpring(): SpringSpec<T> =
    spring(
      dampingRatio = 0.80f,
      stiffness = 340f,
    )

  fun <T> snappySpring(): SpringSpec<T> =
    spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMedium,
    )
}

/**
 * Borderless Frosted Glass & Multi-Layer Diffused Shadow surface for floating browser pills.
 * Strictly renders zero hard BorderStroke and keeps foreground icons/text sharp by
 * blurring only an underlying backdrop layer on Android 12+ (API 31+, RenderEffect-backed).
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
  val primaryElevation = (12f + (clampedBlur / 32f) * 8f + if (isFocused) 4f else 0f).coerceAtLeast(16f).dp
  val outerAmbientElevation = (primaryElevation.value + 8f).dp
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

  Box(
    modifier =
      modifier
        .shadow(
          elevation = outerAmbientElevation,
          shape = shape,
          clip = false,
          ambientColor = Color.Black.copy(alpha = 0.12f),
          spotColor = Color.Transparent,
        )
        .shadow(
          elevation = primaryElevation,
          shape = shape,
          clip = false,
          ambientColor = Color.Black.copy(alpha = 0.16f),
          spotColor = Color.Black.copy(alpha = 0.24f),
        )
        .clip(shape),
  ) {
    Box(
      modifier =
        Modifier
          .matchParentSize()
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
  return animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = BrowserMotion.expressiveSpring(),
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
  pressedScale: Float = 0.88f,
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
  pressedScale: Float = 0.95f,
  content: @Composable () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scaleState = rememberPressScale(interactionSource, pressedScale)

  val containerColor by
    animateColorAsState(
      targetValue = if (selected) selectedColor else unselectedColor,
      animationSpec = BrowserMotion.expressiveSpring(),
      label = "SpringSurfaceColor",
    )
  val borderColor by
    animateColorAsState(
      targetValue = if (selected) selectedBorderColor else unselectedBorderColor,
      animationSpec = BrowserMotion.expressiveSpring(),
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
