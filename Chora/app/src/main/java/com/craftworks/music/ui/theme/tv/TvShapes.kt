package com.craftworks.music.ui.theme.tv

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Radius tokens (spec §4.4). */
@Immutable
data class TvShapes(
    val chip: Shape = RoundedCornerShape(8.dp),
    val row: Shape = RoundedCornerShape(12.dp),
    val tile: Shape = RoundedCornerShape(16.dp),
    val panel: Shape = RoundedCornerShape(24.dp),
    val sheet: Shape = RoundedCornerShape(28.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),
)

/**
 * Spacing / size tokens (spec §4.4). Dp constants only — no colors or effects here.
 *
 * Sizes are tuned for the **960×540dp** Android TV canvas, not the spec's 1920-point tvOS one:
 * spec point values are halved (they are 2dp each here), then reviewed by hand. The spacing ramp
 * (4/8/12/16/24/32/48/64) is left alone — it is rhythm, not scale — while the sizes that exist to
 * frame text (margins, rail gaps, row heights, button height) come down with the type.
 */
object TvDimens {
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp
    val space64 = 64.dp

    /** Overscan safe area: content stays inside 5%. */
    val safeAreaPercent = 0.05f

    val screenMarginHorizontal = 48.dp
    val screenMarginTop = 32.dp
    val screenMarginBottom = 32.dp

    val railGap = 24.dp
    val railTitleGap = 16.dp

    /**
     * Tab bar. 64dp (not the spec's 96dp) with 20dp item gaps keeps the bar a third of the canvas
     * width instead of ninety percent. 64 rather than 48: 48 is what the halved spec asks for but
     * leaves no room for an icon, a label and the active dot without touching the edges.
     */
    val tabBarHeight = 64.dp
    val tabBarTopInset = 16.dp
    val tabBarItemGap = 20.dp

    /** The bar never comes closer than this to either screen edge, however wide the labels get. */
    val tabBarSidePadding = 120.dp
    val tabIconSize = 20.dp
    val tabDotSize = 3.dp

    val tileSize = 160.dp
    val artistTileSize = 150.dp

    /**
     * Controls and glyphs. The spec's 28/32/40dp inline/control/transport glyphs would halve to
     * 14/16/20dp, which is too fine for a D-pad focus target seen from three metres; these are
     * reviewed values rather than a straight halve.
     */
    val buttonHeight = 44.dp
    val iconInline = 20.dp
    val iconControl = 24.dp
    val iconTransport = 32.dp
    val iconButtonSize = 44.dp

    /** Rows and headers (spec §6/§7). Tall enough for 14sp titles plus row padding. */
    val settingsRowHeight = 56.dp
    val trackRowHeight = 56.dp
    val listRowShape = 12.dp

    /** Detail header (spec §6). */
    val detailArtworkLarge = 260.dp
    val detailArtworkCollapsed = 96.dp
    /** Hero fills the canvas; its text column is capped so 38sp lines stay readable. */
    val heroHeight = 540.dp
    val heroTextWidth = 560.dp
    val minTouchTarget = 48.dp

    /** Panels that float over content. */
    val emptyStateMaxWidth = 520.dp
    val menuWidth = 400.dp
    val menuMaxHeight = 400.dp

    /** Focus language (spec §4.6). The ring stays 3dp: it is a legibility cue, not a scale cue. */
    val focusRingWidth = 3.dp
    val focusGlowWidth = 6.dp
    val focusLift = 6.dp

    /** Artwork drift inside a focused tile, opposite to its position on screen (spec §4.6). */
    val focusParallax = 3.dp
    val focusScaleTile = 1.06f
    val focusScaleTab = 1.04f
    val focusScaleButton = 1.03f
    val pressScale = 0.97f
    val unfocusedTileAlpha = 0.82f
}

val LocalTvShapes = staticCompositionLocalOf { TvShapes() }
