/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import com.metrolist.music.BuildConfig

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    // Nova adopts SoundPod's restrained neutral palette; stable Metrofy keeps its existing theme.
    val soundPodLightScheme = lightColorScheme(
        primary = Color(0xFF111827), onPrimary = Color.White,
        primaryContainer = Color(0xFFE8ECF3), onPrimaryContainer = Color(0xFF182131),
        secondary = Color(0xFF536174), onSecondary = Color.White,
        background = Color(0xFFF6F6F8), onBackground = Color(0xFF101828),
        surface = Color.White, onSurface = Color(0xFF101828),
        surfaceVariant = Color(0xFFE8ECF2), onSurfaceVariant = Color(0xFF626D7E),
        surfaceContainer = Color(0xFFF0F2F6), surfaceContainerLow = Color.White,
    )
    val soundPodDarkScheme = darkColorScheme(
        primary = Color(0xFFF4F6FB), onPrimary = Color(0xFF0D1422),
        primaryContainer = Color(0xFF2A3548), onPrimaryContainer = Color(0xFFE8EDF6),
        secondary = Color(0xFFBAC4D4), onSecondary = Color(0xFF172033),
        background = Color(0xFF090E19), onBackground = Color(0xFFF4F6FB),
        surface = Color(0xFF0D1320), onSurface = Color(0xFFF4F6FB),
        surfaceVariant = Color(0xFF1B2536), onSurfaceVariant = Color(0xFFB6C0D0),
        surfaceContainer = Color(0xFF121B2B), surfaceContainerLow = Color(0xFF151F30),
    )
    val baseColorScheme = when {
        BuildConfig.NOVA_PREVIEW -> if (darkTheme) soundPodDarkScheme else soundPodLightScheme
        useSystemDynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Use the defined AppTypography
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
