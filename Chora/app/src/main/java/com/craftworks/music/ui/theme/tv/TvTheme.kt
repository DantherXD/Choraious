package com.craftworks.music.ui.theme.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * The TV theme (spec §4).
 *
 * Provides the token locals new TV code reads — [LocalTvColors], [LocalTvTypography],
 * [LocalTvShapes], [LocalTvMotion], [LocalGlassTier], [LocalReduceMotion] — and, for the
 * screens that still use `androidx.tv.material3` components, a matching tv-material3
 * `MaterialTheme`. That last part is the only reason a `tv.MaterialTheme` is present:
 * without it the old TV screens fall back to the library's purple defaults. It is replaced,
 * not wrapped, by the new tokens as P1/P2 rewrite the screens.
 *
 * Dark-only: no light scheme, no dynamic color (spec §4.2).
 */
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    val colors = remember { TvColors() }
    val typeScale = remember { defaultTvTypeScale() }
    val shapes = remember { TvShapes() }
    val motion = remember { TvMotion() }
    val reduceMotion = rememberReduceMotion()
    val tier = remember { currentGlassTier() }

    val materialColorScheme = remember(colors) {
        androidx.tv.material3.darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.bgCanvas,
            onBackground = colors.labelPrimary,
            surface = colors.bgCanvasTop,
            onSurface = colors.labelPrimary,
            error = colors.destructive,
            onError = colors.onAccent,
        )
    }

    CompositionLocalProvider(
        LocalTvColors provides colors,
        LocalTvTypography provides typeScale,
        LocalTvShapes provides shapes,
        LocalTvMotion provides motion,
        LocalGlassTier provides tier,
        LocalReduceMotion provides reduceMotion,
    ) {
        androidx.tv.material3.MaterialTheme(colorScheme = materialColorScheme) {
            content()
        }
    }
}
