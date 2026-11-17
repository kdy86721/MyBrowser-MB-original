package com.mybrowser.appui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode(val id: String, val label: String) {
  SYSTEM("SYSTEM", "시스템"),
  LIGHT("LIGHT", "라이트"),
  DARK("DARK", "다크"),
  AMOLED_DARK("AMOLED_DARK", "순수 블랙");

  companion object {
    fun fromId(raw: String): ThemeMode = entries.firstOrNull { it.id == raw } ?: SYSTEM
  }
}

enum class AccentColorPreset(val id: String, val label: String, val swatch: Color) {
  DYNAMIC("DYNAMIC", "다이나믹", CobaltSignal),
  EMERALD("EMERALD", "에메랄드", EmeraldSignal),
  COBALT("COBALT", "코발트", CobaltSignal),
  AMBER("AMBER", "앰버", AmberSignal),
  MONOCHROME("MONOCHROME", "모노크롬", Color(0xFF71717A));

  companion object {
    fun fromId(raw: String): AccentColorPreset =
      entries.firstOrNull { it.id == raw } ?: DYNAMIC
  }
}

private val MyBrowserShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

private fun presetDarkScheme(preset: AccentColorPreset): ColorScheme =
  when (preset) {
    AccentColorPreset.EMERALD ->
      darkColorScheme(
        primary = EmeraldSignal,
        onPrimary = Color(0xFF052017),
        primaryContainer = EmeraldContainer,
        onPrimaryContainer = Color(0xFFA7F3D0),
        secondary = TealShield,
        onSecondary = ObsidianBg,
        secondaryContainer = TealContainer,
        onSecondaryContainer = TealShield,
        tertiary = AmberNotice,
        background = ObsidianBg,
        onBackground = InkPrimary,
        surface = SlateSurface,
        onSurface = InkPrimary,
        surfaceVariant = SlateSurfaceElevated,
        onSurfaceVariant = InkSecondary,
        outline = SlateBorder,
        error = RoseAlert,
      )
    AccentColorPreset.AMBER ->
      darkColorScheme(
        primary = AmberSignal,
        onPrimary = Color(0xFF281600),
        primaryContainer = AmberContainer,
        onPrimaryContainer = Color(0xFFFDE68A),
        secondary = TealShield,
        onSecondary = ObsidianBg,
        secondaryContainer = TealContainer,
        onSecondaryContainer = TealShield,
        tertiary = CobaltSignal,
        background = ObsidianBg,
        onBackground = InkPrimary,
        surface = SlateSurface,
        onSurface = InkPrimary,
        surfaceVariant = SlateSurfaceElevated,
        onSurfaceVariant = InkSecondary,
        outline = SlateBorder,
        error = RoseAlert,
      )
    AccentColorPreset.MONOCHROME ->
      darkColorScheme(
        primary = MonoDarkAccent,
        onPrimary = Color(0xFF09090B),
        primaryContainer = MonoDarkContainer,
        onPrimaryContainer = InkPrimary,
        secondary = Color(0xFFA1A1AA),
        onSecondary = Color(0xFF09090B),
        secondaryContainer = Color(0xFF27272A),
        onSecondaryContainer = Color(0xFFE4E4E7),
        tertiary = Color(0xFFD4D4D8),
        background = Color(0xFF101012),
        onBackground = InkPrimary,
        surface = Color(0xFF18181B),
        onSurface = InkPrimary,
        surfaceVariant = Color(0xFF222226),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF3F3F46),
        error = RoseAlert,
      )
    AccentColorPreset.DYNAMIC,
    AccentColorPreset.COBALT ->
      darkColorScheme(
        primary = CobaltSignal,
        onPrimary = InkPrimary,
        primaryContainer = CobaltContainer,
        onPrimaryContainer = InkPrimary,
        secondary = TealShield,
        onSecondary = ObsidianBg,
        secondaryContainer = TealContainer,
        onSecondaryContainer = TealShield,
        tertiary = AmberNotice,
        background = ObsidianBg,
        onBackground = InkPrimary,
        surface = SlateSurface,
        onSurface = InkPrimary,
        surfaceVariant = SlateSurfaceElevated,
        onSurfaceVariant = InkSecondary,
        outline = SlateBorder,
        error = RoseAlert,
      )
  }

private fun presetLightScheme(preset: AccentColorPreset): ColorScheme =
  when (preset) {
    AccentColorPreset.EMERALD ->
      lightColorScheme(
        primary = EmeraldDeep,
        onPrimary = PaperSurface,
        primaryContainer = EmeraldLightContainer,
        onPrimaryContainer = InkDarkPrimary,
        secondary = TealDeep,
        onSecondary = PaperSurface,
        secondaryContainer = TealLightContainer,
        onSecondaryContainer = TealDeep,
        tertiary = AmberNotice,
        background = AlabasterBg,
        onBackground = InkDarkPrimary,
        surface = PaperSurface,
        onSurface = InkDarkPrimary,
        surfaceVariant = PaperElevated,
        onSurfaceVariant = InkDarkSecondary,
        outline = PaperBorder,
        error = RoseAlert,
      )
    AccentColorPreset.AMBER ->
      lightColorScheme(
        primary = AmberDeep,
        onPrimary = PaperSurface,
        primaryContainer = AmberLightContainer,
        onPrimaryContainer = InkDarkPrimary,
        secondary = TealDeep,
        onSecondary = PaperSurface,
        secondaryContainer = TealLightContainer,
        onSecondaryContainer = TealDeep,
        tertiary = CobaltDeep,
        background = AlabasterBg,
        onBackground = InkDarkPrimary,
        surface = PaperSurface,
        onSurface = InkDarkPrimary,
        surfaceVariant = PaperElevated,
        onSurfaceVariant = InkDarkSecondary,
        outline = PaperBorder,
        error = RoseAlert,
      )
    AccentColorPreset.MONOCHROME ->
      lightColorScheme(
        primary = MonoLightAccent,
        onPrimary = PaperSurface,
        primaryContainer = MonoLightContainer,
        onPrimaryContainer = InkDarkPrimary,
        secondary = Color(0xFF52525B),
        onSecondary = PaperSurface,
        secondaryContainer = Color(0xFFF4F4F5),
        onSecondaryContainer = Color(0xFF18181B),
        tertiary = Color(0xFF3F3F46),
        background = Color(0xFFFAFAFA),
        onBackground = InkDarkPrimary,
        surface = PaperSurface,
        onSurface = InkDarkPrimary,
        surfaceVariant = Color(0xFFF4F4F5),
        onSurfaceVariant = Color(0xFF52525B),
        outline = Color(0xFFD4D4D8),
        error = RoseAlert,
      )
    AccentColorPreset.DYNAMIC,
    AccentColorPreset.COBALT ->
      lightColorScheme(
        primary = CobaltDeep,
        onPrimary = PaperSurface,
        primaryContainer = CobaltLightContainer,
        onPrimaryContainer = InkDarkPrimary,
        secondary = TealDeep,
        onSecondary = PaperSurface,
        secondaryContainer = TealLightContainer,
        onSecondaryContainer = TealDeep,
        tertiary = AmberNotice,
        background = AlabasterBg,
        onBackground = InkDarkPrimary,
        surface = PaperSurface,
        onSurface = InkDarkPrimary,
        surfaceVariant = PaperElevated,
        onSurfaceVariant = InkDarkSecondary,
        outline = PaperBorder,
        error = RoseAlert,
      )
  }

@Composable
fun MyBrowserTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  accentPreset: AccentColorPreset = AccentColorPreset.DYNAMIC,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val useDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> systemDark
      ThemeMode.LIGHT -> false
      ThemeMode.DARK,
      ThemeMode.AMOLED_DARK -> true
    }

  val useDynamic = (accentPreset == AccentColorPreset.DYNAMIC || dynamicColor) &&
    accentPreset == AccentColorPreset.DYNAMIC &&
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  val baseScheme =
    if (useDynamic) {
      val context = LocalContext.current
      if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
      if (useDark) presetDarkScheme(accentPreset) else presetLightScheme(accentPreset)
    }

  val colorScheme =
    if (themeMode == ThemeMode.AMOLED_DARK) {
      baseScheme.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF070709),
        surfaceVariant = Color(0xFF121318),
      )
    } else {
      baseScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    shapes = MyBrowserShapes,
    typography = MyBrowserTypography,
    content = content,
  )
}
