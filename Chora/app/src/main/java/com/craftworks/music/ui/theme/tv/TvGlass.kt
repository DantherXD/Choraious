package com.craftworks.music.ui.theme.tv

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

/**
 * Glass capability tier (spec §9).
 *
 * Blur needs `RenderEffect` (API 31) and the lens effect needs a runtime shader (API 33).
 * This file is the **only** place in the TV code that may look at `Build.VERSION`;
 * screens and components ask [LocalGlassTier] instead.
 */
enum class GlassTier(val supportsBlur: Boolean, val supportsLens: Boolean) {
    /** API 33+: blur + lens. */
    Full(supportsBlur = true, supportsLens = true),

    /** API 31–32: blur, no lens. */
    BlurOnly(supportsBlur = true, supportsLens = false),

    /** API < 31: translucent fill + hairline only. */
    Solid(supportsBlur = false, supportsLens = false),
}

internal fun resolveGlassTier(sdkInt: Int): GlassTier = when {
    sdkInt >= Build.VERSION_CODES.TIRAMISU -> GlassTier.Full
    sdkInt >= Build.VERSION_CODES.S -> GlassTier.BlurOnly
    else -> GlassTier.Solid
}

internal fun currentGlassTier(): GlassTier = resolveGlassTier(Build.VERSION.SDK_INT)

val LocalGlassTier = staticCompositionLocalOf { GlassTier.Solid }

/**
 * Freeze backdrop sampling while a focus animation runs (spec §4.5/§10): the sampled
 * layer stops being re-read and the surface falls back to its tint for the duration.
 * A frozen surface must never be frozen for longer than the focus animation.
 */
val LocalGlassFrozen = staticCompositionLocalOf { false }

/** Simultaneous-surface budget (spec §4.5): at most two large surfaces plus three pills. */
object GlassSurfaceBudget {
    const val MaxLargeSurfaces = 2
    const val MaxPills = 3
}

/**
 * One documented glass material (spec §4.5 table). Effects are resolved by
 * [Modifier.tvGlass] according to the current [GlassTier]; the preset only describes intent.
 */
@Immutable
data class TvGlassPreset(
    val blurRadius: Dp,
    val tint: Color,
    val refractionHeight: Dp,
    val lensAmount: Float,
    val surfaceShape: Shape,
)

object TvGlassPresets {
    /** Top tab bar, floating nav. */
    val chrome = TvGlassPreset(
        blurRadius = 24.dp,
        tint = Color.Black.copy(alpha = 0.25f),
        refractionHeight = 8.dp,
        lensAmount = 8f,
        surfaceShape = RoundedCornerShape(28.dp),
    )

    /** Lyrics panel, queue sheet. */
    val panel = TvGlassPreset(
        blurRadius = 30.dp,
        tint = Color.Black.copy(alpha = 0.40f),
        refractionHeight = 12.dp,
        lensAmount = 10f,
        surfaceShape = RoundedCornerShape(28.dp),
    )

    /** Transport controls, buttons. */
    val pill = TvGlassPreset(
        blurRadius = 20.dp,
        tint = Color.White.copy(alpha = 0.06f),
        refractionHeight = 12.dp,
        lensAmount = 8f,
        surfaceShape = RoundedCornerShape(percent = 50),
    )

    /** Dialogs, menus — no lens (spec §4.5). */
    val overlay = TvGlassPreset(
        blurRadius = 40.dp,
        tint = Color.Black.copy(alpha = 0.45f),
        refractionHeight = 0.dp,
        lensAmount = 0f,
        surfaceShape = RoundedCornerShape(24.dp),
    )

    /** Genre chips, tags. */
    val chip = TvGlassPreset(
        blurRadius = 12.dp,
        tint = Color.White.copy(alpha = 0.08f),
        refractionHeight = 4.dp,
        lensAmount = 4f,
        surfaceShape = RoundedCornerShape(12.dp),
    )
}

/**
 * Draws [preset] glass over whatever the [backdrop] sampled.
 *
 * The caller is responsible for registering the sampled content with
 * `Modifier.layerBackdrop(backdrop)`; this modifier only draws the foreground surface.
 * Blur radius is never animated (spec §4.5) — animate tint alpha, translation or scale.
 */
@Composable
fun Modifier.tvGlass(
    backdrop: Backdrop,
    preset: TvGlassPreset,
    tier: GlassTier = LocalGlassTier.current,
    shape: Shape = preset.surfaceShape,
): Modifier {
    if (LocalGlassFrozen.current || tier == GlassTier.Solid) {
        // No sampling: translucent fill only, so API < 31 and focus animations stay cheap.
        return background(preset.tint, shape)
    }

    val density = LocalDensity.current
    val blurRadiusPx = with(density) { preset.blurRadius.toPx() }
    val refractionPx = with(density) { preset.refractionHeight.toPx() }
    val drawLens = tier.supportsLens && preset.lensAmount > 0f && preset.refractionHeight > 0.dp

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            blur(blurRadiusPx)
            if (drawLens) lens(refractionPx, preset.lensAmount)
        },
        onDrawSurface = { drawRect(preset.tint) },
    )
}
