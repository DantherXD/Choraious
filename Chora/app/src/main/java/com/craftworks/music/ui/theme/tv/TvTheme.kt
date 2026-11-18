package com.craftworks.music.ui.theme.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.managers.settings.AppearanceSettingsManager

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
    val shapes = remember { TvShapes() }

    // Large Text (spec §6.8): +15%, capped so the *total* scale (system font scale × in-app
    // factor) never exceeds 1.3. `sp` already applies the system font scale, so the factor has to
    // come down as that scale rises.
    val context = LocalContext.current
    val systemFontScale = LocalDensity.current.fontScale
    val largeTextEnabled by remember(context) {
        AppearanceSettingsManager(context).largeTextFlow
    }.collectAsStateWithLifecycle(initialValue = false)
    val typeFactor = if (!largeTextEnabled) {
        1f
    } else {
        (1.15f * (1.3f / systemFontScale).coerceAtMost(1f)).coerceAtLeast(1f)
    }
    val typeScale = remember(typeFactor) { defaultTvTypeScale().scaled(typeFactor) }
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
        LocalTvLargeText provides (typeFactor > 1.001f),
    ) {
        androidx.tv.material3.MaterialTheme(colorScheme = materialColorScheme) {
            content()
        }
    }
}
