package com.craftworks.music.ui.theme.tv

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The 10-foot type scale (spec §4.3).
 *
 * Sizes are the spec's **halved** values. The spec is a 1920×1080-point tvOS canvas; Android TV on
 * a 1080p/320dpi panel is a 960×540dp canvas, so one spec point is two dp here. Passing the spec's
 * numbers through unscaled made every label twice its intended size (Gate 1: "nav bar as if we took
 * a phone nav bar and increased its size to the fullest of the screen"). Halving the point values
 * gives the intended physical size; tracking is halved with them so the optical density holds.
 *
 * `tab` is the one deliberate exception: 13sp instead of the arithmetic 11sp, because 11sp labels
 * under an icon are too small to read from a sofa.
 *
 * Every style uses [TvFonts.Display]; the optical-size split was dropped in favour of negative
 * tracking at large sizes and max-lines/ellipsis in layout. Colors are left unspecified on purpose
 * so a style can be reused under any label token (`LocalTvColors.labelSecondary` and friends).
 */
@Immutable
data class TvTypeScale(
    val displayHero: TextStyle,
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val cardSubtitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
    val tab: TextStyle,
    val npTitle: TextStyle,
    val npArtist: TextStyle,
    val lyricsActive: TextStyle,
    val lyricsInactive: TextStyle,
)

/** The bundled TV typeface; the `fonts` flavor decides which files answer these names. */
val TvFontFamily: FontFamily
    get() = TvFonts.Display

private fun tvStyle(
    size: Float,
    lineHeight: Float,
    tracking: Float,
    weight: FontWeight,
): TextStyle = TextStyle(
    fontFamily = TvFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

internal fun defaultTvTypeScale(): TvTypeScale = TvTypeScale(
    displayHero = tvStyle(38f, 40f, -0.8f, FontWeight.Bold),
    screenTitle = tvStyle(28f, 30f, -0.5f, FontWeight.Bold),
    sectionTitle = tvStyle(17f, 20f, -0.25f, FontWeight.SemiBold),
    cardTitle = tvStyle(14f, 16f, -0.15f, FontWeight.SemiBold),
    cardSubtitle = tvStyle(11f, 14f, 0f, FontWeight.Normal),
    body = tvStyle(12f, 16f, 0f, FontWeight.Normal),
    caption = tvStyle(10f, 13f, 0f, FontWeight.Normal),
    button = tvStyle(12f, 13f, 0f, FontWeight.SemiBold),
    tab = tvStyle(13f, 16f, -0.1f, FontWeight.SemiBold),
    npTitle = tvStyle(24f, 26f, -0.4f, FontWeight.Bold),
    npArtist = tvStyle(15f, 18f, 0f, FontWeight.Normal),
    lyricsActive = tvStyle(22f, 26f, -0.25f, FontWeight.Bold),
    lyricsInactive = tvStyle(18f, 22f, -0.15f, FontWeight.SemiBold),
)

val LocalTvTypography = staticCompositionLocalOf { defaultTvTypeScale() }
