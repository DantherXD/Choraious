package com.craftworks.music.ui.theme.tv

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * TV color tokens (spec §4.2).
 *
 * Dark-only by decision: tvOS Music is dark, and light mode would need surface sampling
 * for the glass layers. No Material You, no `dynamicDarkColorScheme` on the TV path —
 * those would hand us a purple palette on a black canvas.
 */
@Immutable
data class TvColors(
    val bgBase: Color = Color(0xFF000000),
    val bgCanvas: Color = Color(0xFF0B0B0F),
    val bgCanvasTop: Color = Color(0xFF131318),

    val surface1: Color = Color.White.copy(alpha = 0.06f),
    val surface2: Color = Color.White.copy(alpha = 0.10f),
    val surface3: Color = Color.White.copy(alpha = 0.14f),
    val surfacePressed: Color = Color.White.copy(alpha = 0.20f),

    val labelPrimary: Color = Color.White,
    val labelSecondary: Color = Color.White.copy(alpha = 0.70f),
    val labelTertiary: Color = Color.White.copy(alpha = 0.45f),
    val labelDisabled: Color = Color.White.copy(alpha = 0.30f),

    val separator: Color = Color.White.copy(alpha = 0.12f),
    val hairline: Color = Color.White.copy(alpha = 0.10f),

    val accent: Color = Color(0xFFFA2D48),
    val accentGradientStart: Color = Color(0xFFFA233B),
    val accentGradientEnd: Color = Color(0xFFFB5C74),
    val accentPressed: Color = Color(0xFFD81F38),
    val onAccent: Color = Color.White,
    val selectionTint: Color = Color(0xFFFA2D48).copy(alpha = 0.18f),
    val destructive: Color = Color(0xFFFF453A),

    val focusRing: Color = Color.White.copy(alpha = 0.90f),
    val focusGlow: Color = Color.White.copy(alpha = 0.12f),

    val scrim: Color = Color.Black.copy(alpha = 0.45f),
) {
    /** Primary action gradient; solid [accent] stays the default for flat fills. */
    val accentGradient: Brush
        get() = Brush.linearGradient(listOf(accentGradientStart, accentGradientEnd))
}

val LocalTvColors = staticCompositionLocalOf { TvColors() }
